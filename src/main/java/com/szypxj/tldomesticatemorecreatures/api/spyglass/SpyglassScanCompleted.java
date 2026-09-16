package com.szypxj.tldomesticatemorecreatures.api.spyglass;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.Objects;

public record SpyglassScanCompleted(ServerPlayer player, LivingEntity target, ResourceLocation entityTypeId) {
    public SpyglassScanCompleted {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(entityTypeId, "entityTypeId");
    }
}
