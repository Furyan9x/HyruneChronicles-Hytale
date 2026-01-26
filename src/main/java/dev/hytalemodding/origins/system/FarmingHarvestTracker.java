package dev.hytalemodding.origins.system;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class FarmingHarvestTracker {
    private static final Map<UUID, Long> LAST_BREAK_MS = new ConcurrentHashMap<>();

    private FarmingHarvestTracker() {
    }

    static void recordBreak(UUID uuid) {
        if (uuid == null) {
            return;
        }
        LAST_BREAK_MS.put(uuid, System.currentTimeMillis());
    }

    static boolean wasRecentBreak(UUID uuid, long windowMs) {
        if (uuid == null) {
            return false;
        }
        Long last = LAST_BREAK_MS.get(uuid);
        if (last == null) {
            return false;
        }
        return System.currentTimeMillis() - last <= windowMs;
    }
}
