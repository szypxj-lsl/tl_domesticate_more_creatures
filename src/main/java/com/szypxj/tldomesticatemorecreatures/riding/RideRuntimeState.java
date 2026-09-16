package com.szypxj.tldomesticatemorecreatures.riding;

import com.szypxj.tldomesticatemorecreatures.riding.capability.RideCapability;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class RideRuntimeState {
    private final UUID riderUuid;
    private RideInputState input = RideInputState.ZERO;
    private int lastAcceptedSequence = Integer.MIN_VALUE;
    private RideEnvironment environment = RideEnvironment.GROUND;
    private boolean previousNoGravity;
    private boolean gravityCaptured;
    private boolean lastJumpDown;
    private int jumpCooldownTicks;
    private Vec3 lastMovementVector = Vec3.ZERO;
    private long configGeneration;
    private RideCapability activeCapability;
    private double currentSpeed;

    public RideRuntimeState(UUID riderUuid, long configGeneration) {
        this.riderUuid = riderUuid;
        this.configGeneration = configGeneration;
    }

    public UUID riderUuid() { return riderUuid; }
    public RideInputState input() { return input; }
    public void input(RideInputState value) { input = value == null ? RideInputState.ZERO : value; }
    public int lastAcceptedSequence() { return lastAcceptedSequence; }
    public void lastAcceptedSequence(int value) { lastAcceptedSequence = value; }
    public RideEnvironment environment() { return environment; }
    public void environment(RideEnvironment value) { environment = value == null ? RideEnvironment.GROUND : value; }
    public boolean previousNoGravity() { return previousNoGravity; }
    public void previousNoGravity(boolean value) { previousNoGravity = value; }
    public boolean gravityCaptured() { return gravityCaptured; }
    public void gravityCaptured(boolean value) { gravityCaptured = value; }
    public boolean lastJumpDown() { return lastJumpDown; }
    public void lastJumpDown(boolean value) { lastJumpDown = value; }
    public int jumpCooldownTicks() { return jumpCooldownTicks; }
    public void jumpCooldownTicks(int value) { jumpCooldownTicks = Math.max(0, value); }
    public Vec3 lastMovementVector() { return lastMovementVector; }
    public void lastMovementVector(Vec3 value) { lastMovementVector = value == null ? Vec3.ZERO : value; }
    public long configGeneration() { return configGeneration; }
    public void configGeneration(long value) { configGeneration = value; }
    public RideCapability activeCapability() { return activeCapability; }
    public void activeCapability(RideCapability value) { activeCapability = value; }
    public double currentSpeed() { return currentSpeed; }
    public void currentSpeed(double value) { currentSpeed = Math.max(0.0D, value); }
}
