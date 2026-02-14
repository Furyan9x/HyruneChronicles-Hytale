package dev.hytalemodding.hyrune.database;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.logger.HytaleLogger;
import dev.hytalemodding.hyrune.playerdata.SocialPlayerData;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;

/**
 * JSON-backed repository for social player data.
 */
public class JsonSocialRepository implements SocialRepository {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final Gson gson;
    private final File dataFolder;

    public JsonSocialRepository(String rootPath) {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.dataFolder = new File(rootPath, "social");
        if (!this.dataFolder.exists() && !this.dataFolder.mkdirs()) {
            LOGGER.at(Level.WARNING).log("Failed to create social data folder at " + this.dataFolder.getAbsolutePath());
        }
    }

    @Override
    public SocialPlayerData load(UUID uuid) {
        File playerFile = new File(this.dataFolder, uuid.toString() + ".json");
        if (!playerFile.exists()) {
            return null;
        }
        try (FileReader reader = new FileReader(playerFile)) {
            return gson.fromJson(reader, SocialPlayerData.class);
        } catch (IOException e) {
            LOGGER.at(Level.WARNING).log("Failed to load social data for " + uuid + ": " + e.getMessage());
            return null;
        }
    }

    @Override
    public void save(SocialPlayerData data) {
        if (data == null || data.getUuid() == null) {
            return;
        }
        File playerFile = new File(this.dataFolder, data.getUuid().toString() + ".json");
        try (FileWriter writer = new FileWriter(playerFile)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            LOGGER.at(Level.WARNING).log("Failed to save social data for " + data.getUuid() + ": " + e.getMessage());
        }
    }
}

