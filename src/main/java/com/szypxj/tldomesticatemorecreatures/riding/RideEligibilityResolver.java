package com.szypxj.tldomesticatemorecreatures.riding;

import com.mojang.logging.LogUtils;
import com.szypxj.tldomesticatemorecreatures.domestication.TamingRuleManager;
import com.szypxj.tldomesticatemorecreatures.game.PetOwnershipService;
import com.szypxj.tldomesticatemorecreatures.riding.config.EntityRideProfile;
import com.szypxj.tldomesticatemorecreatures.riding.config.RidingConfigManager;
import com.szypxj.tldomesticatemorecreatures.riding.provider.RideCompatibilityApi;
import com.szypxj.tldomesticatemorecreatures.torpor.TorporData;
import com.szypxj.tldomesticatemorecreatures.talent.FuryService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

public final class RideEligibilityResolver {
    private static final Logger LOGGER = LogUtils.getLogger();

    private RideEligibilityResolver() {
    }

    public static RideEligibilityResult evaluate(LivingEntity entity, Player rider) {
        if (entity == null || rider == null || entity == rider || !entity.isAlive() || entity.isRemoved()) {
            return RideEligibilityResult.DENY;
        }
        if (!PetOwnershipService.isOwnedBy(entity, rider) && !RideService.mayOtherPlayerRide(rider, entity)) {
            return RideEligibilityResult.DENY;
        }
        if (TorporData.of(entity).unconscious() || FuryService.isBerserk(entity)) {
            return RideEligibilityResult.DENY;
        }

        if (NativeRideCapabilityResolver.hasNativePlayerControl(entity)) {
            return RideEligibilityResult.NATIVE_BYPASS;
        }

        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (entityId == null || TamingRuleManager.ruleFor(entity).isEmpty()) {
            return RideEligibilityResult.DENY;
        }
        EntityRideProfile profile = RidingConfigManager.profile(entityId);
        if (profile.mode() == RideMode.DISABLED) {
            return RideEligibilityResult.DENY;
        }
        if (!RidingConfigManager.settings().allows(entityId) && profile.mode() != RideMode.FORCE_GENERIC) {
            return RideEligibilityResult.DENY;
        }

        RideCompatibilityApi.Decision providerDecision = providerDecision(entity, rider);
        if (providerDecision == RideCompatibilityApi.Decision.DENY) {
            return RideEligibilityResult.DENY;
        }
        if (profile.mode() == RideMode.FORCE_GENERIC || providerDecision == RideCompatibilityApi.Decision.ALLOW) {
            return RideEligibilityResult.ALLOW_GENERIC;
        }
        return RideEligibilityResult.ALLOW_GENERIC;
    }

    private static RideCompatibilityApi.Decision providerDecision(LivingEntity entity, Player rider) {
        for (RideCompatibilityApi.RideEligibilityProvider provider : RideCompatibilityApi.eligibilityProviders()) {
            if (!RideCompatibilityApi.enabled(provider)) {
                continue;
            }
            try {
                if (!provider.supports(entity)) {
                    continue;
                }
                RideCompatibilityApi.Decision decision = provider.evaluate(entity, rider);
                if (decision != null && decision != RideCompatibilityApi.Decision.PASS) {
                    return decision;
                }
            } catch (RuntimeException exception) {
                if (RideCompatibilityApi.disableProvider(provider)) {
                    LOGGER.warn("Ride eligibility provider {} failed and was disabled.", provider.getClass().getName(), exception);
                }
            }
        }
        return RideCompatibilityApi.Decision.PASS;
    }
}
