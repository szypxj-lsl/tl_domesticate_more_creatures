package com.szypxj.tldomesticatemorecreatures.network;

import com.szypxj.tldomesticatemorecreatures.TlDomesticateMoreCreatures;
import com.szypxj.tldomesticatemorecreatures.api.attribute.TdmcAttributeLifecycle;
import com.szypxj.tldomesticatemorecreatures.api.attribute.TdmcAttributeNetwork;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideAction;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideActionInfo;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideActionStatus;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideCapabilities;
import com.szypxj.tldomesticatemorecreatures.data.ProgressData;
import com.szypxj.tldomesticatemorecreatures.command.pet.PetCommand;
import com.szypxj.tldomesticatemorecreatures.command.pet.PetCommandSummary;
import com.szypxj.tldomesticatemorecreatures.command.pet.PetSelectionMode;
import com.szypxj.tldomesticatemorecreatures.game.PetOwnershipService;
import com.szypxj.tldomesticatemorecreatures.game.LevelService;
import com.szypxj.tldomesticatemorecreatures.petmanagement.PetManagementService;
import com.szypxj.tldomesticatemorecreatures.petmanagement.PetRecord;
import com.szypxj.tldomesticatemorecreatures.imprint.ImprintData;
import com.szypxj.tldomesticatemorecreatures.imprint.ImprintService;
import com.szypxj.tldomesticatemorecreatures.network.packet.C2SAllocateStatPacket;
import com.szypxj.tldomesticatemorecreatures.network.packet.C2SInspectPacket;
import com.szypxj.tldomesticatemorecreatures.network.packet.C2SSpyglassScanCandidatePacket;
import com.szypxj.tldomesticatemorecreatures.network.packet.C2STargetHudPacket;
import com.szypxj.tldomesticatemorecreatures.network.packet.C2SOpenPanelPacket;
import com.szypxj.tldomesticatemorecreatures.network.packet.C2SOpenNativePetInventoryPacket;
import com.szypxj.tldomesticatemorecreatures.network.packet.S2CActiveTalentVisualPacket;
import com.szypxj.tldomesticatemorecreatures.network.packet.S2CActiveTalentStatePacket;
import com.szypxj.tldomesticatemorecreatures.network.packet.C2SActiveTalentInputPacket;
import com.szypxj.tldomesticatemorecreatures.network.packet.C2SRideAttackPacket;
import com.szypxj.tldomesticatemorecreatures.network.packet.C2SRideActionPacket;
import com.szypxj.tldomesticatemorecreatures.network.packet.S2CRideControlProfilePacket;
import com.szypxj.tldomesticatemorecreatures.network.packet.S2CRideActionStatePacket;
import com.szypxj.tldomesticatemorecreatures.network.packet.C2SOpenCraftingPacket;
import com.szypxj.tldomesticatemorecreatures.network.packet.C2SRefreshPanelPacket;
import com.szypxj.tldomesticatemorecreatures.network.packet.C2STogglePetSelectionPacket;
import com.szypxj.tldomesticatemorecreatures.network.packet.C2SPetCommandPacket;
import com.szypxj.tldomesticatemorecreatures.network.packet.C2SClearPetCommandPacket;
import com.szypxj.tldomesticatemorecreatures.network.packet.C2SClearPetCommandMarkerPacket;
import com.szypxj.tldomesticatemorecreatures.network.packet.C2SRequestPetCommandSummaryPacket;
import com.szypxj.tldomesticatemorecreatures.network.packet.C2SSetRidePermissionPacket;
import com.szypxj.tldomesticatemorecreatures.network.packet.C2SRideInputPacket;
import com.szypxj.tldomesticatemorecreatures.network.packet.C2SRideMountPacket;
import com.szypxj.tldomesticatemorecreatures.network.packet.S2CRideStatePacket;
import com.szypxj.tldomesticatemorecreatures.network.packet.C2SRequestRidingConfigPacket;
import com.szypxj.tldomesticatemorecreatures.network.packet.S2CRidingConfigSnapshotPacket;
import com.szypxj.tldomesticatemorecreatures.network.packet.S2CRidingProfileUpdatePacket;
import com.szypxj.tldomesticatemorecreatures.network.packet.C2SRideConfigUpdatePacket;
import com.szypxj.tldomesticatemorecreatures.network.packet.C2SRideConfigReloadPacket;
import com.szypxj.tldomesticatemorecreatures.network.packet.C2SRequestTamingEditorPacket;
import com.szypxj.tldomesticatemorecreatures.network.packet.C2SRequestTamingNativeFoodsPacket;
import com.szypxj.tldomesticatemorecreatures.network.packet.C2SSaveTamingEditorPacket;
import com.szypxj.tldomesticatemorecreatures.network.packet.C2SReloadTamingEditorPacket;
import com.szypxj.tldomesticatemorecreatures.network.packet.S2CTamingEditorSnapshotPacket;
import com.szypxj.tldomesticatemorecreatures.network.packet.S2CTamingNativeFoodsPacket;
import com.szypxj.tldomesticatemorecreatures.network.packet.S2CTamingEditorOperationResultPacket;
import com.szypxj.tldomesticatemorecreatures.network.packet.S2CInspectPacket;
import com.szypxj.tldomesticatemorecreatures.network.packet.S2CTargetHudPacket;
import com.szypxj.tldomesticatemorecreatures.network.packet.S2CImprintStatePacket;
import com.szypxj.tldomesticatemorecreatures.network.packet.S2CLevelPacket;
import com.szypxj.tldomesticatemorecreatures.network.packet.S2CPanelPacket;
import com.szypxj.tldomesticatemorecreatures.network.packet.S2CPetSelectionPacket;
import com.szypxj.tldomesticatemorecreatures.network.packet.S2CPetCommandSummaryPacket;
import com.szypxj.tldomesticatemorecreatures.network.packet.S2CPetCommandMarkerPacket;
import com.szypxj.tldomesticatemorecreatures.network.packet.S2CPetOwnershipPacket;
import com.szypxj.tldomesticatemorecreatures.network.packet.S2CUnconsciousStatePacket;
import com.szypxj.tldomesticatemorecreatures.network.packet.C2SRequestPetManagementListPacket;
import com.szypxj.tldomesticatemorecreatures.network.packet.S2CPetManagementListPacket;
import com.szypxj.tldomesticatemorecreatures.network.packet.C2SRequestPetManagementDetailPacket;
import com.szypxj.tldomesticatemorecreatures.network.packet.S2CPetManagementDetailPacket;
import com.szypxj.tldomesticatemorecreatures.network.packet.C2SSetPetShortcutPacket;
import com.szypxj.tldomesticatemorecreatures.network.packet.C2SStoreManagedPetPacket;
import com.szypxj.tldomesticatemorecreatures.network.packet.C2SSummonManagedPetPacket;
import com.szypxj.tldomesticatemorecreatures.network.packet.C2SLocateManagedPetPacket;
import com.szypxj.tldomesticatemorecreatures.network.packet.C2SRemoveDeadPetRecordPacket;
import com.szypxj.tldomesticatemorecreatures.network.packet.C2SSummonPetShortcutPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.List;
import java.util.UUID;

public final class NetworkHandler {
    private static final String VERSION = "28";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.tryBuild(TlDomesticateMoreCreatures.MOD_ID, "main"),
            () -> VERSION,
            VERSION::equals,
            VERSION::equals
    );
    private static int id;

    private NetworkHandler() {
    }

    public static void register() {
        CHANNEL.messageBuilder(C2SOpenPanelPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SOpenPanelPacket::encode)
                .decoder(C2SOpenPanelPacket::decode)
                .consumerMainThread(C2SOpenPanelPacket::handle)
                .add();
        CHANNEL.messageBuilder(C2SOpenCraftingPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SOpenCraftingPacket::encode)
                .decoder(C2SOpenCraftingPacket::decode)
                .consumerMainThread(C2SOpenCraftingPacket::handle)
                .add();
        CHANNEL.messageBuilder(C2SRefreshPanelPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SRefreshPanelPacket::encode)
                .decoder(C2SRefreshPanelPacket::decode)
                .consumerMainThread(C2SRefreshPanelPacket::handle)
                .add();
        CHANNEL.messageBuilder(C2SAllocateStatPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SAllocateStatPacket::encode)
                .decoder(C2SAllocateStatPacket::decode)
                .consumerMainThread(C2SAllocateStatPacket::handle)
                .add();
        CHANNEL.messageBuilder(C2SInspectPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SInspectPacket::encode)
                .decoder(C2SInspectPacket::decode)
                .consumerMainThread(C2SInspectPacket::handle)
                .add();
        CHANNEL.messageBuilder(S2CPanelPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CPanelPacket::encode)
                .decoder(S2CPanelPacket::decode)
                .consumerMainThread(S2CPanelPacket::handle)
                .add();
        CHANNEL.messageBuilder(S2CInspectPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CInspectPacket::encode)
                .decoder(S2CInspectPacket::decode)
                .consumerMainThread(S2CInspectPacket::handle)
                .add();
        CHANNEL.messageBuilder(C2STargetHudPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2STargetHudPacket::encode)
                .decoder(C2STargetHudPacket::decode)
                .consumerMainThread(C2STargetHudPacket::handle)
                .add();
        CHANNEL.messageBuilder(S2CTargetHudPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CTargetHudPacket::encode)
                .decoder(S2CTargetHudPacket::decode)
                .consumerMainThread(S2CTargetHudPacket::handle)
                .add();
        CHANNEL.messageBuilder(S2CLevelPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CLevelPacket::encode)
                .decoder(S2CLevelPacket::decode)
                .consumerMainThread(S2CLevelPacket::handle)
                .add();
        CHANNEL.messageBuilder(C2STogglePetSelectionPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2STogglePetSelectionPacket::encode)
                .decoder(C2STogglePetSelectionPacket::decode)
                .consumerMainThread(C2STogglePetSelectionPacket::handle)
                .add();
        CHANNEL.messageBuilder(S2CPetSelectionPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CPetSelectionPacket::encode)
                .decoder(S2CPetSelectionPacket::decode)
                .consumerMainThread(S2CPetSelectionPacket::handle)
                .add();
        CHANNEL.messageBuilder(S2CPetOwnershipPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CPetOwnershipPacket::encode)
                .decoder(S2CPetOwnershipPacket::decode)
                .consumerMainThread(S2CPetOwnershipPacket::handle)
                .add();
        CHANNEL.messageBuilder(S2CUnconsciousStatePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CUnconsciousStatePacket::encode)
                .decoder(S2CUnconsciousStatePacket::decode)
                .consumerMainThread(S2CUnconsciousStatePacket::handle)
                .add();
        CHANNEL.messageBuilder(S2CImprintStatePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CImprintStatePacket::encode)
                .decoder(S2CImprintStatePacket::decode)
                .consumerMainThread(S2CImprintStatePacket::handle)
                .add();
        CHANNEL.messageBuilder(C2SPetCommandPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SPetCommandPacket::encode)
                .decoder(C2SPetCommandPacket::decode)
                .consumerMainThread(C2SPetCommandPacket::handle)
                .add();
        CHANNEL.messageBuilder(C2SClearPetCommandPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SClearPetCommandPacket::encode)
                .decoder(C2SClearPetCommandPacket::decode)
                .consumerMainThread(C2SClearPetCommandPacket::handle)
                .add();
        CHANNEL.messageBuilder(C2SClearPetCommandMarkerPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SClearPetCommandMarkerPacket::encode)
                .decoder(C2SClearPetCommandMarkerPacket::decode)
                .consumerMainThread(C2SClearPetCommandMarkerPacket::handle)
                .add();
        CHANNEL.messageBuilder(C2SRequestPetCommandSummaryPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SRequestPetCommandSummaryPacket::encode)
                .decoder(C2SRequestPetCommandSummaryPacket::decode)
                .consumerMainThread(C2SRequestPetCommandSummaryPacket::handle)
                .add();
        CHANNEL.messageBuilder(S2CPetCommandSummaryPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CPetCommandSummaryPacket::encode)
                .decoder(S2CPetCommandSummaryPacket::decode)
                .consumerMainThread(S2CPetCommandSummaryPacket::handle)
                .add();
        CHANNEL.messageBuilder(S2CPetCommandMarkerPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CPetCommandMarkerPacket::encode)
                .decoder(S2CPetCommandMarkerPacket::decode)
                .consumerMainThread(S2CPetCommandMarkerPacket::handle)
                .add();
        CHANNEL.messageBuilder(C2SSetRidePermissionPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SSetRidePermissionPacket::encode)
                .decoder(C2SSetRidePermissionPacket::decode)
                .consumerMainThread(C2SSetRidePermissionPacket::handle)
                .add();
        CHANNEL.messageBuilder(C2SRideMountPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SRideMountPacket::encode)
                .decoder(C2SRideMountPacket::decode)
                .consumerMainThread(C2SRideMountPacket::handle)
                .add();
        CHANNEL.messageBuilder(C2SRideInputPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SRideInputPacket::encode)
                .decoder(C2SRideInputPacket::decode)
                .consumerMainThread(C2SRideInputPacket::handle)
                .add();
        CHANNEL.messageBuilder(S2CRideStatePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CRideStatePacket::encode)
                .decoder(S2CRideStatePacket::decode)
                .consumerMainThread(S2CRideStatePacket::handle)
                .add();
        CHANNEL.messageBuilder(C2SRequestRidingConfigPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SRequestRidingConfigPacket::encode)
                .decoder(C2SRequestRidingConfigPacket::decode)
                .consumerMainThread(C2SRequestRidingConfigPacket::handle)
                .add();
        CHANNEL.messageBuilder(S2CRidingConfigSnapshotPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CRidingConfigSnapshotPacket::encode)
                .decoder(S2CRidingConfigSnapshotPacket::decode)
                .consumerMainThread(S2CRidingConfigSnapshotPacket::handle)
                .add();
        CHANNEL.messageBuilder(S2CRidingProfileUpdatePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CRidingProfileUpdatePacket::encode)
                .decoder(S2CRidingProfileUpdatePacket::decode)
                .consumerMainThread(S2CRidingProfileUpdatePacket::handle)
                .add();
        CHANNEL.messageBuilder(C2SRideConfigUpdatePacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SRideConfigUpdatePacket::encode)
                .decoder(C2SRideConfigUpdatePacket::decode)
                .consumerMainThread(C2SRideConfigUpdatePacket::handle)
                .add();
        CHANNEL.messageBuilder(C2SRideConfigReloadPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SRideConfigReloadPacket::encode)
                .decoder(C2SRideConfigReloadPacket::decode)
                .consumerMainThread(C2SRideConfigReloadPacket::handle)
                .add();
        CHANNEL.messageBuilder(C2SRequestTamingEditorPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SRequestTamingEditorPacket::encode)
                .decoder(C2SRequestTamingEditorPacket::decode)
                .consumerMainThread(C2SRequestTamingEditorPacket::handle)
                .add();
        CHANNEL.messageBuilder(C2SRequestTamingNativeFoodsPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SRequestTamingNativeFoodsPacket::encode)
                .decoder(C2SRequestTamingNativeFoodsPacket::decode)
                .consumerMainThread(C2SRequestTamingNativeFoodsPacket::handle)
                .add();
        CHANNEL.messageBuilder(C2SSaveTamingEditorPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SSaveTamingEditorPacket::encode)
                .decoder(C2SSaveTamingEditorPacket::decode)
                .consumerMainThread(C2SSaveTamingEditorPacket::handle)
                .add();
        CHANNEL.messageBuilder(C2SReloadTamingEditorPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SReloadTamingEditorPacket::encode)
                .decoder(C2SReloadTamingEditorPacket::decode)
                .consumerMainThread(C2SReloadTamingEditorPacket::handle)
                .add();
        CHANNEL.messageBuilder(S2CTamingEditorSnapshotPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CTamingEditorSnapshotPacket::encode)
                .decoder(S2CTamingEditorSnapshotPacket::decode)
                .consumerMainThread(S2CTamingEditorSnapshotPacket::handle)
                .add();
        CHANNEL.messageBuilder(S2CTamingNativeFoodsPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CTamingNativeFoodsPacket::encode)
                .decoder(S2CTamingNativeFoodsPacket::decode)
                .consumerMainThread(S2CTamingNativeFoodsPacket::handle)
                .add();
        CHANNEL.messageBuilder(S2CTamingEditorOperationResultPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CTamingEditorOperationResultPacket::encode)
                .decoder(S2CTamingEditorOperationResultPacket::decode)
                .consumerMainThread(S2CTamingEditorOperationResultPacket::handle)
                .add();
        CHANNEL.messageBuilder(C2SRequestPetManagementListPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SRequestPetManagementListPacket::encode).decoder(C2SRequestPetManagementListPacket::decode)
                .consumerMainThread(C2SRequestPetManagementListPacket::handle).add();
        CHANNEL.messageBuilder(S2CPetManagementListPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CPetManagementListPacket::encode).decoder(S2CPetManagementListPacket::decode)
                .consumerMainThread(S2CPetManagementListPacket::handle).add();
        CHANNEL.messageBuilder(C2SRequestPetManagementDetailPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SRequestPetManagementDetailPacket::encode).decoder(C2SRequestPetManagementDetailPacket::decode)
                .consumerMainThread(C2SRequestPetManagementDetailPacket::handle).add();
        CHANNEL.messageBuilder(S2CPetManagementDetailPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CPetManagementDetailPacket::encode).decoder(S2CPetManagementDetailPacket::decode)
                .consumerMainThread(S2CPetManagementDetailPacket::handle).add();
        CHANNEL.messageBuilder(C2SSetPetShortcutPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SSetPetShortcutPacket::encode).decoder(C2SSetPetShortcutPacket::decode)
                .consumerMainThread(C2SSetPetShortcutPacket::handle).add();
        CHANNEL.messageBuilder(C2SStoreManagedPetPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SStoreManagedPetPacket::encode).decoder(C2SStoreManagedPetPacket::decode)
                .consumerMainThread(C2SStoreManagedPetPacket::handle).add();
        CHANNEL.messageBuilder(C2SSummonManagedPetPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SSummonManagedPetPacket::encode).decoder(C2SSummonManagedPetPacket::decode)
                .consumerMainThread(C2SSummonManagedPetPacket::handle).add();
        CHANNEL.messageBuilder(C2SLocateManagedPetPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SLocateManagedPetPacket::encode).decoder(C2SLocateManagedPetPacket::decode)
                .consumerMainThread(C2SLocateManagedPetPacket::handle).add();
        CHANNEL.messageBuilder(C2SRemoveDeadPetRecordPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SRemoveDeadPetRecordPacket::encode).decoder(C2SRemoveDeadPetRecordPacket::decode)
                .consumerMainThread(C2SRemoveDeadPetRecordPacket::handle).add();
        CHANNEL.messageBuilder(C2SSummonPetShortcutPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SSummonPetShortcutPacket::encode).decoder(C2SSummonPetShortcutPacket::decode)
                .consumerMainThread(C2SSummonPetShortcutPacket::handle).add();
        CHANNEL.messageBuilder(C2SOpenNativePetInventoryPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SOpenNativePetInventoryPacket::encode)
                .decoder(C2SOpenNativePetInventoryPacket::decode)
                .consumerMainThread(C2SOpenNativePetInventoryPacket::handle)
                .add();
        CHANNEL.messageBuilder(C2SActiveTalentInputPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SActiveTalentInputPacket::encode)
                .decoder(C2SActiveTalentInputPacket::decode)
                .consumerMainThread(C2SActiveTalentInputPacket::handle)
                .add();
        CHANNEL.messageBuilder(S2CActiveTalentStatePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CActiveTalentStatePacket::encode)
                .decoder(S2CActiveTalentStatePacket::decode)
                .consumerMainThread(S2CActiveTalentStatePacket::handle)
                .add();
        CHANNEL.messageBuilder(S2CActiveTalentVisualPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CActiveTalentVisualPacket::encode)
                .decoder(S2CActiveTalentVisualPacket::decode)
                .consumerMainThread(S2CActiveTalentVisualPacket::handle)
                .add();
        CHANNEL.messageBuilder(C2SRideAttackPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SRideAttackPacket::encode)
                .decoder(C2SRideAttackPacket::decode)
                .consumerMainThread(C2SRideAttackPacket::handle)
                .add();
        CHANNEL.messageBuilder(C2SSpyglassScanCandidatePacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SSpyglassScanCandidatePacket::encode)
                .decoder(C2SSpyglassScanCandidatePacket::decode)
                .consumerMainThread(C2SSpyglassScanCandidatePacket::handle)
                .add();
        CHANNEL.messageBuilder(C2SRideActionPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SRideActionPacket::encode)
                .decoder(C2SRideActionPacket::decode)
                .consumerMainThread(C2SRideActionPacket::handle)
                .add();
        CHANNEL.messageBuilder(S2CRideControlProfilePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CRideControlProfilePacket::encode)
                .decoder(S2CRideControlProfilePacket::decode)
                .consumerMainThread(S2CRideControlProfilePacket::handle)
                .add();
        CHANNEL.messageBuilder(S2CRideActionStatePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CRideActionStatePacket::encode)
                .decoder(S2CRideActionStatePacket::decode)
                .consumerMainThread(S2CRideActionStatePacket::handle)
                .add();
        TdmcAttributeNetwork.register();
        TdmcAttributeLifecycle.initialize();
    }


    public static void sendActiveTalentInput(C2SActiveTalentInputPacket packet) {
        if (packet != null) CHANNEL.sendToServer(packet);
    }

    public static void sendRideAttack(C2SRideAttackPacket packet) {
        if (packet != null) CHANNEL.sendToServer(packet);
    }

    public static void sendRideAction(C2SRideActionPacket packet) {
        if (packet != null) CHANNEL.sendToServer(packet);
    }

    public static void sendRideControlProfile(ServerPlayer player, int mountEntityId, boolean active, RideCapabilities capabilities, List<RideActionInfo> actions) {
        if (player == null) return;
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new S2CRideControlProfilePacket(mountEntityId, active, capabilities, actions));
    }

    public static void sendRideActionState(ServerPlayer player, int mountEntityId, RideAction action, RideActionStatus status) {
        if (player == null || action == null || status == null) return;
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new S2CRideActionStatePacket(mountEntityId, action, status));
    }

    public static void sendActiveTalentState(ServerPlayer player, S2CActiveTalentStatePacket packet) {
        if (player != null && packet != null) CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendActiveTalentVisual(LivingEntity mount, S2CActiveTalentVisualPacket packet) {
        if (mount != null && packet != null) CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> mount), packet);
    }

    public static void sendActiveTalentVisual(ServerPlayer player, S2CActiveTalentVisualPacket packet) {
        if (player != null && packet != null) CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void requestPetManagementList() { CHANNEL.sendToServer(new C2SRequestPetManagementListPacket()); }
    public static void requestPetManagementDetail(UUID petUuid) { if (petUuid != null) CHANNEL.sendToServer(new C2SRequestPetManagementDetailPacket(petUuid)); }
    public static void setPetShortcut(UUID petUuid, int slot) { if (petUuid != null) CHANNEL.sendToServer(new C2SSetPetShortcutPacket(petUuid, slot)); }
    public static void storeManagedPet(UUID petUuid) { if (petUuid != null) CHANNEL.sendToServer(new C2SStoreManagedPetPacket(petUuid)); }
    public static void summonManagedPet(UUID petUuid) { if (petUuid != null) CHANNEL.sendToServer(new C2SSummonManagedPetPacket(petUuid)); }
    public static void locateManagedPet(UUID petUuid) { if (petUuid != null) CHANNEL.sendToServer(new C2SLocateManagedPetPacket(petUuid)); }
    public static void removeDeadPetRecord(UUID petUuid) { if (petUuid != null) CHANNEL.sendToServer(new C2SRemoveDeadPetRecordPacket(petUuid)); }
    public static void summonPetShortcut(int slot) { if (slot >= 1 && slot <= 9) CHANNEL.sendToServer(new C2SSummonPetShortcutPacket(slot)); }

    public static void sendPetManagementList(ServerPlayer player) {
        List<PetManagementSummary> summaries = PetManagementService.recordsFor(player).stream().map(PetManagementSummary::from).toList();
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new S2CPetManagementListPacket(
                summaries, PetManagementService.storedCount(player), PetManagementService.storageCapacity(player)));
    }

    public static void sendPetManagementDetail(ServerPlayer player, UUID petUuid) {
        PetRecord record = PetManagementService.record(player.server, petUuid);
        if (record == null || !player.getUUID().equals(record.ownerUuid())) return;
        PetManagementDetail detail = new PetManagementDetail(PetManagementSummary.from(record), PetManagementService.previewTag(player, petUuid));
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new S2CPetManagementDetailPacket(detail));
    }

    public static void openPanel(int entityId) {
        CHANNEL.sendToServer(new C2SOpenPanelPacket(entityId));
    }

    public static void openNativePetInventory(int entityId) {
        if (entityId >= 0) {
            CHANNEL.sendToServer(new C2SOpenNativePetInventoryPacket(entityId));
        }
    }

    public static void openCrafting() {
        CHANNEL.sendToServer(new C2SOpenCraftingPacket());
    }

    public static void refreshOpenPanel(int entityId) {
        CHANNEL.sendToServer(new C2SRefreshPanelPacket(entityId));
    }

    public static void allocateStat(int entityId, String statId) {
        ResourceLocation parsed = ResourceLocation.tryParse(statId);
        if (parsed != null && statId != null && statId.indexOf(':') >= 0) {
            TdmcAttributeNetwork.allocate(entityId, parsed);
            return;
        }
        CHANNEL.sendToServer(new C2SAllocateStatPacket(entityId, statId));
    }

    public static void inspect(InspectTarget target) {
        CHANNEL.sendToServer(new C2SInspectPacket(target));
    }

    public static void inspect(int entityId) {
        inspect(InspectTarget.entity(entityId));
    }

    public static void sendSpyglassScanCandidate(int entityId) {
        CHANNEL.sendToServer(new C2SSpyglassScanCandidatePacket(entityId));
    }

    public static void requestTargetHud(int entityId) {
        if (entityId >= 0) {
            CHANNEL.sendToServer(new C2STargetHudPacket(entityId));
        }
    }

    public static void selectPet(int entityId, PetSelectionMode mode) {
        CHANNEL.sendToServer(new C2STogglePetSelectionPacket(entityId, mode));
    }

    public static void issuePetCommand(PetCommand command, int targetEntityId, net.minecraft.world.phys.Vec3 position) {
        CHANNEL.sendToServer(new C2SPetCommandPacket(command, targetEntityId, position));
    }

    public static void clearPetCommand() {
        CHANNEL.sendToServer(new C2SClearPetCommandPacket());
    }

    public static void clearPetCommandMarker(UUID markerUuid) {
        if (markerUuid != null) {
            CHANNEL.sendToServer(new C2SClearPetCommandMarkerPacket(markerUuid));
        }
    }

    public static void requestPetCommandSummary() {
        CHANNEL.sendToServer(new C2SRequestPetCommandSummaryPacket());
    }

    public static void setRidePermission(int entityId, boolean allowOtherRiders) {
        CHANNEL.sendToServer(new C2SSetRidePermissionPacket(entityId, allowOtherRiders));
    }

    public static void requestRideMount(int entityId) {
        if (entityId >= 0) {
            CHANNEL.sendToServer(new C2SRideMountPacket(entityId));
        }
    }

    public static void sendRideInput(C2SRideInputPacket packet) {
        CHANNEL.sendToServer(packet);
    }

    public static void sendRideState(ServerPlayer player, LivingEntity mount, boolean active, com.szypxj.tldomesticatemorecreatures.riding.RideEnvironment environment, long generation) {
        if (player == null || mount == null) {
            return;
        }
        CHANNEL.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> mount),
                new S2CRideStatePacket(mount.getId(), player.getId(), active, environment, generation)
        );
    }

    public static void requestRidingConfig() {
        CHANNEL.sendToServer(new C2SRequestRidingConfigPacket());
    }

    public static void sendRidingSnapshot(ServerPlayer player, com.szypxj.tldomesticatemorecreatures.riding.config.RidingConfigSnapshot snapshot) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new S2CRidingConfigSnapshotPacket(snapshot));
    }

    public static void sendRidingEditorSnapshot(ServerPlayer player, com.szypxj.tldomesticatemorecreatures.riding.config.RidingConfigSnapshot snapshot) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new S2CRidingConfigSnapshotPacket(snapshot, true));
    }

    public static void broadcastRidingProfile(long generation, com.szypxj.tldomesticatemorecreatures.riding.config.EntityRideProfile profile) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), new S2CRidingProfileUpdatePacket(generation, profile));
    }

    public static void broadcastRidingSnapshot(com.szypxj.tldomesticatemorecreatures.riding.config.RidingConfigSnapshot snapshot) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), new S2CRidingConfigSnapshotPacket(snapshot));
    }

    public static void updateRidingSettings(com.szypxj.tldomesticatemorecreatures.riding.config.RidingSettings settings) {
        CHANNEL.sendToServer(C2SRideConfigUpdatePacket.settings(settings));
    }

    public static void updateRidingProfile(com.szypxj.tldomesticatemorecreatures.riding.config.EntityRideProfile profile) {
        CHANNEL.sendToServer(C2SRideConfigUpdatePacket.profile(profile));
    }

    public static void reloadRidingConfig() {
        CHANNEL.sendToServer(new C2SRideConfigReloadPacket());
    }

    public static void requestTamingEditor(boolean openEditor) {
        CHANNEL.sendToServer(new C2SRequestTamingEditorPacket(openEditor));
    }

    public static void requestTamingNativeFoods(ResourceLocation entityId) {
        if (entityId != null) {
            CHANNEL.sendToServer(new C2SRequestTamingNativeFoodsPacket(entityId));
        }
    }

    public static void saveTamingEditor(
            java.util.Map<ResourceLocation, com.szypxj.tldomesticatemorecreatures.domestication.editor.TamingEditorRule> upserts,
            java.util.Set<ResourceLocation> deletes
    ) {
        CHANNEL.sendToServer(new C2SSaveTamingEditorPacket(upserts, deletes));
    }

    public static void reloadTamingEditor() {
        CHANNEL.sendToServer(new C2SReloadTamingEditorPacket());
    }

    public static void sendTamingEditorSnapshot(
            ServerPlayer player,
            com.szypxj.tldomesticatemorecreatures.domestication.editor.TamingEditorSnapshot snapshot,
            boolean openEditor
    ) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new S2CTamingEditorSnapshotPacket(snapshot, openEditor));
    }

    public static void sendTamingNativeFoods(
            ServerPlayer player,
            ResourceLocation entityId,
            com.szypxj.tldomesticatemorecreatures.domestication.editor.TamingEditorService.NativeFoodsResult result
    ) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new S2CTamingNativeFoodsPacket(entityId, result.success(), result.items()));
    }

    public static void sendTamingEditorOperationResult(
            ServerPlayer player,
            boolean saveOperation,
            boolean success,
            String messageKey
    ) {
        if (player != null) {
            CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new S2CTamingEditorOperationResultPacket(saveOperation, success, messageKey)
            );
        }
    }

    public static void sendPetSelection(ServerPlayer player, List<Integer> entityIds) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new S2CPetSelectionPacket(entityIds));
    }

    public static void sendPetCommandSummary(ServerPlayer player, PetCommandSummary summary, boolean explicitSelection, boolean hasLandCapablePets) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new S2CPetCommandSummaryPacket(summary, explicitSelection, hasLandCapablePets)
        );
    }

    public static void sendPetCommandMarker(
            ServerPlayer player,
            UUID markerId,
            PetCommand command,
            int targetEntityId,
            UUID targetEntityUuid,
            net.minecraft.world.phys.Vec3 position
    ) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                S2CPetCommandMarkerPacket.show(
                        markerId, command, targetEntityId, targetEntityUuid, position
                )
        );
    }

    public static void removePetCommandMarker(ServerPlayer player, UUID markerId) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                S2CPetCommandMarkerPacket.remove(markerId)
        );
    }

    public static void sendPetOwnership(ServerPlayer player, LivingEntity entity) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new S2CPetOwnershipPacket(entity.getId(), PetOwnershipService.isOwnedBy(entity, player))
        );
    }

    public static void sendUnconsciousState(ServerPlayer player, boolean unconscious) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new S2CUnconsciousStatePacket(unconscious));
    }

    public static void sendPanel(ServerPlayer player, PanelSnapshot snapshot) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new S2CPanelPacket(snapshot));
    }

    public static void sendInspect(ServerPlayer player, InspectSnapshot snapshot) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new S2CInspectPacket(snapshot));
    }

    public static void sendTargetHud(ServerPlayer player, TargetHudSnapshot snapshot) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new S2CTargetHudPacket(snapshot));
    }

    public static void sendImprintState(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide) {
            return;
        }
        CHANNEL.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity),
                imprintStatePacket(entity)
        );
    }

    public static void sendImprintStateTo(ServerPlayer player, LivingEntity entity) {
        if (player == null || entity == null || entity.level().isClientSide) {
            return;
        }
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                imprintStatePacket(entity)
        );
    }

    private static S2CImprintStatePacket imprintStatePacket(LivingEntity entity) {
        ImprintSnapshot snapshot = ImprintService.snapshot(entity);
        int completed = ImprintData.exists(entity) ? ImprintData.of(entity).completed() : 0;
        return new S2CImprintStatePacket(
                entity.getId(),
                snapshot.active(),
                snapshot.finished(),
                snapshot.percent(),
                completed,
                ImprintData.CARE_COUNT,
                snapshot.remainingTicks(),
                snapshot.needType(),
                snapshot.foodNameKey(),
                snapshot.nextNeedTicks(),
                snapshot.bonded()
        );
    }

    public static void sendLevel(LivingEntity entity) {
        if (!LevelService.isAffected(entity) || !ProgressData.exists(entity)) {
            return;
        }
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), new S2CLevelPacket(entity.getId(), ProgressData.of(entity).level()));
    }

    public static void sendLevelTo(ServerPlayer player, LivingEntity entity) {
        if (!LevelService.isAffected(entity) || !ProgressData.exists(entity)) {
            return;
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new S2CLevelPacket(entity.getId(), ProgressData.of(entity).level()));
    }
}
