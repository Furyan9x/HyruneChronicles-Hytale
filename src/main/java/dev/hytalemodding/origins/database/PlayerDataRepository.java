package dev.hytalemodding.origins.database;

import dev.hytalemodding.origins.playerdata.PlayerData;

import java.util.UUID;

/**
 * Generic contract for player data persistence.
 *
 * @param <T> data type being persisted
 */
public interface PlayerDataRepository<T extends PlayerData> {
    /**
     * Loads player data. Returns null if the player has no stored data.
     *
     * @param uuid player UUID
     * @return loaded data or null
     */
    T load(UUID uuid);

    /**
     * Saves player data.
     *
     * @param data data to persist
     */
    void save(T data);
}
