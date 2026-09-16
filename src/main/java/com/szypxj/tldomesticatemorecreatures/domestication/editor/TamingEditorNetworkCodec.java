package com.szypxj.tldomesticatemorecreatures.domestication.editor;

import com.szypxj.tldomesticatemorecreatures.domestication.TamingMethod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TamingEditorNetworkCodec {
    private static final int MAX_ENTITIES = 65536;
    private static final int MAX_FOODS = 8192;

    private TamingEditorNetworkCodec() {
    }

    public static void writeSnapshot(FriendlyByteBuf buffer, TamingEditorSnapshot snapshot) {
        buffer.writeVarLong(snapshot.generation());
        buffer.writeVarInt(snapshot.entities().size());
        for (TamingEditorEntityInfo info : snapshot.entities()) {
            buffer.writeResourceLocation(info.entityId());
            buffer.writeBoolean(info.affected());
        }
        buffer.writeVarInt(snapshot.rules().size());
        for (TamingEditorRule rule : snapshot.rules().values()) {
            writeRule(buffer, rule);
        }
    }

    public static TamingEditorSnapshot readSnapshot(FriendlyByteBuf buffer) {
        long generation = buffer.readVarLong();
        int entityCount = bounded(buffer.readVarInt(), MAX_ENTITIES);
        List<TamingEditorEntityInfo> entities = new ArrayList<>(entityCount);
        for (int i = 0; i < entityCount; i++) {
            entities.add(new TamingEditorEntityInfo(buffer.readResourceLocation(), buffer.readBoolean()));
        }
        int ruleCount = bounded(buffer.readVarInt(), MAX_ENTITIES);
        Map<ResourceLocation, TamingEditorRule> rules = new LinkedHashMap<>();
        for (int i = 0; i < ruleCount; i++) {
            TamingEditorRule rule = readRule(buffer);
            rules.put(rule.entityId(), rule);
        }
        return new TamingEditorSnapshot(generation, entities, rules);
    }

    public static void writeRule(FriendlyByteBuf buffer, TamingEditorRule rule) {
        buffer.writeResourceLocation(rule.entityId());
        buffer.writeEnum(rule.method());
        buffer.writeVarInt(rule.requiredPlayerLevel());
        writeAmounts(buffer, rule.nativeFoods());
        writeAmounts(buffer, rule.extraFoods());
        writeAmounts(buffer, rule.legacyFoods());
        buffer.writeVarInt(rule.removedNativeFoods().size());
        for (ResourceLocation itemId : rule.removedNativeFoods()) {
            buffer.writeResourceLocation(itemId);
        }
    }

    public static TamingEditorRule readRule(FriendlyByteBuf buffer) {
        ResourceLocation entityId = buffer.readResourceLocation();
        TamingMethod method = buffer.readEnum(TamingMethod.class);
        int requiredPlayerLevel = buffer.readVarInt();
        Map<ResourceLocation, Integer> nativeFoods = readAmounts(buffer);
        Map<ResourceLocation, Integer> extraFoods = readAmounts(buffer);
        Map<ResourceLocation, Integer> legacyFoods = readAmounts(buffer);
        int removedCount = bounded(buffer.readVarInt(), MAX_FOODS);
        Set<ResourceLocation> removed = new LinkedHashSet<>();
        for (int i = 0; i < removedCount; i++) {
            removed.add(buffer.readResourceLocation());
        }
        return new TamingEditorRule(entityId, method, requiredPlayerLevel, nativeFoods, extraFoods, legacyFoods, removed);
    }

    public static void writeAmounts(FriendlyByteBuf buffer, Map<ResourceLocation, Integer> amounts) {
        buffer.writeVarInt(amounts.size());
        for (Map.Entry<ResourceLocation, Integer> entry : amounts.entrySet()) {
            buffer.writeResourceLocation(entry.getKey());
            buffer.writeVarInt(entry.getValue() == null ? 0 : Math.max(0, entry.getValue()));
        }
    }

    public static Map<ResourceLocation, Integer> readAmounts(FriendlyByteBuf buffer) {
        int count = bounded(buffer.readVarInt(), MAX_FOODS);
        Map<ResourceLocation, Integer> result = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            result.put(buffer.readResourceLocation(), buffer.readVarInt());
        }
        return Map.copyOf(result);
    }

    public static void writeIds(FriendlyByteBuf buffer, Set<ResourceLocation> ids) {
        buffer.writeVarInt(ids.size());
        for (ResourceLocation id : ids) {
            buffer.writeResourceLocation(id);
        }
    }

    public static Set<ResourceLocation> readIds(FriendlyByteBuf buffer) {
        int count = bounded(buffer.readVarInt(), MAX_ENTITIES);
        Set<ResourceLocation> result = new LinkedHashSet<>();
        for (int i = 0; i < count; i++) {
            result.add(buffer.readResourceLocation());
        }
        return Set.copyOf(result);
    }

    private static int bounded(int value, int maximum) {
        if (value < 0 || value > maximum) {
            throw new IllegalArgumentException("Invalid network collection size: " + value);
        }
        return value;
    }
}
