package com.szypxj.tldomesticatemorecreatures.compat.similarprehistory;

import com.szypxj.tldomesticatemorecreatures.api.creature.compat.CreatureCompatProfileRegistry;
import com.szypxj.tldomesticatemorecreatures.riding.provider.RideCompatibilityApi;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Set;

/** Optional Similar Prehistory integration. */
public final class SimilarPrehistoryCompat {
    private static final Set<String> NATIVE_RIDEABLES = Set.of(
            "dakotaraptor", "oxalaia", "charonosaurus", "coahuilasaurus",
            "charo_baby", "coahuilababy"
    );
    private static boolean registered;

    private SimilarPrehistoryCompat() {}

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        SimilarPrehistoryThreatProvider.register();
        CreatureCompatProfileRegistry.register(new SimilarPrehistoryCompatProfileProvider());
        RideCompatibilityApi.registerNativeRideProvider(new RideCompatibilityApi.NativeRideProvider() {
            @Override public boolean supports(LivingEntity entity) { return isNativeRideable(entity); }
            @Override public boolean hasNativePlayerControl(LivingEntity entity) { return isNativeRideable(entity); }
        });
    }

    private static boolean isNativeRideable(LivingEntity entity) {
        ResourceLocation id = entity == null ? null : ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return id != null && "similar_prehistory".equals(id.getNamespace()) && NATIVE_RIDEABLES.contains(id.getPath());
    }
}
