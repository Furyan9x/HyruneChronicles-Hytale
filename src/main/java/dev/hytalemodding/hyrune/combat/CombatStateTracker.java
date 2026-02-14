package dev.hytalemodding.hyrune.combat;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks combat state.
 */
public final class CombatStateTracker {
    public static final long COMBAT_GRACE_MS = 3000L;
    private static final Map<UUID, Long> LAST_COMBAT = new ConcurrentHashMap<>();

    private CombatStateTracker() {
    }

    public static void markCombat(@Nullable UUID uuid) {
        if (uuid == null) {
            return;
        }
        LAST_COMBAT.put(uuid, System.currentTimeMillis());
    }

    public static boolean isInCombat(@Nullable UUID uuid) {
        if (uuid == null) {
            return false;
        }
        Long last = LAST_COMBAT.get(uuid);
        if (last == null) {
            return false;
        }
        return System.currentTimeMillis() - last < COMBAT_GRACE_MS;
    }
}
