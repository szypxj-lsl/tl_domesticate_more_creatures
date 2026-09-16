package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.config.Config;
import com.szypxj.tldomesticatemorecreatures.game.LevelService;
import com.szypxj.tldomesticatemorecreatures.game.genetics.GeneticBlockCarrierData;
import com.szypxj.tldomesticatemorecreatures.game.genetics.GeneticCarrierEntityMarker;
import com.szypxj.tldomesticatemorecreatures.game.genetics.GeneticEggBlockMarker;
import com.szypxj.tldomesticatemorecreatures.game.genetics.GeneticEntityCarrier;
import com.szypxj.tldomesticatemorecreatures.game.genetics.GeneticItemCarrier;
import com.szypxj.tldomesticatemorecreatures.game.genetics.GeneticItemStackCarrierView;
import com.szypxj.tldomesticatemorecreatures.game.genetics.HatchGeneticPayload;
import com.szypxj.tldomesticatemorecreatures.item.SpyglassItemHelper;
import com.szypxj.tldomesticatemorecreatures.network.InspectTarget;
import com.szypxj.tldomesticatemorecreatures.network.NetworkHandler;
import com.szypxj.tldomesticatemorecreatures.network.SnapshotFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2SInspectPacket(InspectTarget target) {
    public static void encode(C2SInspectPacket packet, FriendlyByteBuf buffer) {
        packet.target().encode(buffer);
    }

    public static C2SInspectPacket decode(FriendlyByteBuf buffer) {
        return new C2SInspectPacket(InspectTarget.decode(buffer));
    }

    public static void handle(C2SInspectPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null || packet.target() == null) {
                return;
            }
            boolean usingSpyglass = SpyglassItemHelper.isUsingSpyglass(sender);
            boolean holdingSpyglass = SpyglassItemHelper.isHoldingSpyglass(sender);
            boolean holdingSuperSpyglass = SpyglassItemHelper.isHoldingSuperSpyglass(sender);
            if (!usingSpyglass && !holdingSpyglass) {
                return;
            }
            double inspectRange = usingSpyglass || holdingSuperSpyglass
                    ? Config.INSPECT_SPYGLASS_RANGE.get()
                    : Config.INSPECT_HANDHELD_RANGE.get();

            if (packet.target().kind() == InspectTarget.Kind.BLOCK) {
                inspectBlock(sender, packet.target(), inspectRange);
            } else {
                inspectEntity(sender, packet.target(), inspectRange);
            }
        });
        context.setPacketHandled(true);
    }

    private static void inspectEntity(ServerPlayer sender, InspectTarget targetRef, double inspectRange) {
        Entity entity = sender.level().getEntity(targetRef.entityId());
        boolean superSpyglassThroughWalls = entity instanceof LivingEntity
                && SpyglassItemHelper.isHoldingSuperSpyglass(sender);
        if (entity == null
                || sender.distanceToSqr(entity) > inspectRange * inspectRange
                || (!superSpyglassThroughWalls && !sender.hasLineOfSight(entity))) {
            return;
        }

        HatchGeneticPayload entityPayload = GeneticEntityCarrier.get(entity).orElse(null);
        if (entityPayload != null || entity instanceof GeneticCarrierEntityMarker) {
            NetworkHandler.sendInspect(sender, SnapshotFactory.inspectEgg(targetRef, entity.getDisplayName(), entityPayload));
            return;
        }

        if (entity instanceof ItemEntity itemEntity) {
            ItemStack stack = itemEntity.getItem();
            HatchGeneticPayload itemPayload = GeneticItemCarrier.get(stack).orElse(null);
            if (itemPayload != null) {
                NetworkHandler.sendInspect(sender, SnapshotFactory.inspectEgg(targetRef, stack.getHoverName(), itemPayload));
            }
            return;
        }

        if (!(entity instanceof LivingEntity living)) {
            return;
        }
        LevelService.initializeIfNeeded(living);
        if (!LevelService.isAffected(living)) {
            NetworkHandler.sendInspect(sender, SnapshotFactory.inspectBasic(living));
            return;
        }
        NetworkHandler.sendInspect(sender, SnapshotFactory.inspect(sender, living));
    }

    private static void inspectBlock(ServerPlayer sender, InspectTarget targetRef, double inspectRange) {
        BlockPos pos = targetRef.blockPosition();
        Vec3 center = Vec3.atCenterOf(pos);
        if (sender.getEyePosition().distanceToSqr(center) > inspectRange * inspectRange) {
            return;
        }
        BlockHitResult hit = sender.level().clip(new ClipContext(
                sender.getEyePosition(),
                center,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                sender
        ));
        if (hit.getType() == HitResult.Type.MISS || !hit.getBlockPos().equals(pos)) {
            return;
        }

        BlockEntity blockEntity = sender.level().getBlockEntity(pos);
        if (blockEntity == null) {
            return;
        }

        HatchGeneticPayload payload = GeneticBlockCarrierData.get(blockEntity).orElse(null);
        Component name = blockEntity.getBlockState().getBlock().getName();
        boolean recognizedCarrier = payload != null || blockEntity instanceof GeneticEggBlockMarker;

        if (blockEntity instanceof GeneticItemStackCarrierView carrierView) {
            ItemStack stack = carrierView.tdmc$getGeneticItemStack();
            if (stack != null && !stack.isEmpty()) {
                recognizedCarrier = true;
                name = stack.getHoverName();
                if (payload == null) {
                    payload = GeneticItemCarrier.get(stack).orElse(null);
                }
            }
        }

        if (!recognizedCarrier) {
            return;
        }
        NetworkHandler.sendInspect(sender, SnapshotFactory.inspectEgg(targetRef, name, payload));
    }
}
