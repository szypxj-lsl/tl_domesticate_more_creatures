package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.command.pet.PetCommand;
import com.szypxj.tldomesticatemorecreatures.compat.CompatBootstrap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record S2CPetCommandMarkerPacket(
        UUID markerId,
        boolean remove,
        PetCommand command,
        int targetEntityId,
        UUID targetEntityUuid,
        Vec3 position
) {
    public static S2CPetCommandMarkerPacket show(
            UUID markerId,
            PetCommand command,
            int targetEntityId,
            UUID targetEntityUuid,
            Vec3 position
    ) {
        return new S2CPetCommandMarkerPacket(
                markerId,
                false,
                command,
                targetEntityId,
                targetEntityUuid,
                position == null ? Vec3.ZERO : position
        );
    }

    public static S2CPetCommandMarkerPacket remove(UUID markerId) {
        return new S2CPetCommandMarkerPacket(markerId, true, PetCommand.MOVE, -1, null, Vec3.ZERO);
    }

    public static void encode(S2CPetCommandMarkerPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.markerId());
        buffer.writeBoolean(packet.remove());
        if (packet.remove()) {
            return;
        }
        buffer.writeEnum(packet.command());
        buffer.writeVarInt(packet.targetEntityId());
        buffer.writeBoolean(packet.targetEntityUuid() != null);
        if (packet.targetEntityUuid() != null) {
            buffer.writeUUID(packet.targetEntityUuid());
        }
        buffer.writeDouble(packet.position().x);
        buffer.writeDouble(packet.position().y);
        buffer.writeDouble(packet.position().z);
    }

    public static S2CPetCommandMarkerPacket decode(FriendlyByteBuf buffer) {
        UUID markerId = buffer.readUUID();
        boolean remove = buffer.readBoolean();
        if (remove) {
            return remove(markerId);
        }
        PetCommand command = buffer.readEnum(PetCommand.class);
        int targetEntityId = buffer.readVarInt();
        UUID targetEntityUuid = buffer.readBoolean() ? buffer.readUUID() : null;
        Vec3 position = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        return show(markerId, command, targetEntityId, targetEntityUuid, position);
    }

    public static void handle(S2CPetCommandMarkerPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (packet.remove()) {
                CompatBootstrap.removeClientCommandMarker(packet.markerId());
            } else {
                CompatBootstrap.showClientCommandMarker(
                        packet.markerId(),
                        packet.command(),
                        packet.targetEntityId(),
                        packet.targetEntityUuid(),
                        packet.position()
                );
            }
        }));
        context.setPacketHandled(true);
    }
}
