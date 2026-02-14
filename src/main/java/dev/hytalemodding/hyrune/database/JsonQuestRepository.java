package dev.hytalemodding.hyrune.database;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.logger.HytaleLogger;
import dev.hytalemodding.hyrune.playerdata.PlayerQuestData;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;

/**
 * JSON-backed repository for quest progress data.
 */
public class JsonQuestRepository implements QuestRepository {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final Gson gson;
    private final File dataFolder;

    public JsonQuestRepository(String rootPath) {
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .excludeFieldsWithModifiers(java.lang.reflect.Modifier.STATIC) // Exclude CODEC
                .serializeNulls()
                .create();
        this.dataFolder = new File(rootPath, "quests");
        if (!this.dataFolder.exists()) {
            if (!this.dataFolder.mkdirs()) {
                LOGGER.at(Level.WARNING).log("Failed to create quests folder at " + this.dataFolder.getAbsolutePath());
            }
        }
    }

    @Override
    public PlayerQuestData load(UUID uuid) {
        File playerFile = new File(this.dataFolder, uuid.toString() + ".json");
        if (!playerFile.exists()) {
            return null;
        }

        try (FileReader reader = new FileReader(playerFile)) {
            return gson.fromJson(reader, PlayerQuestData.class);
        } catch (IOException e) {
            LOGGER.at(Level.WARNING)
                .log("Failed to load quest data for " + uuid + ": " + e.getMessage());
            return null;
        }
    }

    @Override
    public void save(PlayerQuestData data) {
        if (data == null || data.getUuid() == null) {
            return;
        }
        File playerFile = new File(this.dataFolder, data.getUuid().toString() + ".json");
        try (FileWriter writer = new FileWriter(playerFile)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            LOGGER.at(Level.WARNING)
                .log("Failed to save quest data for " + data.getUuid() + ": " + e.getMessage());
        }
    }
}
