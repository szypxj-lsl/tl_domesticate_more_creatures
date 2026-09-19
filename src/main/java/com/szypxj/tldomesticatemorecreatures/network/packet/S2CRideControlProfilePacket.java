package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.api.riding.RideAction;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideActionInfo;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideCapabilities;
import com.szypxj.tldomesticatemorecreatures.client.riding.ClientRideControlState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public record S2CRideControlProfilePacket(
        int mountEntityId,
        boolean active,
        RideCapabilities capabilities,
        List<RideActionInfo> actions
) {
    private static final int MAX_ACTIONS = 16;
    private static final int MAX_KEY_LENGTH = 256;

    public S2CRideControlProfilePacket {
        capabilities = capabilities == null ? RideCapabilities.NONE : capabilities;
        actions = actions == null ? List.of() : List.copyOf(actions.stream().limit(MAX_ACTIONS).toList());
    }

    public static void encode(S2CRideControlProfilePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.mountEntityId());
        buffer.writeBoolean(packet.active());
        if (!packet.active()) {
            return;
        }
        RideCapabilities capabilities = packet.capabilities();
        buffer.writeBoolean(capabilities.groundMovement());
        buffer.writeBoolean(capabilities.flightMovement());
        buffer.writeBoolean(capabilities.swimMovement());
        buffer.writeBoolean(capabilities.jump());
        buffer.writeVarInt(packet.actions().size());
        for (RideActionInfo info : packet.actions()) {
            buffer.writeEnum(info.action());
            buffer.writeUtf(info.nameTranslationKey(), MAX_KEY_LENGTH);
            buffer.writeUtf(info.descriptionTranslationKey(), MAX_KEY_LENGTH);
            buffer.writeVarInt(info.defaultCooldownTicks());
            buffer.writeBoolean(info.holdable());
            buffer.writeBoolean(info.chargeable());
        }
    }

    public static S2CRideControlProfilePacket decode(FriendlyByteBuf buffer) {
        int mountId = buffer.readVarInt();
        boolean active = buffer.readBoolean();
        if (!active) {
            return new S2CRideControlProfilePacket(mountId, false, RideCapabilities.NONE, List.of());
        }
        boolean ground = buffer.readBoolean();
        boolean flight = buffer.readBoolean();
        boolean swim = buffer.readBoolean();
        boolean jump = buffer.readBoolean();
        int count = Math.max(0, Math.min(MAX_ACTIONS, buffer.readVarInt()));
        List<RideActionInfo> actions = new ArrayList<>(count);
        Set<RideAction> supported = EnumSet.noneOf(RideAction.class);
        for (int i = 0; i < count; i++) {
            RideAction action = buffer.readEnum(RideAction.class);
            String nameKey = buffer.readUtf(MAX_KEY_LENGTH);
            String descriptionKey = buffer.readUtf(MAX_KEY_LENGTH);
            int cooldown = Math.max(0, buffer.readVarInt());
            boolean holdable = buffer.readBoolean();
            boolean chargeable = buffer.readBoolean();
            supported.add(action);
            actions.add(new RideActionInfo(action, nameKey, descriptionKey, cooldown, holdable, chargeable));
        }
        return new S2CRideControlProfilePacket(
                mountId,
                true,
                new RideCapabilities(ground, flight, swim, jump, supported),
                actions
        );
    }

    public static void handle(S2CRideControlProfilePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientRideControlState.applyProfile(
                        packet.mountEntityId(), packet.active(), packet.capabilities(), packet.actions()
                )
        ));
        context.setPacketHandled(true);
    }
}
