package dev.hytalemodding.origins.slayer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.hytalemodding.Origins;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;

public class JsonSlayerRepository implements SlayerRepository {

    private final Gson gson;
    private final File dataFolder;

    public JsonSlayerRepository(String rootPath) {
        this.gson = new GsonBuilder().setPrettyPrinting().create();

        this.dataFolder = new File(rootPath, "slayer_players");
        if (!this.dataFolder.exists()) {
            this.dataFolder.mkdirs();
        }
    }

    @Override
    public SlayerPlayerData load(UUID uuid) {
        File playerFile = new File(this.dataFolder, uuid.toString() + ".json");
        if (!playerFile.exists()) {
            return null;
        }

        try (FileReader reader = new FileReader(playerFile)) {
            return gson.fromJson(reader, SlayerPlayerData.class);
        } catch (IOException e) {
            Origins.LOGGER.at(Level.WARNING)
                    .log("Failed to load Slayer data for " + uuid + ": " + e.getMessage());
            return null;
        }
    }

    @Override
    public void save(SlayerPlayerData data) {
        if (data == null || data.getUuid() == null) {
            return;
        }
        File playerFile = new File(this.dataFolder, data.getUuid().toString() + ".json");

        try (FileWriter writer = new FileWriter(playerFile)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            Origins.LOGGER.at(Level.WARNING)
                    .log("Failed to save Slayer data for " + data.getUuid() + ": " + e.getMessage());
        }
    }
}
