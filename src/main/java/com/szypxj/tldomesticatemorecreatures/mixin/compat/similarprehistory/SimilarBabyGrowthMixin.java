package com.szypxj.tldomesticatemorecreatures.mixin.compat.similarprehistory;

import com.szypxj.tldomesticatemorecreatures.game.genetics.EntityStateTransferScope;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Pseudo
@Mixin(targets = {
        "net.mcreator.similarprehistory.procedures.CharobabyAlEstarVivaProcedure",
        "net.mcreator.similarprehistory.procedures.CoahuilababyAlEstarVivaProcedure",
        "net.mcreator.similarprehistory.procedures.DakotaBabyAlEstarVivaProcedure",
        "net.mcreator.similarprehistory.procedures.LatenivenatrixBabyAlEstarVivaProcedure",
        "net.mcreator.similarprehistory.procedures.OxababyAlEstarVivaProcedure",
        "net.mcreator.similarprehistory.procedures.SinoBabyAlEstarVivaProcedure"
}, remap = false)
public abstract class SimilarBabyGrowthMixin {
    @Unique private static final Map<String, String> TDMC_ADULT = Map.of(
            "dakota_baby", "dakotaraptor", "sino_baby", "sinosaurus", "oxa_baby", "oxalaia",
            "latenivenatrix_baby", "latenivenatrix", "charo_baby", "charonosaurus", "coahuilababy", "coahuilasaurus"
    );
    @Unique private static final ThreadLocal<Integer> TDMC_SCOPE_DEPTH = ThreadLocal.withInitial(() -> 0);

    @Inject(method = "execute", at = @At("HEAD"), remap = false, require = 0)
    private static void tdmc$beginGrowth(LevelAccessor level, double x, double y, double z, Entity entity, CallbackInfo ci) {
        if (!(entity instanceof LivingEntity source) || source.level().isClientSide) return;
        ResourceLocation sourceId = ForgeRegistries.ENTITY_TYPES.getKey(source.getType());
        if (sourceId == null || !"similar_prehistory".equals(sourceId.getNamespace())) return;
        String adultPath = TDMC_ADULT.get(sourceId.getPath());
        double age = source.getPersistentData().getDouble("age");
        if (adultPath == null || age < 23999.0D || age >= 24000.0D) return;
        EntityStateTransferScope.begin(source, target -> {
            ResourceLocation targetId = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
            return targetId != null && targetId.toString().equals("similar_prehistory:" + adultPath);
        });
        TDMC_SCOPE_DEPTH.set(TDMC_SCOPE_DEPTH.get() + 1);
    }

    @Inject(method = "execute", at = @At("RETURN"), remap = false, require = 0)
    private static void tdmc$endGrowth(LevelAccessor level, double x, double y, double z, Entity entity, CallbackInfo ci) {
        int depth = TDMC_SCOPE_DEPTH.get();
        if (depth <= 0) return;
        EntityStateTransferScope.end();
        if (depth == 1) TDMC_SCOPE_DEPTH.remove(); else TDMC_SCOPE_DEPTH.set(depth - 1);
    }
}
