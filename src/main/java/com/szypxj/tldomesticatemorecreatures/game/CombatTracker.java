package com.szypxj.tldomesticatemorecreatures.game;

import com.szypxj.tldomesticatemorecreatures.config.Config;
import com.szypxj.tldomesticatemorecreatures.data.ProgressData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CombatTracker {
    private static final Map<UUID, Map<UUID, Long>> PARTICIPANTS = new LinkedHashMap<>();

    private CombatTracker() {
    }

    public static void record(LivingEntity victim, Entity attacker) {
        if (!(victim.level() instanceof ServerLevel serverLevel) || !(attacker instanceof LivingEntity livingAttacker) || attacker == victim) {
            return;
        }
        LevelService.initializeIfNeeded(livingAttacker);
        PARTICIPANTS.computeIfAbsent(victim.getUUID(), key -> new LinkedHashMap<>())
                .put(attacker.getUUID(), serverLevel.getGameTime());
    }

    public static List<LivingEntity> recipients(LivingEntity victim) {
        if (!(victim.level() instanceof ServerLevel serverLevel)) {
            return List.of();
        }

        Map<UUID, Long> entries = PARTICIPANTS.remove(victim.getUUID());
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }

        long cutoff = serverLevel.getGameTime() - Config.COMBAT_PARTICIPATION_SECONDS.get() * 20L;
        Map<UUID, LivingEntity> nonMax = new LinkedHashMap<>();
        Map<UUID, LivingEntity> maxLevelPets = new LinkedHashMap<>();

        for (Map.Entry<UUID, Long> entry : entries.entrySet()) {
            if (entry.getValue() < cutoff) {
                continue;
            }

            Entity entity = serverLevel.getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }

            LevelService.initializeIfNeeded(living);
            if (!(living instanceof ServerPlayer) && !PetOwnershipService.isManagedPet(living)) {
                continue;
            }
            addRecipient(living, nonMax, maxLevelPets);

            if (!(living instanceof ServerPlayer)) {
                PetOwnershipService.ownerUuid(living).ifPresent(ownerUuid -> {
                    ServerPlayer owner = serverLevel.getServer().getPlayerList().getPlayer(ownerUuid);
                    if (owner == null || owner.level() != living.level()) {
                        return;
                    }

                    double range = Config.PET_EXPERIENCE_SHARE_RANGE.get();
                    if (owner.distanceToSqr(living) > range * range) {
                        return;
                    }

                    LevelService.initializeIfNeeded(owner);
                    addRecipient(owner, nonMax, maxLevelPets);
                });
            }
        }

        if (!nonMax.isEmpty()) {
            return new ArrayList<>(nonMax.values());
        }
        if (ExperienceMath.useMaxLevelPetGroup(nonMax.size(), maxLevelPets.size())) {
            return new ArrayList<>(maxLevelPets.values());
        }
        return List.of();
    }

    public static void clear(LivingEntity victim) {
        PARTICIPANTS.remove(victim.getUUID());
    }

    private static void addRecipient(
            LivingEntity entity,
            Map<UUID, LivingEntity> nonMax,
            Map<UUID, LivingEntity> maxLevelPets
    ) {
        if (!ProgressData.exists(entity) || !LevelService.canGainExperience(entity)) {
            return;
        }

        ProgressData data = ProgressData.of(entity);
        if (data.level() < data.maxLevel()) {
            if (LevelService.canLevelUp(entity)) {
                nonMax.put(entity.getUUID(), entity);
            }
            return;
        }

        if (!(entity instanceof ServerPlayer) && LevelService.canStoreKillExperience(entity)) {
            maxLevelPets.put(entity.getUUID(), entity);
        }
    }
}
