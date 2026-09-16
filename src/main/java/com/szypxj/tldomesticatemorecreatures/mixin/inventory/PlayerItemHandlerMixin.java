package com.szypxj.tldomesticatemorecreatures.mixin.inventory;

import com.mojang.authlib.GameProfile;
import com.szypxj.tldomesticatemorecreatures.inventory.CombinedPlayerItemHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerItemHandlerMixin {
    @Shadow(remap = false)
    @Final
    @Mutable
    private LazyOptional<IItemHandler> playerMainHandler;

    @Shadow(remap = false)
    @Final
    @Mutable
    private LazyOptional<IItemHandler> playerJoinedHandler;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void tdmc$appendExtraInventoryToForgeHandlers(Level level, BlockPos pos, float yaw, GameProfile profile, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        LazyOptional<IItemHandler> vanillaMain = this.playerMainHandler;
        LazyOptional<IItemHandler> vanillaJoined = this.playerJoinedHandler;
        this.playerMainHandler = LazyOptional.of(() -> new CombinedPlayerItemHandler(
                vanillaMain.orElseThrow(() -> new IllegalStateException("Missing Forge player main inventory handler")),
                self
        ));
        this.playerJoinedHandler = LazyOptional.of(() -> new CombinedPlayerItemHandler(
                vanillaJoined.orElseThrow(() -> new IllegalStateException("Missing Forge player joined inventory handler")),
                self
        ));
    }
}
