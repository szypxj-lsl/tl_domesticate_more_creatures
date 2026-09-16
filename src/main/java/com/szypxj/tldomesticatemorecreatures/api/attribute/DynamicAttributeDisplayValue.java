package com.szypxj.tldomesticatemorecreatures.api.attribute;

public record DynamicAttributeDisplayValue(double current, double max) {
    public DynamicAttributeDisplayValue {
        max = Double.isFinite(max) ? Math.max(0.0D, max) : 0.0D;
        current = Double.isFinite(current) ? Math.max(0.0D, current) : 0.0D;
        current = Math.min(current, max);
    }
}
