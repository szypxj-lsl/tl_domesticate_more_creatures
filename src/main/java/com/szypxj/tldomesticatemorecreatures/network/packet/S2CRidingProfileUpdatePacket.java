package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.client.riding.ClientRidingConfigCache;
import com.szypxj.tldomesticatemorecreatures.client.riding.RidingEditorScreen;
import net.minecraft.client.Minecraft;
import com.szypxj.tldomesticatemorecreatures.riding.config.EntityRideProfile;
import com.szypxj.tldomesticatemorecreatures.riding.config.RidingNetworkCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record S2CRidingProfileUpdatePacket(long generation, EntityRideProfile profile) {
    public static void encode(S2CRidingProfileUpdatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarLong(packet.generation());
        RidingNetworkCodec.writeProfile(buffer, packet.profile());
    }

    public static S2CRidingProfileUpdatePacket decode(FriendlyByteBuf buffer) {
        return new S2CRidingProfileUpdatePacket(buffer.readVarLong(), RidingNetworkCodec.readProfile(buffer));
    }

    public static void handle(S2CRidingProfileUpdatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> {
                    ClientRidingConfigCache.applyProfile(packet.generation(), packet.profile());
                    if (Minecraft.getInstance().screen instanceof RidingEditorScreen screen) {
                        screen.onProfileSaved(packet.generation(), packet.profile());
                    }
                }
        ));
        context.setPacketHandled(true);
    }
}
