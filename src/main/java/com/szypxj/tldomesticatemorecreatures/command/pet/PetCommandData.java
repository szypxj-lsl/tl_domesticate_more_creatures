package com.szypxj.tldomesticatemorecreatures.command.pet;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

public final class PetCommandData {
    private static final String ROOT_KEY = "tl_domesticate_more_creatures_pet_command";
    private final CompoundTag root;

    private PetCommandData(CompoundTag root) {
        this.root = root;
    }

    public static PetCommandData of(LivingEntity entity) {
        CompoundTag persistent = entity.getPersistentData();
        if (!persistent.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            persistent.put(ROOT_KEY, new CompoundTag());
        }
        return new PetCommandData(persistent.getCompound(ROOT_KEY));
    }

    public boolean active() {
        if (!root.getBoolean("active")) {
            return false;
        }
        if (!root.contains("command", Tag.TAG_STRING)) {
            clear();
            return false;
        }
        try {
            PetCommand.valueOf(root.getString("command"));
            return true;
        } catch (IllegalArgumentException ignored) {
            clear();
            return false;
        }
    }

    public PetCommand command() {
        if (!active()) {
            return PetCommand.MOVE;
        }
        return PetCommand.valueOf(root.getString("command"));
    }

    public Optional<UUID> targetUuid() {
        return root.hasUUID("targetUuid") ? Optional.of(root.getUUID("targetUuid")) : Optional.empty();
    }

    public Optional<UUID> markerUuid() {
        return root.hasUUID("markerUuid") ? Optional.of(root.getUUID("markerUuid")) : Optional.empty();
    }

    public void updateTargetUuid(UUID targetUuid) {
        if (targetUuid == null) {
            root.remove("targetUuid");
        } else {
            root.putUUID("targetUuid", targetUuid);
        }
    }

    public long lastCombatIntentPulse() {
        return root.contains("lastCombatIntentPulse", Tag.TAG_LONG)
                ? root.getLong("lastCombatIntentPulse")
                : Long.MIN_VALUE;
    }

    public void markCombatIntentPulse(long gameTime) {
        root.putLong("lastCombatIntentPulse", gameTime);
    }

    public Vec3 position() {
        return new Vec3(root.getDouble("x"), root.getDouble("y"), root.getDouble("z"));
    }

    public void updatePosition(Vec3 position) {
        Vec3 safePosition = position == null ? Vec3.ZERO : position;
        root.putDouble("x", safePosition.x);
        root.putDouble("y", safePosition.y);
        root.putDouble("z", safePosition.z);
    }

    public void set(PetCommand command, Vec3 position, UUID targetUuid) {
        set(command, position, targetUuid, null);
    }

    public void set(PetCommand command, Vec3 position, UUID targetUuid, UUID markerUuid) {
        root.putBoolean("active", true);
        root.putString("command", command.name());
        root.remove("lastCombatIntentPulse");
        Vec3 safePosition = position == null ? Vec3.ZERO : position;
        root.putDouble("x", safePosition.x);
        root.putDouble("y", safePosition.y);
        root.putDouble("z", safePosition.z);
        if (targetUuid == null) {
            root.remove("targetUuid");
        } else {
            root.putUUID("targetUuid", targetUuid);
        }
        if (markerUuid == null) {
            root.remove("markerUuid");
        } else {
            root.putUUID("markerUuid", markerUuid);
        }
    }

    public void clear() {
        root.putBoolean("active", false);
        root.remove("command");
        root.remove("targetUuid");
        root.remove("markerUuid");
        root.remove("x");
        root.remove("y");
        root.remove("z");
        root.remove("expiresAt");
        root.remove("lastCombatIntentPulse");
    }
}
