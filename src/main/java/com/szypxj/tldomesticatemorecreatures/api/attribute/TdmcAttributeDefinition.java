package com.szypxj.tldomesticatemorecreatures.api.attribute;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.Objects;

public final class TdmcAttributeDefinition {
    private final ResourceLocation id;
    private final String nameKey;
    private final String descriptionKey;
    private final String icon;
    private final int displayOrder;
    private final double defaultValue;
    private final double minValue;
    private final double maxValue;
    private final TdmcAttributeValueFormat valueFormat;
    private final TdmcAttributeFlags flags;
    private final double pointIncrement;
    private final int pointCost;
    private final TdmcAttributeApplicability applicability;
    private final TdmcAttributeValueProvider valueProvider;
    private final TdmcAttributeValueWriter valueWriter;

    private TdmcAttributeDefinition(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "id");
        this.nameKey = requireText(builder.nameKey, "nameKey");
        this.descriptionKey = builder.descriptionKey == null ? "" : builder.descriptionKey;
        this.icon = builder.icon == null || builder.icon.isBlank() ? "minecraft:stone" : builder.icon;
        this.displayOrder = builder.displayOrder;
        this.minValue = finite(builder.minValue, "minValue");
        this.maxValue = finite(builder.maxValue, "maxValue");
        if (maxValue < minValue) {
            throw new IllegalArgumentException("maxValue must be >= minValue");
        }
        this.defaultValue = finite(builder.defaultValue, "defaultValue");
        if (defaultValue < minValue || defaultValue > maxValue) {
            throw new IllegalArgumentException("defaultValue must be inside minValue/maxValue");
        }
        this.valueFormat = Objects.requireNonNullElse(builder.valueFormat, TdmcAttributeValueFormat.NUMBER);
        this.flags = Objects.requireNonNullElse(builder.flags, TdmcAttributeFlags.DEFAULT);
        this.pointIncrement = finite(builder.pointIncrement, "pointIncrement");
        if (pointIncrement <= 0.0D) {
            throw new IllegalArgumentException("pointIncrement must be > 0");
        }
        this.pointCost = Math.max(1, builder.pointCost);
        this.applicability = Objects.requireNonNullElse(builder.applicability, TdmcAttributeApplicability.ALL);
        this.valueProvider = builder.valueProvider;
        this.valueWriter = builder.valueWriter;
    }

    public static Builder builder(ResourceLocation id, String nameKey) {
        return new Builder(id, nameKey);
    }

    public Builder toBuilder() {
        return new Builder(id, nameKey)
                .descriptionKey(descriptionKey)
                .icon(icon)
                .displayOrder(displayOrder)
                .bounds(minValue, maxValue)
                .defaultValue(defaultValue)
                .valueFormat(valueFormat)
                .flags(flags)
                .pointRule(pointIncrement, pointCost)
                .applicability(applicability)
                .valueProvider(valueProvider)
                .valueWriter(valueWriter);
    }

    public TdmcAttributeDefinition withNameKey(String value) {
        return toBuilder().nameKey(value).build();
    }

    public TdmcAttributeDefinition withDescriptionKey(String value) {
        return toBuilder().descriptionKey(value).build();
    }

    public TdmcAttributeDefinition withIcon(String value) {
        return toBuilder().icon(value).build();
    }

    public TdmcAttributeDefinition withDisplayOrder(int value) {
        return toBuilder().displayOrder(value).build();
    }

    public TdmcAttributeDefinition withBounds(double min, double max) {
        double safeDefault = Math.max(min, Math.min(max, defaultValue));
        return toBuilder().bounds(min, max).defaultValue(safeDefault).build();
    }

    public TdmcAttributeDefinition withDefaultValue(double value) {
        return toBuilder().defaultValue(value).build();
    }

    public TdmcAttributeDefinition withValueFormat(TdmcAttributeValueFormat value) {
        return toBuilder().valueFormat(value).build();
    }

    public TdmcAttributeDefinition withFlags(TdmcAttributeFlags value) {
        return toBuilder().flags(value).build();
    }

    public TdmcAttributeDefinition withPointRule(double increment, int cost) {
        return toBuilder().pointRule(increment, cost).build();
    }

    public TdmcAttributeDefinition withApplicability(TdmcAttributeApplicability value) {
        return toBuilder().applicability(value).build();
    }

    public ResourceLocation id() {
        return id;
    }

    public String nameKey() {
        return nameKey;
    }

    public String descriptionKey() {
        return descriptionKey;
    }

    public String icon() {
        return icon;
    }

    public int displayOrder() {
        return displayOrder;
    }

    public double defaultValue() {
        return defaultValue;
    }

    public double minValue() {
        return minValue;
    }

    public double maxValue() {
        return maxValue;
    }

    public TdmcAttributeValueFormat valueFormat() {
        return valueFormat;
    }

    public TdmcAttributeFlags flags() {
        return flags;
    }

    public double pointIncrement() {
        return pointIncrement;
    }

    public int pointCost() {
        return pointCost;
    }

    public TdmcAttributeApplicability applicability() {
        return applicability;
    }

    public TdmcAttributeValueProvider valueProvider() {
        return valueProvider;
    }

    public TdmcAttributeValueWriter valueWriter() {
        return valueWriter;
    }

    public boolean appliesTo(LivingEntity entity) {
        return entity != null && applicability.appliesTo(entity);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    private static double finite(double value, String field) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(field + " must be finite");
        }
        return value;
    }

    public static final class Builder {
        private final ResourceLocation id;
        private String nameKey;
        private String descriptionKey = "";
        private String icon = "minecraft:stone";
        private int displayOrder = 10000;
        private double defaultValue = 0.0D;
        private double minValue = 0.0D;
        private double maxValue = Double.MAX_VALUE;
        private TdmcAttributeValueFormat valueFormat = TdmcAttributeValueFormat.NUMBER;
        private TdmcAttributeFlags flags = TdmcAttributeFlags.DEFAULT;
        private double pointIncrement = 1.0D;
        private int pointCost = 1;
        private TdmcAttributeApplicability applicability = TdmcAttributeApplicability.ALL;
        private TdmcAttributeValueProvider valueProvider;
        private TdmcAttributeValueWriter valueWriter;

        private Builder(ResourceLocation id, String nameKey) {
            this.id = Objects.requireNonNull(id, "id");
            this.nameKey = nameKey;
        }

        public Builder nameKey(String value) {
            this.nameKey = value;
            return this;
        }

        public Builder descriptionKey(String value) {
            this.descriptionKey = value;
            return this;
        }

        public Builder icon(String value) {
            this.icon = value;
            return this;
        }

        public Builder displayOrder(int value) {
            this.displayOrder = value;
            return this;
        }

        public Builder defaultValue(double value) {
            this.defaultValue = value;
            return this;
        }

        public Builder bounds(double min, double max) {
            this.minValue = min;
            this.maxValue = max;
            return this;
        }

        public Builder valueFormat(TdmcAttributeValueFormat value) {
            this.valueFormat = value;
            return this;
        }

        public Builder flags(TdmcAttributeFlags value) {
            this.flags = value;
            return this;
        }

        public Builder pointRule(double increment, int cost) {
            this.pointIncrement = increment;
            this.pointCost = cost;
            return this;
        }

        public Builder applicability(TdmcAttributeApplicability value) {
            this.applicability = value;
            return this;
        }

        public Builder valueProvider(TdmcAttributeValueProvider value) {
            this.valueProvider = value;
            return this;
        }

        public Builder valueWriter(TdmcAttributeValueWriter value) {
            this.valueWriter = value;
            return this;
        }

        public TdmcAttributeDefinition build() {
            return new TdmcAttributeDefinition(this);
        }
    }
}
