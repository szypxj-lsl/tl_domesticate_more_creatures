package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.client.ClientState;
import com.szypxj.tldomesticatemorecreatures.command.pet.PetCommandSummary;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record S2CPetCommandSummaryPacket(PetCommandSummary summary, boolean explicitSelection, boolean hasLandCapablePets) {
    public static void encode(S2CPetCommandSummaryPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.summary());
        buffer.writeBoolean(packet.explicitSelection());
        buffer.writeBoolean(packet.hasLandCapablePets());
    }

    public static S2CPetCommandSummaryPacket decode(FriendlyByteBuf buffer) {
        return new S2CPetCommandSummaryPacket(buffer.readEnum(PetCommandSummary.class), buffer.readBoolean(), buffer.readBoolean());
    }

    public static void handle(S2CPetCommandSummaryPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClientState.setPetCommandSummary(packet.summary(), packet.explicitSelection(), packet.hasLandCapablePets())
        ));
        context.setPacketHandled(true);
    }
}
