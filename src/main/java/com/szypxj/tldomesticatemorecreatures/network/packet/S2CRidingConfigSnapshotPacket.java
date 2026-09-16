package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.client.riding.ClientRidingConfigCache;
import com.szypxj.tldomesticatemorecreatures.client.riding.RidingEditorScreen;
import net.minecraft.client.Minecraft;
import com.szypxj.tldomesticatemorecreatures.riding.config.EntityRideProfile;
import com.szypxj.tldomesticatemorecreatures.riding.config.RidingConfigSnapshot;
import com.szypxj.tldomesticatemorecreatures.riding.config.RidingNetworkCodec;
import com.szypxj.tldomesticatemorecreatures.riding.config.RidingSettings;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.function.Supplier;

public record S2CRidingConfigSnapshotPacket(RidingConfigSnapshot snapshot, boolean openEditor) {
    public S2CRidingConfigSnapshotPacket(RidingConfigSnapshot snapshot) {
        this(snapshot, false);
    }
    public static void encode(S2CRidingConfigSnapshotPacket packet, FriendlyByteBuf buffer) {
        RidingConfigSnapshot snapshot = packet.snapshot();
        buffer.writeVarLong(snapshot.generation());
        RidingNetworkCodec.writeSettings(buffer, snapshot.settings());
        buffer.writeVarInt(snapshot.modifiedProfiles().size());
        for (EntityRideProfile profile : snapshot.modifiedProfiles().values()) {
            RidingNetworkCodec.writeProfile(buffer, profile);
        }
        buffer.writeVarInt(snapshot.configurableEntityIds().size());
        for (ResourceLocation entityId : snapshot.configurableEntityIds()) {
            buffer.writeResourceLocation(entityId);
        }
        buffer.writeBoolean(packet.openEditor());
    }

    public static S2CRidingConfigSnapshotPacket decode(FriendlyByteBuf buffer) {
        long generation = buffer.readVarLong();
        RidingSettings settings = RidingNetworkCodec.readSettings(buffer);
        int size = Math.max(0, Math.min(65536, buffer.readVarInt()));
        Map<ResourceLocation, EntityRideProfile> modifiedProfiles = new LinkedHashMap<>();
        for (int i = 0; i < size; i++) {
            EntityRideProfile profile = RidingNetworkCodec.readProfile(buffer);
            modifiedProfiles.put(profile.entityId(), profile);
        }
        int configurableSize = Math.max(0, Math.min(65536, buffer.readVarInt()));
        Set<ResourceLocation> configurableEntityIds = new LinkedHashSet<>();
        for (int i = 0; i < configurableSize; i++) {
            configurableEntityIds.add(buffer.readResourceLocation());
        }
        return new S2CRidingConfigSnapshotPacket(new RidingConfigSnapshot(generation, settings, modifiedProfiles, configurableEntityIds), buffer.readBoolean());
    }

    public static void handle(S2CRidingConfigSnapshotPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> {
                    ClientRidingConfigCache.applySnapshot(packet.snapshot());
                    Minecraft minecraft = Minecraft.getInstance();
                    if (packet.openEditor()) {
                        minecraft.setScreen(new RidingEditorScreen());
                    } else if (minecraft.screen instanceof RidingEditorScreen screen) {
                        screen.onServerSnapshot(packet.snapshot());
                    }
                }
        ));
        context.setPacketHandled(true);
    }
}
