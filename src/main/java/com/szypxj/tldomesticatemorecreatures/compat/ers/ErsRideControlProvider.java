package com.szypxj.tldomesticatemorecreatures.compat.ers;

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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/** Routes TDMC unified actions into ERS' own vehicle attack/flight entry points. */
public final class ErsRideControlProvider implements RideControlProvider {
    private static final ResourceLocation ID = Objects.requireNonNull(
            ResourceLocation.tryParse("tl_domesticate_more_creatures:ers_vehicle_control")
    );
    private static final ResourceLocation TERRIDENSAURUS_SAEVUS = Objects.requireNonNull(
            ResourceLocation.tryParse("ers:terridensaurus_saevus")
    );
    private static final ResourceLocation IMPERIOVENATOR_REGIUS = Objects.requireNonNull(
            ResourceLocation.tryParse("oasis:imperiovenator_regius")
    );
    private static final ErsRideControlProvider INSTANCE = new ErsRideControlProvider();
    private static boolean registered;

    private ErsRideControlProvider() {}

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        RideControlApi.register(ID, INSTANCE);
    }

    @Override
    public int priority() {
        return 1_100;
    }

    @Override
    public boolean supports(LivingEntity mount) {
        return mount instanceof ErsRideBridge;
    }

    @Override
    public RideCapabilities capabilities(LivingEntity mount) {
        if (!(mount instanceof ErsRideBridge)) return RideCapabilities.NONE;
        Set<RideAction> actions = EnumSet.of(
                RideAction.PRIMARY_ATTACK,
                RideAction.SECONDARY_ATTACK,
                RideAction.SKILL_1,
                RideAction.SKILL_2,
                RideAction.SKILL_3,
                RideAction.UTILITY
        );
        if (hasNativeRoar(mount)) actions.add(RideAction.ROAR);
        boolean flyable = mount instanceof ErsFlyableRideBridge;
        if (flyable) actions.add(RideAction.MOVEMENT_SPECIAL);
        return new RideCapabilities(true, flyable, false, true, actions);
    }

    @Override
    public RideActionInfo actionInfo(LivingEntity mount, RideAction action) {
        if (!(mount instanceof ErsRideBridge) || action == null) return null;
        return switch (action) {
            case PRIMARY_ATTACK -> instant(action, "default_attack");
            case SECONDARY_ATTACK -> instant(action, "special_attack");
            case SKILL_1 -> instant(action, "judgement_attack");
            case SKILL_2 -> instant(action, "turn_attack");
            case SKILL_3 -> instant(action, "jump_attack");
            case ROAR -> hasNativeRoar(mount) ? instant(action, "roar") : null;
            case MOVEMENT_SPECIAL -> mount instanceof ErsFlyableRideBridge ? held(action, "ascend") : null;
            case UTILITY -> held(action, "descend");
            default -> null;
        };
    }

    @Override
    public RideActionStatus actionStatus(ServerPlayer rider, LivingEntity mount, RideAction action) {
        if (!(mount instanceof ErsRideBridge bridge) || action == null) return RideActionStatus.UNSUPPORTED;
        boolean active = switch (action) {
            case MOVEMENT_SPECIAL -> mount instanceof ErsFlyableRideBridge fly && fly.tdmc$getFlightVerticalInput() > 0;
            case UTILITY -> mount instanceof ErsFlyableRideBridge fly
                    ? fly.tdmc$getFlightVerticalInput() < 0
                    : bridge.tdmc$isDiving();
            default -> false;
        };
        return active ? RideActionStatus.active(1.0F) : RideActionStatus.READY;
    }

    @Override
    public RideActionResult execute(RideActionContext context, RideAction action) {
        if (!(context.mount() instanceof ErsRideBridge bridge) || action == null) return RideActionResult.PASS;
        if (isPress(context)) {
            switch (action) {
                case PRIMARY_ATTACK -> bridge.tdmc$defaultAttack();
                case SECONDARY_ATTACK -> bridge.tdmc$specialAttack();
                case SKILL_1 -> bridge.tdmc$judgementAttack();
                case SKILL_2 -> bridge.tdmc$turnAttack();
                case SKILL_3 -> bridge.tdmc$jumpAttack();
                case ROAR -> {
                    if (isTerridensaurusSaevus(context.mount())) {
                        bridge.tdmc$judgementAttack();
                    } else if (isImperiovenatorRegius(context.mount())) {
                        bridge.tdmc$specialAttack();
                    } else {
                        return RideActionResult.REJECTED;
                    }
                }
                default -> { }
            }
        }

        boolean active = context.phase() != RideInputPhase.RELEASE;
        if (action == RideAction.MOVEMENT_SPECIAL) {
            if (context.mount() instanceof ErsFlyableRideBridge fly) {
                if (active) {
                    fly.tdmc$setFlightVerticalInput(1);
                } else if (fly.tdmc$getFlightVerticalInput() > 0) {
                    fly.tdmc$setFlightVerticalInput(0);
                }
            }
            return RideActionResult.HANDLED;
        }
        if (action == RideAction.UTILITY) {
            if (context.mount() instanceof ErsFlyableRideBridge fly) {
                if (active) {
                    fly.tdmc$setFlightVerticalInput(-1);
                } else if (fly.tdmc$getFlightVerticalInput() < 0) {
                    fly.tdmc$setFlightVerticalInput(0);
                }
            } else {
                bridge.tdmc$setDiving(active);
            }
            return RideActionResult.HANDLED;
        }

        return switch (action) {
            case PRIMARY_ATTACK, SECONDARY_ATTACK, SKILL_1, SKILL_2, SKILL_3, ROAR -> RideActionResult.HANDLED;
            default -> RideActionResult.PASS;
        };
    }

    private static boolean hasNativeRoar(LivingEntity mount) {
        return isTerridensaurusSaevus(mount) || isImperiovenatorRegius(mount);
    }

    private static boolean isTerridensaurusSaevus(LivingEntity mount) {
        return TERRIDENSAURUS_SAEVUS.equals(BuiltInRegistries.ENTITY_TYPE.getKey(mount.getType()));
    }

    private static boolean isImperiovenatorRegius(LivingEntity mount) {
        return IMPERIOVENATOR_REGIUS.equals(BuiltInRegistries.ENTITY_TYPE.getKey(mount.getType()));
    }

    private static boolean isPress(RideActionContext context) {
        return context.phase() == RideInputPhase.PRESS;
    }

    private static RideActionInfo instant(RideAction action, String suffix) {
        return info(action, suffix, false);
    }

    private static RideActionInfo held(RideAction action, String suffix) {
        return info(action, suffix, true);
    }

    private static RideActionInfo info(RideAction action, String suffix, boolean holdable) {
        String base = "gui.tl_domesticate_more_creatures.riding.control.ers." + suffix;
        return new RideActionInfo(action, base, base + ".desc", 0, holdable, false);
    }
}
