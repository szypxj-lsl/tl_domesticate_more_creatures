package com.szypxj.tldomesticatemorecreatures.client.riding;

import com.mojang.logging.LogUtils;
import com.szypxj.tldomesticatemorecreatures.client.ClientState;
import com.szypxj.tldomesticatemorecreatures.riding.NativeRideCapabilityResolver;
import com.szypxj.tldomesticatemorecreatures.riding.RideMode;
import com.szypxj.tldomesticatemorecreatures.riding.config.EntityRideProfile;
import com.szypxj.tldomesticatemorecreatures.riding.config.RiderPosePreset;
import com.szypxj.tldomesticatemorecreatures.riding.config.RiderVisualProfile;
import com.szypxj.tldomesticatemorecreatures.riding.provider.RideCompatibilityApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

public final class RiderVisualHooks {
    private static final Logger LOGGER = LogUtils.getLogger();

    private RiderVisualHooks() {
    }

    public static boolean shouldOverrideSeat(Entity mount) {
        if (mount == null) {
            return false;
        }
        if (mount instanceof LivingEntity living && NativeRideCapabilityResolver.hasNativePlayerControl(living)) {
            return false;
        }
        if (RideVisualAuthority.isClientAuthorized(mount.getUUID())) {
            return true;
        }
        EntityRideProfile profile = ClientRidingConfigCache.profile(entityId(mount));
        return ClientState.isGenericRideMount(mount.getId())
                || profile.mode() == RideMode.FORCE_GENERIC
                || profile.visualOverride();
    }


    public static RiderVisualProfile visualProfile(Entity mount) {
        if (mount == null) {
            return null;
        }
        if (mount instanceof LivingEntity living && NativeRideCapabilityResolver.hasNativePlayerControl(living)) {
            return null;
        }
        RiderVisualProfile authorized = RideVisualAuthority.clientProfile(mount.getUUID());
        if (authorized != null) {
            return authorized;
        }

        ResourceLocation id = entityId(mount);
        EntityRideProfile configured = ClientRidingConfigCache.profile(id);
        if (ClientRidingConfigCache.hasModifiedProfile(id) || !(mount instanceof LivingEntity living)) {
            return configured.visual();
        }
        for (RideCompatibilityApi.SeatProfileProvider provider : RideCompatibilityApi.seatProfileProviders()) {
            if (!RideCompatibilityApi.enabled(provider)) {
                continue;
            }
            try {
                if (!provider.supports(living)) {
                    continue;
                }
                RiderVisualProfile supplied = provider.profile(living);
                if (supplied != null) {
                    return supplied.validated();
                }
            } catch (RuntimeException exception) {
                if (RideCompatibilityApi.disableProvider(provider)) {
                    LOGGER.warn("Seat profile provider {} failed and was disabled.", provider.getClass().getName(), exception);
                }
            }
        }
        return configured.visual();
    }

    public static RiderVisualProfile profileForRider(LivingEntity rendered) {
        if (!(rendered instanceof AbstractClientPlayer player)) {
            return null;
        }
        Entity vehicle = player.getVehicle();
        if (vehicle == null || !shouldOverrideSeat(vehicle)) {
            return null;
        }
        return visualProfile(vehicle);
    }

    public static boolean usesSittingBase(RiderVisualProfile profile) {
        if (profile == null) {
            return false;
        }
        RiderPosePreset preset = profile.posePreset();
        return preset == RiderPosePreset.SIT_NORMAL
                || preset == RiderPosePreset.SIT_FORWARD
                || preset == RiderPosePreset.SIT_WIDE;
    }


    /**
     * Applies TDMC-specific adjustments after vanilla has already selected the
     * riding/standing base pose. The base riding boolean itself is controlled
     * from LivingEntityRenderer in the same place Salvation controls it.
     */
    public static void applyProfilePose(PlayerModel<?> model, RiderVisualProfile profile) {
        if (model == null || profile == null) {
            return;
        }

        applyPresetAdjustments(model, profile.posePreset());
        model.body.xRot += profile.bodyPitch() * Mth.DEG_TO_RAD;
        model.body.yRot += profile.bodyYawOffset() * Mth.DEG_TO_RAD;
        addRotation(model.leftLeg, profile.leftLeg());
        addRotation(model.rightLeg, profile.rightLeg());
        addRotation(model.leftArm, profile.leftArm());
        addRotation(model.rightArm, profile.rightArm());

        model.leftPants.copyFrom(model.leftLeg);
        model.rightPants.copyFrom(model.rightLeg);
        model.leftSleeve.copyFrom(model.leftArm);
        model.rightSleeve.copyFrom(model.rightArm);
        model.jacket.copyFrom(model.body);
    }

    private static void applyPresetAdjustments(PlayerModel<?> model, RiderPosePreset preset) {
        switch (preset) {
            case SIT_NORMAL -> {
                // Vanilla riding pose supplied by LivingEntityRenderer/model.riding.
            }
            case SIT_FORWARD -> {
                model.body.xRot += 0.28F;
                model.leftArm.xRot -= 0.18F;
                model.rightArm.xRot -= 0.18F;
            }
            case SIT_WIDE -> {
                model.leftLeg.yRot += 0.28F;
                model.rightLeg.yRot -= 0.28F;
            }
            case CROUCH -> {
                model.body.xRot += 0.5F;
                model.leftLeg.xRot -= 0.8F;
                model.rightLeg.xRot -= 0.8F;
            }
            case STAND, CUSTOM -> {
                // The renderer selects a non-riding base pose; custom angles below
                // are applied directly to that base.
            }
        }
    }

    private static void addRotation(net.minecraft.client.model.geom.ModelPart part, RiderVisualProfile.LimbRotation rotation) {
        part.xRot += rotation.pitch() * Mth.DEG_TO_RAD;
        part.yRot += rotation.yaw() * Mth.DEG_TO_RAD;
        part.zRot += rotation.roll() * Mth.DEG_TO_RAD;
    }

    public static RideCameraContext cameraContext(Entity cameraEntity) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return null;
        }
        Entity mount = player.getVehicle();
        if (mount == null || !shouldOverrideSeat(mount)) {
            return null;
        }
        boolean supportedCameraEntity = cameraEntity == player || cameraEntity == mount;
        if (!supportedCameraEntity) {
            return null;
        }
        return new RideCameraContext(mount, player);
    }

    public static Vec3 riderEyePosition(LocalPlayer player, float partialTick) {
        return player == null ? Vec3.ZERO : player.getEyePosition(partialTick);
    }

    public static Vec3 cameraAnchorCorrection(Entity cameraEntity, LocalPlayer player, float partialTick) {
        if (cameraEntity == null || player == null || cameraEntity == player) {
            return Vec3.ZERO;
        }
        return player.getEyePosition(partialTick).subtract(cameraEntity.getEyePosition(partialTick));
    }

    public static Vec3 cameraOffset(Entity mount) {
        RiderVisualProfile profile = visualProfile(mount);
        if (profile == null) {
            return Vec3.ZERO;
        }
        double radians = Math.toRadians(mount.getYRot());
        double back = profile.cameraBackwardOffset();
        double x = Math.sin(radians) * back;
        double z = -Math.cos(radians) * back;
        return new Vec3(x, profile.cameraVerticalOffset(), z);
    }

    public record RideCameraContext(Entity mount, LocalPlayer rider) {
    }

    private static ResourceLocation entityId(Entity entity) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (id == null) {
            throw new IllegalStateException("Unregistered entity type");
        }
        return id;
    }
}
