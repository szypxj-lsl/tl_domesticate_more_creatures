package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.client.ClientState;
import com.szypxj.tldomesticatemorecreatures.network.PetManagementDetail;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record S2CPetManagementDetailPacket(PetManagementDetail detail) {
    public static void encode(S2CPetManagementDetailPacket packet, FriendlyByteBuf buffer) {
        packet.detail.encode(buffer);
    }

    public static S2CPetManagementDetailPacket decode(FriendlyByteBuf buffer) {
        return new S2CPetManagementDetailPacket(PetManagementDetail.decode(buffer));
    }

    public static void handle(S2CPetManagementDetailPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientState.setPetManagementDetail(packet.detail)));
        context.setPacketHandled(true);
    }
}
