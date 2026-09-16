package com.szypxj.tldomesticatemorecreatures.backpack;

import com.szypxj.tldomesticatemorecreatures.TlDomesticateMoreCreatures;
import com.szypxj.tldomesticatemorecreatures.domestication.TamingService;
import com.szypxj.tldomesticatemorecreatures.game.PetOwnershipService;
import com.szypxj.tldomesticatemorecreatures.torpor.TorporData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TlDomesticateMoreCreatures.MOD_ID)
public final class PetBackpackEvents {
    private PetBackpackEvents() {
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || entity.tickCount % 20 != 0
                || !TorporData.exists(entity) || !TorporData.of(entity).unconscious()
                || !PetBackpackService.isKnockoutBackpackTarget(entity)) {
            return;
        }
        TamingService.consumeBackpackTamingFood(entity);
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || PetOwnershipService.isTamed(entity)) {
            return;
        }
        var items = PetBackpackService.contents(entity);
        boolean any = false;
        for (ItemStack stack : items) {
            if (stack.isEmpty()) {
                continue;
            }
            any = true;
            event.getDrops().add(new ItemEntity(entity.level(), entity.getX(), entity.getY(), entity.getZ(), stack.copy()));
        }
        if (any) {
            PetBackpackService.clear(entity);
        }
    }
}
