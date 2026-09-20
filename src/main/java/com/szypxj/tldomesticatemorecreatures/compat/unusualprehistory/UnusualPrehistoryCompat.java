package com.szypxj.tldomesticatemorecreatures.compat.unusualprehistory;

import com.szypxj.tldomesticatemorecreatures.api.riding.*;
import com.szypxj.tldomesticatemorecreatures.riding.provider.RideCompatibilityApi;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.Objects;

/** Optional Unusual Prehistory 2 ride bridge. Standard BreedableMob breeding remains on Forge's generic path. */
public final class UnusualPrehistoryCompat {
    private static final ResourceLocation ULUGH = Objects.requireNonNull(ResourceLocation.tryParse(
            "unusual_prehistory:ulughbegsaurus"));
    private static final ResourceLocation HIBBER = Objects.requireNonNull(ResourceLocation.tryParse(
            "unusual_prehistory:hibbertopterus"));
    private static final ResourceLocation PROVIDER_ID = Objects.requireNonNull(ResourceLocation.tryParse(
            "tl_domesticate_more_creatures:unusual_prehistory_control"));
    private static boolean registered;

    private UnusualPrehistoryCompat() {}

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        RideCompatibilityApi.registerNativeRideProvider(new RideCompatibilityApi.NativeRideProvider() {
            @Override public boolean supports(LivingEntity entity) { return isNativeRideable(entity); }
            @Override public boolean hasNativePlayerControl(LivingEntity entity) { return isNativeRideable(entity); }
        });
        RideControlApi.register(PROVIDER_ID, new RideControlProvider() {
            @Override public int priority() { return 1_050; }
            @Override public boolean supports(LivingEntity mount) { return isUlugh(mount); }
            @Override public RideCapabilities capabilities(LivingEntity mount) {
                return isUlugh(mount)
                        ? new RideCapabilities(true, false, false, true, EnumSet.of(RideAction.SKILL_1))
                        : RideCapabilities.NONE;
            }
            @Override public RideActionInfo actionInfo(LivingEntity mount, RideAction action) {
                if (!isUlugh(mount) || action != RideAction.SKILL_1) return null;
                String base = "gui.tl_domesticate_more_creatures.riding.control.unusual_prehistory.ulugh_attack";
                return new RideActionInfo(action, base, base + ".desc", 0, false, false);
            }
            @Override public RideActionResult execute(RideActionContext context, RideAction action) {
                if (!isUlugh(context.mount()) || action != RideAction.SKILL_1) return RideActionResult.PASS;
                if (context.phase() != RideInputPhase.PRESS) return RideActionResult.HANDLED;
                try {
                    Method method = context.mount().getClass().getMethod("onKeyPacket", Entity.class, int.class);
                    method.invoke(context.mount(), context.rider(), 3);
                    return RideActionResult.HANDLED;
                } catch (ReflectiveOperationException | RuntimeException exception) {
                    return RideActionResult.REJECTED;
                }
            }
        });
    }

    private static boolean isNativeRideable(LivingEntity entity) {
        ResourceLocation id = entity == null ? null : ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return ULUGH.equals(id) || HIBBER.equals(id);
    }

    private static boolean isUlugh(LivingEntity entity) {
        ResourceLocation id = entity == null ? null : ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return ULUGH.equals(id);
    }
}
