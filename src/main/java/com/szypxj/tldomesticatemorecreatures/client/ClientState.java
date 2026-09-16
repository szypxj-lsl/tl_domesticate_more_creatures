package com.szypxj.tldomesticatemorecreatures.client;

import com.szypxj.tldomesticatemorecreatures.command.pet.PetCommandSummary;
import com.szypxj.tldomesticatemorecreatures.network.InspectSnapshot;
import com.szypxj.tldomesticatemorecreatures.network.PanelSnapshot;
import com.szypxj.tldomesticatemorecreatures.network.PetManagementDetail;
import com.szypxj.tldomesticatemorecreatures.network.PetManagementSummary;
import com.szypxj.tldomesticatemorecreatures.network.TargetHudSnapshot;
import com.szypxj.tldomesticatemorecreatures.riding.RideEnvironment;
import net.minecraft.client.Minecraft;

import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientState {
    private static final Map<Integer, Integer> LEVELS = new ConcurrentHashMap<>();
    private static final Map<Integer, Long> IMPRINT_END_TICKS = new ConcurrentHashMap<>();
    private static volatile InspectSnapshot inspect;
    private static volatile long inspectTick;
    private static volatile TargetHudSnapshot targetHud;
    private static volatile long targetHudTick;
    private static volatile Set<Integer> selectedPetIds = Set.of();
    private static final Set<Integer> ownedPetIds = ConcurrentHashMap.newKeySet();
    private static volatile PetCommandSummary petCommandSummary = PetCommandSummary.NONE;
    private static volatile boolean petCommandExplicitSelection;
    private static volatile boolean petCommandHasLandCapablePets;
    private static final Set<Integer> genericRideMountIds = ConcurrentHashMap.newKeySet();
    private static volatile int genericRideMountId = -1;
    private static volatile RideEnvironment genericRideEnvironment = RideEnvironment.GROUND;
    private static volatile long genericRideGeneration;
    private static volatile int panelCompanionEntityId = -1;
    private static volatile int inventoryReturnPanelEntityId = -1;
    private static volatile List<PetManagementSummary> petManagementList = List.of();
    private static volatile PetManagementDetail petManagementDetail;
    private static volatile int petManagementCapacity;
    private static volatile int petManagementStoredCount;

    private ClientState() {
    }

    public static void openPanel(PanelSnapshot snapshot) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof AttributePanelScreen screen && screen.entityId() == snapshot.entityId()) {
            screen.updateSnapshot(snapshot);
        }
    }

    public static void setLevel(int entityId, int level) {
        LEVELS.put(entityId, level);
    }

    public static Integer level(int entityId) {
        return LEVELS.get(entityId);
    }

    public static void removeLevel(int entityId) {
        LEVELS.remove(entityId);
    }


    public static void setImprintState(int entityId, boolean active, long remainingTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!active || remainingTicks <= 0L || minecraft.level == null) {
            IMPRINT_END_TICKS.remove(entityId);
            return;
        }
        long now = minecraft.level.getGameTime();
        long end = remainingTicks > Long.MAX_VALUE - now ? Long.MAX_VALUE : now + remainingTicks;
        IMPRINT_END_TICKS.put(entityId, end);
    }

    public static long imprintRemainingTicks(int entityId) {
        Minecraft minecraft = Minecraft.getInstance();
        Long end = IMPRINT_END_TICKS.get(entityId);
        if (end == null || minecraft.level == null) {
            return -1L;
        }
        long remaining = end - minecraft.level.getGameTime();
        if (remaining <= 0L) {
            IMPRINT_END_TICKS.remove(entityId);
            return -1L;
        }
        return remaining;
    }

    public static boolean hasActiveImprint(int entityId) {
        return imprintRemainingTicks(entityId) >= 0L;
    }

    public static void removeImprintState(int entityId) {
        IMPRINT_END_TICKS.remove(entityId);
    }

    public static void setInspect(InspectSnapshot snapshot) {
        inspect = snapshot;
        Minecraft minecraft = Minecraft.getInstance();
        inspectTick = minecraft.level == null ? 0L : minecraft.level.getGameTime();
    }

    public static InspectSnapshot inspect() {
        return inspect;
    }

    public static boolean inspectFresh() {
        Minecraft minecraft = Minecraft.getInstance();
        return inspect != null && minecraft.level != null && minecraft.level.getGameTime() - inspectTick <= 20L;
    }


    public static void setTargetHud(TargetHudSnapshot snapshot) {
        targetHud = snapshot;
        Minecraft minecraft = Minecraft.getInstance();
        targetHudTick = minecraft.level == null ? 0L : minecraft.level.getGameTime();
    }

    public static TargetHudSnapshot targetHud() {
        return targetHud;
    }

    public static boolean targetHudFresh() {
        Minecraft minecraft = Minecraft.getInstance();
        return targetHud != null && minecraft.level != null && minecraft.level.getGameTime() - targetHudTick <= 20L;
    }

    public static void clearTargetHud() {
        targetHud = null;
        targetHudTick = 0L;
    }

    public static void setSelectedPetIds(Iterable<Integer> entityIds) {
        Set<Integer> result = new HashSet<>();
        for (Integer entityId : entityIds) {
            if (entityId != null && entityId >= 0) {
                result.add(entityId);
            }
        }
        selectedPetIds = Set.copyOf(result);
    }

    public static Set<Integer> selectedPetIds() {
        return selectedPetIds;
    }

    public static void setOwnedPet(int entityId, boolean ownedByPlayer) {
        if (ownedByPlayer) {
            ownedPetIds.add(entityId);
        } else {
            ownedPetIds.remove(entityId);
        }
    }

    public static boolean isOwnedPet(int entityId) {
        return ownedPetIds.contains(entityId);
    }

    public static void removeOwnedPet(int entityId) {
        ownedPetIds.remove(entityId);
    }


    public static void setPetCommandSummary(PetCommandSummary summary, boolean explicitSelection, boolean hasLandCapablePets) {
        petCommandSummary = summary == null ? PetCommandSummary.NONE : summary;
        petCommandExplicitSelection = explicitSelection;
        petCommandHasLandCapablePets = hasLandCapablePets;
    }

    public static PetCommandSummary petCommandSummary() {
        return petCommandSummary;
    }

    public static boolean petCommandExplicitSelection() {
        return petCommandExplicitSelection;
    }

    public static boolean petCommandHasLandCapablePets() {
        return petCommandHasLandCapablePets;
    }

    public static void setGenericRideState(int mountEntityId, int riderEntityId, boolean active, RideEnvironment environment, long generation) {
        if (active) {
            genericRideMountIds.add(mountEntityId);
        } else {
            genericRideMountIds.remove(mountEntityId);
        }
        Minecraft minecraft = Minecraft.getInstance();
        boolean localRider = minecraft.player != null && minecraft.player.getId() == riderEntityId;
        if (localRider) {
            genericRideMountId = active ? mountEntityId : -1;
            genericRideEnvironment = environment == null ? RideEnvironment.GROUND : environment;
            genericRideGeneration = generation;
        } else if (!active && genericRideMountId == mountEntityId) {
            genericRideMountId = -1;
            genericRideEnvironment = RideEnvironment.GROUND;
            genericRideGeneration = generation;
        }
    }


    public static void setPanelCompanionEntityId(int entityId) {
        panelCompanionEntityId = entityId;
    }

    public static int panelCompanionEntityId() {
        return panelCompanionEntityId;
    }

    public static void clearPanelCompanionEntityId() {
        panelCompanionEntityId = -1;
    }

    public static void setInventoryReturnPanelEntityId(int entityId) {
        inventoryReturnPanelEntityId = entityId;
    }

    public static int inventoryReturnPanelEntityId() {
        return inventoryReturnPanelEntityId;
    }

    public static void clearInventoryReturnPanelEntityId() {
        inventoryReturnPanelEntityId = -1;
        petManagementList = List.of();
        petManagementDetail = null;
        petManagementCapacity = 0;
        petManagementStoredCount = 0;
    }

    public static void setPetManagementList(List<PetManagementSummary> pets, int storedCount, int capacity) {
        petManagementList = pets == null ? List.of() : List.copyOf(pets);
        petManagementStoredCount = Math.max(0, storedCount);
        petManagementCapacity = Math.max(0, capacity);
    }

    public static List<PetManagementSummary> petManagementList() { return petManagementList; }
    public static int petManagementStoredCount() { return petManagementStoredCount; }
    public static int petManagementCapacity() { return petManagementCapacity; }

    public static void setPetManagementDetail(PetManagementDetail detail) {
        petManagementDetail = detail;
    }

    public static PetManagementDetail petManagementDetail() { return petManagementDetail; }

    public static boolean hasGenericRide() { return genericRideMountId >= 0; }
    public static int genericRideMountId() { return genericRideMountId; }
    public static boolean isGenericRideMount(int entityId) { return genericRideMountIds.contains(entityId); }
    public static RideEnvironment genericRideEnvironment() { return genericRideEnvironment; }
    public static long genericRideGeneration() { return genericRideGeneration; }

    public static void clearInspect() {
        inspect = null;
        inspectTick = 0L;
    }

    public static void clearAll() {
        LEVELS.clear();
        IMPRINT_END_TICKS.clear();
        selectedPetIds = Set.of();
        ownedPetIds.clear();
        petCommandSummary = PetCommandSummary.NONE;
        petCommandExplicitSelection = false;
        petCommandHasLandCapablePets = false;
        genericRideMountIds.clear();
        genericRideMountId = -1;
        genericRideEnvironment = RideEnvironment.GROUND;
        genericRideGeneration = 0L;
        panelCompanionEntityId = -1;
        inventoryReturnPanelEntityId = -1;
        petManagementList = List.of();
        petManagementDetail = null;
        petManagementCapacity = 0;
        petManagementStoredCount = 0;
        clearInspect();
        clearTargetHud();
    }
}
