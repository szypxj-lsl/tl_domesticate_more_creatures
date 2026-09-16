package com.szypxj.tldomesticatemorecreatures.petmanagement.summon;

import com.szypxj.tldomesticatemorecreatures.config.Config;
import com.szypxj.tldomesticatemorecreatures.game.PetOwnershipService;
import com.szypxj.tldomesticatemorecreatures.network.NetworkHandler;
import com.szypxj.tldomesticatemorecreatures.petmanagement.PetManagementService;
import com.szypxj.tldomesticatemorecreatures.petmanagement.PetRecord;
import com.szypxj.tldomesticatemorecreatures.petmanagement.PetRecordState;
import com.szypxj.tldomesticatemorecreatures.riding.RideCapabilityResolver;
import com.szypxj.tldomesticatemorecreatures.riding.RideService;
import com.szypxj.tldomesticatemorecreatures.riding.config.RidingConfigManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class PetSummonService {
    private static final Map<UUID, PetSummonSession> ACTIVE = new HashMap<>();

    private PetSummonService() {
    }

    public static boolean startShortcut(ServerPlayer player, int slot) {
        if (player == null || slot < 1 || slot > 9) return false;
        PetRecord record = PetManagementService.shortcut(player, slot);
        if (record == null) return fail(player, "msg.tl_domesticate_more_creatures.pet_management.summon_failed");
        return start(player, record.petUuid());
    }

    public static boolean start(ServerPlayer player, UUID petUuid) {
        if (player == null || petUuid == null || !player.isAlive() || player.isSpectator()) return false;
        PetRecord record = PetManagementService.record(player.server, petUuid);
        if (record == null || !player.getUUID().equals(record.ownerUuid()) || !record.rideable()) {
            return fail(player, "msg.tl_domesticate_more_creatures.pet_management.summon_failed");
        }
        if (record.state() == PetRecordState.DEAD) {
            return fail(player, "msg.tl_domesticate_more_creatures.pet_management.dead");
        }
        if (player.getVehicle() instanceof LivingEntity current && current.getUUID().equals(petUuid)) return true;
        if (reuseExistingSession(player, petUuid)) return true;

        boolean emergencyRescue = isEmergencyFall(player);
        LivingEntity pet;
        if (record.state() == PetRecordState.STORED) {
            Vec3 entry = initialEntry(player, null, record, emergencyRescue);
            if (entry == null) return fail(player, "msg.tl_domesticate_more_creatures.pet_management.summon_failed");
            pet = PetEntityTransferService.restoreStored(player.serverLevel(), record, entry);
            if (pet == null) return fail(player, "msg.tl_domesticate_more_creatures.pet_management.summon_failed");
        } else {
            pet = PetManagementService.locate(player, petUuid, true);
            if (pet == null) {
                PetRecord refreshed = PetManagementService.record(player.server, petUuid);
                if (refreshed != null && refreshed.state() == PetRecordState.UNLOCATED) {
                    return fail(player, "msg.tl_domesticate_more_creatures.pet_management.unlocated");
                }
                return fail(player, "msg.tl_domesticate_more_creatures.pet_management.summon_failed");
            }
            if (!pet.isAlive() || !PetOwnershipService.isOwnedBy(pet, player)) {
                return fail(player, "msg.tl_domesticate_more_creatures.pet_management.summon_failed");
            }
            int nearby = Math.max(1, Config.PET_MANAGEMENT_NEARBY_SUMMON_RANGE.get());
            boolean sameLevel = pet.level() == player.level();
            boolean nearbyLoaded = sameLevel && pet.distanceToSqr(player) <= (double) nearby * nearby;
            if (emergencyRescue || !nearbyLoaded) {
                Vec3 entry = initialEntry(player, pet, record, emergencyRescue);
                if (entry == null) return fail(player, "msg.tl_domesticate_more_creatures.pet_management.summon_failed");
                pet = PetEntityTransferService.transferTo(pet, player.serverLevel(), entry);
                if (pet == null) return fail(player, "msg.tl_domesticate_more_creatures.pet_management.summon_failed");
            }
        }

        cancelForReplacement(player);
        if (player.isPassenger()) player.stopRiding();

        PetSummonSession session = new PetSummonSession(player.getUUID(), petUuid);
        session.flying(isFlying(pet));
        session.airborneRescue(emergencyRescue);
        session.state(PetSummonSession.State.APPROACHING);
        PetApproachController.beginApproach(pet, session);
        if (emergencyRescue) PetRescueFallProtection.protect(player, pet);

        record.state(PetRecordState.SUMMONING);
        record.storedEntityTag(null);
        record.lastLocation(player.serverLevel().dimension().location(), pet.getX(), pet.getY(), pet.getZ(), player.serverLevel().getGameTime());
        PetManagementService.saveRecord(player.server, record);
        ACTIVE.put(player.getUUID(), session);

        if (emergencyRescue) {
            PetApproachController.finishApproach(pet, session);
            if (RideService.trySummonMount(player, pet)) {
                session.state(PetSummonSession.State.COMPLETE);
            } else {
                PetApproachController.beginApproach(pet, session);
            }
        }
        return true;
    }

    public static void tick(MinecraftServer server) {
        if (server == null || ACTIVE.isEmpty()) return;
        Iterator<Map.Entry<UUID, PetSummonSession>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PetSummonSession> entry = iterator.next();
            PetSummonSession session = entry.getValue();
            ServerPlayer player = server.getPlayerList().getPlayer(session.playerUuid());

            if (session.state() == PetSummonSession.State.COMPLETE) {
                finishCompleted(server, session);
                iterator.remove();
                continue;
            }
            if (session.state() == PetSummonSession.State.CANCELLED) {
                finishCancelled(server, session);
                iterator.remove();
                continue;
            }
            if (player == null || !player.isAlive() || player.isSpectator()) {
                restoreApproachState(server, session);
                finishAsWorld(server, session);
                PetRescueFallProtection.clear(session.playerUuid());
                iterator.remove();
                continue;
            }

            LivingEntity pet = PetManagementService.locate(player, session.petUuid(), false);
            if (pet == null || !pet.isAlive() || !PetOwnershipService.isOwnedBy(pet, player)) {
                restoreApproachState(server, session);
                finishAsWorld(server, session);
                if (session.airborneRescue()) PetRescueFallProtection.releaseWhenGrounded(session.playerUuid());
                else PetRescueFallProtection.clear(session.playerUuid());
                iterator.remove();
                continue;
            }

            if (pet.level() != player.level()) {
                Vec3 relocation = transferFallbackPosition(player, pet, session);
                if (relocation != null) {
                    PetApproachController.finishApproach(pet, session);
                    LivingEntity moved = PetEntityTransferService.transferTo(pet, player.serverLevel(), relocation);
                    if (moved != null) {
                        pet = moved;
                        PetApproachController.beginApproach(pet, session);
                    }
                }
            }

            if (player.getVehicle() == pet) {
                session.state(PetSummonSession.State.COMPLETE);
                finishCompleted(server, session);
                iterator.remove();
                continue;
            }

            if (session.airborneRescue() && player.onGround()) {
                session.airborneRescue(false);
                PetRescueFallProtection.clear(session.playerUuid());
            }

            PetApproachController.tick(player, pet, session);
            if (session.state() == PetSummonSession.State.COMPLETE) {
                finishCompleted(server, session);
                iterator.remove();
                continue;
            }

            if (PetApproachController.timedOut(session)) {
                PetApproachController.finishApproach(pet, session);
                finishAsWorld(server, session);
                if (session.airborneRescue()) PetRescueFallProtection.releaseWhenGrounded(session.playerUuid());
                else PetRescueFallProtection.clear(session.playerUuid());
                iterator.remove();
            }
        }
    }

    public static void cancel(UUID playerUuid) {
        if (playerUuid == null) return;
        PetSummonSession session = ACTIVE.get(playerUuid);
        if (session != null) session.state(PetSummonSession.State.CANCELLED);
    }

    public static boolean isSummoning(UUID petUuid) {
        if (petUuid == null) return false;
        return ACTIVE.values().stream().anyMatch(session -> petUuid.equals(session.petUuid()));
    }

    static PetSummonSession session(UUID playerUuid) {
        return ACTIVE.get(playerUuid);
    }

    private static boolean reuseExistingSession(ServerPlayer player, UUID petUuid) {
        PetSummonSession existing = ACTIVE.get(player.getUUID());
        if (existing == null || !petUuid.equals(existing.petUuid())) return false;
        if (existing.state() == PetSummonSession.State.COMPLETE
                || existing.state() == PetSummonSession.State.CANCELLED) {
            return false;
        }

        if (isEmergencyFall(player) && !existing.airborneRescue()) {
            LivingEntity pet = PetManagementService.locate(player, petUuid, false);
            if (pet != null && pet.isAlive() && PetOwnershipService.isOwnedBy(pet, player)) {
                PetApproachController.finishApproach(pet, existing);
                existing.airborneRescue(true);
                existing.flying(isFlying(pet));
                existing.approachTicks(0);
                Vec3 intercept = PetPlacementFinder.findEmergencyIntercept(player, pet);
                if (intercept != null) {
                    if (pet.level() != player.level()) {
                        LivingEntity moved = PetEntityTransferService.transferTo(pet, player.serverLevel(), intercept);
                        if (moved != null) pet = moved;
                    } else {
                        relocatePet(pet, intercept);
                    }
                }
                PetRescueFallProtection.protect(player, pet);
                PetApproachController.beginApproach(pet, existing);
                PetApproachController.finishApproach(pet, existing);
                if (RideService.trySummonMount(player, pet)) {
                    existing.state(PetSummonSession.State.COMPLETE);
                } else {
                    PetApproachController.beginApproach(pet, existing);
                }
            }
        }
        return true;
    }

    private static void cancelForReplacement(ServerPlayer player) {
        PetSummonSession previous = ACTIVE.remove(player.getUUID());
        if (previous == null) return;
        LivingEntity pet = findLoadedPet(player.server, previous.petUuid());
        if (pet != null) PetApproachController.finishApproach(pet, previous);
        previous.state(PetSummonSession.State.CANCELLED);
        finishAsWorld(player.server, previous);
        if (previous.airborneRescue()) PetRescueFallProtection.releaseWhenGrounded(player.getUUID());
        else PetRescueFallProtection.clear(player.getUUID());
    }

    private static Vec3 transferFallbackPosition(ServerPlayer player, LivingEntity pet, PetSummonSession session) {
        if (session.airborneRescue() && !player.onGround()) {
            Vec3 intercept = PetPlacementFinder.findEmergencyIntercept(player, pet);
            if (intercept != null) return intercept;
        }
        return session.flying() ? PetPlacementFinder.findAirBehind(player, pet) : PetPlacementFinder.findGroundBehind(player, pet);
    }

    private static Vec3 initialEntry(ServerPlayer player, LivingEntity pet, PetRecord record, boolean emergencyRescue) {
        if (pet == null) {
            return PetEntityTransferService.previewForPlacement(player.serverLevel(), record, preview -> {
                if (emergencyRescue) return PetPlacementFinder.findEmergencyIntercept(player, preview);
                return isFlying(preview) ? PetPlacementFinder.findAirBehind(player, preview) : PetPlacementFinder.findGroundBehind(player, preview);
            });
        }
        if (emergencyRescue) return PetPlacementFinder.findEmergencyIntercept(player, pet);
        return isFlying(pet) ? PetPlacementFinder.findAirBehind(player, pet) : PetPlacementFinder.findGroundBehind(player, pet);
    }

    private static void relocatePet(LivingEntity pet, Vec3 position) {
        pet.teleportTo(position.x, position.y, position.z);
        pet.setDeltaMovement(Vec3.ZERO);
        pet.fallDistance = 0.0F;
        if (pet instanceof Mob mob) mob.getNavigation().stop();
    }

    private static boolean isEmergencyFall(ServerPlayer player) {
        if (player == null || player.onGround() || player.getAbilities().flying || player.isFallFlying()) return false;
        return player.getDeltaMovement().y < -0.35D || player.fallDistance >= 3.0F;
    }

    private static boolean isFlying(LivingEntity pet) {
        if (!(pet instanceof Mob mob)) return false;
        return RideCapabilityResolver.profile(mob, RidingConfigManager.profile(pet.getType())).flight();
    }

    private static boolean fail(ServerPlayer player, String key) {
        if (player != null) player.sendSystemMessage(Component.translatable(key));
        return false;
    }

    private static void finishCompleted(MinecraftServer server, PetSummonSession session) {
        restoreApproachState(server, session);
        if (session.airborneRescue()) PetRescueFallProtection.releaseWhenGrounded(session.playerUuid());
        else PetRescueFallProtection.clear(session.playerUuid());
        finishAsWorld(server, session);
    }

    private static void finishCancelled(MinecraftServer server, PetSummonSession session) {
        restoreApproachState(server, session);
        finishAsWorld(server, session);
        if (session.airborneRescue()) PetRescueFallProtection.releaseWhenGrounded(session.playerUuid());
        else PetRescueFallProtection.clear(session.playerUuid());
    }

    private static void restoreApproachState(MinecraftServer server, PetSummonSession session) {
        LivingEntity pet = findLoadedPet(server, session.petUuid());
        if (pet != null) PetApproachController.finishApproach(pet, session);
    }

    private static LivingEntity findLoadedPet(MinecraftServer server, UUID petUuid) {
        if (server == null || petUuid == null) return null;
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(petUuid);
            if (entity instanceof LivingEntity living) return living;
        }
        return null;
    }

    private static void finishAsWorld(MinecraftServer server, PetSummonSession session) {
        PetRecord record = PetManagementService.record(server, session.petUuid());
        if (record == null || record.state() == PetRecordState.DEAD) return;
        record.state(PetRecordState.WORLD);
        LivingEntity pet = findLoadedPet(server, session.petUuid());
        if (pet != null) {
            record.lastLocation(pet.level().dimension().location(), pet.getX(), pet.getY(), pet.getZ(), pet.level().getGameTime());
        }
        PetManagementService.saveRecord(server, record);
        ServerPlayer owner = server.getPlayerList().getPlayer(record.ownerUuid());
        if (owner != null) NetworkHandler.sendPetManagementList(owner);
    }
}
