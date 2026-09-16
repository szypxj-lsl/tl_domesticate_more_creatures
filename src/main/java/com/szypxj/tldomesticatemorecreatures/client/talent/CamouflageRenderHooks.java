package com.szypxj.tldomesticatemorecreatures.client.talent;

import net.minecraft.world.entity.LivingEntity;

public final class CamouflageRenderHooks {
    public static final float DEFAULT_RENDER_ALPHA = 0.30F;

    private CamouflageRenderHooks() {
    }

    public static float renderAlpha(LivingEntity entity) {
        if (entity == null) return 1.0F;
        float alpha = ClientActiveTalentState.renderAlpha(entity.getId());
        if (!Float.isFinite(alpha)) return DEFAULT_RENDER_ALPHA;
        return Math.max(0.05F, Math.min(1.0F, alpha));
    }
}
