package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.data.ProgressData;
import com.szypxj.tldomesticatemorecreatures.backpack.PetBackpackService;
import com.szypxj.tldomesticatemorecreatures.game.PetOwnershipService;
import com.szypxj.tldomesticatemorecreatures.network.NetworkHandler;
import com.szypxj.tldomesticatemorecreatures.network.SnapshotFactory;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2SRefreshPanelPacket(int entityId) {
    public static void encode(C2SRefreshPanelPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.entityId());
    }

    public static C2SRefreshPanelPacket decode(FriendlyByteBuf buffer) {
        return new C2SRefreshPanelPacket(buffer.readInt());
    }

    public static void handle(C2SRefreshPanelPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            LivingEntity target = resolve(sender, packet.entityId());
            if (target == null) {
                return;
            }
            NetworkHandler.sendPanel(sender, SnapshotFactory.panel(sender, target));
        });
        context.setPacketHandled(true);
    }

    private static LivingEntity resolve(ServerPlayer sender, int id) {
        if (id == sender.getId()) {
            return sender;
        }
        Entity entity = sender.level().getEntity(id);
        if (!(entity instanceof LivingEntity living)) {
            return null;
        }
        if (sender.getVehicle() != living && sender.distanceToSqr(living) > 64.0D) {
            return null;
        }
        boolean normalPanel = PetOwnershipService.canUsePanel(living) && ProgressData.exists(living);
        boolean knockoutPanel = PetBackpackService.canOpenKnockoutPanel(sender, living);
        return normalPanel || knockoutPanel ? living : null;
    }
}
