package com.szypxj.tldomesticatemorecreatures.torpor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;

public final class TorporData {
    private static final String ROOT_KEY = "tl_domesticate_more_creatures_torpor";
    private final CompoundTag root;

    private TorporData(CompoundTag root) {
        this.root = root;
    }

    public static TorporData of(LivingEntity entity) {
        CompoundTag persistent = entity.getPersistentData();
        if (!persistent.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            persistent.put(ROOT_KEY, new CompoundTag());
        }
        return new TorporData(persistent.getCompound(ROOT_KEY));
    }

    public static boolean exists(LivingEntity entity) {
        return entity.getPersistentData().contains(ROOT_KEY, Tag.TAG_COMPOUND);
    }

    public static void remove(LivingEntity entity) {
        entity.getPersistentData().remove(ROOT_KEY);
    }

    public double current() {
        return Math.max(0.0D, root.getDouble("current"));
    }

    public void current(double value) {
        root.putDouble("current", Math.max(0.0D, value));
    }

    public boolean unconscious() {
        return root.getBoolean("unconscious");
    }

    public void unconscious(boolean value) {
        root.putBoolean("unconscious", value);
    }

    public long lastUpdateGameTime() {
        return root.getLong("lastUpdateGameTime");
    }

    public void lastUpdateGameTime(long value) {
        root.putLong("lastUpdateGameTime", Math.max(0L, value));
    }

    public long lastIncreaseGameTime() {
        return root.getLong("lastIncreaseGameTime");
    }

    public void lastIncreaseGameTime(long value) {
        root.putLong("lastIncreaseGameTime", Math.max(0L, value));
    }

    public boolean previousNoAi() {
        return root.getBoolean("previousNoAi");
    }

    public void previousNoAi(boolean value) {
        root.putBoolean("previousNoAi", value);
    }

    public boolean previousNoAiStored() {
        return root.getBoolean("previousNoAiStored");
    }

    public void previousNoAiStored(boolean value) {
        root.putBoolean("previousNoAiStored", value);
    }

    public boolean darknessAppliedByTorpor() {
        return root.getBoolean("darknessAppliedByTorpor");
    }

    public void darknessAppliedByTorpor(boolean value) {
        root.putBoolean("darknessAppliedByTorpor", value);
    }

    public void clear() {
        current(0.0D);
        unconscious(false);
        lastUpdateGameTime(0L);
        lastIncreaseGameTime(0L);
        previousNoAiStored(false);
        darknessAppliedByTorpor(false);
    }
}
