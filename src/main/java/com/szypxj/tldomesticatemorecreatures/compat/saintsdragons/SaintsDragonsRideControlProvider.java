package com.szypxj.tldomesticatemorecreatures.compat.saintsdragons;

import com.szypxj.tldomesticatemorecreatures.api.riding.RideAction;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideActionContext;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideActionInfo;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideActionResult;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideActionStatus;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideCapabilities;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideControlApi;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideControlProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.util.EnumSet;
import java.util.Objects;

/**
 * Unified profile for Saint's Dragons rideables.
 *
 * <p>Actual abilities remain entirely inside Saint's Dragons. The client input bridge remaps
 * TDMC Z/X/C/Space/Alt into Saint's own KeyMappings immediately before its native input handler.
 * This provider only makes those actions visible to TDMC and consumes the parallel TDMC packets.</p>
 */
public final class SaintsDragonsRideControlProvider implements RideControlProvider {
    private static final ResourceLocation ID = Objects.requireNonNull(
            ResourceLocation.tryParse("tl_domesticate_more_creatures:saintsdragons_native_control")
    );
    private static final TagKey<EntityType<?>> RIDEABLE = tag("rideable_dragons");
    private static final TagKey<EntityType<?>> FLYING = tag("flying_dragons");
    private static final TagKey<EntityType<?>> SWIMMING = tag("swimming_dragons");
    private static final SaintsDragonsRideControlProvider INSTANCE = new SaintsDragonsRideControlProvider();
    private static boolean registered;

    private SaintsDragonsRideControlProvider() {}

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        RideControlApi.register(ID, INSTANCE);
    }

    public static boolean isRideableDragon(LivingEntity mount) {
        return mount != null && mount.getType().builtInRegistryHolder().is(RIDEABLE);
    }

    @Override public int priority() { return 1_100; }

    @Override
    public boolean supports(LivingEntity mount) {
        return isRideableDragon(mount);
    }

    @Override
    public RideCapabilities capabilities(LivingEntity mount) {
        if (!isRideableDragon(mount)) return RideCapabilities.NONE;
        boolean flying = mount.getType().builtInRegistryHolder().is(FLYING);
        boolean swimming = mount.getType().builtInRegistryHolder().is(SWIMMING);
        EnumSet<RideAction> actions = EnumSet.of(
                RideAction.PRIMARY_ATTACK,
                RideAction.SECONDARY_ATTACK,
                RideAction.SKILL_1,
                RideAction.SKILL_2,
                RideAction.SKILL_3,
                RideAction.MOVEMENT_SPECIAL,
                RideAction.UTILITY
        );
        if (hasNativeRoar(mount)) {
            actions.add(RideAction.ROAR);
        }
        return new RideCapabilities(true, flying, swimming, true, actions);
    }

    @Override
    public RideActionInfo actionInfo(LivingEntity mount, RideAction action) {
        if (!isRideableDragon(mount) || action == null) return null;
        return switch (action) {
            case PRIMARY_ATTACK -> info(action, "primary_attack");
            case SECONDARY_ATTACK -> info(action, "attack_mode");
            case ROAR -> hasNativeRoar(mount) ? info(action, "roar") : null;
            case SKILL_1 -> info(action, "skill_1");
            case SKILL_2 -> info(action, "skill_2");
            case SKILL_3 -> info(action, "skill_3");
            case MOVEMENT_SPECIAL -> info(action, "ascend");
            case UTILITY -> info(action, "descend");
            default -> null;
        };
    }

    @Override
    public RideActionStatus actionStatus(ServerPlayer rider, LivingEntity mount, RideAction action) {
        return supports(mount) ? RideActionStatus.READY : RideActionStatus.UNSUPPORTED;
    }

    @Override
    public RideActionResult execute(RideActionContext context, RideAction action) {
        if (!isRideableDragon(context.mount()) || action == null) return RideActionResult.PASS;
        return switch (action) {
            case PRIMARY_ATTACK, SECONDARY_ATTACK, SKILL_1, SKILL_2, SKILL_3, MOVEMENT_SPECIAL, UTILITY -> RideActionResult.HANDLED;
            case ROAR -> hasNativeRoar(context.mount()) ? RideActionResult.HANDLED : RideActionResult.PASS;
            default -> RideActionResult.PASS;
        };
    }

    private static boolean hasNativeRoar(LivingEntity mount) {
        if (mount == null) return false;
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(mount.getType());
        if (typeId == null || !"saintsdragons".equals(typeId.getNamespace())) return false;
        return switch (typeId.getPath()) {
            case "raevyx", "volitans", "ignivorus" -> true;
            default -> false;
        };
    }

    private static RideActionInfo info(RideAction action, String suffix) {
        String base = "gui.tl_domesticate_more_creatures.riding.control.saintsdragons." + suffix;
        // Saint's own client handler polls held state each tick, so TDMC does not need HOLD packets here.
        return new RideActionInfo(action, base, base + ".desc", 0, false, false);
    }

    private static TagKey<EntityType<?>> tag(String path) {
        return TagKey.create(
                Registries.ENTITY_TYPE,
                Objects.requireNonNull(ResourceLocation.tryParse("saintsdragons:" + path))
        );
    }
}
