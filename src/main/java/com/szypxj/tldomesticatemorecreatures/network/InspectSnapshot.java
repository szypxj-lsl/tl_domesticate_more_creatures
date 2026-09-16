package com.szypxj.tldomesticatemorecreatures.network;

import com.szypxj.tldomesticatemorecreatures.api.creature.BaseStats;
import com.szypxj.tldomesticatemorecreatures.api.creature.BaseStatsSource;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record InspectSnapshot(
        InspectTarget target,
        Component name,
        Component ownerName,
        ResourceLocation entityTypeId,
        boolean tdmcAffected,
        BaseStats baseStats,
        int level,
        boolean elite,
        boolean unborn,
        boolean geneticsAvailable,
        int paternalMutations,
        int maternalMutations,
        boolean mutated,
        boolean inheritedOwner,
        RideStatusSnapshot riding,
        ImprintSnapshot imprint,
        List<TalentSnapshot> talents,
        FurySnapshot fury,
        RadarSnapshot radar,
        List<InspectStat> stats,
        String tamingMethodKey,
        List<TamingFoodSnapshot> tamingFoods,
        boolean tamingActive,
        double tamingProgress,
        boolean torporAvailable,
        double torpor,
        double maxTorpor,
        int requiredTamingLevel
) {
    public InspectSnapshot {
        ownerName = ownerName == null ? Component.empty() : ownerName;
        baseStats = baseStats == null ? BaseStats.NONE : baseStats;
    }

    public boolean hasTamingRule() {
        return !unborn && tamingMethodKey != null && !tamingMethodKey.isBlank();
    }

    public void encode(FriendlyByteBuf buffer) {
        target.encode(buffer);
        buffer.writeComponent(name);
        buffer.writeComponent(ownerName == null ? Component.empty() : ownerName);
        buffer.writeBoolean(entityTypeId != null);
        if (entityTypeId != null) {
            buffer.writeResourceLocation(entityTypeId);
        }
        buffer.writeBoolean(tdmcAffected);
        buffer.writeDouble(baseStats.maxHealth());
        buffer.writeDouble(baseStats.attackDamage());
        buffer.writeDouble(baseStats.movementSpeed());
        buffer.writeEnum(baseStats.source());
        buffer.writeVarInt(level);
        buffer.writeBoolean(elite);
        buffer.writeBoolean(unborn);
        buffer.writeBoolean(geneticsAvailable);
        buffer.writeVarInt(paternalMutations);
        buffer.writeVarInt(maternalMutations);
        buffer.writeBoolean(mutated);
        buffer.writeBoolean(inheritedOwner);
        riding.encode(buffer);
        imprint.encode(buffer);
        buffer.writeVarInt(talents.size());
        for (TalentSnapshot talent : talents) {
            talent.encode(buffer);
        }
        fury.encode(buffer);
        radar.encode(buffer);
        buffer.writeVarInt(stats.size());
        for (InspectStat stat : stats) {
            buffer.writeUtf(stat.id());
            buffer.writeUtf(stat.nameKey());
            buffer.writeVarInt(stat.points());
            buffer.writeDouble(stat.displayedValue());
            buffer.writeUtf(stat.displayFormat());
            buffer.writeDouble(stat.currentValue());
            buffer.writeDouble(stat.maxValue());
        }
        buffer.writeUtf(tamingMethodKey == null ? "" : tamingMethodKey);
        buffer.writeVarInt(tamingFoods.size());
        for (TamingFoodSnapshot food : tamingFoods) {
            buffer.writeUtf(food.nameKey());
            buffer.writeVarInt(food.amount());
            buffer.writeBoolean(food.configured());
        }
        buffer.writeBoolean(tamingActive);
        buffer.writeDouble(tamingProgress);
        buffer.writeBoolean(torporAvailable);
        buffer.writeDouble(torpor);
        buffer.writeDouble(maxTorpor);
        buffer.writeVarInt(requiredTamingLevel);
    }

    public static InspectSnapshot decode(FriendlyByteBuf buffer) {
        InspectTarget target = InspectTarget.decode(buffer);
        Component name = buffer.readComponent();
        Component ownerName = buffer.readComponent();
        ResourceLocation entityTypeId = buffer.readBoolean() ? buffer.readResourceLocation() : null;
        boolean tdmcAffected = buffer.readBoolean();
        BaseStats baseStats = new BaseStats(
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readEnum(BaseStatsSource.class)
        );
        int level = buffer.readVarInt();
        boolean elite = buffer.readBoolean();
        boolean unborn = buffer.readBoolean();
        boolean geneticsAvailable = buffer.readBoolean();
        int paternalMutations = buffer.readVarInt();
        int maternalMutations = buffer.readVarInt();
        boolean mutated = buffer.readBoolean();
        boolean inheritedOwner = buffer.readBoolean();
        RideStatusSnapshot riding = RideStatusSnapshot.decode(buffer);
        ImprintSnapshot imprint = ImprintSnapshot.decode(buffer);
        int talentSize = buffer.readVarInt();
        List<TalentSnapshot> talents = new ArrayList<>(talentSize);
        for (int i = 0; i < talentSize; i++) {
            talents.add(TalentSnapshot.decode(buffer));
        }
        FurySnapshot fury = FurySnapshot.decode(buffer);
        RadarSnapshot radar = RadarSnapshot.decode(buffer);
        int statSize = buffer.readVarInt();
        List<InspectStat> stats = new ArrayList<>(statSize);
        for (int i = 0; i < statSize; i++) {
            stats.add(new InspectStat(
                    buffer.readUtf(),
                    buffer.readUtf(),
                    buffer.readVarInt(),
                    buffer.readDouble(),
                    buffer.readUtf(),
                    buffer.readDouble(),
                    buffer.readDouble()
            ));
        }
        String tamingMethodKey = buffer.readUtf();
        int foodSize = buffer.readVarInt();
        List<TamingFoodSnapshot> foods = new ArrayList<>(foodSize);
        for (int i = 0; i < foodSize; i++) {
            foods.add(new TamingFoodSnapshot(buffer.readUtf(), buffer.readVarInt(), buffer.readBoolean()));
        }
        boolean tamingActive = buffer.readBoolean();
        double tamingProgress = buffer.readDouble();
        boolean torporAvailable = buffer.readBoolean();
        double torpor = buffer.readDouble();
        double maxTorpor = buffer.readDouble();
        int requiredTamingLevel = buffer.readVarInt();
        return new InspectSnapshot(
                target,
                name,
                ownerName,
                entityTypeId,
                tdmcAffected,
                baseStats,
                level,
                elite,
                unborn,
                geneticsAvailable,
                paternalMutations,
                maternalMutations,
                mutated,
                inheritedOwner,
                riding,
                imprint,
                List.copyOf(talents),
                fury,
                radar,
                List.copyOf(stats),
                tamingMethodKey,
                List.copyOf(foods),
                tamingActive,
                tamingProgress,
                torporAvailable,
                torpor,
                maxTorpor,
                requiredTamingLevel
        );
    }


    public record RadarSnapshot(boolean available, int power, int life, int speed) {
        public static final RadarSnapshot NONE = new RadarSnapshot(false, 0, 0, 0);

        public RadarSnapshot {
            power = clamp(power);
            life = clamp(life);
            speed = clamp(speed);
        }

        public void encode(FriendlyByteBuf buffer) {
            buffer.writeBoolean(available);
            buffer.writeVarInt(power);
            buffer.writeVarInt(life);
            buffer.writeVarInt(speed);
        }

        public static RadarSnapshot decode(FriendlyByteBuf buffer) {
            return new RadarSnapshot(
                    buffer.readBoolean(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt()
            );
        }

        private static int clamp(int value) {
            return Math.max(0, Math.min(100, value));
        }
    }

    public record InspectStat(
            String id,
            String nameKey,
            int points,
            double displayedValue,
            String displayFormat,
            double currentValue,
            double maxValue
    ) {
    }

    public record TamingFoodSnapshot(String nameKey, int amount, boolean configured) {
    }
}
