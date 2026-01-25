package dev.hytalemodding.origins.database;

import java.util.UUID;

import dev.hytalemodding.origins.playerdata.PlayerLvlData;

public interface LevelRepository {
    /**
     * Loads player data. Returns null if the player has never played before.
     */
    PlayerLvlData load(UUID uuid);

    /**
     * Saves player data to disk/database.
     */
    void save(PlayerLvlData data);
}
