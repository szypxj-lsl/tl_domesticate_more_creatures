package com.szypxj.tldomesticatemorecreatures.talent.active;

import com.szypxj.tldomesticatemorecreatures.api.riding.RideAction;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideActionApi;
import com.szypxj.tldomesticatemorecreatures.data.ProgressData;
import com.szypxj.tldomesticatemorecreatures.game.PetOwnershipService;
import com.szypxj.tldomesticatemorecreatures.network.NetworkHandler;
import com.szypxj.tldomesticatemorecreatures.network.packet.S2CActiveTalentStatePacket;
import com.szypxj.tldomesticatemorecreatures.talent.SpecialTalentService;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ActiveTalentService {
    private static final Map<UUID, ActiveTalentRuntimeState> RUNTIME = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> LAST_SEQUENCE = new ConcurrentHashMap<>();

    private ActiveTalentService() {
    }

    public static void activateOrInput(ServerPlayer sender, int mountEntityId, int sequence) {
        if (sender == null || sequence < 0 || !(sender.getVehicle() instanceof LivingEntity mount)) return;
        if (mount.getId() != mountEntityId || sender.level() != mount.level() || !canActivate(sender, mount)) return;
        int last = LAST_SEQUENCE.getOrDefault(sender.getUUID(), -1);
        if (sequence <= last) return;
        LAST_SEQUENCE.put(sender.getUUID(), sequence);
        if (!RideActionApi.allows(sender, mount, RideAction.ABILITY)) return;

        selfHealActiveTalents(mount);
        ProgressData data = ProgressData.of(mount);
        recoverInterruptedPersistentState(mount, data);
        ActiveTalentRuntimeState current = RUNTIME.get(mount.getUUID());
        if (current != null) {
            if (ActiveTalentIds.SHADOWSTEP.equals(current.skillId()) && current.phase() == ActiveTalentPhase.MARKING) {
                ShadowstepService.tryAddMark(sender, mount, current);
            }
            return;
        }

        long now = mount.level().getGameTime();
        if (data.activeTalentCooldownEndGameTime() > now) {
            sync(sender, mount, null);
            return;
        }
        Optional<String> active = ActiveTalentRegistry.activeTalent(data.specialTalents());
        if (active.isEmpty() || !SpecialTalentService.hasActive(mount, active.get())) return;
        if (ActiveTalentIds.SHADOWSTEP.equals(active.get())) {
            ShadowstepService.start(sender, mount, ActiveTalentConfigManager.shadowstep());
        } else if (ActiveTalentIds.CAMOUFLAGE.equals(active.get())) {
            CamouflageService.start(sender, mount, ActiveTalentConfigManager.camouflage());
        }
    }

    public static boolean canActivate(ServerPlayer rider, LivingEntity mount) {
        return rider != null
                && mount != null
                && rider.isAlive()
                && !rider.isSpectator()
                && mount.isAlive()
                && !mount.isRemoved()
                && PetOwnershipService.isTamed(mount)
                && PetOwnershipService.isOwnedBy(mount, rider);
    }

    public static void selfHealActiveTalents(LivingEntity entity) {
        if (entity == null || !ProgressData.exists(entity)) return;
        ProgressData data = ProgressData.of(entity);
        Set<String> original = data.specialTalents();
        Set<String> normalized = ActiveTalentRegistry.normalizeToSingleActive(entity.getUUID(), original);
        if (!normalized.equals(original)) data.specialTalents(normalized);
        recoverInterruptedPersistentState(entity, data);
    }

    public static void recoverInterruptedPersistentState(LivingEntity mount, ProgressData data) {
        if (mount == null || data == null || data.activeTalentInProgress().isBlank() || RUNTIME.containsKey(mount.getUUID())) return;
        String skillId = data.activeTalentInProgress();
        int cooldownTicks = fullCooldownTicks(skillId);
        long now = mount.level().getGameTime();
        data.activeTalentCooldownSkillId(skillId);
        data.activeTalentCooldownEndGameTime(now + cooldownTicks);
        data.clearActiveTalentInProgress();
    }

    public static void tick(MinecraftServer server) {
        if (server == null || RUNTIME.isEmpty()) return;
        for (ActiveTalentRuntimeState state : new ArrayList<>(RUNTIME.values())) {
            LivingEntity mount = findLiving(server, state.mountUuid());
            ServerPlayer rider = server.getPlayerList().getPlayer(state.riderUuid());
            if (mount == null || rider == null || !canContinue(rider, mount, state)) {
                cancelForInterruption(mount, state);
                continue;
            }
            if (!RideActionApi.allows(rider, mount, RideAction.ABILITY)) {
                cancelForInterruption(mount, state);
                continue;
            }
            if (ActiveTalentIds.SHADOWSTEP.equals(state.skillId())) {
                ShadowstepService.tick(rider, mount, state);
            } else if (ActiveTalentIds.CAMOUFLAGE.equals(state.skillId())) {
                CamouflageService.tick(rider, mount, state);
            } else {
                cancelForInterruption(mount, state);
            }
        }
    }

    public static boolean blocksRideInput(LivingEntity mount) {
        ActiveTalentRuntimeState state = runtime(mount);
        return state != null && ActiveTalentIds.SHADOWSTEP.equals(state.skillId()) && state.phase() == ActiveTalentPhase.EXECUTING;
    }

    public static boolean isShadowstepExecuting(LivingEntity mount) {
        ActiveTalentRuntimeState state = runtime(mount);
        return state != null && ActiveTalentIds.SHADOWSTEP.equals(state.skillId()) && state.phase() == ActiveTalentPhase.EXECUTING;
    }

    public static boolean isAttackableTarget(LivingEntity mount, LivingEntity target) {
        if (mount == null || target == null || target == mount || !target.isAlive() || target.isRemoved() || target instanceof Player) return false;
        UUID owner = PetOwnershipService.ownerUuid(mount).orElse(null);
        return owner == null || !PetOwnershipService.ownerUuid(target).map(owner::equals).orElse(false);
    }

    public static ActiveTalentRuntimeState runtime(LivingEntity mount) {
        return mount == null ? null : RUNTIME.get(mount.getUUID());
    }

    static void install(LivingEntity mount, ActiveTalentRuntimeState state) {
        if (mount == null || state == null) return;
        RUNTIME.put(mount.getUUID(), state);
        ProgressData data = ProgressData.of(mount);
        data.activeTalentInProgress(state.skillId());
        data.activeTalentCooldownSkillId("");
        data.activeTalentCooldownEndGameTime(0L);
    }

    static void finish(ServerPlayer rider, LivingEntity mount, ActiveTalentRuntimeState state, int cooldownTicks) {
        if (mount == null || state == null) return;
        RUNTIME.remove(mount.getUUID(), state);
        ProgressData data = ProgressData.of(mount);
        data.clearActiveTalentInProgress();
        data.activeTalentCooldownSkillId(state.skillId());
        data.activeTalentCooldownEndGameTime(mount.level().getGameTime() + Math.max(0, cooldownTicks));
        if (rider != null) sync(rider, mount, null);
    }

    public static void cancelForInterruption(LivingEntity mount, ActiveTalentRuntimeState state) {
        if (state == null) return;
        if (ActiveTalentIds.SHADOWSTEP.equals(state.skillId())) ShadowstepService.cleanupSlow(mount, state);
        if (ActiveTalentIds.CAMOUFLAGE.equals(state.skillId())) CamouflageService.cleanupRuntimeTargets(state);
        ServerPlayer rider = null;
        if (mount != null && mount.level() instanceof ServerLevel level) rider = level.getServer().getPlayerList().getPlayer(state.riderUuid());
        if (mount != null) finish(rider, mount, state, fullCooldownTicks(state));
        else RUNTIME.remove(state.mountUuid(), state);
    }

    static void sync(ServerPlayer rider, LivingEntity mount, ActiveTalentRuntimeState state) {
        if (rider == null || mount == null) return;
        ProgressData data = ProgressData.of(mount);
        long now = mount.level().getGameTime();
        String skillId;
        if (state != null) {
            skillId = state.skillId();
        } else if (data.activeTalentCooldownEndGameTime() > now && !data.activeTalentCooldownSkillId().isBlank()) {
            skillId = data.activeTalentCooldownSkillId();
        } else {
            skillId = ActiveTalentRegistry.activeTalent(data.specialTalents()).orElse("");
        }
        String phase = state == null ? "" : state.phase().name();
        long phaseEnd = state == null ? 0L : state.phaseEndGameTime();
        int marks = state == null ? 0 : state.marks().size();
        int maxMarks = state == null || state.shadowstepConfig() == null ? 0 : state.shadowstepConfig().maxMarks();
        NetworkHandler.sendActiveTalentState(rider, new S2CActiveTalentStatePacket(
                mount.getId(), skillId, phase, phaseEnd, marks, maxMarks, data.activeTalentCooldownEndGameTime()
        ));
    }


    public static void syncCurrent(ServerPlayer rider, LivingEntity mount) {
        if (rider == null || mount == null || !PetOwnershipService.isOwnedBy(mount, rider)) return;
        selfHealActiveTalents(mount);
        sync(rider, mount, runtime(mount));
    }

    public static int remainingCooldownTicks(LivingEntity mount) {
        if (mount == null || !ProgressData.exists(mount)) return 0;
        long remaining = ProgressData.of(mount).activeTalentCooldownEndGameTime() - mount.level().getGameTime();
        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, remaining));
    }

    public static void clearInputSequence(ServerPlayer player) {
        if (player != null) LAST_SEQUENCE.remove(player.getUUID());
    }

    public static void clearAll(MinecraftServer server) {
        for (ActiveTalentRuntimeState state : new ArrayList<>(RUNTIME.values())) {
            LivingEntity mount = server == null ? null : findLiving(server, state.mountUuid());
            if (mount != null) {
                cancelForInterruption(mount, state);
            } else {
                RUNTIME.remove(state.mountUuid(), state);
            }
        }
        RUNTIME.clear();
        LAST_SEQUENCE.clear();
        CamouflageService.clearAll();
    }

    public static void clearAll() {
        clearAll(null);
    }

    private static boolean canContinue(ServerPlayer rider, LivingEntity mount, ActiveTalentRuntimeState state) {
        return canActivate(rider, mount)
                && rider.getVehicle() == mount
                && mount.getUUID().equals(state.mountUuid())
                && rider.getUUID().equals(state.riderUuid())
                && rider.level() == mount.level();
    }

    private static int fullCooldownTicks(ActiveTalentRuntimeState state) {
        if (state == null) return 0;
        if (ActiveTalentIds.SHADOWSTEP.equals(state.skillId()) && state.shadowstepConfig() != null) {
            return state.shadowstepConfig().cooldownTicks();
        }
        if (ActiveTalentIds.CAMOUFLAGE.equals(state.skillId()) && state.camouflageConfig() != null) {
            return state.camouflageConfig().cooldownTicks();
        }
        return fullCooldownTicks(state.skillId());
    }

    private static int fullCooldownTicks(String skillId) {
        if (ActiveTalentIds.SHADOWSTEP.equals(skillId)) return ActiveTalentConfigManager.shadowstep().cooldownTicks();
        if (ActiveTalentIds.CAMOUFLAGE.equals(skillId)) return ActiveTalentConfigManager.camouflage().cooldownTicks();
        return 0;
    }

    private static LivingEntity findLiving(MinecraftServer server, UUID uuid) {
        if (uuid == null) return null;
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(uuid);
            if (entity instanceof LivingEntity living) return living;
        }
        return null;
    }
}
