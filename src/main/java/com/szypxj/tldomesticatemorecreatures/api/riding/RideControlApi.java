package com.szypxj.tldomesticatemorecreatures.api.riding;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Public registry for optional ride-control integrations.
 */
public final class RideControlApi {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final CopyOnWriteArrayList<Entry> ENTRIES = new CopyOnWriteArrayList<>();
    private static final Set<ResourceLocation> DISABLED = ConcurrentHashMap.newKeySet();
    private static final AtomicLong GENERATION = new AtomicLong();

    private RideControlApi() {
    }

    public static void register(ResourceLocation id, RideControlProvider provider) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(provider, "provider");
        for (Entry entry : ENTRIES) {
            if (entry.id().equals(id)) {
                throw new IllegalArgumentException("Ride control provider already registered: " + id);
            }
        }
        int priority = provider.priority();
        ENTRIES.add(new Entry(id, provider, priority));
        ENTRIES.sort((left, right) -> {
            int byPriority = Integer.compare(right.priority(), left.priority());
            return byPriority != 0 ? byPriority : left.id().toString().compareTo(right.id().toString());
        });
        DISABLED.remove(id);
        GENERATION.incrementAndGet();
    }

    public static List<RideControlProvider> matchingProviders(LivingEntity mount) {
        if (mount == null) {
            return List.of();
        }
        List<RideControlProvider> result = new ArrayList<>();
        for (Entry entry : ENTRIES) {
            if (DISABLED.contains(entry.id())) {
                continue;
            }
            try {
                if (entry.provider().supports(mount)) {
                    result.add(entry.provider());
                }
            } catch (RuntimeException exception) {
                if (DISABLED.add(entry.id())) {
                    LOGGER.warn("Ride control provider {} failed during supports() and was disabled.", entry.id(), exception);
                    GENERATION.incrementAndGet();
                }
            }
        }
        return List.copyOf(result);
    }

    public static boolean disableProvider(RideControlProvider provider) {
        if (provider == null) {
            return false;
        }
        for (Entry entry : ENTRIES) {
            if (entry.provider() == provider && DISABLED.add(entry.id())) {
                LOGGER.warn("Ride control provider {} was disabled after a runtime failure.", entry.id());
                GENERATION.incrementAndGet();
                return true;
            }
        }
        return false;
    }

    public static long generation() {
        return GENERATION.get();
    }

    public static void clearForTests() {
        ENTRIES.clear();
        DISABLED.clear();
        GENERATION.incrementAndGet();
    }

    private record Entry(ResourceLocation id, RideControlProvider provider, int priority) {
    }
}
