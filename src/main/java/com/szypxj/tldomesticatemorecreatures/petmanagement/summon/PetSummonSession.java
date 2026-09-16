package com.szypxj.tldomesticatemorecreatures.petmanagement.summon;

import net.minecraft.world.entity.ai.goal.Goal;

import java.util.UUID;

public final class PetSummonSession {
    public enum State {
        APPROACHING,
        CATCHING,
        COMPLETE,
        CANCELLED
    }

    private final UUID playerUuid;
    private final UUID petUuid;
    private State state;
    private boolean flying;
    private boolean airborneRescue;
    private long lastEmergencyRepositionGameTime;
    private int approachTicks;
    private boolean approachPrepared;
    private boolean originalNoPhysics;
    private boolean originalNoGravity;
    private boolean originalNoAi;
    private Goal movementLease;

    public PetSummonSession(UUID playerUuid, UUID petUuid) {
        this.playerUuid = playerUuid;
        this.petUuid = petUuid;
        this.lastEmergencyRepositionGameTime = Long.MIN_VALUE / 4L;
        this.state = State.APPROACHING;
    }

    public UUID playerUuid() { return playerUuid; }
    public UUID petUuid() { return petUuid; }
    public State state() { return state; }
    public void state(State state) { this.state = state; }
    public boolean flying() { return flying; }
    public void flying(boolean flying) { this.flying = flying; }
    public boolean airborneRescue() { return airborneRescue; }
    public void airborneRescue(boolean airborneRescue) { this.airborneRescue = airborneRescue; }
    public long lastEmergencyRepositionGameTime() { return lastEmergencyRepositionGameTime; }
    public void lastEmergencyRepositionGameTime(long gameTime) { this.lastEmergencyRepositionGameTime = gameTime; }
    public int approachTicks() { return approachTicks; }
    public void approachTicks(int approachTicks) { this.approachTicks = Math.max(0, approachTicks); }
    public void incrementApproachTicks() { this.approachTicks++; }
    public boolean approachPrepared() { return approachPrepared; }
    public void approachPrepared(boolean approachPrepared) { this.approachPrepared = approachPrepared; }
    public boolean originalNoPhysics() { return originalNoPhysics; }
    public void originalNoPhysics(boolean originalNoPhysics) { this.originalNoPhysics = originalNoPhysics; }
    public boolean originalNoGravity() { return originalNoGravity; }
    public void originalNoGravity(boolean originalNoGravity) { this.originalNoGravity = originalNoGravity; }
    public boolean originalNoAi() { return originalNoAi; }
    public void originalNoAi(boolean originalNoAi) { this.originalNoAi = originalNoAi; }
    public Goal movementLease() { return movementLease; }
    public void movementLease(Goal movementLease) { this.movementLease = movementLease; }
}
