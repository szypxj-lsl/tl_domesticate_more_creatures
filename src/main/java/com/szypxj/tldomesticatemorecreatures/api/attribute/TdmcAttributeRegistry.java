package com.szypxj.tldomesticatemorecreatures.api.attribute;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

public final class TdmcAttributeRegistry {
    private static final Map<ResourceLocation, TdmcAttributeHandle> entries = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, List<UnaryOperator<TdmcAttributeDefinition>>> pendingModifiers = new ConcurrentHashMap<>();
    private static final Set<ResourceLocation> suppressedIds = ConcurrentHashMap.newKeySet();
    private static volatile List<TdmcAttributeHandle> snapshot = List.of();
    private static volatile boolean ready;

    private TdmcAttributeRegistry() {
    }

    public static synchronized TdmcAttributeHandle register(String ownerModId, TdmcAttributeDefinition definition) {
        requireOwner(ownerModId);
        if (entries.containsKey(definition.id())) {
            TdmcAttributeHandle current = entries.get(definition.id());
            throw new IllegalStateException("Attribute already registered: " + definition.id() + " by " + current.ownerModId());
        }
        suppressedIds.remove(definition.id());
        TdmcAttributeDefinition resolved = applyPending(definition);
        TdmcAttributeHandle handle = new TdmcAttributeHandle(ownerModId, resolved);
        entries.put(resolved.id(), handle);
        rebuildSnapshot();
        return handle;
    }

    public static synchronized TdmcAttributeHandle registerIfAbsent(String ownerModId, TdmcAttributeDefinition definition) {
        requireOwner(ownerModId);
        TdmcAttributeHandle existing = entries.get(definition.id());
        if (existing != null) {
            return existing;
        }
        if (suppressedIds.contains(definition.id())) {
            return null;
        }
        TdmcAttributeDefinition resolved = applyPending(definition);
        TdmcAttributeHandle handle = new TdmcAttributeHandle(ownerModId, resolved);
        entries.put(resolved.id(), handle);
        rebuildSnapshot();
        return handle;
    }

    public static Optional<TdmcAttributeDefinition> get(ResourceLocation id) {
        TdmcAttributeHandle handle = entries.get(id);
        return handle == null ? Optional.empty() : Optional.of(handle.definition());
    }

    public static Optional<TdmcAttributeHandle> getHandle(ResourceLocation id) {
        return Optional.ofNullable(entries.get(id));
    }

    public static boolean contains(ResourceLocation id) {
        return id != null && entries.containsKey(id);
    }

    public static List<TdmcAttributeHandle> getAll() {
        return snapshot;
    }

    public static List<TdmcAttributeDefinition> definitions() {
        return snapshot.stream().map(TdmcAttributeHandle::definition).toList();
    }

    public static synchronized boolean modify(ResourceLocation id, UnaryOperator<TdmcAttributeDefinition> modifier) {
        if (id == null || modifier == null) {
            return false;
        }
        TdmcAttributeHandle current = entries.get(id);
        if (current == null) {
            pendingModifiers.computeIfAbsent(id, ignored -> new ArrayList<>()).add(modifier);
            return true;
        }
        TdmcAttributeDefinition changed = modifier.apply(current.definition());
        validateReplacement(id, changed);
        entries.put(id, new TdmcAttributeHandle(current.ownerModId(), changed));
        rebuildSnapshot();
        return true;
    }

    public static boolean setVisible(ResourceLocation id, boolean visible) {
        return modify(id, definition -> definition.withFlags(definition.flags().withVisible(visible)));
    }

    public static synchronized Optional<TdmcAttributeHandle> unregister(ResourceLocation id) {
        if (id == null) {
            return Optional.empty();
        }
        TdmcAttributeHandle removed = entries.remove(id);
        pendingModifiers.remove(id);
        suppressedIds.add(id);
        rebuildSnapshot();
        return Optional.ofNullable(removed);
    }

    public static boolean isSuppressed(ResourceLocation id) {
        return id != null && suppressedIds.contains(id);
    }

    public static synchronized void allowRegistration(ResourceLocation id) {
        if (id != null) {
            suppressedIds.remove(id);
        }
    }

    public static Optional<String> owner(ResourceLocation id) {
        TdmcAttributeHandle handle = entries.get(id);
        return handle == null ? Optional.empty() : Optional.of(handle.ownerModId());
    }

    public static boolean ready() {
        return ready;
    }

    static void markReady() {
        ready = true;
    }

    private static TdmcAttributeDefinition applyPending(TdmcAttributeDefinition definition) {
        List<UnaryOperator<TdmcAttributeDefinition>> modifiers = pendingModifiers.remove(definition.id());
        TdmcAttributeDefinition result = definition;
        if (modifiers != null) {
            for (UnaryOperator<TdmcAttributeDefinition> modifier : modifiers) {
                result = modifier.apply(result);
                validateReplacement(definition.id(), result);
            }
        }
        return result;
    }

    private static void validateReplacement(ResourceLocation expectedId, TdmcAttributeDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("Attribute modifier returned null for " + expectedId);
        }
        if (!expectedId.equals(definition.id())) {
            throw new IllegalArgumentException("Attribute id cannot be changed in-place: " + expectedId + " -> " + definition.id());
        }
    }

    private static void requireOwner(String ownerModId) {
        if (ownerModId == null || ownerModId.isBlank()) {
            throw new IllegalArgumentException("ownerModId must not be blank");
        }
    }

    private static void rebuildSnapshot() {
        snapshot = entries.values().stream()
                .sorted(Comparator
                        .comparingInt((TdmcAttributeHandle handle) -> handle.definition().displayOrder())
                        .thenComparing(handle -> handle.definition().id().toString()))
                .toList();
    }
}
