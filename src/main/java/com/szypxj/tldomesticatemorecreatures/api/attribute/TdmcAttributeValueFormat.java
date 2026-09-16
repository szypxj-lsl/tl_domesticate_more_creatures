package com.szypxj.tldomesticatemorecreatures.api.attribute;

public enum TdmcAttributeValueFormat {
    NUMBER,
    MULTIPLIER,
    PERCENT;

    public String wireName() {
        return name();
    }

    public static TdmcAttributeValueFormat fromWireName(String value) {
        if (value == null || value.isBlank()) {
            return NUMBER;
        }
        try {
            return valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return NUMBER;
        }
    }
}
