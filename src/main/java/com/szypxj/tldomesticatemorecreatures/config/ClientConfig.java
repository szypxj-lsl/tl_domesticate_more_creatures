package com.szypxj.tldomesticatemorecreatures.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ClientConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.ConfigValue<String> SPYGLASS_PANEL_ANCHOR = BUILDER
            .define("inspect.panel.anchor", "RIGHT_CENTER");

    public static final ForgeConfigSpec.IntValue SPYGLASS_PANEL_OFFSET_X = BUILDER
            .defineInRange("inspect.panel.offsetX", 0, -10000, 10000);

    public static final ForgeConfigSpec.IntValue SPYGLASS_PANEL_OFFSET_Y = BUILDER
            .defineInRange("inspect.panel.offsetY", 0, -10000, 10000);

    public static final ForgeConfigSpec.BooleanValue EFFECT_PANEL_CUSTOM_POSITION = BUILDER
            .define("inventory.effectPanel.customPosition", false);

    public static final ForgeConfigSpec.IntValue EFFECT_PANEL_X = BUILDER
            .defineInRange("inventory.effectPanel.x", 0, -10000, 10000);

    public static final ForgeConfigSpec.IntValue EFFECT_PANEL_Y = BUILDER
            .defineInRange("inventory.effectPanel.y", 0, -10000, 10000);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private ClientConfig() {
    }

    public static void setSpyglassPanelPosition(String anchor, int offsetX, int offsetY) {
        SPYGLASS_PANEL_ANCHOR.set(anchor);
        SPYGLASS_PANEL_OFFSET_X.set(offsetX);
        SPYGLASS_PANEL_OFFSET_Y.set(offsetY);
        SPYGLASS_PANEL_ANCHOR.save();
    }

    public static void setEffectPanelPosition(int x, int y) {
        EFFECT_PANEL_CUSTOM_POSITION.set(true);
        EFFECT_PANEL_X.set(x);
        EFFECT_PANEL_Y.set(y);
        EFFECT_PANEL_CUSTOM_POSITION.save();
    }
}
