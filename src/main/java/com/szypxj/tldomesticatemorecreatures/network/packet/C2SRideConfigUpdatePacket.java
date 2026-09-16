package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.network.NetworkHandler;
import com.szypxj.tldomesticatemorecreatures.riding.config.EntityRideProfile;
import com.szypxj.tldomesticatemorecreatures.riding.config.RidingConfigManager;
import com.szypxj.tldomesticatemorecreatures.riding.config.RidingNetworkCodec;
import com.szypxj.tldomesticatemorecreatures.riding.config.RidingSettings;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

public record C2SRideConfigUpdatePacket(UpdateKind kind, RidingSettings settings, EntityRideProfile profile) {
    public enum UpdateKind {
        SETTINGS,
        PROFILE
    }

    public static C2SRideConfigUpdatePacket settings(RidingSettings settings) {
        return new C2SRideConfigUpdatePacket(UpdateKind.SETTINGS, settings, null);
    }

    public static C2SRideConfigUpdatePacket profile(EntityRideProfile profile) {
        return new C2SRideConfigUpdatePacket(UpdateKind.PROFILE, null, profile);
    }

    public static void encode(C2SRideConfigUpdatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.kind());
        if (packet.kind() == UpdateKind.SETTINGS) {
            RidingNetworkCodec.writeSettings(buffer, packet.settings());
        } else {
            RidingNetworkCodec.writeProfile(buffer, packet.profile());
        }
    }

    public static C2SRideConfigUpdatePacket decode(FriendlyByteBuf buffer) {
        UpdateKind kind = buffer.readEnum(UpdateKind.class);
        return kind == UpdateKind.SETTINGS
                ? settings(RidingNetworkCodec.readSettings(buffer))
                : profile(RidingNetworkCodec.readProfile(buffer));
    }

    public static void handle(C2SRideConfigUpdatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null || !RidingConfigManager.mayManage(sender)) {
                if (sender != null) {
                    sender.sendSystemMessage(Component.translatable("msg.tl_domesticate_more_creatures.riding.no_permission"));
                }
                return;
            }
            if (packet.kind() == UpdateKind.SETTINGS) {
                RidingSettings settings = packet.settings() == null ? null : packet.settings().validated();
                RidingConfigManager.SaveResult result = RidingConfigManager.saveSettings(sender, settings);
                sender.sendSystemMessage(Component.translatable(result.messageKey()));
                if (result.success()) {
                    NetworkHandler.broadcastRidingSnapshot(RidingConfigManager.snapshot(sender.serverLevel()));
                }
                return;
            }
            EntityRideProfile profile = packet.profile() == null ? null : packet.profile().validated();
            if (profile == null || !ForgeRegistries.ENTITY_TYPES.containsKey(profile.entityId())) {
                sender.sendSystemMessage(Component.translatable("msg.tl_domesticate_more_creatures.riding.invalid_profile"));
                return;
            }
            RidingConfigManager.SaveResult result = RidingConfigManager.saveProfile(sender, profile);
            sender.sendSystemMessage(Component.translatable(result.messageKey()));
            if (result.success()) {
                NetworkHandler.broadcastRidingProfile(result.generation(), profile);
            }
        });
        context.setPacketHandled(true);
    }
}
