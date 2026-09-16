package com.szypxj.tldomesticatemorecreatures.api.attribute;

import com.szypxj.tldomesticatemorecreatures.network.NetworkHandler;
import com.szypxj.tldomesticatemorecreatures.network.packet.C2SModifyAttributePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;

public final class TdmcAttributeNetwork {
    private static final int MESSAGE_ID = 1000;
    private static boolean registered;

    private TdmcAttributeNetwork() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        NetworkHandler.CHANNEL.messageBuilder(C2SModifyAttributePacket.class, MESSAGE_ID, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SModifyAttributePacket::encode)
                .decoder(C2SModifyAttributePacket::decode)
                .consumerMainThread(C2SModifyAttributePacket::handle)
                .add();
    }

    public static void allocate(int entityId, ResourceLocation attributeId) {
        if (attributeId != null) {
            NetworkHandler.CHANNEL.sendToServer(new C2SModifyAttributePacket(entityId, attributeId, 1));
        }
    }
}
