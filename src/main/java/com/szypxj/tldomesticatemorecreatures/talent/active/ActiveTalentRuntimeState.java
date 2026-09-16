package com.szypxj.tldomesticatemorecreatures.talent.active;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class ActiveTalentRuntimeState {
    private final UUID riderUuid;
    private final UUID mountUuid;
    private final int mountEntityId;
    private final String skillId;
    private ActiveTalentPhase phase;
    private long phaseEndGameTime;
    private final ActiveTalentConfig.ShadowstepConfig shadowstepConfig;
    private final ActiveTalentConfig.CamouflageConfig camouflageConfig;
    private final List<ShadowstepMark> marks = new ArrayList<>();
    private final Set<UUID> slowedMobs = new LinkedHashSet<>();
    private int markCursor;
    private int successfulAttacks;
    private int blockedDashTicks;

    public ActiveTalentRuntimeState(UUID riderUuid, UUID mountUuid, int mountEntityId, String skillId,
                                    ActiveTalentPhase phase, long phaseEndGameTime,
                                    ActiveTalentConfig.ShadowstepConfig shadowstepConfig,
                                    ActiveTalentConfig.CamouflageConfig camouflageConfig) {
        this.riderUuid = riderUuid;
        this.mountUuid = mountUuid;
        this.mountEntityId = mountEntityId;
        this.skillId = skillId;
        this.phase = phase;
        this.phaseEndGameTime = phaseEndGameTime;
        this.shadowstepConfig = shadowstepConfig;
        this.camouflageConfig = camouflageConfig;
    }

    public UUID riderUuid() { return riderUuid; }
    public UUID mountUuid() { return mountUuid; }
    public int mountEntityId() { return mountEntityId; }
    public String skillId() { return skillId; }
    public ActiveTalentPhase phase() { return phase; }
    public void phase(ActiveTalentPhase value) { phase = value; }
    public long phaseEndGameTime() { return phaseEndGameTime; }
    public void phaseEndGameTime(long value) { phaseEndGameTime = value; }
    public ActiveTalentConfig.ShadowstepConfig shadowstepConfig() { return shadowstepConfig; }
    public ActiveTalentConfig.CamouflageConfig camouflageConfig() { return camouflageConfig; }
    public List<ShadowstepMark> marks() { return marks; }
    public Set<UUID> slowedMobs() { return slowedMobs; }
    public int markCursor() { return markCursor; }
    public void markCursor(int value) { markCursor = Math.max(0, value); }
    public int successfulAttacks() { return successfulAttacks; }
    public void successfulAttacks(int value) { successfulAttacks = Math.max(0, value); }
    public int blockedDashTicks() { return blockedDashTicks; }
    public void blockedDashTicks(int value) { blockedDashTicks = Math.max(0, value); }
}
