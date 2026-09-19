package com.szypxj.tldomesticatemorecreatures.client.riding;

import com.szypxj.tldomesticatemorecreatures.TlDomesticateMoreCreatures;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideAction;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideActionInfo;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideInputPhase;
import com.szypxj.tldomesticatemorecreatures.client.ClientKeys;
import com.szypxj.tldomesticatemorecreatures.network.NetworkHandler;
import com.szypxj.tldomesticatemorecreatures.network.packet.C2SRideActionPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = TlDomesticateMoreCreatures.MOD_ID, value = Dist.CLIENT)
public final class RideControlClientHandler {
    private static final int HOLD_SEND_INTERVAL_TICKS = 4;
    private static final Map<RideAction, Boolean> DOWN = new EnumMap<>(RideAction.class);
    private static int sequence;
    private static int holdTicker;

    private RideControlClientHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        ClientRideControlState.clearIfMountMismatch(minecraft);
        if (minecraft.player == null || minecraft.level == null || !ClientRideControlState.activeForCurrentMount(minecraft)) {
            resetLocalState();
            return;
        }
        if (minecraft.screen != null) {
            releasePressedActions(minecraft);
            return;
        }

        holdTicker++;
        List<RideActionInfo> actions = ClientRideControlState.actions();
        for (RideActionInfo info : actions) {
            RideAction action = info.action();
            boolean down = isActionKeyDown(minecraft, action);
            boolean wasDown = DOWN.getOrDefault(action, false);
            if (down && !wasDown) {
                send(action, RideInputPhase.PRESS);
            } else if (down && wasDown && info.holdable() && holdTicker % HOLD_SEND_INTERVAL_TICKS == 0) {
                send(action, RideInputPhase.HOLD);
            } else if (!down && wasDown) {
                send(action, RideInputPhase.RELEASE);
            }
            DOWN.put(action, down);
        }
        DOWN.keySet().removeIf(action -> !ClientRideControlState.supports(action));
    }

    public static boolean tryConsumePrimaryAttack() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.screen == null
                && ClientRideControlState.activeForCurrentMount(minecraft)
                && ClientRideControlState.supports(RideAction.PRIMARY_ATTACK);
    }

    public static boolean tryConsumeSecondaryAttack() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.screen == null
                && ClientRideControlState.activeForCurrentMount(minecraft)
                && ClientRideControlState.supports(RideAction.SECONDARY_ATTACK);
    }

    public static boolean isControlledRide(Minecraft minecraft) {
        return ClientRideControlState.activeForCurrentMount(minecraft);
    }

    public static Component keyMessage(Minecraft minecraft, RideAction action) {
        KeyMapping mapping = keyMapping(minecraft, action);
        return mapping == null ? Component.empty() : mapping.getTranslatedKeyMessage();
    }

    private static boolean isActionKeyDown(Minecraft minecraft, RideAction action) {
        KeyMapping mapping = keyMapping(minecraft, action);
        return mapping != null && mapping.isDown();
    }

    private static KeyMapping keyMapping(Minecraft minecraft, RideAction action) {
        if (minecraft == null || action == null) return null;
        return switch (action) {
            case PRIMARY_ATTACK -> minecraft.options.keyAttack;
            case SECONDARY_ATTACK -> minecraft.options.keyUse;
            case SKILL_1 -> ClientKeys.RIDE_SKILL_1;
            case SKILL_2 -> ClientKeys.RIDE_SKILL_2;
            case SKILL_3 -> ClientKeys.RIDE_SKILL_3;
            case ROAR -> ClientKeys.RIDE_ROAR;
            case MOVEMENT_SPECIAL -> minecraft.options.keyJump;
            case UTILITY -> ClientKeys.RIDE_UTILITY;
            default -> null;
        };
    }

    private static void releasePressedActions(Minecraft minecraft) {
        if (!ClientRideControlState.activeForCurrentMount(minecraft)) {
            resetLocalState();
            return;
        }
        for (RideAction action : List.copyOf(DOWN.keySet())) {
            if (DOWN.getOrDefault(action, false) && ClientRideControlState.supports(action)) {
                send(action, RideInputPhase.RELEASE);
            }
        }
        DOWN.clear();
    }

    private static void send(RideAction action, RideInputPhase phase) {
        int mountId = ClientRideControlState.mountEntityId();
        if (mountId < 0 || action == null || phase == null) return;
        sequence = sequence == Integer.MAX_VALUE ? 1 : sequence + 1;
        NetworkHandler.sendRideAction(new C2SRideActionPacket(mountId, action, phase, sequence));
    }

    private static void resetLocalState() {
        DOWN.clear();
        holdTicker = 0;
    }
}
