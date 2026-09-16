package com.szypxj.tldomesticatemorecreatures.item;

import com.szypxj.tldomesticatemorecreatures.data.ProgressData;
import com.szypxj.tldomesticatemorecreatures.game.ExperienceSource;
import com.szypxj.tldomesticatemorecreatures.game.LevelService;
import com.szypxj.tldomesticatemorecreatures.game.PetOwnershipService;
import com.szypxj.tldomesticatemorecreatures.registry.ModItems;
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

public final class PetExperienceBottleItem extends Item {
    public PetExperienceBottleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!PetOwnershipService.isManagedPet(target)) {
            return InteractionResult.PASS;
        }
        if (!PetOwnershipService.isOwnedBy(target, player)) {
            return InteractionResult.FAIL;
        }

        LevelService.initializeIfNeeded(target);
        if (!ProgressData.exists(target) || !LevelService.canGainExperience(target) || !LevelService.canLevelUp(target)) {
            return InteractionResult.FAIL;
        }

        ProgressData data = ProgressData.of(target);
        if (data.level() >= data.maxLevel()) {
            return InteractionResult.FAIL;
        }

        long stored = PetExperienceBottleData.get(stack);
        if (stored <= 0L) {
            player.setItemInHand(hand, new ItemStack(ModItems.EMPTY_PET_EXPERIENCE_BOTTLE.get()));
            return InteractionResult.CONSUME;
        }

        long consumed = LevelService.addExperience(target, stored, ExperienceSource.ITEM);
        if (consumed <= 0L) {
            return InteractionResult.FAIL;
        }

        long remaining = PetExperienceBottleMath.remaining(stored, consumed);
        if (remaining <= 0L) {
            player.setItemInHand(hand, new ItemStack(ModItems.EMPTY_PET_EXPERIENCE_BOTTLE.get()));
        } else {
            PetExperienceBottleData.set(stack, remaining);
        }

        player.level().playSound(null, target.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.7F, 1.0F);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        Component amount = Component.literal(Long.toString(PetExperienceBottleData.get(stack)))
                .withStyle(ChatFormatting.AQUA);
        tooltip.add(Component.translatable(
                "tip.tl_domesticate_more_creatures.pet_experience_bottle",
                amount
        ));
    }
}
