package com.szypxj.tldomesticatemorecreatures.client.talent;

import com.szypxj.tldomesticatemorecreatures.network.packet.S2CActiveTalentStatePacket;
import com.szypxj.tldomesticatemorecreatures.network.packet.S2CActiveTalentVisualPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class ClientActiveTalentState {
    private static final Map<Integer, Snapshot> STATES = new ConcurrentHashMap<>();
    private static final Map<Integer, Float> CAMOUFLAGE_ALPHA = new ConcurrentHashMap<>();
    private static final Map<Integer, Integer> MARKED_TARGETS = new ConcurrentHashMap<>();
    private static final ConcurrentLinkedQueue<VisualEvent> VISUALS = new ConcurrentLinkedQueue<>();

    private ClientActiveTalentState() {
    }

    public static void applyState(S2CActiveTalentStatePacket packet) {
        if (packet == null) return;
        Snapshot previous = STATES.get(packet.mountEntityId());
        if (previous != null && "MARKING".equals(previous.phase()) && !"MARKING".equals(packet.phase())) MARKED_TARGETS.clear();
        STATES.put(packet.mountEntityId(), new Snapshot(
                packet.skillId(), packet.phase(), packet.phaseEndGameTime(), packet.marks(), packet.maxMarks(), packet.cooldownEndGameTime()
        ));
    }

    public static void applyVisual(S2CActiveTalentVisualPacket packet) {
        if (packet == null) return;
        if (packet.type() == S2CActiveTalentVisualPacket.VisualType.SHADOWSTEP_MARK && packet.targetEntityId() >= 0) {
            MARKED_TARGETS.merge(packet.targetEntityId(), 1, Integer::sum);
        } else if (packet.type() == S2CActiveTalentVisualPacket.VisualType.SHADOWSTEP_START
                || packet.type() == S2CActiveTalentVisualPacket.VisualType.SHADOWSTEP_CLEAR) {
            MARKED_TARGETS.clear();
        }
        if (packet.type() == S2CActiveTalentVisualPacket.VisualType.CAMOUFLAGE_START) {
            CAMOUFLAGE_ALPHA.put(packet.mountEntityId(), packet.value());
            if (packet.targetEntityId() >= 0) CAMOUFLAGE_ALPHA.put(packet.targetEntityId(), packet.value());
        } else if (packet.type() == S2CActiveTalentVisualPacket.VisualType.CAMOUFLAGE_END) {
            CAMOUFLAGE_ALPHA.remove(packet.mountEntityId());
            if (packet.targetEntityId() >= 0) CAMOUFLAGE_ALPHA.remove(packet.targetEntityId());
        }
        VISUALS.offer(new VisualEvent(
                packet.mountEntityId(),
                packet.targetEntityId(),
                packet.type(),
                packet.value(),
                packet.startX(),
                packet.startY(),
                packet.startZ(),
                packet.endX(),
                packet.endY(),
                packet.endZ()
        ));
    }

    public static Snapshot currentForMountedPet() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !(minecraft.player.getVehicle() instanceof LivingEntity mount)) return null;
        return STATES.get(mount.getId());
    }

    public static Snapshot mountedSnapshot() {
        return currentForMountedPet();
    }

    public static int markedCount(int entityId) {
        return Math.max(0, MARKED_TARGETS.getOrDefault(entityId, 0));
    }

    public static float renderAlpha(int entityId) {
        return CAMOUFLAGE_ALPHA.getOrDefault(entityId, 1.0F);
    }

    public static VisualEvent consumeVisual() {
        return VISUALS.poll();
    }

    public static void removeEntity(int entityId) {
        STATES.remove(entityId);
        CAMOUFLAGE_ALPHA.remove(entityId);
        MARKED_TARGETS.remove(entityId);
    }

    public static void clear() {
        STATES.clear();
        CAMOUFLAGE_ALPHA.clear();
        MARKED_TARGETS.clear();
        VISUALS.clear();
    }

    public record Snapshot(String skillId, String phase, long phaseEndGameTime, int marks, int maxMarks, long cooldownEndGameTime) {
    }

    public record VisualEvent(
            int mountEntityId,
            int targetEntityId,
            S2CActiveTalentVisualPacket.VisualType type,
            float value,
            double startX,
            double startY,
            double startZ,
            double endX,
            double endY,
            double endZ
    ) {
        public boolean hasTrailSegment() {
            return Double.isFinite(startX) && Double.isFinite(startY) && Double.isFinite(startZ)
                    && Double.isFinite(endX) && Double.isFinite(endY) && Double.isFinite(endZ);
        }
    }
}
