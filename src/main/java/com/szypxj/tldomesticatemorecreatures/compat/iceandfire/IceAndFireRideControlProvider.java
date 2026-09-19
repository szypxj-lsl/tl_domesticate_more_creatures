package com.szypxj.tldomesticatemorecreatures.compat.iceandfire;

import com.szypxj.tldomesticatemorecreatures.api.riding.RideAction;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideActionContext;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideActionInfo;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideActionResult;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideActionStatus;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideCapabilities;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideControlApi;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideControlProvider;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideInputPhase;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Full native-control bridge for Ice and Fire dragons.
 *
 * <p>The provider only toggles Ice and Fire's own rider control-state bits. The
 * dragon's native updateRider logic remains responsible for bite animations,
 * target selection, damage, breath effects, takeoff, ascent and descent.</p>
 */
public final class IceAndFireRideControlProvider implements RideControlProvider {
    private static final ResourceLocation ID = Objects.requireNonNull(
            ResourceLocation.tryParse("tl_domesticate_more_creatures:iceandfire_dragon_control")
    );
    private static final IceAndFireRideControlProvider INSTANCE = new IceAndFireRideControlProvider();
    private static boolean registered;

    private IceAndFireRideControlProvider() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        RideControlApi.register(ID, INSTANCE);
    }

    @Override
    public int priority() {
        return 1_000;
    }

    @Override
    public boolean supports(LivingEntity mount) {
        return mount instanceof IceAndFireRideBridge;
    }

    @Override
    public RideCapabilities capabilities(LivingEntity mount) {
        if (!(mount instanceof IceAndFireRideBridge bridge)) {
            return RideCapabilities.NONE;
        }
        Set<RideAction> actions = EnumSet.of(
                RideAction.PRIMARY_ATTACK,
                RideAction.ROAR,
                RideAction.MOVEMENT_SPECIAL,
                RideAction.UTILITY
        );
        if (bridge.tdmc$getDragonStage() > 1) {
            actions.add(RideAction.SECONDARY_ATTACK);
        }
        return new RideCapabilities(true, true, false, true, actions);
    }

    @Override
    public RideActionInfo actionInfo(LivingEntity mount, RideAction action) {
        if (!(mount instanceof IceAndFireRideBridge bridge) || action == null) {
            return null;
        }
        return switch (action) {
            case PRIMARY_ATTACK -> info(
                    action,
                    "gui.tl_domesticate_more_creatures.riding.control.iceandfire.primary_attack",
                    "gui.tl_domesticate_more_creatures.riding.control.iceandfire.primary_attack.desc"
            );
            case ROAR -> instantInfo(
                    action,
                    "gui.tl_domesticate_more_creatures.riding.control.iceandfire.roar",
                    "gui.tl_domesticate_more_creatures.riding.control.iceandfire.roar.desc"
            );
            case SECONDARY_ATTACK -> bridge.tdmc$getDragonStage() > 1 ? info(
                    action,
                    "gui.tl_domesticate_more_creatures.riding.control.iceandfire.dragon_breath",
                    "gui.tl_domesticate_more_creatures.riding.control.iceandfire.dragon_breath.desc"
            ) : null;
            case MOVEMENT_SPECIAL -> info(
                    action,
                    "gui.tl_domesticate_more_creatures.riding.control.iceandfire.ascend",
                    "gui.tl_domesticate_more_creatures.riding.control.iceandfire.ascend.desc"
            );
            case UTILITY -> info(
                    action,
                    "gui.tl_domesticate_more_creatures.riding.control.iceandfire.descend",
                    "gui.tl_domesticate_more_creatures.riding.control.iceandfire.descend.desc"
            );
            default -> null;
        };
    }

    @Override
    public RideActionStatus actionStatus(ServerPlayer rider, LivingEntity mount, RideAction action) {
        if (!(mount instanceof IceAndFireRideBridge bridge) || action == null) {
            return RideActionStatus.UNSUPPORTED;
        }
        boolean active = switch (action) {
            case PRIMARY_ATTACK -> bridge.tdmc$isAttacking();
            case SECONDARY_ATTACK -> bridge.tdmc$isStriking();
            case MOVEMENT_SPECIAL -> bridge.tdmc$isGoingUp();
            case UTILITY -> bridge.tdmc$isGoingDown();
            default -> false;
        };
        return active ? RideActionStatus.active(1.0F) : RideActionStatus.READY;
    }

    @Override
    public RideActionResult execute(RideActionContext context, RideAction action) {
        if (!(context.mount() instanceof IceAndFireRideBridge bridge) || action == null) {
            return RideActionResult.PASS;
        }
        boolean active = context.phase() == RideInputPhase.PRESS || context.phase() == RideInputPhase.HOLD;
        if (context.phase() == RideInputPhase.RELEASE) {
            active = false;
        }

        switch (action) {
            case PRIMARY_ATTACK -> bridge.tdmc$setAttacking(active);
            case ROAR -> {
                if (context.phase() == RideInputPhase.PRESS) {
                    bridge.tdmc$roar();
                }
            }
            case SECONDARY_ATTACK -> {
                if (bridge.tdmc$getDragonStage() <= 1) {
                    return RideActionResult.REJECTED;
                }
                bridge.tdmc$setStriking(active);
            }
            case MOVEMENT_SPECIAL -> {
                if (active) {
                    bridge.tdmc$setGoingDown(false);
                }
                bridge.tdmc$setGoingUp(active);
            }
            case UTILITY -> {
                if (active) {
                    bridge.tdmc$setGoingUp(false);
                }
                bridge.tdmc$setGoingDown(active);
            }
            default -> {
                return RideActionResult.PASS;
            }
        }
        return RideActionResult.HANDLED;
    }

    public static void clearControls(LivingEntity mount) {
        if (mount instanceof IceAndFireRideBridge bridge) {
            bridge.tdmc$clearUnifiedControls();
        }
    }

    private static RideActionInfo instantInfo(RideAction action, String nameKey, String descriptionKey) {
        return new RideActionInfo(action, nameKey, descriptionKey, 0, false, false);
    }

    private static RideActionInfo info(RideAction action, String nameKey, String descriptionKey) {
        return new RideActionInfo(action, nameKey, descriptionKey, 0, true, false);
    }
}
