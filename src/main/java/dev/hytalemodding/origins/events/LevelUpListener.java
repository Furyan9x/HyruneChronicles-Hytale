package dev.hytalemodding.origins.events;

import java.util.UUID;

public interface LevelUpListener {
    /**
     * Called after a skill levels up.
     */
    void onLevelUp(UUID uuid, int newLevel, String source);
}
