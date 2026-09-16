package com.szypxj.tldomesticatemorecreatures.domestication;

import com.szypxj.tldomesticatemorecreatures.TlDomesticateMoreCreatures;
import com.szypxj.tldomesticatemorecreatures.command.pet.PetCommandService;
import com.szypxj.tldomesticatemorecreatures.config.Config;
import com.szypxj.tldomesticatemorecreatures.game.PetOwnershipService;
import com.szypxj.tldomesticatemorecreatures.riding.RideService;
import com.szypxj.tldomesticatemorecreatures.talent.FuryService;
import com.szypxj.tldomesticatemorecreatures.torpor.TorporData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = TlDomesticateMoreCreatures.MOD_ID)
public final class PetAiService {
    private static final Set<LivingEntity> CUSTOM_PETS = Collections.newSetFromMap(new WeakHashMap<>());
    private static final Map<LivingEntity, UUID> ALLOWED_COMBAT_TARGETS = Collections.synchronizedMap(new WeakHashMap<>());

    private PetAiService() {
    }

    public static void register(LivingEntity entity) {
        if (!entity.level().isClientSide && PetOwnershipService.isCustomPet(entity)) {
            CUSTOM_PETS.add(entity);
        }
    }

    public static void unregister(LivingEntity entity) {
        CUSTOM_PETS.remove(entity);
        ALLOWED_COMBAT_TARGETS.remove(entity);
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity living) {
            register(living);
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity living) {
            unregister(living);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % 10 != 0 || CUSTOM_PETS.isEmpty()) {
            return;
        }
        CUSTOM_PETS.removeIf(entity -> {
            if (entity == null || entity.isRemoved() || entity.level().isClientSide || !PetOwnershipService.isCustomPet(entity)) {
                return true;
            }
            if (FuryService.isBerserk(entity)) {
                return false;
            }
            if (!(entity instanceof Mob mob)
                    || TorporData.of(entity).unconscious()
                    || PetCommandService.hasActiveCommand(entity)) {
                return false;
            }
            Player owner = PetOwnershipService.ownerPlayer(entity).orElse(null);
            if (owner == null) {
                return false;
            }
            if (mob.getTarget() != null) {
                LivingEntity currentTarget = mob.getTarget();
                if (FriendlyFireService.areFriendly(mob, currentTarget)
                        || !isAllowedCombatTarget(entity, currentTarget)) {
                    mob.setTarget(null);
                }
            }
            double distanceSq = mob.distanceToSqr(owner);
            if (distanceSq > 100.0D) {
                mob.getNavigation().moveTo(owner, 1.1D);
            } else if (distanceSq < 16.0D && mob.getTarget() == null) {
                mob.getNavigation().stop();
            }
            return false;
        });
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onTargetChange(LivingChangeTargetEvent event) {
        LivingEntity entity = event.getEntity();
        LivingEntity target = event.getNewTarget();
        if (target == null || !PetOwnershipService.isManagedPet(entity)) {
            return;
        }
        if (FuryService.isBerserk(entity)) {
            return;
        }
        if (FriendlyFireService.areFriendly(entity, target)) {
            event.setNewTarget(null);
            return;
        }
        if (PetCommandService.hasActiveCommand(entity)) {
            if (!PetCommandService.acceptsCombatTarget(entity, target)) {
                event.setNewTarget(null);
            }
            return;
        }
        if (RideService.isGenericControlled(entity) && !RideService.allowsAutomaticTarget(entity)) {
            event.setNewTarget(null);
            return;
        }
        if (PetOwnershipService.isCustomPet(entity) && !isAllowedCombatTarget(entity, target)) {
            event.setNewTarget(null);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        LivingEntity victim = event.getEntity();
        Entity source = event.getSource().getEntity();
        if (!(source instanceof LivingEntity attacker)) {
            return;
        }

        if (attacker instanceof ServerPlayer player && !FriendlyFireService.areFriendly(player, victim)) {
            alertOwnedPets(player, victim);
        }
        if (victim instanceof ServerPlayer player && !FriendlyFireService.areFriendly(player, attacker)) {
            alertOwnedPets(player, attacker);
        }
        if (PetOwnershipService.isManagedPet(victim)
                && !FuryService.isBerserk(victim)
                && !PetCommandService.hasActiveCommand(victim)
                && victim instanceof Mob mob
                && !FriendlyFireService.areFriendly(victim, attacker)) {
            assignCombatTarget(mob, attacker);
        }
    }

    private static void assignCombatTarget(Mob pet, LivingEntity target) {
        if (PetOwnershipService.isCustomPet(pet)) {
            ALLOWED_COMBAT_TARGETS.put(pet, target.getUUID());
        }
        pet.setTarget(target);
    }

    private static boolean isAllowedCombatTarget(LivingEntity pet, LivingEntity target) {
        UUID allowed = ALLOWED_COMBAT_TARGETS.get(pet);
        if (allowed == null) {
            return false;
        }
        if (!target.isAlive() || !allowed.equals(target.getUUID())) {
            if (!target.isAlive()) {
                ALLOWED_COMBAT_TARGETS.remove(pet);
            }
            return false;
        }
        return true;
    }

    private static void alertOwnedPets(ServerPlayer owner, LivingEntity target) {
        double range = Config.PET_COMMAND_RANGE.get();
        AABB area = owner.getBoundingBox().inflate(range);
        for (Mob pet : owner.serverLevel().getEntitiesOfClass(
                Mob.class,
                area,
                candidate -> PetOwnershipService.isOwnedBy(candidate, owner)
                        && !TorporData.of(candidate).unconscious()
                        && !FuryService.isBerserk(candidate)
        )) {
            if (!PetCommandService.hasActiveCommand(pet) && !FriendlyFireService.areFriendly(pet, target)) {
                assignCombatTarget(pet, target);
            }
        }
    }
}
