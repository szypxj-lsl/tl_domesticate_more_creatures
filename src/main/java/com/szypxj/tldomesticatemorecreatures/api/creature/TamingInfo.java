package com.szypxj.tldomesticatemorecreatures.api.creature;

import java.util.List;

public record TamingInfo(boolean tameable, String methodName, int requiredPlayerLevel, List<TamingFoodInfo> foods) {
    public static final TamingInfo NOT_TAMEABLE = new TamingInfo(false, "", 1, List.of());

    public TamingInfo {
        methodName = methodName == null ? "" : methodName;
        requiredPlayerLevel = Math.max(1, requiredPlayerLevel);
        foods = foods == null ? List.of() : List.copyOf(foods);
    }
}
