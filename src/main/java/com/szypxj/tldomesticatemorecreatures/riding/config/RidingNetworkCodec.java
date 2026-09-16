package com.szypxj.tldomesticatemorecreatures.riding.config;

import com.szypxj.tldomesticatemorecreatures.riding.RideMode;
import com.szypxj.tldomesticatemorecreatures.riding.RideMovementMode;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.Set;

public final class RidingNetworkCodec {
    private RidingNetworkCodec() {
    }

    public static void writeSettings(FriendlyByteBuf buffer, RidingSettings settings) {
        RidingSettings value = settings.validated();
        buffer.writeEnum(value.filterMode());
        buffer.writeVarInt(value.filterEntries().size());
        for (ResourceLocation id : value.filterEntries()) {
            buffer.writeResourceLocation(id);
        }
    }

    public static RidingSettings readSettings(FriendlyByteBuf buffer) {
        RideFilterMode mode = buffer.readEnum(RideFilterMode.class);
        int size = Math.max(0, Math.min(65536, buffer.readVarInt()));
        Set<ResourceLocation> entries = new LinkedHashSet<>();
        for (int i = 0; i < size; i++) {
            entries.add(buffer.readResourceLocation());
        }
        return new RidingSettings(mode, entries).validated();
    }

    public static void writeProfile(FriendlyByteBuf buffer, EntityRideProfile profile) {
        EntityRideProfile value = profile.validated();
        buffer.writeResourceLocation(value.entityId());
        buffer.writeEnum(value.mode());
        buffer.writeEnum(value.movementMode());
        buffer.writeDouble(value.groundSpeedMultiplier());
        buffer.writeDouble(value.turnRateDegrees());
        buffer.writeDouble(value.acceleration());
        buffer.writeDouble(value.deceleration());
        buffer.writeDouble(value.jumpStrength());
        buffer.writeDouble(value.flightSpeedMultiplier());
        buffer.writeDouble(value.ascentSpeed());
        buffer.writeDouble(value.descentSpeed());
        buffer.writeDouble(value.swimSpeedMultiplier());
        buffer.writeBoolean(value.autoAttackWhileRidden());
        buffer.writeBoolean(value.visualOverride());
        writeVisual(buffer, value.visual());
    }

    public static EntityRideProfile readProfile(FriendlyByteBuf buffer) {
        ResourceLocation id = buffer.readResourceLocation();
        return new EntityRideProfile(
                id,
                buffer.readEnum(RideMode.class),
                buffer.readEnum(RideMovementMode.class),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                readVisual(buffer)
        ).validated();
    }

    public static void writeVisual(FriendlyByteBuf buffer, RiderVisualProfile visual) {
        RiderVisualProfile value = visual.validated();
        buffer.writeDouble(value.lateralRatio());
        buffer.writeDouble(value.verticalRatio());
        buffer.writeDouble(value.forwardRatio());
        buffer.writeDouble(value.offsetX());
        buffer.writeDouble(value.offsetY());
        buffer.writeDouble(value.offsetZ());
        buffer.writeEnum(value.posePreset());
        buffer.writeFloat(value.bodyPitch());
        buffer.writeFloat(value.bodyYawOffset());
        writeLimb(buffer, value.leftLeg());
        writeLimb(buffer, value.rightLeg());
        writeLimb(buffer, value.leftArm());
        writeLimb(buffer, value.rightArm());
        buffer.writeFloat(value.visualPlayerScale());
        buffer.writeDouble(value.cameraVerticalOffset());
        buffer.writeDouble(value.cameraBackwardOffset());
    }

    public static RiderVisualProfile readVisual(FriendlyByteBuf buffer) {
        return new RiderVisualProfile(
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readEnum(RiderPosePreset.class),
                buffer.readFloat(), buffer.readFloat(),
                readLimb(buffer), readLimb(buffer), readLimb(buffer), readLimb(buffer),
                buffer.readFloat(), buffer.readDouble(), buffer.readDouble()
        ).validated();
    }

    private static void writeLimb(FriendlyByteBuf buffer, RiderVisualProfile.LimbRotation value) {
        RiderVisualProfile.LimbRotation limb = value == null ? RiderVisualProfile.LimbRotation.ZERO : value;
        buffer.writeFloat(limb.pitch());
        buffer.writeFloat(limb.yaw());
        buffer.writeFloat(limb.roll());
    }

    private static RiderVisualProfile.LimbRotation readLimb(FriendlyByteBuf buffer) {
        return new RiderVisualProfile.LimbRotation(buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
    }
}
