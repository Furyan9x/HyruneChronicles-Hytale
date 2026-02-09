package dev.hytalemodding.origins.quests;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.hytalemodding.Origins;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;

public class JsonQuestRepository implements QuestRepository {

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
            boolean created = this.dataFolder.mkdirs();
            System.out.println("[DEBUG] Created quests folder: " + created + " at " + this.dataFolder.getAbsolutePath());
        }
    }

    @Override
    public QuestManager.PlayerQuestData load(UUID uuid) {
        File playerFile = new File(this.dataFolder, uuid.toString() + ".json");
        if (!playerFile.exists()) {
            return null;
        }

        try (FileReader reader = new FileReader(playerFile)) {
            return gson.fromJson(reader, QuestManager.PlayerQuestData.class);
        } catch (IOException e) {
            Origins.LOGGER.at(Level.WARNING)
                    .log("Failed to load quest data for " + uuid + ": " + e.getMessage());
            return null;
        }
    }

    @Override
    public void save(QuestManager.PlayerQuestData data) {
        if (data == null || data.getUuid() == null) return;
        File playerFile = new File(this.dataFolder, data.getUuid().toString() + ".json");
        try (FileWriter writer = new FileWriter(playerFile)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}