package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.client.ClientState;
import com.szypxj.tldomesticatemorecreatures.network.PetManagementSummary;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record S2CPetManagementListPacket(List<PetManagementSummary> pets, int storedCount, int capacity) {
    public S2CPetManagementListPacket {
        pets = List.copyOf(pets);
    }

    public static void encode(S2CPetManagementListPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.pets.size());
        for (PetManagementSummary summary : packet.pets) summary.encode(buffer);
        buffer.writeVarInt(packet.storedCount);
        buffer.writeVarInt(packet.capacity);
    }

    public static S2CPetManagementListPacket decode(FriendlyByteBuf buffer) {
        int size = Math.min(10000, Math.max(0, buffer.readVarInt()));
        List<PetManagementSummary> pets = new ArrayList<>(size);
        for (int i = 0; i < size; i++) pets.add(PetManagementSummary.decode(buffer));
        return new S2CPetManagementListPacket(pets, buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(S2CPetManagementListPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClientState.setPetManagementList(packet.pets, packet.storedCount, packet.capacity)));
        context.setPacketHandled(true);
    }
}
