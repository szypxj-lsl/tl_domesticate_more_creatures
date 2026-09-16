package com.szypxj.tldomesticatemorecreatures.domestication;

import com.szypxj.tldomesticatemorecreatures.TlDomesticateMoreCreatures;
import com.szypxj.tldomesticatemorecreatures.game.PetOwnershipService;
import com.szypxj.tldomesticatemorecreatures.game.LevelService;
import com.szypxj.tldomesticatemorecreatures.elite.EliteService;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TlDomesticateMoreCreatures.MOD_ID)
public final class TamingEvents {
    private TamingEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity().level().isClientSide || !(event.getTarget() instanceof LivingEntity target)) {
            return;
        }
        if (!LevelService.isAffected(target)) {
            return;
        }
        InteractionResult result = TamingService.tryInteract(event.getEntity(), target, event.getItemStack());
        if (result != InteractionResult.PASS) {
            event.setCancellationResult(result);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAnimalTame(AnimalTameEvent event) {
        if (event.getAnimal().level().isClientSide || !LevelService.isAffected(event.getAnimal())) {
            return;
        }
        LevelService.initializeIfNeeded(event.getAnimal());
        if (EliteService.isElite(event.getAnimal()) && !EliteService.canBeTamed(event.getAnimal())) {
            event.setCanceled(true);
            return;
        }
        if (TamingRuleManager.ruleFor(event.getAnimal()).isPresent()
                && !PetOwnershipService.isCustomPet(event.getAnimal())) {
            event.setCanceled(true);
        }
    }
}
