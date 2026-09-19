package com.szypxj.tldomesticatemorecreatures.client;

public record ClientImprintState(
        boolean active,
        boolean finished,
        int percent,
        int completed,
        int total,
        long endTick,
        String needType,
        String foodNameKey,
        long nextNeedAt,
        boolean bonded
) {
    public ClientImprintState {
        percent = Math.max(0, Math.min(100, percent));
        total = Math.max(0, total);
        completed = Math.max(0, Math.min(total, completed));
        endTick = Math.max(0L, endTick);
        needType = needType == null ? "" : needType;
        foodNameKey = foodNameKey == null ? "" : foodNameKey;
        nextNeedAt = Math.max(0L, nextNeedAt);
    }

    public long remainingTicks(long now) {
        return active ? Math.max(0L, endTick - Math.max(0L, now)) : 0L;
    }

    public long nextNeedTicks(long now) {
        return active ? Math.max(0L, nextNeedAt - Math.max(0L, now)) : 0L;
    }
}
