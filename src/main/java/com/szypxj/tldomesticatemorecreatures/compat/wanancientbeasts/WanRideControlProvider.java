package com.szypxj.tldomesticatemorecreatures.compat.wanancientbeasts;

import com.szypxj.tldomesticatemorecreatures.api.riding.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.Objects;

/** Exposes Wan's original vehicleAbility without replacing the mod's own travel code. */
public final class WanRideControlProvider implements RideControlProvider {
    private static final ResourceLocation PROVIDER_ID = Objects.requireNonNull(ResourceLocation.tryParse(
            "tl_domesticate_more_creatures:wan_ancient_beasts_control"));
    private static final WanRideControlProvider INSTANCE = new WanRideControlProvider();
    private static boolean registered;

    private WanRideControlProvider() {}

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        RideControlApi.register(PROVIDER_ID, INSTANCE);
    }

    @Override public int priority() { return 1_050; }

    @Override
    public boolean supports(LivingEntity mount) {
        ResourceLocation id = mount == null ? null : ForgeRegistries.ENTITY_TYPES.getKey(mount.getType());
        return id != null && "wan_ancient_beasts".equals(id.getNamespace())
                && ("charger".equals(id.getPath()) || "surfer".equals(id.getPath()));
    }

    @Override
    public RideCapabilities capabilities(LivingEntity mount) {
        if (!supports(mount)) return RideCapabilities.NONE;
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(mount.getType());
        boolean surfer = id != null && "surfer".equals(id.getPath());
        return new RideCapabilities(true, false, surfer, true, EnumSet.of(RideAction.SKILL_1));
    }

    @Override
    public RideActionInfo actionInfo(LivingEntity mount, RideAction action) {
        if (!supports(mount) || action != RideAction.SKILL_1) return null;
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(mount.getType());
        String suffix = id != null && "surfer".equals(id.getPath()) ? "oxygen" : "charge";
        String base = "gui.tl_domesticate_more_creatures.riding.control.wan_ancient_beasts." + suffix;
        return new RideActionInfo(action, base, base + ".desc", 0, false, false);
    }

    @Override
    public RideActionResult execute(RideActionContext context, RideAction action) {
        if (!supports(context.mount()) || action != RideAction.SKILL_1) return RideActionResult.PASS;
        if (context.phase() != RideInputPhase.PRESS) return RideActionResult.HANDLED;
        try {
            Method method = context.mount().getClass().getMethod("vehicleAbility", Player.class);
            method.invoke(context.mount(), context.rider());
            return RideActionResult.HANDLED;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return RideActionResult.REJECTED;
        }
    }
}
