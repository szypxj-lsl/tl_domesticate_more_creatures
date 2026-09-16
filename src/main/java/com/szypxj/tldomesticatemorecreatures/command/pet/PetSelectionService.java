package com.szypxj.tldomesticatemorecreatures.command.pet;

import com.szypxj.tldomesticatemorecreatures.TlDomesticateMoreCreatures;
import com.szypxj.tldomesticatemorecreatures.config.Config;
import com.szypxj.tldomesticatemorecreatures.game.PetOwnershipService;
import com.szypxj.tldomesticatemorecreatures.network.NetworkHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = TlDomesticateMoreCreatures.MOD_ID)
public final class PetSelectionService {
    private static final Map<UUID, LinkedHashSet<UUID>> SELECTED = new ConcurrentHashMap<>();

    private PetSelectionService() {
    }

    public static boolean select(ServerPlayer player, LivingEntity pet, PetSelectionMode mode) {
        if (!PetOwnershipService.isOwnedBy(pet, player)) {
            return false;
        }
        LinkedHashSet<UUID> set = SELECTED.computeIfAbsent(player.getUUID(), ignored -> new LinkedHashSet<>());
        UUID id = pet.getUUID();
        PetSelectionLogic.apply(set, id, mode);
        if (set.isEmpty()) {
            SELECTED.remove(player.getUUID());
            return false;
        }
        return set.contains(id);
    }

    public static boolean hasSelection(ServerPlayer player) {
        LinkedHashSet<UUID> set = SELECTED.get(player.getUUID());
        return set != null && !set.isEmpty();
    }

    public static List<Integer> selectedEntityIds(ServerPlayer player) {
        List<LivingEntity> selected = resolveSelected(player);
        List<Integer> result = new ArrayList<>(selected.size());
        for (LivingEntity living : selected) {
            result.add(living.getId());
        }
        return List.copyOf(result);
    }

    public static Set<UUID> selectedIds(ServerPlayer player) {
        LinkedHashSet<UUID> set = SELECTED.get(player.getUUID());
        return set == null ? Set.of() : Set.copyOf(set);
    }

    public static List<LivingEntity> resolveSelected(ServerPlayer player) {
        LinkedHashSet<UUID> set = SELECTED.get(player.getUUID());
        if (set == null || set.isEmpty()) {
            return List.of();
        }
        double maxDistanceSq = (double) Config.PET_COMMAND_RANGE.get() * Config.PET_COMMAND_RANGE.get();
        List<LivingEntity> result = new ArrayList<>();
        set.removeIf(uuid -> {
            Entity entity = player.serverLevel().getEntity(uuid);
            if (!(entity instanceof LivingEntity living)
                    || !PetOwnershipService.isOwnedBy(living, player)
                    || player.distanceToSqr(living) > maxDistanceSq) {
                return true;
            }
            result.add(living);
            return false;
        });
        if (set.isEmpty()) {
            SELECTED.remove(player.getUUID());
        }
        return result;
    }

    public static void clear(ServerPlayer player) {
        SELECTED.remove(player.getUUID());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % 20 != 0 || SELECTED.isEmpty()) {
            return;
        }
        for (UUID playerId : List.copyOf(SELECTED.keySet())) {
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(playerId);
            if (player == null) {
                SELECTED.remove(playerId);
                continue;
            }
            Set<UUID> before = selectedIds(player);
            List<Integer> entityIds = selectedEntityIds(player);
            Set<UUID> after = selectedIds(player);
            if (!before.equals(after)) {
                NetworkHandler.sendPetSelection(player, entityIds);
            }
        }
    }
 }
