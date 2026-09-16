package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.client.taming.TamingEditorScreen;
import com.szypxj.tldomesticatemorecreatures.domestication.editor.TamingEditorService;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record S2CTamingNativeFoodsPacket(
        ResourceLocation entityId,
        boolean success,
        List<ResourceLocation> items
) {
    public S2CTamingNativeFoodsPacket {
        items = List.copyOf(items == null ? List.of() : items);
    }

    public static void encode(S2CTamingNativeFoodsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.entityId());
        buffer.writeBoolean(packet.success());
        buffer.writeVarInt(packet.items().size());
        for (ResourceLocation itemId : packet.items()) {
            buffer.writeResourceLocation(itemId);
        }
    }

    public static S2CTamingNativeFoodsPacket decode(FriendlyByteBuf buffer) {
        ResourceLocation entityId = buffer.readResourceLocation();
        boolean success = buffer.readBoolean();
        int count = buffer.readVarInt();
        if (count < 0 || count > 8192) {
            throw new IllegalArgumentException("Invalid native food count: " + count);
        }
        List<ResourceLocation> items = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            items.add(buffer.readResourceLocation());
        }
        return new S2CTamingNativeFoodsPacket(entityId, success, items);
    }

    public static void handle(S2CTamingNativeFoodsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen instanceof TamingEditorScreen screen) {
                screen.onNativeFoods(packet.entityId(), new TamingEditorService.NativeFoodsResult(packet.success(), packet.items()));
            }
        }));
        context.setPacketHandled(true);
    }
}
