package com.szypxj.tldomesticatemorecreatures.mixin.client.inventory;

import com.szypxj.tldomesticatemorecreatures.inventory.PlayerExtraInventory;
import com.szypxj.tldomesticatemorecreatures.menu.ExtraInventoryMenuAccess;
import com.szypxj.tldomesticatemorecreatures.menu.ExtraInventoryMenuBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundHorseScreenOpenPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerExtraInventoryMixin {
    private static final int HIDDEN_SLOT = -10000;

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "handleOpenScreen", at = @At("RETURN"), require = 0)
    private void tdmc$attachExtraInventoryAfterOpenScreen(ClientboundOpenScreenPacket packet, CallbackInfo ci) {
        tdmc$attachCurrentMenu(packet.getContainerId());
    }

    @Inject(method = "handleHorseScreenOpen", at = @At("RETURN"), require = 0)
    private void tdmc$attachExtraInventoryAfterHorseScreen(ClientboundHorseScreenOpenPacket packet, CallbackInfo ci) {
        tdmc$attachCurrentMenu(packet.getContainerId());
    }

    @Inject(method = "handleContainerContent", at = @At("HEAD"), require = 0)
    private void tdmc$ensureExtraInventoryBeforeFullContent(ClientboundContainerSetContentPacket packet, CallbackInfo ci) {
        Player player = minecraft.player;
        if (player == null) {
            return;
        }
        AbstractContainerMenu menu = player.containerMenu;
        if (menu == null || menu.containerId != packet.getContainerId() || tdmc$isExtraAttached(menu)) {
            return;
        }
        if (packet.getItems().size() - menu.slots.size() == PlayerExtraInventory.SIZE) {
            ExtraInventoryMenuBridge.attachClient(menu, player, HIDDEN_SLOT, HIDDEN_SLOT, 18, 18);
        }
    }

    @Inject(method = "handleContainerSetSlot", at = @At("HEAD"), require = 0)
    private void tdmc$ensureExtraInventoryBeforeSingleSlot(ClientboundContainerSetSlotPacket packet, CallbackInfo ci) {
        Player player = minecraft.player;
        if (player == null) {
            return;
        }
        AbstractContainerMenu menu = player.containerMenu;
        if (menu == null || menu.containerId != packet.getContainerId() || tdmc$isExtraAttached(menu)) {
            return;
        }
        int slot = packet.getSlot();
        if (slot >= menu.slots.size() && slot < menu.slots.size() + PlayerExtraInventory.SIZE) {
            ExtraInventoryMenuBridge.attachClient(menu, player, HIDDEN_SLOT, HIDDEN_SLOT, 18, 18);
        }
    }

    private static boolean tdmc$isExtraAttached(AbstractContainerMenu menu) {
        return menu instanceof ExtraInventoryMenuAccess access && access.tdmc$extraInventoryAttached();
    }

    private void tdmc$attachCurrentMenu(int containerId) {
        Player player = minecraft.player;
        if (player == null) {
            return;
        }
        AbstractContainerMenu menu = player.containerMenu;
        if (menu == null || menu.containerId != containerId) {
            return;
        }
        ExtraInventoryMenuBridge.attachClient(menu, player, HIDDEN_SLOT, HIDDEN_SLOT, 18, 18);
    }
}
