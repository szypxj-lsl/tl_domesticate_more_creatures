package com.szypxj.tldomesticatemorecreatures.command.pet;

import com.szypxj.tldomesticatemorecreatures.TlDomesticateMoreCreatures;
import com.mojang.logging.LogUtils;
import com.szypxj.tldomesticatemorecreatures.command.pet.capability.PetCapabilityResolver;
import com.szypxj.tldomesticatemorecreatures.command.pet.executor.PetCommandExecutor;
import com.szypxj.tldomesticatemorecreatures.command.pet.executor.PetCommandExecutors;
import com.szypxj.tldomesticatemorecreatures.config.Config;
import com.szypxj.tldomesticatemorecreatures.domestication.FriendlyFireService;
import com.szypxj.tldomesticatemorecreatures.game.PetOwnershipService;
import com.szypxj.tldomesticatemorecreatures.imprint.ImprintService;
import com.szypxj.tldomesticatemorecreatures.network.NetworkHandler;
import com.szypxj.tldomesticatemorecreatures.riding.RideService;
import com.szypxj.tldomesticatemorecreatures.talent.FuryService;
import com.szypxj.tldomesticatemorecreatures.torpor.TorporData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = TlDomesticateMoreCreatures.MOD_ID)
public final class PetCommandService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<LivingEntity> ACTIVE = Collections.newSetFromMap(new WeakHashMap<>());

    private PetCommandService() {
    }

    public static void issue(ServerPlayer owner, PetCommand command, Vec3 position, UUID targetUuid) {
        if (owner == null || command == null) {
            return;
        }

        UUID effectiveTargetUuid = targetUuid;
        Vec3 effectivePosition = position == null ? owner.position() : position;
        LivingEntity attackTarget = null;
        if (command == PetCommand.ATTACK) {
            Entity targetEntity = targetUuid == null ? null : owner.serverLevel().getEntity(targetUuid);
            if (!(targetEntity instanceof LivingEntity target)
                    || !target.isAlive()
                    || target.isRemoved()
                    || FriendlyFireService.areFriendly(owner, target)) {
                sendSummary(owner);
                return;
            }
            attackTarget = target;
            effectivePosition = target.position();
        } else if (command == PetCommand.FOLLOW || command == PetCommand.RETREAT) {
            effectiveTargetUuid = owner.getUUID();
            effectivePosition = owner.position();
        } else {
            effectiveTargetUuid = null;
        }

        RecipientSet recipientSet = resolveRecipients(owner);
        List<LivingEntity> eligiblePets = recipientSet.recipients().stream()
                .filter(pet -> pet instanceof Mob && !TorporData.of(pet).unconscious() && !FuryService.isBerserk(pet))
                .filter(pet -> !isMovementCommand(command) || !RideService.blocksMovementCommand(pet))
                .filter(pet -> command != PetCommand.LAND || PetCapabilityResolver.canLand(pet))
                .toList();
        if (eligiblePets.isEmpty()) {
            sendSummary(owner);
            return;
        }

        UUID markerUuid = command == PetCommand.ATTACK || command == PetCommand.MOVE
                ? UUID.randomUUID()
                : null;
        Set<UUID> replacedMarkers = new HashSet<>();
        int startedPets = 0;

        for (LivingEntity pet : eligiblePets) {
            if (!(pet instanceof Mob mob)) {
                continue;
            }
            PetCommandData data = PetCommandData.of(mob);
            data.markerUuid().ifPresent(replacedMarkers::add);
            cancelCurrent(mob, data);

            data.set(command, effectivePosition, effectiveTargetUuid, markerUuid);
            PetCommandRuntimeState runtime = PetCommandRuntimeState.of(mob);
            PetCommandExecutor executor = PetCommandExecutors.get(command);
            boolean started = false;
            try {
                started = executor != null && executor.supports(mob) && executor.start(mob, data, runtime);
            } catch (RuntimeException exception) {
                logExecutorFailure("start", mob, command, exception);
            }
            if (!started) {
                if (executor != null) {
                    try {
                        executor.cancel(mob, data, runtime);
                    } catch (RuntimeException exception) {
                        logExecutorFailure("cancel-after-start-failure", mob, command, exception);
                    }
                }
                data.clear();
                PetCommandRuntimeState.remove(mob);
                ACTIVE.remove(mob);
                continue;
            }

            ACTIVE.add(mob);
            startedPets++;
            if (command == PetCommand.FOLLOW) {
                ImprintService.onFollowCommand(owner, mob);
            }
        }

        if (markerUuid != null && startedPets > 0) {
            int targetEntityId = attackTarget == null ? -1 : attackTarget.getId();
            UUID markerTargetUuid = attackTarget == null ? null : attackTarget.getUUID();
            NetworkHandler.sendPetCommandMarker(
                    owner,
                    markerUuid,
                    command,
                    targetEntityId,
                    markerTargetUuid,
                    effectivePosition
            );
        }

        for (UUID replacedMarker : replacedMarkers) {
            if (!replacedMarker.equals(markerUuid)) {
                removeMarkerIfUnused(owner, replacedMarker);
            }
        }
        sendSummary(owner);
    }

    public static boolean hasActiveCommand(LivingEntity entity) {
        return PetCommandData.of(entity).active();
    }

    public static boolean acceptsCombatTarget(LivingEntity pet, LivingEntity target) {
        if (pet == null || target == null) {
            return false;
        }
        if (FuryService.isBerserk(pet)) {
            return true;
        }
        PetCommandData data = PetCommandData.of(pet);
        if (!data.active()) {
            return true;
        }
        if (FriendlyFireService.areFriendly(pet, target)) {
            return false;
        }
        return switch (data.command()) {
            case ATTACK -> data.targetUuid().map(target.getUUID()::equals).orElse(false);
            case DEFEND -> target.distanceToSqr(data.position()) <= 100.0D;
            case MOVE, FOLLOW, RETREAT, LAND -> false;
        };
    }

    public static void registerIfActive(LivingEntity entity) {
        PetCommandData data = PetCommandData.of(entity);
        if (!data.active() || !(entity instanceof Mob mob)) {
            return;
        }
        if (FuryService.isBerserk(entity)) {
            ACTIVE.add(entity);
            restoreMarker(mob, data);
            return;
        }
        if ((data.command() == PetCommand.ATTACK || data.command() == PetCommand.MOVE)
                && data.markerUuid().isEmpty()) {
            data.set(
                    data.command(),
                    data.position(),
                    data.targetUuid().orElse(null),
                    UUID.randomUUID()
            );
        }

        PetCommandExecutor executor = PetCommandExecutors.get(data.command());
        PetCommandRuntimeState runtime = PetCommandRuntimeState.of(mob);
        boolean started = false;
        try {
            started = executor != null && executor.supports(mob) && executor.start(mob, data, runtime);
        } catch (RuntimeException exception) {
            logExecutorFailure("restore-start", mob, data.command(), exception);
        }
        if (!started) {
            UUID markerUuid = data.markerUuid().orElse(null);
            ServerPlayer owner = ownerPlayer(mob);
            if (executor != null) {
                try {
                    executor.cancel(mob, data, runtime);
                } catch (RuntimeException exception) {
                    logExecutorFailure("restore-cancel", mob, data.command(), exception);
                }
            }
            data.clear();
            PetCommandRuntimeState.remove(mob);
            ACTIVE.remove(mob);
            if (owner != null && markerUuid != null) {
                removeMarkerIfUnused(owner, markerUuid);
            }
            return;
        }

        ACTIVE.add(mob);
        restoreMarker(mob, data);
    }

    public static void clear(LivingEntity entity) {
        if (entity == null) {
            return;
        }
        PetCommandData data = PetCommandData.of(entity);
        UUID markerUuid = data.markerUuid().orElse(null);
        ServerPlayer owner = ownerPlayer(entity);
        if (entity instanceof Mob mob) {
            cancelCurrent(mob, data);
        }
        data.clear();
        PetCommandRuntimeState.remove(entity);
        ACTIVE.remove(entity);
        if (owner != null && markerUuid != null) {
            removeMarkerIfUnused(owner, markerUuid);
        }
    }

    public static void clearForOwner(ServerPlayer owner) {
        RecipientSet recipientSet = resolveRecipients(owner);
        for (LivingEntity pet : recipientSet.recipients()) {
            clear(pet);
        }
        sendSummary(owner);
    }

    public static void clearMarkerForOwner(ServerPlayer owner, UUID markerUuid) {
        if (owner == null || markerUuid == null) {
            return;
        }
        for (LivingEntity pet : List.copyOf(ACTIVE)) {
            if (pet == null || pet.isRemoved() || !PetOwnershipService.isOwnedBy(pet, owner)) {
                continue;
            }
            PetCommandData data = PetCommandData.of(pet);
            if (data.active() && data.markerUuid().map(markerUuid::equals).orElse(false)) {
                clear(pet);
            }
        }
        sendSummary(owner);
    }

    public static void complete(LivingEntity entity) {
        ServerPlayer owner = ownerPlayer(entity);
        clear(entity);
        if (owner != null) {
            sendSummary(owner);
        }
    }

    public static void sendSummary(ServerPlayer owner) {
        RecipientSet recipientSet = resolveRecipients(owner);
        PetCommandSummary summary = summarize(recipientSet.recipients());
        boolean hasLandCapablePets = recipientSet.recipients().stream()
                .filter(pet -> pet instanceof Mob && !TorporData.of(pet).unconscious() && !FuryService.isBerserk(pet))
                .anyMatch(PetCapabilityResolver::canLand);
        NetworkHandler.sendPetCommandSummary(owner, summary, recipientSet.explicitSelection(), hasLandCapablePets);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        for (LivingEntity entity : List.copyOf(ACTIVE)) {
            PetCommandData data = PetCommandData.of(entity);
            if (entity instanceof Mob mob && data.active()) {
                cancelCurrent(mob, data);
            }
            PetCommandRuntimeState.remove(entity);
        }
        ACTIVE.clear();
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity living) {
            registerIfActive(living);
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof LivingEntity living)) {
            return;
        }

        UUID leavingUuid = living.getUUID();
        for (LivingEntity pet : List.copyOf(ACTIVE)) {
            if (pet == living) {
                continue;
            }
            PetCommandData data = PetCommandData.of(pet);
            if (data.active()
                    && data.command() == PetCommand.ATTACK
                    && data.targetUuid().map(leavingUuid::equals).orElse(false)) {
                complete(pet);
            }
        }

        if (!ACTIVE.contains(living)) {
            return;
        }

        PetCommandData leavingData = PetCommandData.of(living);
        UUID markerUuid = leavingData.markerUuid().orElse(null);
        ServerPlayer owner = ownerPlayer(living);
        if (living instanceof Mob mob) {
            cancelCurrent(mob, leavingData);
        }
        PetCommandRuntimeState.remove(living);
        ACTIVE.remove(living);
        if (owner != null && markerUuid != null) {
            removeMarkerIfUnused(owner, markerUuid);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % 5 != 0 || ACTIVE.isEmpty()) {
            return;
        }
        for (LivingEntity entity : List.copyOf(ACTIVE)) {
            if (!tickCommand(entity)) {
                ACTIVE.remove(entity);
            }
        }
    }

    private static boolean tickCommand(LivingEntity entity) {
        if (entity == null || entity.isRemoved() || entity.level().isClientSide) {
            return false;
        }
        PetCommandData data = PetCommandData.of(entity);
        if (!data.active()) {
            return false;
        }
        if (TorporData.of(entity).unconscious() || FuryService.isBerserk(entity) || !PetOwnershipService.isManagedPet(entity)) {
            return true;
        }
        if (!(entity instanceof Mob mob)) {
            complete(entity);
            return false;
        }

        PetCommandExecutor executor = PetCommandExecutors.get(data.command());
        if (executor == null) {
            complete(mob);
            return false;
        }
        PetCommandRuntimeState runtime = PetCommandRuntimeState.peek(mob);
        if (runtime == null) {
            runtime = PetCommandRuntimeState.of(mob);
            boolean started = false;
            try {
                started = executor.supports(mob) && executor.start(mob, data, runtime);
            } catch (RuntimeException exception) {
                logExecutorFailure("tick-restart", mob, data.command(), exception);
            }
            if (!started) {
                complete(mob);
                return false;
            }
        }

        boolean active;
        try {
            active = executor.tick(mob, data, runtime);
        } catch (RuntimeException exception) {
            logExecutorFailure("tick", mob, data.command(), exception);
            active = false;
        }
        if (active) {
            return true;
        }
        complete(mob);
        return false;
    }

    private static void cancelCurrent(Mob mob, PetCommandData data) {
        PetCommandRuntimeState runtime = PetCommandRuntimeState.peek(mob);
        if (!data.active()) {
            if (runtime != null) {
                runtime.clearExecution();
                PetCommandRuntimeState.remove(mob);
            }
            return;
        }
        PetCommandExecutor executor = PetCommandExecutors.get(data.command());
        if (executor == null) {
            PetCommandRuntimeState.remove(mob);
            mob.getNavigation().stop();
            return;
        }
        if (runtime == null) {
            runtime = PetCommandRuntimeState.of(mob);
        }
        try {
            executor.cancel(mob, data, runtime);
        } catch (RuntimeException exception) {
            logExecutorFailure("cancel", mob, data.command(), exception);
        }
    }

    private static void logExecutorFailure(String operation, Mob mob, PetCommand command, RuntimeException exception) {
        LOGGER.warn(
                "Pet command {} {} failed for entity {} ({}).",
                command,
                operation,
                mob.getUUID(),
                mob.getType(),
                exception
        );
    }

    private static void restoreMarker(LivingEntity entity, PetCommandData data) {
        UUID markerUuid = data.markerUuid().orElse(null);
        ServerPlayer owner = ownerPlayer(entity);
        if (markerUuid == null || owner == null) {
            return;
        }
        if (data.command() == PetCommand.MOVE) {
            NetworkHandler.sendPetCommandMarker(
                    owner, markerUuid, PetCommand.MOVE, -1, null, data.position()
            );
            return;
        }
        if (data.command() != PetCommand.ATTACK) {
            return;
        }
        UUID targetUuid = data.targetUuid().orElse(null);
        Entity targetEntity = targetUuid == null ? null : owner.serverLevel().getEntity(targetUuid);
        if (targetEntity instanceof LivingEntity target && target.isAlive() && !target.isRemoved()) {
            NetworkHandler.sendPetCommandMarker(
                    owner,
                    markerUuid,
                    PetCommand.ATTACK,
                    target.getId(),
                    target.getUUID(),
                    target.position()
            );
        }
    }

    private static boolean markerInUse(UUID markerUuid) {
        if (markerUuid == null) {
            return false;
        }
        for (LivingEntity pet : List.copyOf(ACTIVE)) {
            if (pet == null || pet.isRemoved()) {
                continue;
            }
            PetCommandData data = PetCommandData.of(pet);
            if (data.active() && data.markerUuid().map(markerUuid::equals).orElse(false)) {
                return true;
            }
        }
        return false;
    }

    private static void removeMarkerIfUnused(ServerPlayer owner, UUID markerUuid) {
        if (owner != null && markerUuid != null && !markerInUse(markerUuid)) {
            NetworkHandler.removePetCommandMarker(owner, markerUuid);
        }
    }

    private static ServerPlayer ownerPlayer(LivingEntity entity) {
        ServerPlayer direct = PetOwnershipService.ownerPlayer(entity)
                .filter(ServerPlayer.class::isInstance)
                .map(ServerPlayer.class::cast)
                .orElse(null);
        if (direct != null) {
            return direct;
        }
        if (!(entity.level() instanceof ServerLevel level)) {
            return null;
        }
        for (ServerPlayer player : level.players()) {
            if (PetOwnershipService.isOwnedBy(entity, player)) {
                return player;
            }
        }
        return null;
    }

    private static boolean isMovementCommand(PetCommand command) {
        return command == PetCommand.MOVE
                || command == PetCommand.FOLLOW
                || command == PetCommand.RETREAT
                || command == PetCommand.LAND;
    }

    private static RecipientSet resolveRecipients(ServerPlayer owner) {
        boolean hadSelection = PetSelectionService.hasSelection(owner);
        List<LivingEntity> recipients = PetSelectionService.resolveSelected(owner);
        if (!hadSelection) {
            double range = Config.PET_COMMAND_RANGE.get();
            AABB area = owner.getBoundingBox().inflate(range);
            recipients = owner.serverLevel().getEntitiesOfClass(
                    LivingEntity.class,
                    area,
                    entity -> entity != owner && PetOwnershipService.isOwnedBy(entity, owner)
            );
        } else {
            List<Integer> selectedIds = recipients.stream().map(Entity::getId).toList();
            NetworkHandler.sendPetSelection(owner, selectedIds);
        }
        return new RecipientSet(List.copyOf(recipients), hadSelection);
    }

    private static PetCommandSummary summarize(List<LivingEntity> recipients) {
        PetCommand first = null;
        boolean any = false;
        for (LivingEntity pet : recipients) {
            PetCommandData data = PetCommandData.of(pet);
            if (!data.active()) {
                if (any) {
                    return PetCommandSummary.MIXED;
                }
                continue;
            }
            PetCommand command = data.command();
            if (!any) {
                any = true;
                first = command;
            } else if (first != command) {
                return PetCommandSummary.MIXED;
            }
        }
        if (!any) {
            return PetCommandSummary.NONE;
        }
        for (LivingEntity pet : recipients) {
            if (!PetCommandData.of(pet).active()) {
                return PetCommandSummary.MIXED;
            }
        }
        return PetCommandSummary.of(first);
    }

    private record RecipientSet(List<LivingEntity> recipients, boolean explicitSelection) {
    }
}
