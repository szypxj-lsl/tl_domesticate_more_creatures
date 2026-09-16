package com.szypxj.tldomesticatemorecreatures.client.talent;

import com.szypxj.tldomesticatemorecreatures.TlDomesticateMoreCreatures;
import com.szypxj.tldomesticatemorecreatures.client.ClientKeys;
import com.szypxj.tldomesticatemorecreatures.network.NetworkHandler;
import com.szypxj.tldomesticatemorecreatures.network.packet.C2SActiveTalentInputPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TlDomesticateMoreCreatures.MOD_ID, value = Dist.CLIENT)
public final class ActiveTalentKeyHandler {
    private static int sequence;

    private ActiveTalentKeyHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        while (ClientKeys.ACTIVE_TALENT.consumeClick()) {
            if (minecraft.player == null || minecraft.screen != null || !(minecraft.player.getVehicle() instanceof LivingEntity mount)) continue;
            NetworkHandler.sendActiveTalentInput(new C2SActiveTalentInputPacket(mount.getId(), ++sequence));
        }
    }
}
