package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.menu.PortableCraftingMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

import java.util.function.Supplier;

public record C2SOpenCraftingPacket() {
    public static void encode(C2SOpenCraftingPacket packet, FriendlyByteBuf buffer) {
    }

    public static C2SOpenCraftingPacket decode(FriendlyByteBuf buffer) {
        return new C2SOpenCraftingPacket();
    }

    public static void handle(C2SOpenCraftingPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            NetworkHooks.openScreen(
                    sender,
                    new SimpleMenuProvider(
                            (containerId, inventory, player) -> new PortableCraftingMenu(containerId, inventory),
                            Component.translatable("container.crafting")
                    )
            );
        });
        context.setPacketHandled(true);
    }
}
