package com.szypxj.tldomesticatemorecreatures.inventory;

import com.szypxj.tldomesticatemorecreatures.TlDomesticateMoreCreatures;
import com.szypxj.tldomesticatemorecreatures.menu.ExtraInventoryMenuBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TlDomesticateMoreCreatures.MOD_ID)
public final class PlayerExtraInventoryEvents {
    private PlayerExtraInventoryEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDrops(LivingDropsEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            return;
        }
        PlayerExtraInventory extra = PlayerExtraInventoryManager.get(player);
        for (int slot = 0; slot < extra.getContainerSize(); slot++) {
            var stack = extra.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            ItemEntity drop = new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), stack.copy());
            drop.setPickUpDelay(40);
            event.getDrops().add(drop);
        }
        extra.clearContent();
    }


    @SubscribeEvent
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ExtraInventoryMenuBridge.attachServer(event.getContainer(), player);
        }
    }


    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        PlayerExtraInventoryManager.reload(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        PlayerExtraInventoryManager.invalidate(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()
                || event.getEntity().level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            PlayerExtraInventory.copy(event.getOriginal(), event.getEntity());
        }
        PlayerExtraInventoryManager.invalidate(event.getOriginal());
        PlayerExtraInventoryManager.reload(event.getEntity());
    }
}
