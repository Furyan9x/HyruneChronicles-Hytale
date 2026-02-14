package dev.hytalemodding.hyrune.database;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.logger.HytaleLogger;
import dev.hytalemodding.hyrune.playerdata.PlayerLvlData;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;

/**
 * JSON-backed repository for player level data.
 */
public class JsonLevelRepository implements LevelRepository {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final Gson gson;
    private final File dataFolder;

    public JsonLevelRepository(String rootPath) {
        // Pretty printing makes the JSON easier for us to read/edit manually
        this.gson = new GsonBuilder().setPrettyPrinting().create();

        // Ensure the directory exists: <rootPath>/players
        this.dataFolder = new File(rootPath, "players");
        if (!this.dataFolder.exists()) {
            if (!this.dataFolder.mkdirs()) {
                LOGGER.at(Level.WARNING).log("Failed to create player data folder at " + this.dataFolder.getAbsolutePath());
            }
        }
    }

    @Override
    public PlayerLvlData load(UUID uuid) {
        File playerFile = new File(this.dataFolder, uuid.toString() + ".json");

        if (!playerFile.exists()) {
            return null; // Triggers the Service to create a new default profile
        }

        try (FileReader reader = new FileReader(playerFile)) {
            return gson.fromJson(reader, PlayerLvlData.class);
        } catch (IOException e) {
            LOGGER.at(Level.WARNING).log("Failed to load level data for " + uuid + ": " + e.getMessage());
            return null;
        }
    }

    @Override
    public void save(PlayerLvlData data) {
        if (data == null || data.getUuid() == null) {
            return;
        }
        File playerFile = new File(this.dataFolder, data.getUuid().toString() + ".json");

        try (FileWriter writer = new FileWriter(playerFile)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            LOGGER.at(Level.WARNING).log("Failed to save level data for " + data.getUuid() + ": " + e.getMessage());
        }
    }
}
