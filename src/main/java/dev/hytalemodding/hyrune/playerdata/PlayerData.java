package dev.hytalemodding.hyrune.playerdata;

import java.util.UUID;

/**
 * Common contract for persisted player data containers.
 */
public interface PlayerData {
    /**
     * Returns the owning player UUID.
     *
     * @return player UUID
     */
    UUID getUuid();
}
