package com.szypxj.tldomesticatemorecreatures.domestication;

import com.szypxj.tldomesticatemorecreatures.config.Config;
import com.szypxj.tldomesticatemorecreatures.game.PetOwnershipService;
import com.szypxj.tldomesticatemorecreatures.talent.FuryService;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.server.level.ServerLevel;

import java.util.Optional;
import java.util.UUID;

public final class FriendlyFireService {
    private FriendlyFireService() {
    }

    public static boolean shouldBlock(LivingEntity victim, DamageSource source) {
        if (!Config.PREVENT_PET_FRIENDLY_FIRE.get()) {
            return false;
        }
        Entity attacker = resolveAttacker(source);
        if (attacker instanceof LivingEntity living && FuryService.isBerserk(living)) {
            return false;
        }
        return attacker instanceof LivingEntity living && areFriendly(living, victim);
    }

    public static boolean areFriendly(LivingEntity first, LivingEntity second) {
        if (first == second || first.isAlliedTo(second) || second.isAlliedTo(first)) {
            return true;
        }

        Optional<UUID> firstOwner = PetOwnershipService.ownerUuid(first);
        Optional<UUID> secondOwner = PetOwnershipService.ownerUuid(second);

        if (firstOwner.isPresent() && secondOwner.isPresent()) {
            if (firstOwner.get().equals(secondOwner.get())) {
                return true;
            }
            if (first.level() instanceof ServerLevel level && second.level() == first.level()) {
                Player firstOwnerPlayer = level.getPlayerByUUID(firstOwner.get());
                Player secondOwnerPlayer = level.getPlayerByUUID(secondOwner.get());
                if (firstOwnerPlayer != null
                        && secondOwnerPlayer != null
                        && (firstOwnerPlayer.isAlliedTo(secondOwnerPlayer) || secondOwnerPlayer.isAlliedTo(firstOwnerPlayer))) {
                    return true;
                }
            }
        }
        if (firstOwner.isPresent() && second instanceof Player player && firstOwner.get().equals(player.getUUID())) {
            return true;
        }
        if (secondOwner.isPresent() && first instanceof Player player && secondOwner.get().equals(player.getUUID())) {
            return true;
        }

        if (firstOwner.isPresent() && second instanceof Player player && player.level() instanceof ServerLevel level) {
            Player owner = level.getPlayerByUUID(firstOwner.get());
            if (owner != null && owner.isAlliedTo(player)) {
                return true;
            }
        }
        if (secondOwner.isPresent() && first instanceof Player player && player.level() instanceof ServerLevel level) {
            Player owner = level.getPlayerByUUID(secondOwner.get());
            if (owner != null && owner.isAlliedTo(player)) {
                return true;
            }
        }

        return false;
    }

    private static Entity resolveAttacker(DamageSource source) {
        Entity attacker = source.getEntity();
        if (attacker != null) {
            return attacker;
        }
        Entity direct = source.getDirectEntity();
        if (direct instanceof Projectile projectile && projectile.getOwner() != null) {
            return projectile.getOwner();
        }
        return direct;
    }
}
