package com.szypxj.tldomesticatemorecreatures.compat.wanancientbeasts;

import com.szypxj.tldomesticatemorecreatures.api.creature.compat.CreatureCompatProfileRegistry;
import com.szypxj.tldomesticatemorecreatures.riding.provider.RideCompatibilityApi;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Set;

/** Optional Wan's Ancient Beasts integration; contains no compile-time dependency on Wan classes. */
public final class WanAncientBeastsCompat {
    private static final String MOD_ID = "wan_ancient_beasts";
    private static final Set<String> NATIVE_RIDEABLES = Set.of("charger", "surfer", "walker", "soarer");
    private static boolean registered;

    private WanAncientBeastsCompat() {}

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        WanThreatProvider.register();
        WanRideControlProvider.register();
        CreatureCompatProfileRegistry.register(new WanCompatProfileProvider());
        RideCompatibilityApi.registerNativeRideProvider(new RideCompatibilityApi.NativeRideProvider() {
            @Override public boolean supports(LivingEntity entity) { return isNativeRideable(entity); }
            @Override public boolean hasNativePlayerControl(LivingEntity entity) { return isNativeRideable(entity); }
        });
    }

    public static boolean isNativeRideable(LivingEntity entity) {
        ResourceLocation id = entity == null ? null : ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return id != null && MOD_ID.equals(id.getNamespace()) && NATIVE_RIDEABLES.contains(id.getPath());
    }
}
