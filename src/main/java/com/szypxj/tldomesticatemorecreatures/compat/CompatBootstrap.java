package com.szypxj.tldomesticatemorecreatures.compat;

import com.szypxj.tldomesticatemorecreatures.command.pet.PetCommand;
import com.szypxj.tldomesticatemorecreatures.compat.iceandfire.IceAndFireCommandCompat;
import com.szypxj.tldomesticatemorecreatures.compat.iceandfire.IceAndFireRideControlProvider;
import com.szypxj.tldomesticatemorecreatures.compat.iceandfire.IceAndFireThreatProvider;
import com.szypxj.tldomesticatemorecreatures.compat.ers.ErsRideControlProvider;
import com.szypxj.tldomesticatemorecreatures.compat.ror.RorRideControlProvider;
import com.szypxj.tldomesticatemorecreatures.compat.saintsdragons.SaintsDragonsRideControlProvider;
import com.szypxj.tldomesticatemorecreatures.compat.saintsdragons.SaintsDragonsThreatProvider;
import com.szypxj.tldomesticatemorecreatures.compat.saintsdragons.SaintsDragonCommandCompat;
import com.szypxj.tldomesticatemorecreatures.compat.wanancientbeasts.WanAncientBeastsCompat;
import com.szypxj.tldomesticatemorecreatures.compat.unusualprehistory.UnusualPrehistoryCompat;
import com.szypxj.tldomesticatemorecreatures.compat.fossil.FossilCompat;
import com.szypxj.tldomesticatemorecreatures.compat.similarprehistory.SimilarPrehistoryCompat;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.UUID;

public final class CompatBootstrap {
    private static boolean initialized;

    private CompatBootstrap() {
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        SaintsDragonCommandCompat.register();
        IceAndFireCommandCompat.register();
        IceAndFireThreatProvider.register();
        SaintsDragonsThreatProvider.register();
        IceAndFireRideControlProvider.register();
        SaintsDragonsRideControlProvider.register();
        ErsRideControlProvider.register();
        RorRideControlProvider.register();
        WanAncientBeastsCompat.register();
        UnusualPrehistoryCompat.register();
        FossilCompat.register();
        SimilarPrehistoryCompat.register();
        if (!ModList.get().isLoaded("tl_marking")) {
            return;
        }
        if (FMLEnvironment.dist == Dist.CLIENT) {
            TlMarkingPresent.registerClient();
        }
    }

    public static void refreshClientSelectionVisual() {
        if (ModList.get().isLoaded("tl_marking") && FMLEnvironment.dist == Dist.CLIENT) {
            TlMarkingPresent.refreshClient();
        }
    }

    public static void showClientCommandMarker(
            UUID markerId,
            PetCommand command,
            int targetEntityId,
            UUID targetEntityUuid,
            Vec3 position
    ) {
        if (ModList.get().isLoaded("tl_marking") && FMLEnvironment.dist == Dist.CLIENT) {
            TlMarkingPresent.showCommandMarker(
                    markerId, command, targetEntityId, targetEntityUuid, position
            );
        }
    }

    public static void removeClientCommandMarker(UUID markerId) {
        if (ModList.get().isLoaded("tl_marking") && FMLEnvironment.dist == Dist.CLIENT) {
            TlMarkingPresent.removeCommandMarker(markerId);
        }
    }

    private static final class TlMarkingPresent {
        private static void registerClient() {
            com.szypxj.tldomesticatemorecreatures.compat.tlmarking.TlMarkingClientCompat.register();
        }

        private static void refreshClient() {
            com.szypxj.tldomesticatemorecreatures.compat.tlmarking.TlMarkingClientCompat.refreshSelectionVisual();
        }

        private static void showCommandMarker(
                UUID markerId,
                PetCommand command,
                int targetEntityId,
                UUID targetEntityUuid,
                Vec3 position
        ) {
            com.szypxj.tldomesticatemorecreatures.compat.tlmarking.TlMarkingClientCompat.showCommandMarker(
                    markerId, command, targetEntityId, targetEntityUuid, position
            );
        }

        private static void removeCommandMarker(UUID markerId) {
            com.szypxj.tldomesticatemorecreatures.compat.tlmarking.TlMarkingClientCompat.removeCommandMarker(markerId);
        }
    }
}
