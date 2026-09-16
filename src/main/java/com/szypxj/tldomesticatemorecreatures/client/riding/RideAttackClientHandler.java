package com.szypxj.tldomesticatemorecreatures.client.riding;

import com.szypxj.tldomesticatemorecreatures.client.ClientState;
import com.szypxj.tldomesticatemorecreatures.network.NetworkHandler;
import com.szypxj.tldomesticatemorecreatures.network.packet.C2SRideAttackPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class RideAttackClientHandler {
    private static int sequence;

    private RideAttackClientHandler() {
    }

    public static boolean tryHandleAttack() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!isControlledPetRide(minecraft)) return false;
        Entity vehicle = minecraft.player.getVehicle();
        sequence = sequence == Integer.MAX_VALUE ? 1 : sequence + 1;
        NetworkHandler.sendRideAttack(new C2SRideAttackPacket(vehicle.getId(), sequence));
        return true;
    }

    public static boolean isControlledPetRide(Minecraft minecraft) {
        if (minecraft == null || minecraft.player == null || minecraft.level == null || minecraft.screen != null) return false;
        Entity vehicle = minecraft.player.getVehicle();
        if (!(vehicle instanceof LivingEntity)) return false;
        boolean genericRide = ClientState.hasGenericRide() && vehicle.getId() == ClientState.genericRideMountId();
        return genericRide;
    }
}
