package com.szypxj.tldomesticatemorecreatures.equipment;

import com.szypxj.tldomesticatemorecreatures.TlDomesticateMoreCreatures;
import com.szypxj.tldomesticatemorecreatures.game.PetOwnershipService;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TlDomesticateMoreCreatures.MOD_ID)
public final class PetEquipmentEvents {
    private PetEquipmentEvents() {
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof LivingEntity living && PetOwnershipService.isTamed(living)) {
            PetEquipmentService.refreshAttributeBonuses(living);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity living = event.getEntity();
        if (!living.level().isClientSide && PetOwnershipService.isTamed(living)) {
            PetEquipmentService.dropCustomEquipment(living);
        }
    }

    @SubscribeEvent
    public static void onLivingEquipmentChange(LivingEquipmentChangeEvent event) {
        LivingEntity living = event.getEntity();
        if (!living.level().isClientSide && PetOwnershipService.isTamed(living)) {
            PetEquipmentService.refreshAttributeBonuses(living);
        }
    }
}
