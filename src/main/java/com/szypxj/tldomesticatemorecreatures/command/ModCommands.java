package com.szypxj.tldomesticatemorecreatures.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.szypxj.tldomesticatemorecreatures.TlDomesticateMoreCreatures;
import com.szypxj.tldomesticatemorecreatures.config.StatConfigManager;
import com.szypxj.tldomesticatemorecreatures.config.TalentConfigManager;
import com.szypxj.tldomesticatemorecreatures.config.SpecialTalentConfigManager;
import com.szypxj.tldomesticatemorecreatures.domestication.TamingRuleManager;
import com.szypxj.tldomesticatemorecreatures.domestication.editor.TamingEditorService;
import com.szypxj.tldomesticatemorecreatures.config.TalentDefinition;
import com.szypxj.tldomesticatemorecreatures.data.ProgressData;
import com.szypxj.tldomesticatemorecreatures.game.AttributeService;
import com.szypxj.tldomesticatemorecreatures.game.LevelService;
import com.szypxj.tldomesticatemorecreatures.network.NetworkHandler;
import com.szypxj.tldomesticatemorecreatures.riding.config.RidingConfigManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TlDomesticateMoreCreatures.MOD_ID)
public final class ModCommands {
    private ModCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tdmc")
                .then(Commands.literal("reload")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> reload(context.getSource())))
                .then(Commands.literal("riding")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("edit")
                                .executes(context -> openRidingEditor(context.getSource())))
                        .then(Commands.literal("reload")
                                .executes(context -> reloadRiding(context.getSource()))))
                .then(Commands.literal("taming")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("edit")
                                .executes(context -> openTamingEditor(context.getSource())))
                        .then(Commands.literal("reload")
                                .executes(context -> reloadTamingEditor(context.getSource()))))
                .then(Commands.literal("info")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> info(context.getSource(), defaultTarget(context.getSource())))
                        .then(Commands.argument("target", EntityArgument.entity())
                                .executes(context -> info(context.getSource(), living(EntityArgument.getEntity(context, "target"))))))
                .then(Commands.literal("level")
                        .then(Commands.literal("set")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("level", IntegerArgumentType.integer(1))
                                        .executes(context -> setLevel(context.getSource(), defaultTarget(context.getSource()), IntegerArgumentType.getInteger(context, "level")))
                                        .then(Commands.argument("target", EntityArgument.entity())
                                                .executes(context -> setLevel(context.getSource(), living(EntityArgument.getEntity(context, "target")), IntegerArgumentType.getInteger(context, "level")))))))
                .then(Commands.literal("xp")
                        .then(Commands.literal("add")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(context -> addXp(context.getSource(), defaultTarget(context.getSource()), IntegerArgumentType.getInteger(context, "amount")))
                                        .then(Commands.argument("target", EntityArgument.entity())
                                                .executes(context -> addXp(context.getSource(), living(EntityArgument.getEntity(context, "target")), IntegerArgumentType.getInteger(context, "amount")))))))
                .then(Commands.literal("points")
                        .then(Commands.literal("add")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("amount", IntegerArgumentType.integer())
                                        .executes(context -> addPoints(context.getSource(), defaultTarget(context.getSource()), IntegerArgumentType.getInteger(context, "amount")))
                                        .then(Commands.argument("target", EntityArgument.entity())
                                                .executes(context -> addPoints(context.getSource(), living(EntityArgument.getEntity(context, "target")), IntegerArgumentType.getInteger(context, "amount")))))))
                .then(Commands.literal("stat")
                        .then(Commands.literal("set")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("stat", StringArgumentType.word())
                                        .then(Commands.argument("points", IntegerArgumentType.integer(0))
                                                .executes(context -> setStat(context.getSource(), defaultTarget(context.getSource()), StringArgumentType.getString(context, "stat"), IntegerArgumentType.getInteger(context, "points")))
                                                .then(Commands.argument("target", EntityArgument.entity())
                                                        .executes(context -> setStat(context.getSource(), living(EntityArgument.getEntity(context, "target")), StringArgumentType.getString(context, "stat"), IntegerArgumentType.getInteger(context, "points"))))))))
                .then(Commands.literal("reset")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> reset(context.getSource(), defaultTarget(context.getSource())))
                        .then(Commands.argument("target", EntityArgument.entity())
                                .executes(context -> reset(context.getSource(), living(EntityArgument.getEntity(context, "target")))))));
    }

    private static int openRidingEditor(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        NetworkHandler.sendRidingEditorSnapshot(player, RidingConfigManager.snapshot(player.serverLevel()));
        return 1;
    }

    private static int openTamingEditor(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        NetworkHandler.sendTamingEditorSnapshot(player, TamingEditorService.snapshot(), true);
        return 1;
    }

    private static int reloadTamingEditor(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        TamingEditorService.ReloadResult result = TamingEditorService.reload(player);
        if (result.success()) {
            NetworkHandler.sendTamingEditorSnapshot(player, TamingEditorService.snapshot(), false);
            source.sendSuccess(() -> Component.translatable(result.messageKey()), true);
            return 1;
        }
        source.sendFailure(Component.translatable(result.messageKey()));
        return 0;
    }

    private static int reloadRiding(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        RidingConfigManager.ReloadResult result = RidingConfigManager.reload(player);
        if (result.success()) {
            NetworkHandler.broadcastRidingSnapshot(RidingConfigManager.snapshot(player.serverLevel()));
            source.sendSuccess(() -> Component.translatable(result.messageKey()), true);
            return 1;
        }
        source.sendFailure(Component.translatable(result.messageKey()));
        return 0;
    }

    private static int reload(CommandSourceStack source) {
        StatConfigManager.reload();
        TalentConfigManager.reload();
        SpecialTalentConfigManager.reload();
        TamingRuleManager.reload();
        source.getServer().reloadResources(source.getServer().getPackRepository().getSelectedIds())
                .thenRunAsync(() -> {
                    LevelService.reapplyAll(source.getServer());
                    source.sendSuccess(() -> Component.translatable("cmd.tl_domesticate_more_creatures.reload_success"), true);
                }, source.getServer());
        return 1;
    }

    private static int info(CommandSourceStack source, LivingEntity target) {
        if (target == null) {
            source.sendFailure(Component.translatable("cmd.tl_domesticate_more_creatures.no_target"));
            return 0;
        }
        LevelService.initializeIfNeeded(target);
        if (!ProgressData.exists(target)) {
            source.sendFailure(Component.translatable("cmd.tl_domesticate_more_creatures.not_affected"));
            return 0;
        }
        ProgressData data = ProgressData.of(target);
        source.sendSuccess(() -> Component.translatable("cmd.tl_domesticate_more_creatures.info", target.getDisplayName(), data.level(), data.maxLevel(), data.experience(), data.unspentPoints()), false);
        AttributeService.definitionsFor(target).forEach(definition -> {
            ProgressData.StatPoints points = data.stat(definition.id());
            source.sendSuccess(() -> Component.translatable("cmd.tl_domesticate_more_creatures.info_stat", definition.id(), points.wild(), points.trained(), points.talent(), points.total()), false);
        });
        data.talents().forEach((id, talentLevel) -> {
            TalentDefinition definition = TalentConfigManager.byId(id);
            if (definition != null) {
                source.sendSuccess(() -> Component.translatable(
                        "cmd.tl_domesticate_more_creatures.info_talent",
                        Component.translatable(definition.nameKey()),
                        definition.clampLevel(talentLevel)
                ), false);
            }
        });
        return 1;
    }

    private static int setLevel(CommandSourceStack source, LivingEntity target, int level) {
        if (target == null) {
            return failTarget(source);
        }
        LevelService.setLevel(target, level);
        source.sendSuccess(() -> Component.translatable("cmd.tl_domesticate_more_creatures.level_set", target.getDisplayName(), ProgressData.of(target).level()), true);
        return 1;
    }

    private static int addXp(CommandSourceStack source, LivingEntity target, int amount) {
        if (target == null) {
            return failTarget(source);
        }
        LevelService.addExperience(target, amount);
        source.sendSuccess(() -> Component.translatable("cmd.tl_domesticate_more_creatures.xp_add", target.getDisplayName(), amount), true);
        return 1;
    }

    private static int addPoints(CommandSourceStack source, LivingEntity target, int amount) {
        if (target == null) {
            return failTarget(source);
        }
        LevelService.addUnspentPoints(target, amount);
        source.sendSuccess(() -> Component.translatable("cmd.tl_domesticate_more_creatures.points_add", target.getDisplayName(), amount), true);
        return 1;
    }

    private static int setStat(CommandSourceStack source, LivingEntity target, String stat, int points) {
        if (target == null) {
            return failTarget(source);
        }
        if (!LevelService.setStatPoints(target, stat, points)) {
            source.sendFailure(Component.translatable("cmd.tl_domesticate_more_creatures.unknown_stat", stat));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("cmd.tl_domesticate_more_creatures.stat_set", target.getDisplayName(), stat, points), true);
        return 1;
    }

    private static int reset(CommandSourceStack source, LivingEntity target) {
        if (target == null) {
            return failTarget(source);
        }
        LevelService.reset(target);
        source.sendSuccess(() -> Component.translatable("cmd.tl_domesticate_more_creatures.reset", target.getDisplayName()), true);
        return 1;
    }

    private static int failTarget(CommandSourceStack source) {
        source.sendFailure(Component.translatable("cmd.tl_domesticate_more_creatures.no_target"));
        return 0;
    }

    private static LivingEntity defaultTarget(CommandSourceStack source) {
        Entity sourceEntity = source.getEntity();
        if (!(sourceEntity instanceof ServerPlayer player)) {
            return null;
        }
        LivingEntity looked = lookedAt(player, 32.0D);
        return looked == null ? player : looked;
    }

    private static LivingEntity living(Entity entity) {
        return entity instanceof LivingEntity living ? living : null;
    }

    private static LivingEntity lookedAt(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = start.add(look.scale(range));
        AABB box = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, start, end, box, entity -> entity instanceof LivingEntity && entity.isPickable(), range * range);
        return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
    }
}
