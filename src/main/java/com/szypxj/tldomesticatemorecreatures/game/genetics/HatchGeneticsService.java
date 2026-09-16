package com.szypxj.tldomesticatemorecreatures.game.genetics;

import com.szypxj.tldomesticatemorecreatures.game.HatchSeedMath;
import com.szypxj.tldomesticatemorecreatures.game.LevelService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

public final class HatchGeneticsService {
    private HatchGeneticsService() {
    }

    public static HatchGeneticPayload createPayload(LivingEntity parentA, LivingEntity parentB, long seed) {
        ParentGeneticsSnapshot a = LevelService.captureParentGenetics(parentA);
        ParentGeneticsSnapshot b = LevelService.captureParentGenetics(parentB);
        ResourceLocation childId = parentA == null ? null : ForgeRegistries.ENTITY_TYPES.getKey(parentA.getType());
        if (a == null || b == null || childId == null) {
            return null;
        }
        ChildGeneticsResult result = LevelService.resolveInheritedResult(
                childId.toString(),
                a,
                b,
                RandomSource.create(HatchSeedMath.childSeed(seed, 0))
        );
        if (result == null) {
            return null;
        }
        return HatchGeneticPayload.create(seed, childId.toString(), a, b, result);
    }
}
