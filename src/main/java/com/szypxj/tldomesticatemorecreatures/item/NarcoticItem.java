package com.szypxj.tldomesticatemorecreatures.item;

import com.szypxj.tldomesticatemorecreatures.registry.ModEffects;
import com.szypxj.tldomesticatemorecreatures.torpor.TorporService;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public final class NarcoticItem extends Item {
    private static final int DURATION_TICKS = 20 * 20;

    private final int effectLevel;
    private final String tooltipKey;

    public NarcoticItem(Properties properties, int effectLevel, String tooltipKey) {
        super(properties);
        this.effectLevel = Math.max(1, effectLevel);
        this.tooltipKey = tooltipKey;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!TorporService.isUnconscious(target)) {
            return InteractionResult.PASS;
        }

        target.addEffect(new MobEffectInstance(
                ModEffects.NARCOTIC.get(),
                DURATION_TICKS,
                effectLevel - 1,
                false,
                true,
                true
        ));

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(tooltipKey));
    }
}
