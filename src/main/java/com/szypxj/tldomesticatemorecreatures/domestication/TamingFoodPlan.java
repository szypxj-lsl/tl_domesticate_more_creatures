package com.szypxj.tldomesticatemorecreatures.domestication;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class TamingFoodPlan {
    private TamingFoodPlan() {
    }

    static Result resolve(
            List<String> nativeItems,
            List<Spec> nativeFoods,
            List<Spec> extraFoods,
            List<Spec> legacyFoods,
            List<String> removedNativeFoods
    ) {
        Map<String, Integer> nativeAmounts = amounts(nativeFoods);
        Map<String, Integer> extraAmounts = amounts(extraFoods);
        Map<String, Integer> legacyAmounts = amounts(legacyFoods);
        Set<String> nativeSet = new LinkedHashSet<>(nativeItems);
        Set<String> removedSet = new LinkedHashSet<>(removedNativeFoods);
        List<Entry> display = new ArrayList<>();
        List<Entry> usable = new ArrayList<>();
        Set<String> added = new LinkedHashSet<>();

        for (String item : nativeItems) {
            if (removedSet.contains(item)) {
                added.add(item);
                continue;
            }
            int amount = firstPositive(nativeAmounts.get(item), legacyAmounts.get(item), extraAmounts.get(item));
            Entry entry = new Entry(item, amount, amount > 0);
            display.add(entry);
            if (entry.configured()) {
                usable.add(entry);
            }
            added.add(item);
        }

        appendExtras(display, usable, added, nativeSet, extraFoods);
        appendLegacyExtras(display, usable, added, nativeSet, legacyFoods);

        return new Result(List.copyOf(display), List.copyOf(usable));
    }

    private static void appendExtras(
            List<Entry> display,
            List<Entry> usable,
            Set<String> added,
            Set<String> nativeSet,
            List<Spec> specs
    ) {
        for (Spec spec : specs) {
            if (nativeSet.contains(spec.item()) || spec.amount() < 1 || !added.add(spec.item())) {
                continue;
            }
            Entry entry = new Entry(spec.item(), spec.amount(), true);
            display.add(entry);
            usable.add(entry);
        }
    }

    private static void appendLegacyExtras(
            List<Entry> display,
            List<Entry> usable,
            Set<String> added,
            Set<String> nativeSet,
            List<Spec> specs
    ) {
        for (Spec spec : specs) {
            if (nativeSet.contains(spec.item()) || spec.amount() < 1 || !added.add(spec.item())) {
                continue;
            }
            Entry entry = new Entry(spec.item(), spec.amount(), true);
            display.add(entry);
            usable.add(entry);
        }
    }

    private static Map<String, Integer> amounts(List<Spec> specs) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Spec spec : specs) {
            if (spec.item() != null && !spec.item().isBlank() && spec.amount() > 0) {
                result.put(spec.item(), spec.amount());
            }
        }
        return result;
    }

    private static int firstPositive(Integer first, Integer second, Integer third) {
        if (first != null && first > 0) {
            return first;
        }
        if (second != null && second > 0) {
            return second;
        }
        if (third != null && third > 0) {
            return third;
        }
        return 0;
    }

    record Spec(String item, int amount) {
    }

    record Entry(String item, int amount, boolean configured) {
    }

    record Result(List<Entry> display, List<Entry> usable) {
        Result {
            display = List.copyOf(display);
            usable = List.copyOf(usable);
        }
    }
}
