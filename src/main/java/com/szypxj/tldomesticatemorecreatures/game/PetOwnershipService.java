package com.szypxj.tldomesticatemorecreatures.game;

import com.szypxj.tldomesticatemorecreatures.config.Config;
import com.szypxj.tldomesticatemorecreatures.domestication.DomesticationData;
import com.szypxj.tldomesticatemorecreatures.elite.EliteService;
import com.szypxj.tldomesticatemorecreatures.network.NetworkHandler;
import com.szypxj.tldomesticatemorecreatures.petmanagement.PetManagementService;
import com.szypxj.tldomesticatemorecreatures.command.pet.provider.PetCommandCompatibilityApi;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.ref.WeakReference;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class PetOwnershipService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<Class<?>> FAILED_OWNERSHIP_PROVIDERS = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, PendingTameInteraction> PENDING_TAME_INTERACTIONS = new ConcurrentHashMap<>();
    private static final int TAME_INTERACTION_WINDOW_TICKS = 6;
    private PetOwnershipService() {
    }

    public static boolean isManagedPet(LivingEntity entity) {
        return entity != null
                && !(entity instanceof Player)
                && LevelService.isAffected(entity)
                && isTamed(entity);
    }

    public static boolean canUsePanel(LivingEntity entity) {
        return isManagedPet(entity) && isPanelEntityAllowed(entity);
    }

    public static boolean isCustomPet(LivingEntity entity) {
        return entity != null
                && !(entity instanceof Player)
                && LevelService.isAffected(entity)
                && DomesticationData.of(entity).customTamed();
    }

    public static boolean isOwnedBy(LivingEntity entity, Player player) {
        if (entity == null || player == null) {
            return false;
        }
        for (PetCommandCompatibilityApi.OwnershipProvider provider : PetCommandCompatibilityApi.ownershipProviders()) {
            Class<?> providerType = provider.getClass();
            if (FAILED_OWNERSHIP_PROVIDERS.contains(providerType)) {
                continue;
            }
            try {
                if (provider.supports(entity)) {
                    return provider.isOwnedBy(entity, player);
                }
            } catch (RuntimeException exception) {
                if (FAILED_OWNERSHIP_PROVIDERS.add(providerType)) {
                    LOGGER.warn("Pet ownership provider {} failed and was disabled.", providerType.getName(), exception);
                }
            }
        }
        if (!isManagedPet(entity)) {
            return false;
        }
        return ownerUuid(entity).map(player.getUUID()::equals).orElse(false);
    }

    public static Optional<UUID> ownerUuid(LivingEntity entity) {
        if (entity == null || entity instanceof Player || !LevelService.isAffected(entity) || EliteService.isElite(entity)) {
            return Optional.empty();
        }

        DomesticationData custom = DomesticationData.of(entity);
        if (custom.customTamed()) {
            return custom.ownerUuid();
        }

        if (entity instanceof TamableAnimal tamable && tamable.isTame()) {
            UUID ownerUuid = tamable.getOwnerUUID();
            if (ownerUuid != null) {
                return Optional.of(ownerUuid);
            }
        }

        if (isTamed(entity) && entity instanceof OwnableEntity ownable) {
            UUID ownerUuid = ownable.getOwnerUUID();
            if (ownerUuid != null) {
                return Optional.of(ownerUuid);
            }
        }

        return Optional.empty();
    }

    public static Optional<Player> ownerPlayer(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return Optional.empty();
        }
        return ownerUuid(entity).map(level::getPlayerByUUID);
    }

    public static boolean isTamed(LivingEntity entity) {
        if (entity == null || entity instanceof Player || !LevelService.isAffected(entity) || EliteService.isElite(entity)) {
            return false;
        }
        if (DomesticationData.of(entity).customTamed()) {
            return true;
        }
        return entity instanceof TamableAnimal tamable && tamable.isTame();
    }

    public static void setCustomOwner(LivingEntity entity, Player owner) {
        if (!LevelService.isAffected(entity) || EliteService.isElite(entity)) {
            return;
        }
        DomesticationData data = DomesticationData.of(entity);
        data.customTamed(true);
        data.ownerUuid(owner.getUUID());
        data.resetTamingSession();
        if (entity instanceof TamableAnimal tamable) {
            tamable.tame(owner);
        }
        PetManagementService.track(entity);
        if (owner instanceof ServerPlayer serverPlayer) {
            NetworkHandler.sendPetOwnership(serverPlayer, entity);
        }
    }

    public static void setInheritedOwner(LivingEntity entity, UUID ownerUuid) {
        if (!LevelService.isAffected(entity) || EliteService.isElite(entity)) {
            return;
        }
        DomesticationData data = DomesticationData.of(entity);
        data.customTamed(true);
        data.ownerUuid(ownerUuid);
        data.resetTamingSession();
        PetManagementService.track(entity);
        if (entity.level() instanceof ServerLevel level) {
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerUuid);
            if (owner != null) {
                NetworkHandler.sendPetOwnership(owner, entity);
            }
        }
    }

    public static void observeTameInteraction(ServerPlayer player, LivingEntity entity) {
        if (player == null || entity == null || entity == player || entity.level().isClientSide
                || !LevelService.isAffected(entity) || EliteService.isElite(entity)) {
            return;
        }

        if (isTamed(entity)) {
            reconcileKnownOwnership(entity, player);
            return;
        }

        PENDING_TAME_INTERACTIONS.put(
                entity.getUUID(),
                new PendingTameInteraction(
                        player.getUUID(),
                        new WeakReference<>(entity),
                        player.getServer().getTickCount() + TAME_INTERACTION_WINDOW_TICKS,
                        -1
                )
        );
    }

    public static boolean reconcileKnownOwnership(LivingEntity entity, ServerPlayer player) {
        if (entity == null || player == null || !isTamed(entity) || !isOwnedBy(entity, player)) {
            return false;
        }
        if (ownerUuid(entity).isEmpty()) {
            adoptExternalTame(entity, player);
            return ownerUuid(entity).filter(player.getUUID()::equals).isPresent();
        }
        LevelService.markTamed(entity);
        PetManagementService.track(entity);
        NetworkHandler.sendPetOwnership(player, entity);
        return true;
    }

    public static void tickPendingTameInteractions(MinecraftServer server) {
        if (server == null || PENDING_TAME_INTERACTIONS.isEmpty()) {
            return;
        }
        int tick = server.getTickCount();
        for (Map.Entry<UUID, PendingTameInteraction> entry : PENDING_TAME_INTERACTIONS.entrySet()) {
            PendingTameInteraction pending = entry.getValue();
            LivingEntity entity = pending.entity().get();
            if (entity == null || entity.isRemoved() || entity.getServer() != server || tick > pending.expiresAtTick()) {
                PENDING_TAME_INTERACTIONS.remove(entry.getKey(), pending);
                continue;
            }
            if (!isTamed(entity)) {
                continue;
            }

            ServerPlayer player = server.getPlayerList().getPlayer(pending.playerUuid());
            if (player == null) {
                PENDING_TAME_INTERACTIONS.remove(entry.getKey(), pending);
                continue;
            }

            Optional<UUID> owner = ownerUuid(entity);
            if (owner.isPresent()) {
                if (owner.get().equals(player.getUUID())) {
                    reconcileKnownOwnership(entity, player);
                }
                PENDING_TAME_INTERACTIONS.remove(entry.getKey(), pending);
                continue;
            }

            if (isOwnedBy(entity, player)) {
                adoptExternalTame(entity, player);
                PENDING_TAME_INTERACTIONS.remove(entry.getKey(), pending);
                continue;
            }

            if (!player.getAbilities().instabuild) {
                PENDING_TAME_INTERACTIONS.remove(entry.getKey(), pending);
                continue;
            }

            if (pending.tamedObservedTick() < 0) {
                PENDING_TAME_INTERACTIONS.replace(
                        entry.getKey(),
                        pending,
                        pending.withTamedObservedTick(tick)
                );
                continue;
            }
            if (tick <= pending.tamedObservedTick()) {
                continue;
            }

            adoptExternalTame(entity, player);
            PENDING_TAME_INTERACTIONS.remove(entry.getKey());
        }
    }

    private static void adoptExternalTame(LivingEntity entity, ServerPlayer player) {
        if (entity == null || player == null || !isTamed(entity) || ownerUuid(entity).isPresent()) {
            return;
        }
        LevelService.markTamed(entity);
        DomesticationData data = DomesticationData.of(entity);
        data.customTamed(true);
        data.ownerUuid(player.getUUID());
        data.resetTamingSession();
        PetManagementService.track(entity);
        NetworkHandler.sendPetOwnership(player, entity);
    }

    private record PendingTameInteraction(
            UUID playerUuid,
            WeakReference<LivingEntity> entity,
            int expiresAtTick,
            int tamedObservedTick
    ) {
        private PendingTameInteraction withTamedObservedTick(int tick) {
            return new PendingTameInteraction(playerUuid, entity, expiresAtTick, tick);
        }
    }

    public static boolean haveSameOwner(LivingEntity first, LivingEntity second) {
        Optional<UUID> a = ownerUuid(first);
        Optional<UUID> b = ownerUuid(second);
        return a.isPresent() && b.isPresent() && a.get().equals(b.get());
    }

    public static boolean isPanelEntityAllowed(LivingEntity entity) {
        if (entity == null || entity instanceof Player || !LevelService.isAffected(entity)) {
            return false;
        }

        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (id == null) {
            return false;
        }

        String mode = Config.PANEL_TAMEABLE_ENTITY_MODE.get().toUpperCase(Locale.ROOT);
        boolean listed = Config.PANEL_TAMEABLE_ENTITIES.get().contains(id.toString());
        return "WHITELIST".equals(mode) ? listed : !listed;
    }
}
