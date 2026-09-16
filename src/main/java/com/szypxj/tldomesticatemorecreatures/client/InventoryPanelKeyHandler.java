package com.szypxj.tldomesticatemorecreatures.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.szypxj.tldomesticatemorecreatures.TlDomesticateMoreCreatures;
import com.szypxj.tldomesticatemorecreatures.client.riding.RideActionRadialScreen;
import com.szypxj.tldomesticatemorecreatures.client.riding.RidePermissionRadialScreen;
import com.szypxj.tldomesticatemorecreatures.game.PetOwnershipService;
import com.szypxj.tldomesticatemorecreatures.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = TlDomesticateMoreCreatures.MOD_ID, value = Dist.CLIENT)
public final class InventoryPanelKeyHandler {
    public static final int HOLD_TICKS = 8;
    private static boolean keyWasDown;
    private static boolean suppressUntilRelease;
    private static int heldTicks;
    private static int pressedTargetEntityId = -1;

    private InventoryPanelKeyHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            resetKeyState();
            return;
        }
        boolean pressedEvent = false;
        while (minecraft.options.keyInventory.consumeClick()) pressedEvent = true;
        boolean keyDown = isInventoryKeyPhysicallyDown();

        if (minecraft.screen instanceof AttributePanelScreen && (pressedEvent || (keyDown && !keyWasDown))) {
            keyWasDown = true;
            suppressUntilRelease = true;
            heldTicks = 0;
            pressedTargetEntityId = -1;
            minecraft.screen.onClose();
            return;
        }

        if (minecraft.screen != null) {
            if (minecraft.screen instanceof RideActionRadialScreen || minecraft.screen instanceof RidePermissionRadialScreen) suppressUntilRelease = true;
            if (!keyDown) resetKeyState();
            else keyWasDown = true;
            return;
        }
        if (suppressUntilRelease) {
            if (!keyDown) resetKeyState();
            return;
        }
        if (pressedEvent || (keyDown && !keyWasDown)) {
            keyWasDown = true;
            heldTicks = 0;
            pressedTargetEntityId = captureTargetEntityId(minecraft);
            if (!keyDown) {
                executeInventoryShortPress(minecraft);
                resetKeyState();
                return;
            }
        }
        if (keyDown) {
            heldTicks++;
            if (pressedTargetEntityId >= 0 && !isValidPetWheelTarget(minecraft, pressedTargetEntityId)) {
                suppressUntilRelease = true;
                heldTicks = 0;
                pressedTargetEntityId = -1;
                return;
            }
            if (heldTicks >= HOLD_TICKS && pressedTargetEntityId >= 0) {
                suppressUntilRelease = true;
                minecraft.setScreen(new RideActionRadialScreen(pressedTargetEntityId));
                return;
            }
            if (heldTicks >= HOLD_TICKS && pressedTargetEntityId < 0) {
                suppressUntilRelease = true;
            }
            return;
        }
        if (keyWasDown) {
            if (heldTicks > 0 && heldTicks < HOLD_TICKS) executeInventoryShortPress(minecraft);
            resetKeyState();
        }
    }


    public static void suppressInventoryKeyUntilRelease() {
        keyWasDown = true;
        suppressUntilRelease = true;
        heldTicks = 0;
        pressedTargetEntityId = -1;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null) {
            while (minecraft.options.keyInventory.consumeClick()) {
            }
        }
    }

    public static boolean isInventoryKeyPhysicallyDown() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) return false;
        InputConstants.Key key = InputConstants.getKey(minecraft.options.keyInventory.saveString());
        long window = minecraft.getWindow().getWindow();
        if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
        }
        return InputConstants.isKeyDown(window, key.getValue());
    }

    private static void executeInventoryShortPress(Minecraft minecraft) {
        if (minecraft.player == null) return;
        if (minecraft.player.getVehicle() instanceof LivingEntity mount
                && mount.isAlive()
                && (ClientState.isOwnedPet(mount.getId()) || PetOwnershipService.isOwnedBy(mount, minecraft.player))) {
            NetworkHandler.openPanel(mount.getId());
            return;
        }
        minecraft.setScreen(new InventoryScreen(minecraft.player));
    }

    private static int captureTargetEntityId(Minecraft minecraft) {
        if (!(minecraft.hitResult instanceof EntityHitResult hit) || !(hit.getEntity() instanceof LivingEntity living)) return -1;
        return isValidPetWheelTarget(minecraft, living.getId()) ? living.getId() : -1;
    }

    private static boolean isValidPetWheelTarget(Minecraft minecraft, int entityId) {
        if (minecraft == null || minecraft.player == null || minecraft.level == null || entityId < 0) return false;
        if (!(minecraft.hitResult instanceof EntityHitResult hit) || hit.getEntity().getId() != entityId) return false;
        if (!(hit.getEntity() instanceof LivingEntity living) || !living.isAlive()) return false;
        return ClientState.isOwnedPet(entityId) || PetOwnershipService.isOwnedBy(living, minecraft.player);
    }

    private static void resetKeyState() {
        keyWasDown = false;
        suppressUntilRelease = false;
        heldTicks = 0;
        pressedTargetEntityId = -1;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onVanillaInventoryKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        Screen screen = event.getScreen();
        if (!(screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen)) return;
        if (screen.getFocused() instanceof EditBox editBox && editBox.isFocused()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.options.keyInventory.isActiveAndMatches(InputConstants.getKey(event.getKeyCode(), event.getScanCode()))) return;
        suppressInventoryKeyUntilRelease();
        event.setCanceled(true);
        screen.onClose();
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        if (!(screen instanceof InventoryScreen) && !(screen instanceof CreativeModeInventoryScreen)) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;

        boolean creative = screen instanceof CreativeModeInventoryScreen;
        int guiWidth = creative ? 195 : 176;
        int guiHeight = creative ? 136 : 166;
        int guiLeft = (screen.width - guiWidth) / 2;
        int guiTop = (screen.height - guiHeight) / 2;
        int buttonWidth = 52;
        int buttonHeight = 20;
        int x = guiLeft + guiWidth + 6;
        int y = guiTop + 6;
        if (x + buttonWidth > screen.width - 4) x = guiLeft + guiWidth - buttonWidth - 6;

        event.addListener(Button.builder(
                Component.translatable("gui.tl_domesticate_more_creatures.inventory_switch.tdmc"),
                pressed -> openTdmcReturnPanel()
        ).bounds(x, y, buttonWidth, buttonHeight).build());
    }

    private static void openTdmcReturnPanel() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        int playerEntityId = minecraft.player.getId();
        int targetEntityId = ClientState.inventoryReturnPanelEntityId();
        if (targetEntityId != playerEntityId) {
            if (targetEntityId < 0 || minecraft.level == null
                    || !(minecraft.level.getEntity(targetEntityId) instanceof LivingEntity living)
                    || !living.isAlive()) targetEntityId = playerEntityId;
        }
        ClientState.clearInventoryReturnPanelEntityId();
        if (targetEntityId == playerEntityId) ClientState.clearPanelCompanionEntityId();
        else ClientState.setPanelCompanionEntityId(targetEntityId);
        NetworkHandler.openPanel(targetEntityId);
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (!(event.getNewScreen() instanceof InventoryScreen) || event.getCurrentScreen() != null) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;
        ClientState.clearInventoryReturnPanelEntityId();
        int targetEntityId = -1;
        if (minecraft.player.getVehicle() instanceof LivingEntity mount) targetEntityId = mount.getId();
        else if (minecraft.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity living) targetEntityId = living.getId();
        if (minecraft.player.isCreative() && targetEntityId < 0) return;
        ClientState.clearPanelCompanionEntityId();
        event.setCanceled(true);
        NetworkHandler.openPanel(targetEntityId);
    }
}
