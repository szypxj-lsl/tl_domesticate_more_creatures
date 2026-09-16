package com.szypxj.tldomesticatemorecreatures.client.inventory;

import com.szypxj.tldomesticatemorecreatures.TlDomesticateMoreCreatures;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TlDomesticateMoreCreatures.MOD_ID, value = Dist.CLIENT)
public final class ExtraInventorySidecarRenderEvents {
    private ExtraInventorySidecarRenderEvents() {
    }

    @SubscribeEvent
    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        if (event.getScreen() instanceof ExtraInventorySidecarScreenAccess access) {
            access.tdmc$renderExtraInventorySidecar(event.getGuiGraphics(), event.getMouseX(), event.getMouseY());
        }
    }
}
