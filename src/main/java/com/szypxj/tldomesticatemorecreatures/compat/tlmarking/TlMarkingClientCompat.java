package com.szypxj.tldomesticatemorecreatures.compat.tlmarking;

import com.mojang.blaze3d.platform.InputConstants;
import com.szypxj.tldomesticatemorecreatures.client.ClientState;
import com.szypxj.tldomesticatemorecreatures.client.pet.PetCommandRadialScreen;
import com.szypxj.tldomesticatemorecreatures.client.pet.PetCommandTargetSnapshot;
import com.szypxj.tldomesticatemorecreatures.command.pet.PetCommand;
import com.szypxj.tldomesticatemorecreatures.command.pet.PetSelectionMode;
import com.szypxj.tldomesticatemorecreatures.config.Config;
import com.szypxj.tldomesticatemorecreatures.game.PetOwnershipService;
import com.szypxj.tldomesticatemorecreatures.network.NetworkHandler;
import com.szypxj.tlmarking.compat.client.ClientCompatManager;
import com.szypxj.tlmarking.input.PingKeyHandler;
import com.szypxj.tlmarking.input.PingSinglePressHooks;
import com.szypxj.tlmarking.input.PingRadialExtensionHooks;
import com.szypxj.tlmarking.ping.PingCategory;
import com.szypxj.tlmarking.ping.PingInstance;
import com.szypxj.tlmarking.ping.PingManager;
import com.szypxj.tlmarking.ping.PingTargetKind;
import com.szypxj.tlmarking.ping.PingTypeData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class TlMarkingClientCompat {
    private static final TlMarkingClientCompat INSTANCE = new TlMarkingClientCompat();
    private static final PingTypeData SELECTED_TYPE = new PingTypeData(
            "tdmc_selected_pet",
            "ping.tl_domesticate_more_creatures.selected_pet",
            0x55FF88,
            100
    );
    private static final Map<PetCommand, PingTypeData> COMMAND_TYPES = createCommandTypes();
    private final Map<UUID, Integer> markerEntityIds = new HashMap<>();

    private TlMarkingClientCompat() {
    }

    public static void register() {
        PingSinglePressHooks.register(INSTANCE::handleSinglePress);
        PingRadialExtensionHooks.register(INSTANCE::openPetCommandWheel);
    }

    public static void refreshSelectionVisual() {
        INSTANCE.syncSelectionVisual();
    }

    public static boolean handleConfirmedSingle(Object rawTarget, UUID selectedPingId) {
        if (!(rawTarget instanceof PingKeyHandler.TargetHit target)) {
            return false;
        }
        return INSTANCE.handleSinglePress(target, selectedPingId);
    }

    public static void showCommandMarker(
            UUID markerId,
            PetCommand command,
            int targetEntityId,
            UUID targetEntityUuid,
            Vec3 position
    ) {
        INSTANCE.showCommandMarkerInternal(markerId, command, targetEntityId, targetEntityUuid, position);
    }

    public static void removeCommandMarker(UUID markerId) {
        INSTANCE.removeCommandMarkerInternal(markerId);
    }

    private static Map<PetCommand, PingTypeData> createCommandTypes() {
        Map<PetCommand, PingTypeData> types = new EnumMap<>(PetCommand.class);
        for (PetCommand command : PetCommand.values()) {
            String id = command.name().toLowerCase(Locale.ROOT);
            types.put(command, new PingTypeData(
                    "tdmc_command_" + id,
                    "gui.tl_domesticate_more_creatures.pet_command." + id,
                    0x55FF88,
                    95
            ));
        }
        return Map.copyOf(types);
    }

    private void showCommandMarkerInternal(
            UUID markerId,
            PetCommand command,
            int targetEntityId,
            UUID targetEntityUuid,
            Vec3 position
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (markerId == null || command == null || minecraft.level == null || minecraft.player == null) {
            return;
        }

        Entity target = targetEntityId >= 0 ? minecraft.level.getEntity(targetEntityId) : null;
        Integer entityId = targetEntityId >= 0 ? targetEntityId : null;
        UUID entityUuid = targetEntityUuid != null ? targetEntityUuid : target == null ? null : target.getUUID();
        Vec3 markerPosition = target != null
                ? target.position()
                : position == null ? minecraft.player.position() : position;
        String literalName = target == null ? "" : target.getName().getString();
        long now = minecraft.level.getGameTime();
        PingInstance ping = new PingInstance(
                markerId,
                minecraft.player.getUUID(),
                minecraft.player.getGameProfile().getName(),
                COMMAND_TYPES.get(command),
                PingCategory.MANUAL,
                entityId == null ? PingTargetKind.POSITION : PingTargetKind.ENTITY,
                markerPosition,
                entityId,
                entityUuid,
                null,
                null,
                literalName,
                "",
                ItemStack.EMPTY,
                now,
                now,
                now,
                entityId != null,
                false,
                0L
        );
        ClientCompatManager.removePingFromOptionalMaps(markerId);
        PingManager.get(minecraft.level).addOrUpdateClient(ping);
    }

    private void removeCommandMarkerInternal(UUID markerId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (markerId == null || minecraft.level == null) {
            return;
        }
        PingManager.get(minecraft.level).removePingClient(markerId);
        ClientCompatManager.removePingFromOptionalMaps(markerId);
    }

    private boolean openPetCommandWheel(PingRadialExtensionHooks.Context context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return false;
        }
        PetCommandTargetSnapshot target = PetCommandTargetSnapshot.from(context);
        minecraft.setScreen(new PetCommandRadialScreen(target, TlMarkingClientCompat::isPingKeyPhysicallyDown));
        return true;
    }


    private static boolean isPingKeyPhysicallyDown() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return false;
        }
        InputConstants.Key key = PingKeyHandler.PING_KEY.getKey();
        long window = minecraft.getWindow().getWindow();
        if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
        }
        return InputConstants.isKeyDown(window, key.getValue());
    }

    private boolean handleSinglePress(PingKeyHandler.TargetHit target, UUID selectedPingId) {
        Integer selectedEntityId = selectedPingId == null ? null : markerEntityIds.get(selectedPingId);
        if (selectedEntityId != null) {
            sendSelection(selectedEntityId);
            return true;
        }

        if (selectedPingId != null || target == null) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return false;
        }

        double range = Config.PET_COMMAND_RANGE.get();
        double rangeSqr = range * range;

        if (target.entityHit() != null) {
            Entity entity = target.entityHit().getEntity();
            if (!(entity instanceof LivingEntity living)) {
                return false;
            }
            if (minecraft.player.distanceToSqr(living) > rangeSqr) {
                return false;
            }

            if (ClientState.isOwnedPet(living.getId()) || PetOwnershipService.isOwnedBy(living, minecraft.player)) {
                sendSelection(living.getId());
                return true;
            }

            if (ClientState.selectedPetIds().isEmpty() || isQuickAttackTargetFriendly(living, minecraft.player)) {
                return false;
            }

            NetworkHandler.issuePetCommand(PetCommand.ATTACK, living.getId(), target.hitPosition());
            return true;
        }

        if (ClientState.selectedPetIds().isEmpty() || target.blockHit() == null) {
            return false;
        }
        if (minecraft.player.position().distanceToSqr(target.hitPosition()) > rangeSqr) {
            return false;
        }

        NetworkHandler.issuePetCommand(PetCommand.MOVE, -1, target.hitPosition());
        return true;
    }

    private static boolean isQuickAttackTargetFriendly(LivingEntity target, Player player) {
        return target == player
                || player.isAlliedTo(target)
                || target.isAlliedTo(player)
                || ClientState.isOwnedPet(target.getId())
                || PetOwnershipService.isOwnedBy(target, player);
    }

    private static void sendSelection(int entityId) {
        NetworkHandler.selectPet(
                entityId,
                Screen.hasShiftDown() ? PetSelectionMode.MULTI_TOGGLE : PetSelectionMode.SINGLE
        );
    }

    private void syncSelectionVisual() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            markerEntityIds.clear();
            return;
        }

        PingManager manager = PingManager.get(minecraft.level);
        Map<UUID, Integer> desired = new HashMap<>();
        long now = minecraft.level.getGameTime();

        for (int entityId : ClientState.selectedPetIds()) {
            Entity entity = minecraft.level.getEntity(entityId);
            if (entity == null || entity.isRemoved()) {
                continue;
            }
            UUID markerId = markerId(entity.getUUID());
            desired.put(markerId, entityId);
            PingInstance ping = new PingInstance(
                    markerId,
                    minecraft.player.getUUID(),
                    minecraft.player.getGameProfile().getName(),
                    SELECTED_TYPE,
                    PingCategory.MANUAL,
                    PingTargetKind.ENTITY,
                    entity.position(),
                    entity.getId(),
                    entity.getUUID(),
                    null,
                    null,
                    entity.getName().getString(),
                    "",
                    ItemStack.EMPTY,
                    now,
                    now,
                    now,
                    true,
                    false,
                    0L
            );
            ClientCompatManager.removePingFromOptionalMaps(markerId);
            manager.addOrUpdateClient(ping);
        }

        for (UUID old : Map.copyOf(markerEntityIds).keySet()) {
            if (!desired.containsKey(old)) {
                manager.removePingClient(old);
                ClientCompatManager.removePingFromOptionalMaps(old);
            }
        }
        markerEntityIds.clear();
        markerEntityIds.putAll(desired);
    }

    private static UUID markerId(UUID entityUuid) {
        return UUID.nameUUIDFromBytes(("tdmc-selection:" + entityUuid).getBytes(StandardCharsets.UTF_8));
    }
}
