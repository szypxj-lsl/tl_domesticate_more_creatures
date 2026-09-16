package com.szypxj.tldomesticatemorecreatures.game;

import com.szypxj.tldomesticatemorecreatures.TlDomesticateMoreCreatures;
import com.szypxj.tldomesticatemorecreatures.config.StatConfigManager;
import com.szypxj.tldomesticatemorecreatures.config.TalentConfigManager;
import com.szypxj.tldomesticatemorecreatures.config.SpecialTalentConfigManager;
import com.szypxj.tldomesticatemorecreatures.data.EntityRuleManager;
import com.szypxj.tldomesticatemorecreatures.data.ProgressData;
import com.szypxj.tldomesticatemorecreatures.domestication.TamingRuleManager;
import com.szypxj.tldomesticatemorecreatures.elite.EliteService;
import com.szypxj.tldomesticatemorecreatures.imprint.ImprintService;
import com.szypxj.tldomesticatemorecreatures.command.pet.PetSelectionService;
import com.szypxj.tldomesticatemorecreatures.network.NetworkHandler;
import com.szypxj.tldomesticatemorecreatures.petmanagement.PetManagementService;
import com.szypxj.tldomesticatemorecreatures.torpor.TorporService;
import com.szypxj.tldomesticatemorecreatures.talent.SpecialTalentService;
import com.szypxj.tldomesticatemorecreatures.talent.active.ActiveTalentConfigManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.szypxj.tldomesticatemorecreatures.game.genetics.GeneticEntityCarrier;
import com.szypxj.tldomesticatemorecreatures.game.genetics.GeneticEntityTransferScope;
import com.szypxj.tldomesticatemorecreatures.game.genetics.GeneticItemEntityTransferScope;
import com.szypxj.tldomesticatemorecreatures.game.genetics.HatchScope;
import com.szypxj.tldomesticatemorecreatures.registry.ModItems;
import com.szypxj.tldomesticatemorecreatures.spyglass.SpyglassRadarBaseline;
import com.szypxj.tldomesticatemorecreatures.spyglass.SpyglassScanService;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import java.util.List;

@Mod.EventBusSubscriber(modid = TlDomesticateMoreCreatures.MOD_ID)
public final class GameplayEvents {
    private GameplayEvents() {
    }

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        BaseAttributeSnapshotService.clearTypeCache();
        StatConfigManager.reload();
        TalentConfigManager.reload();
        SpecialTalentConfigManager.reload();
        ActiveTalentConfigManager.reload();
        TamingRuleManager.reload();
        SpyglassRadarBaseline.refresh();
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(EntityRuleManager.INSTANCE);
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        if (GeneticItemEntityTransferScope.tryTransfer(event.getEntity())) {
            return;
        }

        if (GeneticEntityTransferScope.tryTransfer(event.getEntity())) {
            return;
        }

        if (!(event.getEntity() instanceof LivingEntity living)) {
            return;
        }

        if (HatchScope.tryApply(living)) {
            EliteService.ensurePresentation(living);
            return;
        }

        if (GeneticEntityCarrier.has(living)) {
            return;
        }

        if (!LevelService.isAffected(living)) {
            AttributeService.applyAll(living);
            return;
        }

        boolean alreadyInitialized = ProgressData.exists(living);
        LevelService.initializeIfNeeded(living);

        if (!ProgressData.exists(living)) {
            return;
        }

        if (alreadyInitialized) {
            TalentService.ensureTalents(living);
            SpecialTalentService.ensureTalents(living);
            AttributeService.applyAll(living);
        }
        EliteService.ensurePresentation(living);
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity living) {
            CombatTracker.clear(living);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        ProgressData.copy(event.getOriginal(), event.getEntity());
        LevelService.initializeIfNeeded(event.getEntity());
        TalentService.ensureTalents(event.getEntity());
        SpecialTalentService.ensureTalents(event.getEntity());
        AttributeService.applyAll(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        LevelService.initializeIfNeeded(event.getEntity());
        TalentService.ensureTalents(event.getEntity());
        SpecialTalentService.ensureTalents(event.getEntity());
        AttributeService.applyAll(event.getEntity());
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            NetworkHandler.sendLevelTo(serverPlayer, serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            PetSelectionService.clear(serverPlayer);
            SpyglassScanService.clear(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer serverPlayer) {
            SpyglassScanService.tickPlayer(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        SpyglassScanService.clearAll();
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getTarget() instanceof LivingEntity target) {
            if (!LevelService.isAffected(target)) {
                return;
            }
            LevelService.initializeIfNeeded(target);
            if (ProgressData.exists(target)) {
                NetworkHandler.sendLevelTo(player, target);
            }
            NetworkHandler.sendImprintStateTo(player, target);
            NetworkHandler.sendPetOwnership(player, target);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onTame(AnimalTameEvent event) {
        if (!event.getAnimal().level().isClientSide) {
            if (!LevelService.isAffected(event.getAnimal())) {
                return;
            }
            if (EliteService.isElite(event.getAnimal())) {
                if (!EliteService.canBeTamed(event.getAnimal())) {
                    return;
                }
                LevelService.normalizeEliteForTaming(event.getAnimal());
            }
            LevelService.markTamed(event.getAnimal());
            TorporService.clear(event.getAnimal());
            PetManagementService.track(event.getAnimal());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer
                && event.getTarget() instanceof LivingEntity target) {
            PetOwnershipService.observeTameInteraction(serverPlayer, target);
        }

        ItemStack stack = event.getItemStack();

        if (!stack.is(ModItems.PET_EXPERIENCE_POTION.get())
                && !stack.is(ModItems.NARCOTIC.get())
                && !stack.is(ModItems.STRONG_NARCOTIC.get())
                && !stack.is(ModItems.CONCENTRATED_NARCOTIC.get())
                && !stack.is(ModItems.EMPTY_PET_EXPERIENCE_BOTTLE.get())
                && !stack.is(ModItems.PET_EXPERIENCE_BOTTLE.get())
                && !stack.is(ModItems.ATTRIBUTE_RESET_CRYSTAL.get())) {
            return;
        }

        if (!(event.getTarget() instanceof LivingEntity target)) {
            return;
        }

        InteractionResult result = stack.getItem().interactLivingEntity(
                stack,
                event.getEntity(),
                target,
                event.getHand()
        );

        if (result == InteractionResult.PASS) {
            return;
        }

        event.setCancellationResult(result);
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBabySpawn(BabyEntitySpawnEvent event) {
        AgeableMob child = event.getChild();
        if (child == null || child.level().isClientSide) {
            return;
        }
        LevelService.initializeInherited(child, event.getParentA(), event.getParentB(), child.getRandom());
        ImprintService.markOffspring(child);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) {
            return;
        }
        Entity sourceEntity = event.getSource().getEntity();
        if (sourceEntity instanceof LivingEntity attacker) {
            LevelService.initializeIfNeeded(attacker);
            CombatTracker.record(victim, attacker);
            double multiplier = AttributeService.damageMultiplier(attacker);
            if (multiplier != 1.0D) {
                event.setAmount((float) Math.max(0.0D, event.getAmount() * multiplier));
            }
        }
        LevelService.initializeIfNeeded(victim);
        double resistance = AttributeService.resistance(victim);
        if (resistance > 0.0D) {
            event.setAmount((float) Math.max(0.0D, event.getAmount() * (1.0D - resistance)));
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) {
            return;
        }
        LevelService.initializeIfNeeded(victim);
        if (victim instanceof Player) {
            CombatTracker.clear(victim);
            return;
        }
        if (!LevelService.isAffected(victim)) {
            CombatTracker.clear(victim);
            return;
        }
        long total = LevelService.killExperience(victim);
        List<LivingEntity> recipients = CombatTracker.recipients(victim);
        if (total <= 0L || recipients.isEmpty()) {
            return;
        }
        long each = total / recipients.size();
        long remainder = total % recipients.size();
        LivingEntity killer = event.getSource().getEntity() instanceof LivingEntity living ? living : null;
        for (LivingEntity recipient : recipients) {
            long grant = each;
            if (remainder > 0L && killer != null && recipient.getUUID().equals(killer.getUUID())) {
                grant += remainder;
                remainder = 0L;
            }
            LevelService.addExperience(recipient, grant, ExperienceSource.KILL);
        }
        if (remainder > 0L) {
            LevelService.addExperience(recipients.get(0), remainder, ExperienceSource.KILL);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onXpChange(PlayerXpEvent.XpChange event) {
        if (!event.getEntity().level().isClientSide && event.getAmount() > 0) {
            LevelService.addExperience(event.getEntity(), event.getAmount(), ExperienceSource.VANILLA);
        }
    }
}
