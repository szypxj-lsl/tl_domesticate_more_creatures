package com.szypxj.tldomesticatemorecreatures.client.riding;

import com.mojang.blaze3d.platform.InputConstants;
import com.szypxj.tldomesticatemorecreatures.TlDomesticateMoreCreatures;
import com.szypxj.tldomesticatemorecreatures.client.ClientKeys;
import com.szypxj.tldomesticatemorecreatures.client.petmanagement.PetShortcutRadialScreen;
import com.szypxj.tldomesticatemorecreatures.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = TlDomesticateMoreCreatures.MOD_ID, value = Dist.CLIENT)
public final class RideActionKeyHandler {
    public static final int HOLD_TICKS = 8;

    private static boolean keyWasDown;
    private static boolean suppressUntilRelease;
    private static int heldTicks;
    private static int pressedTargetEntityId = -1;
    private static final boolean[] numberWasDown = new boolean[9];

    private RideActionKeyHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            reset();
            return;
        }

        boolean pressedEvent = false;
        while (ClientKeys.RIDE.consumeClick()) pressedEvent = true;
        boolean keyDown = isActionKeyPhysicallyDown();

        if (minecraft.screen != null) {
            if (minecraft.screen instanceof PetShortcutRadialScreen) suppressUntilRelease = true;
            if (!keyDown) resetPressState();
            else keyWasDown = true;
            return;
        }

        if (suppressUntilRelease) {
            if (!keyDown) resetPressState();
            return;
        }

        if (pressedEvent || (keyDown && !keyWasDown)) {
            keyWasDown = true;
            heldTicks = 0;
            pressedTargetEntityId = captureTargetEntityId(minecraft);
            if (!keyDown) {
                executeShortPress();
                resetPressState();
                return;
            }
        }

        if (keyDown) {
            int shortcut = consumeShortcutNumber(minecraft);
            if (shortcut > 0) {
                NetworkHandler.summonPetShortcut(shortcut);
                suppressUntilRelease = true;
                return;
            }
            heldTicks++;
            if (heldTicks >= HOLD_TICKS) {
                suppressUntilRelease = true;
                minecraft.setScreen(new PetShortcutRadialScreen());
            }
            return;
        }

        clearNumberState();
        if (keyWasDown) {
            keyWasDown = false;
            if (heldTicks > 0 && heldTicks < HOLD_TICKS) executeShortPress();
            heldTicks = 0;
        }
    }

    public static boolean isActionKeyPhysicallyDown() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) return false;
        InputConstants.Key key = InputConstants.getKey(ClientKeys.RIDE.saveString());
        long window = minecraft.getWindow().getWindow();
        if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
        }
        return InputConstants.isKeyDown(window, key.getValue());
    }

    private static int consumeShortcutNumber(Minecraft minecraft) {
        long window = minecraft.getWindow().getWindow();
        int result = -1;
        for (int i = 0; i < 9; i++) {
            int glfwKey = GLFW.GLFW_KEY_1 + i;
            boolean down = InputConstants.isKeyDown(window, glfwKey);
            while (minecraft.options.keyHotbarSlots[i].consumeClick()) {
                if (!numberWasDown[i] && down) result = i + 1;
            }
            if (down && !numberWasDown[i]) result = i + 1;
            numberWasDown[i] = down;
        }
        return result;
    }

    private static void executeShortPress() {
        NetworkHandler.requestRideMount(pressedTargetEntityId);
    }

    private static int captureTargetEntityId(Minecraft minecraft) {
        if (minecraft.player != null && minecraft.player.getVehicle() instanceof LivingEntity mount) return mount.getId();
        if (minecraft.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity living) return living.getId();
        return -1;
    }

    private static void clearNumberState() {
        for (int i = 0; i < numberWasDown.length; i++) numberWasDown[i] = false;
    }

    private static void resetPressState() {
        keyWasDown = false;
        suppressUntilRelease = false;
        heldTicks = 0;
        pressedTargetEntityId = -1;
        clearNumberState();
    }

    private static void reset() {
        resetPressState();
    }
}
