package com.szypxj.tldomesticatemorecreatures.command.pet.capability;

import com.szypxj.tldomesticatemorecreatures.command.pet.PetCommand;
import com.szypxj.tldomesticatemorecreatures.command.pet.provider.PetCommandCompatibilityApi;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class PetCapabilityResolver {
    private static final ConcurrentHashMap<Class<?>, CacheEntry> CACHE = new ConcurrentHashMap<>();

    private PetCapabilityResolver() {
    }

    public static PetCapabilityProfile resolve(Mob mob) {
        long generation = PetCommandCompatibilityApi.generation();
        Class<?> type = mob.getClass();
        CacheEntry cached = CACHE.get(type);
        if (cached != null && cached.generation == generation) {
            return cached.profile;
        }
        PetCapabilityProfile profile = build(mob);
        CACHE.put(type, new CacheEntry(generation, profile));
        return profile;
    }

    public static boolean canLand(LivingEntity pet) {
        return pet instanceof Mob mob && resolve(mob).canLand();
    }

    public static void invalidate(Class<?> type) {
        if (type != null) {
            CACHE.remove(type);
        }
    }

    public static void invalidateAll() {
        CACHE.clear();
    }

    private static PetCapabilityProfile build(Mob mob) {
        List<PetCommandCapability> attack = new ArrayList<>();
        List<PetCommandCapability> move = new ArrayList<>();
        List<PetCommandCapability> follow = new ArrayList<>();
        List<PetCommandCapability> defend = new ArrayList<>();
        List<PetCommandCapability> retreat = new ArrayList<>();
        List<PetCommandCapability> land = new ArrayList<>();
        List<PetCommandCompatibilityApi.NativeStateProvider> nativeStates = supportedNativeStateProviders(mob);

        addSupported(PetCommandCompatibilityApi.combatProviders(), mob, attack);
        attack.add(StandardPetCapabilities.combatIntent());
        if (StandardPetCapabilities.isMemoryRegistered(mob, MemoryModuleType.ATTACK_TARGET)) {
            attack.add(StandardPetCapabilities.brainAttackTarget());
        }

        addSupported(PetCommandCompatibilityApi.movementProviders(), mob, move);
        Optional<PetCommandCapability> nativeMovement = ReflectiveNativeCapabilities.nativeMovement(mob);
        nativeMovement.ifPresent(move::add);
        if (StandardPetCapabilities.supportsBrainMovement(mob)) {
            move.add(StandardPetCapabilities.brainMovement());
        }
        move.add(StandardPetCapabilities.navigationMovement());

        addSupported(PetCommandCompatibilityApi.followProviders(), mob, follow);
        for (PetCommandCompatibilityApi.NativeStateProvider provider : nativeStates) {
            try {
                if (provider.supports(mob, PetCommand.FOLLOW)) {
                    follow.add(StandardPetCapabilities.nativeStateFollow(provider));
                }
            } catch (RuntimeException ignored) {
            }
        }
        if (mob instanceof TamableAnimal) {
            follow.add(StandardPetCapabilities.tamableFollow());
        }
        if (StandardPetCapabilities.supportsBrainMovement(mob)) {
            follow.add(StandardPetCapabilities.brainMovement());
        }
        follow.add(StandardPetCapabilities.positionFollow());

        addSupported(PetCommandCompatibilityApi.defenseProviders(), mob, defend);
        defend.add(StandardPetCapabilities.combatIntent());
        if (StandardPetCapabilities.isMemoryRegistered(mob, MemoryModuleType.ATTACK_TARGET)) {
            defend.add(StandardPetCapabilities.brainAttackTarget());
        }

        addSupported(PetCommandCompatibilityApi.movementProviders(), mob, retreat);
        nativeMovement.ifPresent(retreat::add);
        if (StandardPetCapabilities.supportsBrainMovement(mob)) {
            retreat.add(StandardPetCapabilities.brainMovement());
        }
        retreat.add(StandardPetCapabilities.navigationMovement());

        addSupported(PetCommandCompatibilityApi.landingProviders(), mob, land);
        ReflectiveNativeCapabilities.nativeLanding(mob).ifPresent(land::add);
        List<PetCommandCompatibilityApi.FlightProvider> flightProviders = supportedFlightProviders(mob);
        if (!flightProviders.isEmpty() && nativeMovement.isPresent()) {
            PetCommandCapability landingViaNativeMovement = StandardPetCapabilities.safeLandingViaMovement(
                    nativeMovement.get(),
                    flightProviders
            );
            land.add(landingViaNativeMovement);
        }
        if (!flightProviders.isEmpty() && StandardPetCapabilities.supportsBrainMovement(mob)) {
            PetCommandCapability landingViaBrainMovement = StandardPetCapabilities.safeLandingViaMovement(
                    StandardPetCapabilities.brainMovement(),
                    flightProviders
            );
            land.add(landingViaBrainMovement);
        }
        if (mob.getNavigation() instanceof FlyingPathNavigation) {
            land.add(StandardPetCapabilities.flyingPathLanding());
        }

        return new PetCapabilityProfile(attack, move, follow, defend, retreat, land, nativeStates);
    }

    private static List<PetCommandCompatibilityApi.NativeStateProvider> supportedNativeStateProviders(Mob mob) {
        List<PetCommandCompatibilityApi.NativeStateProvider> supported = new ArrayList<>();
        for (PetCommandCompatibilityApi.NativeStateProvider provider : PetCommandCompatibilityApi.nativeStateProviders()) {
            try {
                for (PetCommand command : PetCommand.values()) {
                    if (provider.supports(mob, command)) {
                        supported.add(provider);
                        break;
                    }
                }
            } catch (RuntimeException ignored) {
            }
        }
        return List.copyOf(supported);
    }

    private static List<PetCommandCompatibilityApi.FlightProvider> supportedFlightProviders(Mob mob) {
        List<PetCommandCompatibilityApi.FlightProvider> supported = new ArrayList<>();
        ReflectiveNativeCapabilities.nativeFlight(mob).ifPresent(supported::add);
        for (PetCommandCompatibilityApi.FlightProvider provider : PetCommandCompatibilityApi.flightProviders()) {
            try {
                if (provider.supports(mob)) {
                    supported.add(provider);
                }
            } catch (RuntimeException ignored) {
            }
        }
        return List.copyOf(supported);
    }

    private static <T extends PetCommandCapability> void addSupported(List<T> providers, Mob mob, List<PetCommandCapability> target) {
        for (T provider : providers) {
            try {
                boolean supported;
                if (provider instanceof PetCommandCompatibilityApi.MovementProvider movement) {
                    supported = movement.supports(mob);
                } else if (provider instanceof PetCommandCompatibilityApi.FollowProvider follow) {
                    supported = follow.supports(mob);
                } else if (provider instanceof PetCommandCompatibilityApi.CombatProvider combat) {
                    supported = combat.supports(mob);
                } else if (provider instanceof PetCommandCompatibilityApi.DefenseProvider defense) {
                    supported = defense.supports(mob);
                } else if (provider instanceof PetCommandCompatibilityApi.LandingProvider landing) {
                    supported = landing.supports(mob);
                } else {
                    supported = false;
                }
                if (supported) {
                    target.add(provider);
                }
            } catch (RuntimeException ignored) {
            }
        }
    }

    private record CacheEntry(long generation, PetCapabilityProfile profile) {
    }
}
