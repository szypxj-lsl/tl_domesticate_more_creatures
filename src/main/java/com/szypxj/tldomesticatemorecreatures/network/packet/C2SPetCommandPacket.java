package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.command.pet.PetCommand;
import com.szypxj.tldomesticatemorecreatures.command.pet.PetCommandService;
import com.szypxj.tldomesticatemorecreatures.config.Config;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record C2SPetCommandPacket(PetCommand command, int targetEntityId, Vec3 position) {
    public static void encode(C2SPetCommandPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.command());
        buffer.writeVarInt(packet.targetEntityId());
        buffer.writeDouble(packet.position().x);
        buffer.writeDouble(packet.position().y);
        buffer.writeDouble(packet.position().z);
    }

    public static C2SPetCommandPacket decode(FriendlyByteBuf buffer) {
        return new C2SPetCommandPacket(
                buffer.readEnum(PetCommand.class),
                buffer.readVarInt(),
                new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble())
        );
    }

    public static void handle(C2SPetCommandPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            double range = Config.PET_COMMAND_RANGE.get();
            if ((packet.command() == PetCommand.MOVE || packet.command() == PetCommand.DEFEND)
                    && sender.position().distanceToSqr(packet.position()) > range * range) {
                PetCommandService.sendSummary(sender);
                return;
            }
            UUID targetUuid = null;
            if (packet.command() == PetCommand.ATTACK) {
                Entity target = sender.serverLevel().getEntity(packet.targetEntityId());
                if (!(target instanceof LivingEntity living) || sender.distanceToSqr(living) > range * range) {
                    PetCommandService.sendSummary(sender);
                    return;
                }
                targetUuid = living.getUUID();
            }
            PetCommandService.issue(sender, packet.command(), packet.position(), targetUuid);
        });
        context.setPacketHandled(true);
    }
}
