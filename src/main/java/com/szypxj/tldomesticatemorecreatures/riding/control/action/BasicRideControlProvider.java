package com.szypxj.tldomesticatemorecreatures.riding.control.action;

import com.szypxj.tldomesticatemorecreatures.api.riding.RideAction;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideActionContext;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideActionInfo;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideActionResult;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideActionStatus;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideCapabilities;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideControlProvider;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideInputPhase;
import com.szypxj.tldomesticatemorecreatures.riding.RideAttackService;
import com.szypxj.tldomesticatemorecreatures.riding.RideEnvironment;
import com.szypxj.tldomesticatemorecreatures.riding.RideRuntimeState;
import com.szypxj.tldomesticatemorecreatures.riding.RideService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.Set;

/** Generic TDMC fallback. It is only appended by the dispatcher for Generic Ride. */
public final class BasicRideControlProvider implements RideControlProvider {
    public static final BasicRideControlProvider INSTANCE = new BasicRideControlProvider();

    private BasicRideControlProvider() {
    }

    @Override
    public int priority() {
        return Integer.MIN_VALUE;
    }

    @Override
    public boolean supports(LivingEntity mount) {
        return mount instanceof Mob;
    }

    @Override
    public RideCapabilities capabilities(LivingEntity mount) {
        RideRuntimeState runtime = RideService.runtime(mount);
        RideEnvironment environment = runtime == null ? RideEnvironment.GROUND : runtime.environment();
        return new RideCapabilities(
                true,
                environment == RideEnvironment.AIR,
                environment == RideEnvironment.WATER,
                true,
                Set.of(RideAction.PRIMARY_ATTACK)
        );
    }

    @Override
    public RideActionInfo actionInfo(LivingEntity mount, RideAction action) {
        if (action != RideAction.PRIMARY_ATTACK || !(mount instanceof Mob mob)) {
            return null;
        }
        return new RideActionInfo(
                RideAction.PRIMARY_ATTACK,
                "gui.tl_domesticate_more_creatures.riding.control.primary_attack",
                "gui.tl_domesticate_more_creatures.riding.control.primary_attack.desc",
                RideAttackService.attackIntervalTicks(mob),
                true,
                false
        );
    }

    @Override
    public RideActionStatus actionStatus(ServerPlayer rider, LivingEntity mount, RideAction action) {
        if (action != RideAction.PRIMARY_ATTACK || !(mount instanceof Mob mob)) {
            return RideActionStatus.UNSUPPORTED;
        }
        return RideAttackService.attackStatus(rider, mob);
    }

    @Override
    public RideActionResult execute(RideActionContext context, RideAction action) {
        if (action != RideAction.PRIMARY_ATTACK || !(context.mount() instanceof Mob mob)) {
            return RideActionResult.PASS;
        }
        if (context.phase() == RideInputPhase.RELEASE) {
            return RideActionResult.HANDLED;
        }
        RideAttackService.BasicAttackOutcome outcome = RideAttackService.performBasicAttack(context.rider(), mob);
        return switch (outcome) {
            case HIT, NO_TARGET -> RideActionResult.HANDLED;
            case COOLDOWN, INVALID -> RideActionResult.REJECTED;
        };
    }
}
