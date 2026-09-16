package com.szypxj.tldomesticatemorecreatures.item;

import com.szypxj.tldomesticatemorecreatures.data.ProgressData;
import com.szypxj.tldomesticatemorecreatures.game.LevelService;
import com.szypxj.tldomesticatemorecreatures.game.PetOwnershipService;
import com.szypxj.tldomesticatemorecreatures.registry.ModItems;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class EmptyPetExperienceBottleItem extends Item {
    public EmptyPetExperienceBottleItem(Properties properties) {
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
        if (!ProgressData.exists(target)) {
            return InteractionResult.FAIL;
        }

        ProgressData data = ProgressData.of(target);
        if (data.level() < data.maxLevel() || data.experience() <= 0L) {
            return InteractionResult.FAIL;
        }

        long extracted = PetExperienceBottleMath.extractAmount(data.experience(), player.isShiftKeyDown());
        if (extracted <= 0L) {
            return InteractionResult.FAIL;
        }

        data.experience(PetExperienceBottleMath.remaining(data.experience(), extracted));
        ItemStack filled = new ItemStack(ModItems.PET_EXPERIENCE_BOTTLE.get());
        PetExperienceBottleData.set(filled, extracted);
        player.setItemInHand(hand, filled);
        player.level().playSound(null, target.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.7F, 1.0F);
        return InteractionResult.CONSUME;
    }
}
