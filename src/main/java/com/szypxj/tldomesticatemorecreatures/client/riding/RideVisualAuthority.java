package com.szypxj.tldomesticatemorecreatures.client.riding;

import com.szypxj.tldomesticatemorecreatures.riding.config.RiderVisualProfile;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side visual authority used by the riding editor preview.
 *
 * <p>Like Salvation's client ride authority, preview state is keyed by the
 * mount UUID rather than by the rider. That keeps passenger positioning,
 * riding-pose selection and model adjustments on the same authority source.</p>
 */
public final class RideVisualAuthority {
    private static final Map<UUID, RiderVisualProfile> CLIENT_PROFILES = new ConcurrentHashMap<>();

    private RideVisualAuthority() {
    }

    public static void authorizeClient(UUID mountId, RiderVisualProfile profile) {
        if (mountId == null || profile == null) {
            return;
        }
        CLIENT_PROFILES.put(mountId, profile.validated());
    }

    public static void revokeClient(UUID mountId) {
        if (mountId != null) {
            CLIENT_PROFILES.remove(mountId);
        }
    }

    public static RiderVisualProfile clientProfile(UUID mountId) {
        return mountId == null ? null : CLIENT_PROFILES.get(mountId);
    }

    public static boolean isClientAuthorized(UUID mountId) {
        return clientProfile(mountId) != null;
    }

    public static void clearClient() {
        CLIENT_PROFILES.clear();
    }
}
