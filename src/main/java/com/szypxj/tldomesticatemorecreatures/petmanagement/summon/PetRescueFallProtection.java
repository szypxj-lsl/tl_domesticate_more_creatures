package com.szypxj.tldomesticatemorecreatures.petmanagement.summon;

import com.szypxj.tldomesticatemorecreatures.TlDomesticateMoreCreatures;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = TlDomesticateMoreCreatures.MOD_ID)
public final class PetRescueFallProtection {
    private static final Set<UUID> PROTECTED_PLAYERS = new HashSet<>();
    private static final Set<UUID> PROTECTED_MOUNTS = new HashSet<>();
    private static final Map<UUID, UUID> PLAYER_MOUNTS = new HashMap<>();
    private static final Set<UUID> RELEASE_WHEN_GROUNDED = new HashSet<>();

    private PetRescueFallProtection() {
    }

    public static void protect(ServerPlayer player) {
        protect(player, null);
    }

    public static void protect(ServerPlayer player, LivingEntity mount) {
        if (player == null) return;
        UUID playerUuid = player.getUUID();
        PROTECTED_PLAYERS.add(playerUuid);
        RELEASE_WHEN_GROUNDED.remove(playerUuid);
        player.fallDistance = 0.0F;
        if (mount != null) {
            UUID oldMount = PLAYER_MOUNTS.put(playerUuid, mount.getUUID());
            if (oldMount != null) PROTECTED_MOUNTS.remove(oldMount);
            PROTECTED_MOUNTS.add(mount.getUUID());
            mount.fallDistance = 0.0F;
        }
    }

    public static void clear(UUID playerUuid) {
        if (playerUuid == null) return;
        PROTECTED_PLAYERS.remove(playerUuid);
        RELEASE_WHEN_GROUNDED.remove(playerUuid);
        UUID mountUuid = PLAYER_MOUNTS.remove(playerUuid);
        if (mountUuid != null) PROTECTED_MOUNTS.remove(mountUuid);
    }

    public static void releaseWhenGrounded(UUID playerUuid) {
        if (playerUuid == null || !PROTECTED_PLAYERS.contains(playerUuid)) return;
        RELEASE_WHEN_GROUNDED.add(playerUuid);
    }

    public static boolean isProtected(UUID playerUuid) {
        return playerUuid != null && PROTECTED_PLAYERS.contains(playerUuid);
    }

    public static void tick(MinecraftServer server) {
        if (server == null || PROTECTED_PLAYERS.isEmpty()) return;
        for (UUID playerUuid : new HashSet<>(PROTECTED_PLAYERS)) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
            if (player == null || !player.isAlive() || player.isSpectator()) {
                clear(playerUuid);
                continue;
            }
            player.fallDistance = 0.0F;
            LivingEntity mount = protectedMount(player);
            if (mount != null) mount.fallDistance = 0.0F;
            if (RELEASE_WHEN_GROUNDED.contains(playerUuid)
                    && (player.onGround() || (mount != null && mount.onGround()))) {
                clear(playerUuid);
            }
        }
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        LivingEntity entity = event.getEntity();
        boolean protectedPlayer = entity instanceof ServerPlayer player && isProtected(player.getUUID());
        boolean protectedMount = PROTECTED_MOUNTS.contains(entity.getUUID());
        if (!protectedPlayer && !protectedMount) return;
        entity.fallDistance = 0.0F;
        event.setCanceled(true);
    }

    private static LivingEntity protectedMount(ServerPlayer player) {
        UUID mountUuid = PLAYER_MOUNTS.get(player.getUUID());
        if (mountUuid == null) return null;
        if (player.getVehicle() instanceof LivingEntity vehicle && mountUuid.equals(vehicle.getUUID())) return vehicle;
        Entity entity = player.serverLevel().getEntity(mountUuid);
        return entity instanceof LivingEntity living ? living : null;
    }
}
