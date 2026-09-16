package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.data.ProgressData;
import com.szypxj.tldomesticatemorecreatures.domestication.TamingRuleManager;
import com.szypxj.tldomesticatemorecreatures.game.LevelService;
import com.szypxj.tldomesticatemorecreatures.game.PetOwnershipService;
import com.szypxj.tldomesticatemorecreatures.network.NetworkHandler;
import com.szypxj.tldomesticatemorecreatures.network.TargetHudSnapshot;
import com.szypxj.tldomesticatemorecreatures.torpor.TorporService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2STargetHudPacket(int entityId) {
    public static void encode(C2STargetHudPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId());
    }

    public static C2STargetHudPacket decode(FriendlyByteBuf buffer) {
        return new C2STargetHudPacket(buffer.readVarInt());
    }

    public static void handle(C2STargetHudPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        if (sender == null) {
            context.setPacketHandled(true);
            return;
        }
        context.enqueueWork(() -> {
            Entity entity = sender.serverLevel().getEntity(packet.entityId());
            if (!(entity instanceof LivingEntity target) || target == sender) {
                return;
            }
            if (sender.distanceToSqr(target) > 49.0D || !sender.hasLineOfSight(target)) {
                return;
            }
            boolean tamed = PetOwnershipService.isTamed(target);
            boolean torporAvailable = !tamed && TorporService.isEnabled(target);
            double maxTorpor = torporAvailable ? TorporService.maxTorpor(target) : 0.0D;
            torporAvailable = torporAvailable && maxTorpor > 0.0D;
            double torpor = torporAvailable ? TorporService.currentTorpor(target) : 0.0D;
            int requiredTamingLevel = 0;
            if (!tamed) {
                var tamingRule = TamingRuleManager.ruleFor(target);
                if (tamingRule.isPresent()) {
                    LevelService.initializeIfNeeded(sender);
                    int playerLevel = ProgressData.of(sender).level();
                    int requiredPlayerLevel = tamingRule.get().requiredPlayerLevel();
                    if (playerLevel < requiredPlayerLevel) {
                        requiredTamingLevel = requiredPlayerLevel;
                    }
                }
            }
            NetworkHandler.sendTargetHud(sender, new TargetHudSnapshot(
                    target.getId(),
                    tamed,
                    target.getHealth(),
                    target.getMaxHealth(),
                    torporAvailable,
                    torpor,
                    maxTorpor,
                    requiredTamingLevel
            ));
        });
        context.setPacketHandled(true);
    }
}
