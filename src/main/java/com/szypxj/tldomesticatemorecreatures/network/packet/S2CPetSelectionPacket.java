package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.client.ClientState;
import com.szypxj.tldomesticatemorecreatures.compat.CompatBootstrap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record S2CPetSelectionPacket(List<Integer> entityIds) {
    public S2CPetSelectionPacket {
        entityIds = List.copyOf(entityIds);
    }

    public static void encode(S2CPetSelectionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityIds().size());
        for (int entityId : packet.entityIds()) {
            buffer.writeVarInt(entityId);
        }
    }

    public static S2CPetSelectionPacket decode(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        List<Integer> ids = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ids.add(buffer.readVarInt());
        }
        return new S2CPetSelectionPacket(ids);
    }

    public static void handle(S2CPetSelectionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            ClientState.setSelectedPetIds(packet.entityIds());
            CompatBootstrap.refreshClientSelectionVisual();
        }));
        context.setPacketHandled(true);
    }
}
