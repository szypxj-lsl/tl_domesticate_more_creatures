package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.client.talent.ClientActiveTalentState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record S2CActiveTalentVisualPacket(
        int mountEntityId,
        int targetEntityId,
        VisualType type,
        float value,
        double startX,
        double startY,
        double startZ,
        double endX,
        double endY,
        double endZ
) {
    public enum VisualType {
        SHADOWSTEP_START,
        SHADOWSTEP_MARK,
        SHADOWSTEP_CLEAR,
        SHADOWSTEP_TRAIL,
        SHADOWSTEP_HIT,
        CAMOUFLAGE_START,
        CAMOUFLAGE_END
    }

    public S2CActiveTalentVisualPacket(int mountEntityId, int targetEntityId, VisualType type, float value) {
        this(mountEntityId, targetEntityId, type, value,
                Double.NaN, Double.NaN, Double.NaN,
                Double.NaN, Double.NaN, Double.NaN);
    }

    public static S2CActiveTalentVisualPacket shadowstepTrail(int mountEntityId, int targetEntityId, Vec3 start, Vec3 end) {
        return new S2CActiveTalentVisualPacket(
                mountEntityId,
                targetEntityId,
                VisualType.SHADOWSTEP_TRAIL,
                1.0F,
                start.x,
                start.y,
                start.z,
                end.x,
                end.y,
                end.z
        );
    }

    public boolean hasTrailSegment() {
        return Double.isFinite(startX) && Double.isFinite(startY) && Double.isFinite(startZ)
                && Double.isFinite(endX) && Double.isFinite(endY) && Double.isFinite(endZ);
    }

    public static void encode(S2CActiveTalentVisualPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.mountEntityId());
        buffer.writeVarInt(packet.targetEntityId());
        buffer.writeEnum(packet.type());
        buffer.writeFloat(packet.value());
        buffer.writeDouble(packet.startX());
        buffer.writeDouble(packet.startY());
        buffer.writeDouble(packet.startZ());
        buffer.writeDouble(packet.endX());
        buffer.writeDouble(packet.endY());
        buffer.writeDouble(packet.endZ());
    }

    public static S2CActiveTalentVisualPacket decode(FriendlyByteBuf buffer) {
        return new S2CActiveTalentVisualPacket(
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readEnum(VisualType.class),
                buffer.readFloat(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble()
        );
    }

    public static void handle(S2CActiveTalentVisualPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientActiveTalentState.applyVisual(packet)));
        context.setPacketHandled(true);
    }
}
