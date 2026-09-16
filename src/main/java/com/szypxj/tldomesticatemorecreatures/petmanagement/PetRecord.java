package com.szypxj.tldomesticatemorecreatures.petmanagement;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public final class PetRecord {
    private static final String PET_UUID = "petUuid";
    private static final String OWNER_UUID = "ownerUuid";
    private static final String ENTITY_TYPE = "entityTypeId";
    private static final String DISPLAY_NAME = "displayName";
    private static final String LEVEL = "level";
    private static final String STATE = "state";
    private static final String SHORTCUT = "shortcutSlot";
    private static final String RIDEABLE = "rideable";
    private static final String LAST_DIMENSION = "lastDimension";
    private static final String LAST_X = "lastX";
    private static final String LAST_Y = "lastY";
    private static final String LAST_Z = "lastZ";
    private static final String LAST_SEEN = "lastSeenGameTime";
    private static final String STORED_TAG = "storedEntityTag";
    private static final String DEAD_TAG = "deadEntityTag";

    private final UUID petUuid;
    private UUID ownerUuid;
    private ResourceLocation entityTypeId;
    private String displayName;
    private int level;
    private PetRecordState state;
    private int shortcutSlot;
    private boolean rideable;
    private ResourceLocation lastDimension;
    private double lastX;
    private double lastY;
    private double lastZ;
    private long lastSeenGameTime;
    private CompoundTag storedEntityTag;
    private CompoundTag deadEntityTag;

    public PetRecord(UUID petUuid, UUID ownerUuid, ResourceLocation entityTypeId, String displayName) {
        this.petUuid = petUuid;
        this.ownerUuid = ownerUuid;
        this.entityTypeId = entityTypeId;
        this.displayName = displayName == null ? "" : displayName;
        this.level = 1;
        this.state = PetRecordState.WORLD;
    }

    public UUID petUuid() {
        return petUuid;
    }

    public UUID ownerUuid() {
        return ownerUuid;
    }

    public void ownerUuid(UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
    }

    public ResourceLocation entityTypeId() {
        return entityTypeId;
    }

    public void entityTypeId(ResourceLocation entityTypeId) {
        this.entityTypeId = entityTypeId;
    }

    public String displayName() {
        return displayName;
    }

    public void displayName(String displayName) {
        this.displayName = displayName == null ? "" : displayName;
    }

    public int level() {
        return Math.max(1, level);
    }

    public void level(int level) {
        this.level = Math.max(1, level);
    }

    public PetRecordState state() {
        return state;
    }

    public void state(PetRecordState state) {
        this.state = state == null ? PetRecordState.UNLOCATED : state;
    }

    public boolean rideable() {
        return rideable;
    }

    public void rideable(boolean rideable) {
        this.rideable = rideable;
    }

    public int shortcutSlot() {
        return shortcutSlot;
    }

    public void shortcutSlot(int shortcutSlot) {
        this.shortcutSlot = shortcutSlot >= 1 && shortcutSlot <= 9 ? shortcutSlot : 0;
    }

    public ResourceLocation lastDimension() {
        return lastDimension;
    }

    public double lastX() {
        return lastX;
    }

    public double lastY() {
        return lastY;
    }

    public double lastZ() {
        return lastZ;
    }

    public long lastSeenGameTime() {
        return lastSeenGameTime;
    }

    public void lastLocation(ResourceLocation dimension, double x, double y, double z, long gameTime) {
        this.lastDimension = dimension;
        this.lastX = x;
        this.lastY = y;
        this.lastZ = z;
        this.lastSeenGameTime = gameTime;
    }

    public CompoundTag storedEntityTag() {
        return storedEntityTag == null ? null : storedEntityTag.copy();
    }

    public void storedEntityTag(CompoundTag tag) {
        this.storedEntityTag = tag == null ? null : tag.copy();
    }

    public CompoundTag deadEntityTag() {
        return deadEntityTag == null ? null : deadEntityTag.copy();
    }

    public void deadEntityTag(CompoundTag tag) {
        this.deadEntityTag = tag == null ? null : tag.copy();
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID(PET_UUID, petUuid);
        if (ownerUuid != null) tag.putUUID(OWNER_UUID, ownerUuid);
        if (entityTypeId != null) tag.putString(ENTITY_TYPE, entityTypeId.toString());
        tag.putString(DISPLAY_NAME, displayName);
        tag.putInt(LEVEL, level());
        tag.putString(STATE, state().name());
        tag.putInt(SHORTCUT, shortcutSlot());
        tag.putBoolean(RIDEABLE, rideable);
        if (lastDimension != null) tag.putString(LAST_DIMENSION, lastDimension.toString());
        tag.putDouble(LAST_X, lastX);
        tag.putDouble(LAST_Y, lastY);
        tag.putDouble(LAST_Z, lastZ);
        tag.putLong(LAST_SEEN, lastSeenGameTime);
        if (storedEntityTag != null) tag.put(STORED_TAG, storedEntityTag.copy());
        if (deadEntityTag != null) tag.put(DEAD_TAG, deadEntityTag.copy());
        return tag;
    }

    public static PetRecord load(CompoundTag tag) {
        if (tag == null || !tag.hasUUID(PET_UUID)) return null;
        UUID petUuid = tag.getUUID(PET_UUID);
        UUID ownerUuid = tag.hasUUID(OWNER_UUID) ? tag.getUUID(OWNER_UUID) : null;
        ResourceLocation typeId = ResourceLocation.tryParse(tag.getString(ENTITY_TYPE));
        PetRecord record = new PetRecord(petUuid, ownerUuid, typeId, tag.getString(DISPLAY_NAME));
        record.level(tag.getInt(LEVEL));
        try {
            record.state(PetRecordState.valueOf(tag.getString(STATE)));
        } catch (IllegalArgumentException ignored) {
            record.state(PetRecordState.UNLOCATED);
        }
        record.shortcutSlot(tag.getInt(SHORTCUT));
        record.rideable(tag.getBoolean(RIDEABLE));
        ResourceLocation dimension = ResourceLocation.tryParse(tag.getString(LAST_DIMENSION));
        record.lastLocation(dimension, tag.getDouble(LAST_X), tag.getDouble(LAST_Y), tag.getDouble(LAST_Z), tag.getLong(LAST_SEEN));
        if (tag.contains(STORED_TAG, Tag.TAG_COMPOUND)) record.storedEntityTag(tag.getCompound(STORED_TAG));
        if (tag.contains(DEAD_TAG, Tag.TAG_COMPOUND)) record.deadEntityTag(tag.getCompound(DEAD_TAG));
        return record;
    }
}
