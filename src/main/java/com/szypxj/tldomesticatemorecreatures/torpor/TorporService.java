package com.szypxj.tldomesticatemorecreatures.torpor;

import com.szypxj.tldomesticatemorecreatures.api.torpor.TorporRecoveryModifierRegistry;
import com.szypxj.tldomesticatemorecreatures.compat.saintsdragons.SaintsDragonsTorporCompat;
import com.szypxj.tldomesticatemorecreatures.config.Config;
import com.szypxj.tldomesticatemorecreatures.domestication.TamingService;
import com.szypxj.tldomesticatemorecreatures.elite.EliteService;
import com.szypxj.tldomesticatemorecreatures.game.LevelService;
import com.szypxj.tldomesticatemorecreatures.network.NetworkHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Locale;

public final class TorporService {
    public static final String STAT_ID = "torpor";
    private static final int UNCONSCIOUS_DARKNESS_DURATION_TICKS = 80;
    private static final int UNCONSCIOUS_DARKNESS_REFRESH_THRESHOLD_TICKS = 40;

    private TorporService() {
    }

    public static boolean isEnabled(LivingEntity entity) {
        if (entity instanceof Player) {
            return true;
        }
        if (EliteService.isElite(entity) && !EliteService.canBeTamed(entity)) {
            return false;
        }
        if (!LevelService.isAffected(entity)) {
            return false;
        }
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (id == null) {
            return false;
        }
        String mode = Config.TORPOR_FILTER_MODE.get().toUpperCase(Locale.ROOT);
        boolean listed = Config.TORPOR_ENTITIES.get().contains(id.toString());
        return "WHITELIST".equals(mode) ? listed : !listed;
    }

    public static boolean canGainTorpor(LivingEntity entity) {
        return isEnabled(entity) && !isPlayerTorporImmune(entity);
    }

    public static boolean isPlayerTorporImmune(LivingEntity entity) {
        return entity instanceof Player player && (player.isCreative() || player.isSpectator());
    }

    public static double maxTorpor(LivingEntity entity) {
        if (!isEnabled(entity)) {
            return 0.0D;
        }
        return TorporMath.maxMobTorpor(
                entity.getMaxHealth(),
                Config.TORPOR_MOB_HEALTH_MULTIPLIER.get()
        );
    }

    public static double currentTorpor(LivingEntity entity) {
        if (!isEnabled(entity)) {
            return 0.0D;
        }
        refresh(entity);
        return TorporData.of(entity).current();
    }

    public static boolean isUnconscious(LivingEntity entity) {
        if (!isEnabled(entity)) {
            return false;
        }
        refresh(entity);
        return TorporData.of(entity).unconscious();
    }

    public static void addTorpor(LivingEntity entity, double amount) {
        if (entity.level().isClientSide || amount <= 0.0D || !canGainTorpor(entity)) {
            return;
        }
        LevelService.initializeIfNeeded(entity);
        refresh(entity);
        TorporData data = TorporData.of(entity);
        double max = maxTorpor(entity);
        if (max <= 0.0D) {
            return;
        }
        double next = Math.min(max, data.current() + amount);
        data.current(next);
        long now = entity.level().getGameTime();
        data.lastIncreaseGameTime(now);
        data.lastUpdateGameTime(now);
        if (next > 0.0D) {
            UnconsciousManager.register(entity);
        }
        if (!data.unconscious() && TorporMath.shouldEnterUnconscious(next, max)) {
            enterUnconscious(entity, data);
        }
    }

    public static void clear(LivingEntity entity) {
        if (!isEnabled(entity) && !TorporData.exists(entity)) {
            return;
        }
        TorporData data = TorporData.of(entity);
        data.current(0.0D);
        if (data.unconscious()) {
            wake(entity, data);
        } else {
            clearTorporDarkness(entity, data);
            syncPlayerState(entity, false);
            data.lastUpdateGameTime(entity.level().getGameTime());
        }
    }

    public static void reduceFromDamage(LivingEntity entity, double damage) {
        if (entity.level().isClientSide || damage <= 0.0D || !isEnabled(entity)) {
            return;
        }
        refresh(entity);
        TorporData data = TorporData.of(entity);
        if (!data.unconscious()) {
            return;
        }
        data.current(TorporMath.torporAfterDamage(
                data.current(),
                damage,
                Config.TORPOR_DAMAGE_WAKE_MULTIPLIER.get()
        ));
        data.lastUpdateGameTime(entity.level().getGameTime());
        if (TorporMath.shouldWake(data.current())) {
            wake(entity, data);
        }
    }

    public static void refresh(LivingEntity entity) {
        if (entity.level().isClientSide || !isEnabled(entity)) {
            return;
        }
        if (clearIfPlayerImmune(entity)) {
            return;
        }
        TorporData data = TorporData.of(entity);
        long now = entity.level().getGameTime();
        long last = data.lastUpdateGameTime();
        if (last <= 0L || now < last) {
            data.lastUpdateGameTime(now);
            if (data.current() > 0.0D && data.lastIncreaseGameTime() <= 0L) {
                data.lastIncreaseGameTime(now);
            }
            if (data.unconscious()) {
                enforceUnconscious(entity, data);
            }
            if (data.current() > 0.0D || data.unconscious()) {
                UnconsciousManager.register(entity);
            }
            return;
        }

        if (data.current() > 0.0D) {
            long lastIncrease = data.lastIncreaseGameTime();
            if (lastIncrease <= 0L || now < lastIncrease) {
                data.lastIncreaseGameTime(now);
            } else {
                long delayTicks = Math.max(0L, Config.TORPOR_RECOVERY_DELAY_SECONDS.get()) * 20L;
                long recoveryTicks = TorporMath.recoveryTicks(last, lastIncrease, now, delayTicks);
                if (recoveryTicks > 0L) {
                    double max = maxTorpor(entity);
                    double normalPerSecond = TorporMath.naturalRecoveryPerSecond(
                            max,
                            Config.TORPOR_RECOVERY_BASE_DURATION_SECONDS.get(),
                            Config.TORPOR_RECOVERY_DURATION_SCALE_SECONDS.get()
                    );
                    double perSecond = TorporRecoveryModifierRegistry.resolveRecoveryPerSecond(
                            entity,
                            data.current(),
                            max,
                            normalPerSecond,
                            data.unconscious()
                    );
                    double recovery = perSecond * (recoveryTicks / 20.0D);
                    data.current(Math.max(0.0D, data.current() - recovery));
                }
            }
        }
        data.lastUpdateGameTime(now);

        if (data.unconscious()) {
            enforceUnconscious(entity, data);
            if (TorporMath.shouldWake(data.current())) {
                wake(entity, data);
            } else {
                UnconsciousManager.register(entity);
            }
        } else {
            double max = maxTorpor(entity);
            if (TorporMath.shouldEnterUnconscious(data.current(), max)) {
                data.current(max);
                enterUnconscious(entity, data);
            }
        }

        if (data.current() > 0.0D || data.unconscious()) {
            UnconsciousManager.register(entity);
        } else {
            UnconsciousManager.unregister(entity);
        }
    }

    public static boolean clearIfPlayerImmune(LivingEntity entity) {
        if (!isPlayerTorporImmune(entity) || entity.level().isClientSide) {
            return false;
        }
        TorporData data = TorporData.of(entity);
        if (data.current() > 0.0D || data.unconscious() || data.darknessAppliedByTorpor()) {
            clear(entity);
        } else {
            data.lastUpdateGameTime(entity.level().getGameTime());
        }
        return true;
    }

    public static void syncClientState(ServerPlayer player) {
        NetworkHandler.sendUnconsciousState(player, TorporData.of(player).unconscious());
    }

    public static void onEntityJoin(LivingEntity entity) {
        if (!isEnabled(entity)) {
            if (!TorporData.exists(entity)) {
                return;
            }
            TorporData data = TorporData.of(entity);
            if (data.unconscious()) {
                wake(entity, data);
            } else {
                clearTorporDarkness(entity, data);
                syncPlayerState(entity, false);
            }
            UnconsciousManager.unregister(entity);
            TorporData.remove(entity);
            return;
        }
        TorporData data = TorporData.of(entity);
        if (data.current() > 0.0D && data.lastIncreaseGameTime() <= 0L) {
            data.lastIncreaseGameTime(entity.level().getGameTime());
        }
        if (data.unconscious()) {
            enforceUnconscious(entity, data);
        }
        if (data.current() > 0.0D || data.unconscious()) {
            UnconsciousManager.register(entity);
        }
        refresh(entity);
        syncPlayerState(entity, data.unconscious());
    }

    private static void enterUnconscious(LivingEntity entity, TorporData data) {
        data.unconscious(true);
        enforceUnconscious(entity, data);
        syncPlayerState(entity, true);
        TamingService.onKnockoutStart(entity);
        UnconsciousManager.register(entity);
    }

    private static void enforceUnconscious(LivingEntity entity, TorporData data) {
        if (entity instanceof Mob mob && !data.previousNoAiStored()) {
            data.previousNoAi(mob.isNoAi());
            data.previousNoAiStored(true);
        }
        boolean nativeStun = SaintsDragonsTorporCompat.maintainNativeStun(entity);
        if (!nativeStun && entity instanceof Mob mob) {
            mob.setTarget(null);
            mob.getNavigation().stop();
            mob.setNoAi(true);
        }
        ensurePlayerDarkness(entity, data);
    }

    private static void wake(LivingEntity entity, TorporData data) {
        data.current(0.0D);
        data.unconscious(false);
        SaintsDragonsTorporCompat.clearNativeStun(entity);
        if (entity instanceof Mob mob && data.previousNoAiStored()) {
            mob.setNoAi(data.previousNoAi());
        }
        data.previousNoAiStored(false);
        clearTorporDarkness(entity, data);
        syncPlayerState(entity, false);
        data.lastUpdateGameTime(entity.level().getGameTime());
        TamingService.onWake(entity);
        UnconsciousManager.unregister(entity);
    }

    private static void ensurePlayerDarkness(LivingEntity entity, TorporData data) {
        if (!(entity instanceof Player player)) {
            return;
        }

        MobEffectInstance current = player.getEffect(MobEffects.DARKNESS);
        if (!data.darknessAppliedByTorpor()) {
            if (current != null) {
                return;
            }
            player.addEffect(createTorporDarkness());
            data.darknessAppliedByTorpor(true);
            return;
        }

        if (current == null) {
            player.addEffect(createTorporDarkness());
            return;
        }

        if (isTorporDarkness(current) && current.getDuration() <= UNCONSCIOUS_DARKNESS_REFRESH_THRESHOLD_TICKS) {
            player.addEffect(createTorporDarkness());
        }
    }

    private static void clearTorporDarkness(LivingEntity entity, TorporData data) {
        if (!(entity instanceof Player player)) {
            data.darknessAppliedByTorpor(false);
            return;
        }

        MobEffectInstance current = player.getEffect(MobEffects.DARKNESS);
        if (data.darknessAppliedByTorpor() && isTorporDarkness(current)) {
            player.removeEffect(MobEffects.DARKNESS);
        }
        data.darknessAppliedByTorpor(false);
    }

    private static MobEffectInstance createTorporDarkness() {
        return new MobEffectInstance(
                MobEffects.DARKNESS,
                UNCONSCIOUS_DARKNESS_DURATION_TICKS,
                0,
                false,
                false,
                false
        );
    }

    private static boolean isTorporDarkness(MobEffectInstance effect) {
        return effect != null
                && effect.getEffect() == MobEffects.DARKNESS
                && effect.getAmplifier() == 0
                && !effect.isAmbient()
                && !effect.isVisible()
                && !effect.showIcon();
    }

    private static void syncPlayerState(LivingEntity entity, boolean unconscious) {
        if (entity instanceof ServerPlayer player) {
            NetworkHandler.sendUnconsciousState(player, unconscious);
        }
    }
}

