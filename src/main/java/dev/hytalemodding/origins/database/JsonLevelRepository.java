package dev.hytalemodding.origins.database;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.hytalemodding.origins.playerdata.PlayerLvlData;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;

public class JsonLevelRepository implements LevelRepository {

    private final Gson gson;
    private final File dataFolder;

    public JsonLevelRepository(String rootPath) {
        // Pretty printing makes the JSON easier for us to read/edit manually
        this.gson = new GsonBuilder().setPrettyPrinting().create();

        // Ensure the directory exists: <rootPath>/players
        this.dataFolder = new File(rootPath, "players");
        if (!this.dataFolder.exists()) {
            this.dataFolder.mkdirs();
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
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void save(PlayerLvlData data) {
        File playerFile = new File(this.dataFolder, data.getUuid().toString() + ".json");

        try (FileWriter writer = new FileWriter(playerFile)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
