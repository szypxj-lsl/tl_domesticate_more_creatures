package com.szypxj.tldomesticatemorecreatures.compat.fossil;

import com.szypxj.tldomesticatemorecreatures.api.riding.RideAction;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideActionContext;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideActionInfo;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideActionResult;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideCapabilities;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideControlApi;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideControlProvider;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideInputPhase;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.Objects;

/**
 * Routes TDMC's unified primary-attack input into Fossil's native rider attack.
 * Movement remains entirely owned by Fossil's Prehistoric travel implementation.
 */
public final class FossilRideControlProvider implements RideControlProvider {
    private static final ResourceLocation PROVIDER_ID = Objects.requireNonNull(ResourceLocation.tryParse(
            "tl_domesticate_more_creatures:fossil_control"));
    private static final FossilRideControlProvider INSTANCE = new FossilRideControlProvider();
    private static boolean registered;

    private FossilRideControlProvider() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        RideControlApi.register(PROVIDER_ID, INSTANCE);
    }

    @Override
    public int priority() {
        return 1_060;
    }

    @Override
    public boolean supports(LivingEntity mount) {
        return FossilCompat.isNativeRideable(mount);
    }

    @Override
    public RideCapabilities capabilities(LivingEntity mount) {
        if (!supports(mount)) {
            return RideCapabilities.NONE;
        }
        boolean flight = isInstanceOf(mount, "com.github.teamfossilsarcheology.fossil.entity.prehistoric.base.PrehistoricFlying");
        boolean swim = isInstanceOf(mount, "com.github.teamfossilsarcheology.fossil.entity.prehistoric.base.PrehistoricSwimming")
                || isInstanceOf(mount, "com.github.teamfossilsarcheology.fossil.entity.prehistoric.base.PrehistoricFish");
        return new RideCapabilities(true, flight, swim, true, EnumSet.of(RideAction.PRIMARY_ATTACK));
    }

    @Override
    public RideActionInfo actionInfo(LivingEntity mount, RideAction action) {
        if (!supports(mount) || action != RideAction.PRIMARY_ATTACK) {
            return null;
        }
        return new RideActionInfo(
                RideAction.PRIMARY_ATTACK,
                "gui.tl_domesticate_more_creatures.riding.control.primary_attack",
                "gui.tl_domesticate_more_creatures.riding.control.primary_attack.desc",
                0,
                false,
                false
        );
    }

    @Override
    public RideActionResult execute(RideActionContext context, RideAction action) {
        if (!supports(context.mount()) || action != RideAction.PRIMARY_ATTACK) {
            return RideActionResult.PASS;
        }
        if (context.phase() != RideInputPhase.PRESS) {
            return RideActionResult.HANDLED;
        }
        try {
            Method method = context.mount().getClass().getMethod("attackBoxHit", Player.class);
            method.invoke(context.mount(), context.rider());
            return RideActionResult.HANDLED;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return RideActionResult.REJECTED;
        }
    }

    private static boolean isInstanceOf(LivingEntity entity, String className) {
        if (entity == null) {
            return false;
        }
        Class<?> type = entity.getClass();
        while (type != null) {
            if (className.equals(type.getName())) {
                return true;
            }
            type = type.getSuperclass();
        }
        return false;
    }
}
