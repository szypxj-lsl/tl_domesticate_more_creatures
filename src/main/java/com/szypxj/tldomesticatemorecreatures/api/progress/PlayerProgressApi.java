package com.szypxj.tldomesticatemorecreatures.api.progress;

import com.szypxj.tldomesticatemorecreatures.data.ProgressData;
import net.minecraft.server.level.ServerPlayer;

/**
 * Public server-side access to TDMC player progression values.
 *
 * <p>Addons should use this API instead of reading or mutating TDMC's progression storage directly.</p>
 */
public final class PlayerProgressApi {
    private PlayerProgressApi() {
    }

    public static int getUnspentAttributePoints(ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        return ProgressData.of(player).unspentPoints();
    }

    public static boolean addUnspentAttributePoints(ServerPlayer player, int amount) {
        if (player == null || amount <= 0) {
            return false;
        }

        ProgressData data = ProgressData.of(player);
        int current = data.unspentPoints();
        if (current > Integer.MAX_VALUE - amount) {
            return false;
        }

        data.unspentPoints(current + amount);
        return true;
    }
}
