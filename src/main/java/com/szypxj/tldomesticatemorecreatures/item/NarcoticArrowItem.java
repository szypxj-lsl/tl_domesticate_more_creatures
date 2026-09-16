package com.szypxj.tldomesticatemorecreatures.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class NarcoticArrowItem extends ArrowItem {
    public static final String PROJECTILE_TAG = "tl_domesticate_more_creatures_narcotic_arrow";
    public static final String PROJECTILE_LEVEL_TAG = "tl_domesticate_more_creatures_narcotic_arrow_level";

    private final int effectLevel;

    public NarcoticArrowItem(Properties properties, int effectLevel) {
        super(properties);
        this.effectLevel = Math.max(1, effectLevel);
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack stack, LivingEntity shooter) {
        Arrow arrow = new Arrow(level, shooter);
        arrow.getPersistentData().putBoolean(PROJECTILE_TAG, true);
        arrow.getPersistentData().putInt(PROJECTILE_LEVEL_TAG, effectLevel);
        return arrow;
    }

    public static int getEffectLevel(AbstractArrow arrow) {
        int level = arrow.getPersistentData().getInt(PROJECTILE_LEVEL_TAG);
        return Math.max(1, level);
    }
}
