package com.szypxj.tldomesticatemorecreatures.client.riding;

import com.szypxj.tldomesticatemorecreatures.TlDomesticateMoreCreatures;
import com.szypxj.tldomesticatemorecreatures.client.ClientState;
import com.szypxj.tldomesticatemorecreatures.network.NetworkHandler;
import com.szypxj.tldomesticatemorecreatures.riding.RideEnvironment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TlDomesticateMoreCreatures.MOD_ID, value = Dist.CLIENT)
public final class RideInputHandler {
    private static SentInput lastSent = SentInput.NONE;
    private static int sequence;
    private static int ticksSinceSend;

    private RideInputHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            reset();
            return;
        }
        Entity vehicle = minecraft.player.getVehicle();
        if (!(vehicle instanceof LivingEntity)
                || !ClientState.hasGenericRide()
                || vehicle.getId() != ClientState.genericRideMountId()) {
            reset();
            return;
        }
        int mountId = vehicle.getId();

        float forward = (minecraft.options.keyUp.isDown() ? 1.0F : 0.0F)
                - (minecraft.options.keyDown.isDown() ? 1.0F : 0.0F);
        float strafe = (minecraft.options.keyLeft.isDown() ? 1.0F : 0.0F)
                - (minecraft.options.keyRight.isDown() ? 1.0F : 0.0F);
        boolean jump = minecraft.options.keyJump.isDown();
        boolean descend = minecraft.options.keyShift.isDown()
                && (ClientState.genericRideEnvironment() == RideEnvironment.AIR
                || ClientState.genericRideEnvironment() == RideEnvironment.WATER);

        SentInput current = new SentInput(mountId, forward, strafe, jump, descend);
        ticksSinceSend++;
        if (!current.equals(lastSent) || ticksSinceSend >= 10) {
            sequence = sequence == Integer.MAX_VALUE ? 1 : sequence + 1;
            NetworkHandler.sendRideInput(new com.szypxj.tldomesticatemorecreatures.network.packet.C2SRideInputPacket(
                    mountId, forward, strafe, jump, descend, sequence
            ));
            lastSent = current;
            ticksSinceSend = 0;
        }
    }

    public static boolean consumesDescendKey() {
        Minecraft minecraft = Minecraft.getInstance();
        boolean descendMode = ClientState.hasGenericRide()
                && (ClientState.genericRideEnvironment() == RideEnvironment.AIR
                || ClientState.genericRideEnvironment() == RideEnvironment.WATER);
        return descendMode
                && minecraft.player != null
                && minecraft.level != null
                && minecraft.options.keyShift.isDown();
    }

    public static void reset() {
        lastSent = SentInput.NONE;
        ticksSinceSend = 0;
    }

    private record SentInput(int mountId, float forward, float strafe, boolean jump, boolean descend) {
        private static final SentInput NONE = new SentInput(-1, 0.0F, 0.0F, false, false);
    }
}
