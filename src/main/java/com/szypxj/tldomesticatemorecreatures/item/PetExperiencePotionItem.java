package com.szypxj.tldomesticatemorecreatures.item;

import com.szypxj.tldomesticatemorecreatures.config.Config;
import com.szypxj.tldomesticatemorecreatures.data.ProgressData;
import com.szypxj.tldomesticatemorecreatures.game.LevelService;
import com.szypxj.tldomesticatemorecreatures.game.ExperienceSource;
import com.szypxj.tldomesticatemorecreatures.game.PetOwnershipService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public final class PetExperiencePotionItem extends Item {
    public PetExperiencePotionItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!PetOwnershipService.isOwnedBy(target, player)) {
            return InteractionResult.PASS;
        }

        LevelService.initializeIfNeeded(target);
        if (!ProgressData.exists(target) || !LevelService.canGainExperience(target) || !LevelService.canLevelUp(target)) {
            return InteractionResult.FAIL;
        }

        ProgressData data = ProgressData.of(target);
        if (data.level() >= data.maxLevel()) {
            return InteractionResult.FAIL;
        }

        LevelService.addExperience(target, Config.PET_EXPERIENCE_POTION_AMOUNT.get(), ExperienceSource.ITEM);
        player.level().playSound(null, target.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.7F, 1.0F);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        Component amount = Component.literal(Integer.toString(Config.PET_EXPERIENCE_POTION_AMOUNT.get()))
                .withStyle(ChatFormatting.AQUA);

        tooltip.add(Component.translatable(
                "tip.tl_domesticate_more_creatures.pet_experience_potion",
                amount
        ));
    }
}
