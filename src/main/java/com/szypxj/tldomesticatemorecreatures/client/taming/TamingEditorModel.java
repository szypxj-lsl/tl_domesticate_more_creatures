package com.szypxj.tldomesticatemorecreatures.client.taming;

import com.szypxj.tldomesticatemorecreatures.domestication.TamingMethod;
import com.szypxj.tldomesticatemorecreatures.domestication.editor.TamingEditorEntityInfo;
import com.szypxj.tldomesticatemorecreatures.domestication.editor.TamingEditorRule;
import com.szypxj.tldomesticatemorecreatures.domestication.editor.TamingEditorService;
import com.szypxj.tldomesticatemorecreatures.domestication.editor.TamingEditorSnapshot;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TamingEditorModel {
    private TamingEditorSnapshot serverSnapshot;
    private final Map<ResourceLocation, TamingEditorEntityInfo> entityInfo = new LinkedHashMap<>();
    private final Map<ResourceLocation, TamingEditorRule> drafts = new LinkedHashMap<>();
    private final Set<ResourceLocation> dirty = new LinkedHashSet<>();
    private final Set<ResourceLocation> deleted = new LinkedHashSet<>();
    private final Map<ResourceLocation, TamingEditorService.NativeFoodsResult> nativeFoods = new LinkedHashMap<>();
    private final Set<ResourceLocation> nativePending = new LinkedHashSet<>();
    private ResourceLocation selected;

    public TamingEditorModel(TamingEditorSnapshot snapshot) {
        applySnapshot(snapshot);
    }

    public void applySnapshot(TamingEditorSnapshot snapshot) {
        serverSnapshot = snapshot;
        entityInfo.clear();
        for (TamingEditorEntityInfo info : snapshot.entities()) {
            entityInfo.put(info.entityId(), info);
        }
        drafts.clear();
        drafts.putAll(snapshot.rules());
        dirty.clear();
        deleted.clear();
        nativePending.clear();
        if (selected != null && !entityInfo.containsKey(selected)) {
            selected = null;
        }
    }

    public TamingEditorSnapshot serverSnapshot() {
        return serverSnapshot;
    }

    public List<TamingEditorEntityInfo> entities() {
        return List.copyOf(entityInfo.values());
    }

    public boolean affected(ResourceLocation id) {
        TamingEditorEntityInfo info = entityInfo.get(id);
        return info != null && info.affected();
    }

    public ResourceLocation selected() {
        return selected;
    }

    public void select(ResourceLocation id) {
        if (id != null && entityInfo.containsKey(id)) {
            selected = id;
        }
    }

    public boolean hasRule(ResourceLocation id) {
        return id != null && !deleted.contains(id) && drafts.containsKey(id);
    }

    public TamingEditorRule rule(ResourceLocation id) {
        return hasRule(id) ? drafts.get(id) : null;
    }

    public void enable(ResourceLocation id) {
        if (id == null || !affected(id) || hasRule(id)) {
            return;
        }
        drafts.put(id, TamingEditorRule.defaults(id));
        deleted.remove(id);
        dirty.add(id);
    }

    public void delete(ResourceLocation id) {
        if (id == null || !hasRule(id)) {
            return;
        }
        drafts.remove(id);
        dirty.remove(id);
        if (serverSnapshot.rules().containsKey(id)) {
            deleted.add(id);
        } else {
            deleted.remove(id);
        }
    }

    public void updateRule(TamingEditorRule rule) {
        if (rule == null || rule.entityId() == null) {
            return;
        }
        drafts.put(rule.entityId(), rule);
        deleted.remove(rule.entityId());
        dirty.add(rule.entityId());
    }

    public void cycleMethod(ResourceLocation id) {
        TamingEditorRule rule = rule(id);
        if (rule == null) {
            return;
        }
        updateRule(rule.withMethod(rule.method() == TamingMethod.FEED ? TamingMethod.KNOCKOUT : TamingMethod.FEED));
    }

    public void setRequiredLevel(ResourceLocation id, int level) {
        TamingEditorRule rule = rule(id);
        if (rule != null) {
            updateRule(rule.withRequiredPlayerLevel(Math.max(1, level)));
        }
    }

    public void setNativeAmount(ResourceLocation entityId, ResourceLocation itemId, int amount) {
        TamingEditorRule rule = rule(entityId);
        if (rule == null || itemId == null) {
            return;
        }
        Map<ResourceLocation, Integer> copy = new LinkedHashMap<>(rule.nativeFoods());
        if (amount < 1) {
            copy.remove(itemId);
        } else {
            copy.put(itemId, amount);
        }
        updateRule(rule.withNativeFoods(copy));
    }

    public void excludeNative(ResourceLocation entityId, ResourceLocation itemId) {
        TamingEditorRule rule = rule(entityId);
        if (rule == null || itemId == null) {
            return;
        }
        Map<ResourceLocation, Integer> nativeCopy = new LinkedHashMap<>(rule.nativeFoods());
        nativeCopy.remove(itemId);
        Set<ResourceLocation> removedCopy = new LinkedHashSet<>(rule.removedNativeFoods());
        removedCopy.add(itemId);
        updateRule(rule.withNativeFoods(nativeCopy).withRemovedNativeFoods(removedCopy));
    }

    public void restoreNative(ResourceLocation entityId, ResourceLocation itemId) {
        TamingEditorRule rule = rule(entityId);
        if (rule == null || itemId == null) {
            return;
        }
        Set<ResourceLocation> copy = new LinkedHashSet<>(rule.removedNativeFoods());
        copy.remove(itemId);
        updateRule(rule.withRemovedNativeFoods(copy));
    }

    public void addExtra(ResourceLocation entityId, ResourceLocation itemId) {
        TamingEditorRule rule = rule(entityId);
        if (rule == null || itemId == null || rule.extraFoods().containsKey(itemId)) {
            return;
        }
        Map<ResourceLocation, Integer> copy = new LinkedHashMap<>(rule.extraFoods());
        copy.put(itemId, 0);
        updateRule(rule.withExtraFoods(copy));
    }

    public void setExtraAmount(ResourceLocation entityId, ResourceLocation itemId, int amount) {
        TamingEditorRule rule = rule(entityId);
        if (rule == null || itemId == null || !rule.extraFoods().containsKey(itemId)) {
            return;
        }
        Map<ResourceLocation, Integer> copy = new LinkedHashMap<>(rule.extraFoods());
        copy.put(itemId, Math.max(0, amount));
        updateRule(rule.withExtraFoods(copy));
    }

    public void removeExtra(ResourceLocation entityId, ResourceLocation itemId) {
        TamingEditorRule rule = rule(entityId);
        if (rule == null || itemId == null) {
            return;
        }
        Map<ResourceLocation, Integer> copy = new LinkedHashMap<>(rule.extraFoods());
        copy.remove(itemId);
        updateRule(rule.withExtraFoods(copy));
    }

    public boolean hasNativeFoods(ResourceLocation id) {
        return nativeFoods.containsKey(id);
    }

    public boolean nativePending(ResourceLocation id) {
        return nativePending.contains(id);
    }

    public void markNativePending(ResourceLocation id) {
        if (id != null && !nativeFoods.containsKey(id)) {
            nativePending.add(id);
        }
    }

    public void applyNativeFoods(ResourceLocation id, TamingEditorService.NativeFoodsResult result) {
        if (id == null || result == null) {
            return;
        }
        nativePending.remove(id);
        nativeFoods.put(id, result);
        if (result.success() && hasRule(id)) {
            Set<ResourceLocation> nativeSet = Set.copyOf(result.items());
            TamingEditorRule normalized = drafts.get(id).normalizeLegacy(nativeSet);
            drafts.put(id, normalized);
        }
    }

    public TamingEditorService.NativeFoodsResult nativeFoods(ResourceLocation id) {
        return nativeFoods.get(id);
    }

    public List<ResourceLocation> nativeItems(ResourceLocation id) {
        TamingEditorService.NativeFoodsResult result = nativeFoods.get(id);
        return result == null ? List.of() : result.items();
    }

    public boolean isDirty(ResourceLocation id) {
        return dirty.contains(id) || deleted.contains(id);
    }

    public boolean anyDirty() {
        return !dirty.isEmpty() || !deleted.isEmpty();
    }

    public Map<ResourceLocation, TamingEditorRule> dirtyUpserts() {
        Map<ResourceLocation, TamingEditorRule> result = new LinkedHashMap<>();
        for (ResourceLocation id : dirty) {
            TamingEditorRule rule = drafts.get(id);
            if (rule != null) {
                result.put(id, rule);
            }
        }
        return Map.copyOf(result);
    }

    public Set<ResourceLocation> deletes() {
        return Set.copyOf(deleted);
    }

    public String validationKey(ResourceLocation id) {
        TamingEditorRule rule = rule(id);
        if (rule == null) {
            return "";
        }
        TamingEditorService.NativeFoodsResult nativeResult = nativeFoods.get(id);
        if (nativeResult == null) {
            return "gui.tl_domesticate_more_creatures.taming_editor.validation.waiting_native";
        }
        if (!nativeResult.success() && (!rule.nativeFoods().isEmpty() || !rule.removedNativeFoods().isEmpty())) {
            return "gui.tl_domesticate_more_creatures.taming_editor.validation.native_failed";
        }
        for (Integer amount : rule.extraFoods().values()) {
            if (amount == null || amount < 1) {
                return "gui.tl_domesticate_more_creatures.taming_editor.validation.extra_amount";
            }
        }
        Set<ResourceLocation> nativeSet = new LinkedHashSet<>(nativeResult.items());
        for (ResourceLocation extra : rule.extraFoods().keySet()) {
            if (nativeSet.contains(extra)) {
                return "gui.tl_domesticate_more_creatures.taming_editor.validation.food_conflict";
            }
        }
        boolean effective = false;
        for (ResourceLocation nativeId : nativeSet) {
            if (rule.removedNativeFoods().contains(nativeId)) {
                continue;
            }
            Integer amount = rule.nativeFoods().get(nativeId);
            if (amount != null && amount > 0) {
                effective = true;
                break;
            }
        }
        if (!effective) {
            for (Integer amount : rule.extraFoods().values()) {
                if (amount != null && amount > 0) {
                    effective = true;
                    break;
                }
            }
        }
        if (!effective) {
            for (Map.Entry<ResourceLocation, Integer> entry : rule.legacyFoods().entrySet()) {
                if (entry.getValue() != null && entry.getValue() > 0
                        && (!nativeSet.contains(entry.getKey()) || !rule.removedNativeFoods().contains(entry.getKey()))) {
                    effective = true;
                    break;
                }
            }
        }
        return effective ? "" : "gui.tl_domesticate_more_creatures.taming_editor.validation.no_food";
    }

    public ValidationIssue validateAllDirtyIssue() {
        for (ResourceLocation id : new ArrayList<>(dirty)) {
            String key = validationKey(id);
            if (!key.isEmpty()) {
                return new ValidationIssue(id, key);
            }
        }
        return ValidationIssue.none();
    }

    public String validateAllDirty() {
        return validateAllDirtyIssue().messageKey();
    }

    public record ValidationIssue(ResourceLocation entityId, String messageKey) {
        public ValidationIssue {
            messageKey = messageKey == null ? "" : messageKey;
        }

        public static ValidationIssue none() {
            return new ValidationIssue(null, "");
        }

        public boolean valid() {
            return messageKey.isEmpty();
        }
    }
}
