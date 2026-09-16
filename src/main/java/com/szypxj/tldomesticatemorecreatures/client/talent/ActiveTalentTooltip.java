package com.szypxj.tldomesticatemorecreatures.client.talent;

import com.szypxj.tldomesticatemorecreatures.client.ClientKeys;
import com.szypxj.tldomesticatemorecreatures.network.TalentSnapshot;
import com.szypxj.tldomesticatemorecreatures.talent.active.ActiveTalentIds;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class ActiveTalentTooltip {
    private ActiveTalentTooltip() {
    }

    public static void appendIfActive(List<Component> tooltip, TalentSnapshot talent) {
        if (tooltip == null || talent == null || !talent.special()) return;
        if (!ActiveTalentIds.SHADOWSTEP.equals(talent.id()) && !ActiveTalentIds.CAMOUFLAGE.equals(talent.id())) return;
        tooltip.add(Component.translatable("gui.tl_domesticate_more_creatures.active_talent.key_hint", ClientKeys.ACTIVE_TALENT.getTranslatedKeyMessage()));
        tooltip.add(Component.translatable("talent.tl_domesticate_more_creatures.special." + talent.id() + ".desc"));
        tooltip.add(Component.translatable("talent.tl_domesticate_more_creatures.special." + talent.id() + ".base_cooldown"));
        ClientActiveTalentState.Snapshot state = ClientActiveTalentState.mountedSnapshot();
        Minecraft minecraft = Minecraft.getInstance();
        if (state == null || minecraft.level == null || !talent.id().equals(state.skillId())) return;
        long remaining = Math.max(0L, state.cooldownEndGameTime() - minecraft.level.getGameTime());
        if (remaining > 0L) {
            tooltip.add(Component.translatable(
                    "gui.tl_domesticate_more_creatures.active_talent.cooldown",
                    String.format(java.util.Locale.ROOT, "%.1f", remaining / 20.0D)
            ));
        }
    }
}
