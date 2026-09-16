package com.szypxj.tldomesticatemorecreatures.imprint;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;

public final class ImprintData {
    public static final String ROOT_KEY = "tl_domesticate_more_creatures_imprint";
    public static final int CARE_COUNT = 3;
    private final CompoundTag root;

    private ImprintData(CompoundTag root) {
        this.root = root;
    }

    public static ImprintData of(LivingEntity entity) {
        CompoundTag persistent = entity.getPersistentData();
        if (!persistent.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            persistent.put(ROOT_KEY, new CompoundTag());
        }
        return new ImprintData(persistent.getCompound(ROOT_KEY));
    }

    public static boolean exists(LivingEntity entity) {
        return entity.getPersistentData().contains(ROOT_KEY, Tag.TAG_COMPOUND)
                && entity.getPersistentData().getCompound(ROOT_KEY).getBoolean("initialized");
    }

    public boolean initialized() {
        return root.getBoolean("initialized");
    }

    public void initialized(boolean value) {
        root.putBoolean("initialized", value);
    }

    public boolean active() {
        return root.getBoolean("active");
    }

    public void active(boolean value) {
        root.putBoolean("active", value);
    }

    public boolean finished() {
        return root.getBoolean("finished");
    }

    public void finished(boolean value) {
        root.putBoolean("finished", value);
    }

    public long startedAt() {
        return Math.max(0L, root.getLong("startedAt"));
    }

    public void startedAt(long value) {
        root.putLong("startedAt", Math.max(0L, value));
    }

    public long endsAt() {
        return Math.max(0L, root.getLong("endsAt"));
    }

    public void endsAt(long value) {
        root.putLong("endsAt", Math.max(0L, value));
    }

    public int completed() {
        return Math.max(0, Math.min(CARE_COUNT, root.getInt("completed")));
    }

    public void completed(int value) {
        root.putInt("completed", Math.max(0, Math.min(CARE_COUNT, value)));
    }

    public int finalPercent() {
        return Math.max(0, Math.min(100, root.getInt("finalPercent")));
    }

    public void finalPercent(int value) {
        root.putInt("finalPercent", Math.max(0, Math.min(100, value)));
    }

    public int levelCapBonus() {
        return Math.max(0, root.getInt("levelCapBonus"));
    }

    public void levelCapBonus(int value) {
        root.putInt("levelCapBonus", Math.max(0, value));
    }

    public boolean bonded() {
        return root.getBoolean("bonded");
    }

    public void bonded(boolean value) {
        root.putBoolean("bonded", value);
    }

    public long lastCareAt() {
        return root.contains("lastCareAt", Tag.TAG_LONG) ? root.getLong("lastCareAt") : Long.MIN_VALUE;
    }

    public void lastCareAt(long value) {
        root.putLong("lastCareAt", value);
    }

    public boolean startedAsBaby() {
        return root.getBoolean("startedAsBaby");
    }

    public void startedAsBaby(boolean value) {
        root.putBoolean("startedAsBaby", value);
    }

    public long needAt(int index) {
        return Math.max(0L, root.getLong(key(index, "At")));
    }

    public void needAt(int index, long value) {
        root.putLong(key(index, "At"), Math.max(0L, value));
    }

    public ImprintNeedType needType(int index) {
        String value = root.getString(key(index, "Type"));
        try {
            return ImprintNeedType.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return ImprintNeedType.PET;
        }
    }

    public void needType(int index, ImprintNeedType type) {
        root.putString(key(index, "Type"), type.name());
    }

    public String needFood(int index) {
        return root.getString(key(index, "Food"));
    }

    public void needFood(int index, String itemId) {
        String key = key(index, "Food");
        if (itemId == null || itemId.isBlank()) {
            root.remove(key);
        } else {
            root.putString(key, itemId);
        }
    }

    public boolean needCompleted(int index) {
        return root.getBoolean(key(index, "Done"));
    }

    public void needCompleted(int index, boolean value) {
        root.putBoolean(key(index, "Done"), value);
    }

    public int currentNeedIndex(long gameTime) {
        for (int i = 0; i < CARE_COUNT; i++) {
            if (!needCompleted(i) && needAt(i) <= gameTime) {
                return i;
            }
        }
        return -1;
    }

    public long nextNeedAt(long gameTime) {
        long next = Long.MAX_VALUE;
        for (int i = 0; i < CARE_COUNT; i++) {
            if (!needCompleted(i) && needAt(i) > gameTime) {
                next = Math.min(next, needAt(i));
            }
        }
        return next == Long.MAX_VALUE ? 0L : next;
    }

    private static String key(int index, String suffix) {
        if (index < 0 || index >= CARE_COUNT) {
            throw new IllegalArgumentException("invalid imprint need index: " + index);
        }
        return "need" + index + suffix;
    }
}
