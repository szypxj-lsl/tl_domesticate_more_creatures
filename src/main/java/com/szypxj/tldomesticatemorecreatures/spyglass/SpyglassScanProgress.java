package com.szypxj.tldomesticatemorecreatures.spyglass;

final class SpyglassScanProgress {
    private final int requiredTicks;
    private int candidateEntityId = -1;
    private int ticks;
    private boolean completed;

    SpyglassScanProgress(int requiredTicks) {
        this.requiredTicks = Math.max(1, requiredTicks);
    }

    boolean advance(int candidateEntityId, boolean validThisTick) {
        if (!validThisTick || candidateEntityId < 0) {
            reset();
            return false;
        }
        if (this.candidateEntityId != candidateEntityId) {
            this.candidateEntityId = candidateEntityId;
            this.ticks = 0;
            this.completed = false;
        }
        if (completed) {
            return false;
        }
        ticks++;
        if (ticks >= requiredTicks) {
            completed = true;
            return true;
        }
        return false;
    }

    void reset() {
        candidateEntityId = -1;
        ticks = 0;
        completed = false;
    }

    int candidateEntityId() {
        return candidateEntityId;
    }

    int ticks() {
        return ticks;
    }
}
