package com.szypxj.tldomesticatemorecreatures.compat.ror;

import com.szypxj.tldomesticatemorecreatures.api.riding.RideAction;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideActionContext;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideActionInfo;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideActionResult;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideActionStatus;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideCapabilities;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideControlApi;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideControlProvider;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideInputPhase;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/** Native-control bridge for ROR's currently implemented controllable dinosaurs. */
public final class RorRideControlProvider implements RideControlProvider {
    private static final ResourceLocation ID = Objects.requireNonNull(
            ResourceLocation.tryParse("tl_domesticate_more_creatures:ror_dinosaur_control")
    );
    private static final TagKey<EntityType<?>> CONTROLABLE = TagKey.create(
            Registries.ENTITY_TYPE,
            Objects.requireNonNull(ResourceLocation.tryParse("ror:controlable_dinosaur"))
    );
    private static final ResourceLocation GIGANOTOSAURUS = Objects.requireNonNull(ResourceLocation.tryParse("ror:giganotosaurus"));
    private static final RorRideControlProvider INSTANCE = new RorRideControlProvider();
    private static boolean registered;

    private RorRideControlProvider() {}

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        RideControlApi.register(ID, INSTANCE);
    }

    public static boolean isControllable(LivingEntity mount) {
        return mount != null && mount.getType().builtInRegistryHolder().is(CONTROLABLE);
    }

    @Override public int priority() { return 1_100; }

    @Override
    public boolean supports(LivingEntity mount) {
        return isControllable(mount);
    }

    @Override
    public RideCapabilities capabilities(LivingEntity mount) {
        if (!isControllable(mount)) return RideCapabilities.NONE;
        Set<RideAction> actions = EnumSet.of(
                RideAction.PRIMARY_ATTACK,
                RideAction.SECONDARY_ATTACK,
                RideAction.ROAR,
                RideAction.MOVEMENT_SPECIAL,
                RideAction.UTILITY
        );
        if (isGiganotosaurus(mount)) actions.add(RideAction.SKILL_1);
        return new RideCapabilities(true, false, false, true, actions);
    }

    @Override
    public RideActionInfo actionInfo(LivingEntity mount, RideAction action) {
        if (!isControllable(mount) || action == null) return null;
        return switch (action) {
            case PRIMARY_ATTACK -> held(action, "attack");
            case SECONDARY_ATTACK -> held(action, "secondary");
            case ROAR -> held(action, "roar");
            case SKILL_1 -> isGiganotosaurus(mount) ? instant(action, "toggle_backward") : null;
            case MOVEMENT_SPECIAL -> held(action, "ascend");
            case UTILITY -> held(action, "descend");
            default -> null;
        };
    }

    @Override
    public RideActionStatus actionStatus(ServerPlayer rider, LivingEntity mount, RideAction action) {
        if (!isControllable(mount) || action == null) return RideActionStatus.UNSUPPORTED;
        CompoundTag riderTag = rider.getPersistentData();
        boolean active = switch (action) {
            case PRIMARY_ATTACK -> riderTag.getBoolean("LeftClicked");
            case SECONDARY_ATTACK, ROAR -> riderTag.getBoolean("RightClicked");
            case MOVEMENT_SPECIAL -> riderTag.getBoolean("RisingPressed");
            case UTILITY -> riderTag.getBoolean("SinkPressed");
            default -> false;
        };
        return active ? RideActionStatus.active(1.0F) : RideActionStatus.READY;
    }

    @Override
    public RideActionResult execute(RideActionContext context, RideAction action) {
        LivingEntity mount = context.mount();
        if (!isControllable(mount) || action == null) return RideActionResult.PASS;
        CompoundTag riderTag = context.rider().getPersistentData();
        CompoundTag mountTag = mount.getPersistentData();
        boolean active = context.phase() != RideInputPhase.RELEASE;
        switch (action) {
            case PRIMARY_ATTACK -> {
                riderTag.putBoolean("LeftClicked", active);
                if (context.phase() == RideInputPhase.PRESS) riderTag.putDouble("LeftHold", 1.0D);
            }
            case SECONDARY_ATTACK, ROAR -> {
                riderTag.putBoolean("RightClicked", active);
                if (context.phase() == RideInputPhase.PRESS) riderTag.putDouble("RightHold", 1.0D);
            }
            case MOVEMENT_SPECIAL -> riderTag.putBoolean("RisingPressed", active);
            case UTILITY -> riderTag.putBoolean("SinkPressed", active);
            case SKILL_1 -> {
                if (!isGiganotosaurus(mount)) return RideActionResult.REJECTED;
                if (context.phase() == RideInputPhase.PRESS) {
                    mountTag.putBoolean("ToggleBackward", !mountTag.getBoolean("ToggleBackward"));
                }
            }
            default -> { return RideActionResult.PASS; }
        }
        return RideActionResult.HANDLED;
    }

    private static boolean isGiganotosaurus(LivingEntity mount) {
        return GIGANOTOSAURUS.equals(BuiltInRegistries.ENTITY_TYPE.getKey(mount.getType()));
    }

    private static RideActionInfo held(RideAction action, String suffix) { return info(action, suffix, true); }
    private static RideActionInfo instant(RideAction action, String suffix) { return info(action, suffix, false); }
    private static RideActionInfo info(RideAction action, String suffix, boolean holdable) {
        String base = "gui.tl_domesticate_more_creatures.riding.control.ror." + suffix;
        return new RideActionInfo(action, base, base + ".desc", 0, holdable, false);
    }
}
