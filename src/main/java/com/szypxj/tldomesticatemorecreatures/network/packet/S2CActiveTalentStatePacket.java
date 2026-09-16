package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.client.talent.ClientActiveTalentState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record S2CActiveTalentStatePacket(
        int mountEntityId,
        String skillId,
        String phase,
        long phaseEndGameTime,
        int marks,
        int maxMarks,
        long cooldownEndGameTime
) {
    public static void encode(S2CActiveTalentStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.mountEntityId());
        buffer.writeUtf(packet.skillId(), 64);
        buffer.writeUtf(packet.phase(), 32);
        buffer.writeVarLong(packet.phaseEndGameTime());
        buffer.writeVarInt(packet.marks());
        buffer.writeVarInt(packet.maxMarks());
        buffer.writeVarLong(packet.cooldownEndGameTime());
    }

    public static S2CActiveTalentStatePacket decode(FriendlyByteBuf buffer) {
        return new S2CActiveTalentStatePacket(
                buffer.readVarInt(),
                buffer.readUtf(64),
                buffer.readUtf(32),
                buffer.readVarLong(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarLong()
        );
    }

    public static void handle(S2CActiveTalentStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientActiveTalentState.applyState(packet)));
        context.setPacketHandled(true);
    }
}
