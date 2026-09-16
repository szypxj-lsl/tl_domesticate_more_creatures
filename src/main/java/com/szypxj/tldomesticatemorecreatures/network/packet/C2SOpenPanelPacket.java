package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.data.ProgressData;
import com.szypxj.tldomesticatemorecreatures.backpack.PetBackpackService;
import com.szypxj.tldomesticatemorecreatures.domestication.TamingService;
import com.szypxj.tldomesticatemorecreatures.game.LevelService;
import com.szypxj.tldomesticatemorecreatures.game.PetOwnershipService;
import com.szypxj.tldomesticatemorecreatures.menu.AttributePanelMenu;
import com.szypxj.tldomesticatemorecreatures.network.NetworkHandler;
import com.szypxj.tldomesticatemorecreatures.network.PanelSnapshot;
import com.szypxj.tldomesticatemorecreatures.network.SnapshotFactory;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

import java.util.function.Supplier;

public record C2SOpenPanelPacket(int entityId) {
    public static void encode(C2SOpenPanelPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.entityId());
    }

    public static C2SOpenPanelPacket decode(FriendlyByteBuf buffer) {
        return new C2SOpenPanelPacket(buffer.readInt());
    }

    public static void handle(C2SOpenPanelPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            LivingEntity resolved = resolve(sender, packet.entityId());
            if (resolved == null) {
                resolved = sender;
            }
            LevelService.initializeIfNeeded(resolved);
            if (resolved != sender) {
                PetOwnershipService.reconcileKnownOwnership(resolved, sender);
                boolean normalPanel = PetOwnershipService.canUsePanel(resolved) && ProgressData.exists(resolved);
                boolean knockoutPanel = PetBackpackService.canOpenKnockoutPanel(sender, resolved);
                if (!normalPanel && !knockoutPanel) {
                    resolved = sender;
                } else if (PetBackpackService.isKnockoutBackpackTarget(resolved)
                        && !TamingService.claimKnockoutSession(sender, resolved)) {
                    resolved = sender;
                }
            }
            LivingEntity target = resolved;
            PanelSnapshot snapshot = SnapshotFactory.panel(sender, target);
            NetworkHooks.openScreen(
                    sender,
                    new SimpleMenuProvider(
                            (containerId, inventory, player) -> new AttributePanelMenu(containerId, inventory, target, snapshot),
                            snapshot.name()
                    ),
                    snapshot::encode
            );
            NetworkHandler.sendPanel(sender, snapshot);
        });
        context.setPacketHandled(true);
    }

    private static LivingEntity resolve(ServerPlayer sender, int id) {
        if (id < 0 || id == sender.getId()) {
            return sender;
        }
        Entity entity = sender.level().getEntity(id);
        if (!(entity instanceof LivingEntity living)) {
            return null;
        }
        if (sender.getVehicle() == living) {
            return living;
        }
        return sender.distanceToSqr(living) <= 64.0D ? living : null;
    }
}
