package com.szypxj.tldomesticatemorecreatures.torpor;

import com.szypxj.tldomesticatemorecreatures.TlDomesticateMoreCreatures;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = TlDomesticateMoreCreatures.MOD_ID)
public final class UnconsciousManager {
    private static final Set<LivingEntity> ENTITIES = Collections.newSetFromMap(new WeakHashMap<>());

    private UnconsciousManager() {
    }

    public static void register(LivingEntity entity) {
        if (!entity.level().isClientSide) {
            ENTITIES.add(entity);
        }
    }

    public static void unregister(LivingEntity entity) {
        ENTITIES.remove(entity);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % 20 != 0 || ENTITIES.isEmpty()) {
            return;
        }

        for (LivingEntity entity : new ArrayList<>(ENTITIES)) {
            if (entity == null || entity.isRemoved() || entity.level().isClientSide || !TorporService.isEnabled(entity)) {
                ENTITIES.remove(entity);
                continue;
            }
            TorporData data = TorporData.of(entity);
            if (data.current() <= 0.0D && !data.unconscious()) {
                ENTITIES.remove(entity);
                continue;
            }
            TorporService.refresh(entity);
            data = TorporData.of(entity);
            if (data.current() <= 0.0D && !data.unconscious()) {
                ENTITIES.remove(entity);
            }
        }
    }
}
