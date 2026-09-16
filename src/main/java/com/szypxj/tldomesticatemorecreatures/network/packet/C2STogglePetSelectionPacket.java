package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.command.pet.PetCommandService;
import com.szypxj.tldomesticatemorecreatures.command.pet.PetSelectionMode;
import com.szypxj.tldomesticatemorecreatures.command.pet.PetSelectionService;
import com.szypxj.tldomesticatemorecreatures.game.PetOwnershipService;
import com.szypxj.tldomesticatemorecreatures.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2STogglePetSelectionPacket(int entityId, PetSelectionMode mode) {
    public static void encode(C2STogglePetSelectionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId());
        buffer.writeEnum(packet.mode());
    }

    public static C2STogglePetSelectionPacket decode(FriendlyByteBuf buffer) {
        return new C2STogglePetSelectionPacket(buffer.readVarInt(), buffer.readEnum(PetSelectionMode.class));
    }

    public static void handle(C2STogglePetSelectionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            Entity entity = sender.serverLevel().getEntity(packet.entityId());
            double range = com.szypxj.tldomesticatemorecreatures.config.Config.PET_COMMAND_RANGE.get();
            if (entity instanceof LivingEntity living
                    && sender.distanceToSqr(living) <= range * range
                    && PetOwnershipService.isOwnedBy(living, sender)) {
                PetSelectionService.select(sender, living, packet.mode());
            }
            NetworkHandler.sendPetSelection(sender, PetSelectionService.selectedEntityIds(sender));
            PetCommandService.sendSummary(sender);
        });
        context.setPacketHandled(true);
    }
}
