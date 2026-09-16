package com.szypxj.tldomesticatemorecreatures.api.attribute;

public record TdmcAttributeValue(double current, double max) {
    public TdmcAttributeValue {
        if (!Double.isFinite(current)) {
            current = 0.0D;
        }
        if (!Double.isFinite(max)) {
            max = 0.0D;
        }
    }

    public static TdmcAttributeValue of(double current, double max) {
        return new TdmcAttributeValue(current, max);
    }
}
