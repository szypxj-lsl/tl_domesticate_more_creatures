package com.szypxj.tldomesticatemorecreatures.petmanagement;

import com.szypxj.tldomesticatemorecreatures.TlDomesticateMoreCreatures;
import com.szypxj.tldomesticatemorecreatures.game.PetOwnershipService;
import com.szypxj.tldomesticatemorecreatures.petmanagement.summon.PetRescueFallProtection;
import com.szypxj.tldomesticatemorecreatures.petmanagement.summon.PetSummonService;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TlDomesticateMoreCreatures.MOD_ID)
public final class PetManagementEvents {
    private PetManagementEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof LivingEntity living)) return;
        PetManagementService.reconcileOwnership(living);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.isCanceled()) return;
        LivingEntity living = event.getEntity();
        if (!living.level().isClientSide && PetOwnershipService.ownerUuid(living).isPresent()) {
            PetManagementService.markDead(living);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        PetOwnershipService.tickPendingTameInteractions(event.getServer());
        PetManagementService.refreshTracked(event.getServer());
        PetSummonService.tick(event.getServer());
        PetRescueFallProtection.tick(event.getServer());
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity living = event.getEntity();
        if (!living.level().isClientSide && PetOwnershipService.ownerUuid(living).isPresent()) {
            PetManagementService.recordHurt(living);
        }
    }
}
