package dev.hytalemodding.hyrune.events;

import java.util.UUID;

/**
 * Contract for level up listener.
 */
public interface LevelUpListener {
    /**
     * Called after a skill levels up.
     */
    void onLevelUp(UUID uuid, int newLevel, String source);
}
