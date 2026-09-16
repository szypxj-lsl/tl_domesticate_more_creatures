package com.szypxj.tldomesticatemorecreatures.client.pet;

import com.szypxj.tlmarking.input.PingRadialExtensionHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public record PetCommandTargetSnapshot(int entityId, Vec3 position) {
    public static PetCommandTargetSnapshot from(PingRadialExtensionHooks.Context context) {
        if (context == null) {
            return new PetCommandTargetSnapshot(-1, Vec3.ZERO);
        }
        return new PetCommandTargetSnapshot(context.entityId(), new Vec3(context.x(), context.y(), context.z()));
    }

    public static PetCommandTargetSnapshot capture(Minecraft minecraft) {
        if (minecraft == null || minecraft.player == null) {
            return new PetCommandTargetSnapshot(-1, Vec3.ZERO);
        }
        HitResult hit = minecraft.hitResult;
        if (hit instanceof EntityHitResult entityHit) {
            return new PetCommandTargetSnapshot(entityHit.getEntity().getId(), entityHit.getLocation());
        }
        if (hit instanceof BlockHitResult blockHit) {
            return new PetCommandTargetSnapshot(-1, blockHit.getLocation());
        }
        return new PetCommandTargetSnapshot(-1, minecraft.player.position());
    }
}
