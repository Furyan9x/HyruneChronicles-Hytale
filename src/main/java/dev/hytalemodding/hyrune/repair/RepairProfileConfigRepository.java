package dev.hytalemodding.hyrune.repair;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.logger.HytaleLogger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

/**
 * Loads/writes repair profile config for data-driven balancing.
 */
public class RepairProfileConfigRepository {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String FILE_NAME = "repair_profiles.json";

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File configFile;

    public RepairProfileConfigRepository(String rootPath) {
        File root = new File(rootPath);
        if (!root.exists() && !root.mkdirs()) {
            LOGGER.at(Level.WARNING).log("Failed to create root config directory at " + root.getAbsolutePath());
        }
        this.configFile = new File(root, FILE_NAME);
    }

    public RepairProfileConfig loadOrCreate(List<RepairProfileDefinition> defaults) {
        RepairProfileConfig loaded = load();
        if (loaded != null && loaded.profiles != null && !loaded.profiles.isEmpty()) {
            return loaded;
        }

        RepairProfileConfig fallback = new RepairProfileConfig();
        if (defaults != null) {
            fallback.profiles.addAll(defaults);
        }
        save(fallback);
        return fallback;
    }

    private RepairProfileConfig load() {
        if (!configFile.exists()) {
            return null;
        }
        try (FileReader reader = new FileReader(configFile)) {
            return gson.fromJson(reader, RepairProfileConfig.class);
        } catch (IOException e) {
            LOGGER.at(Level.WARNING).log("Failed to load repair profiles: " + e.getMessage());
            return null;
        }
    }

    private void save(RepairProfileConfig config) {
        try (FileWriter writer = new FileWriter(configFile)) {
            gson.toJson(config, writer);
        } catch (IOException e) {
            LOGGER.at(Level.WARNING).log("Failed to save repair profiles: " + e.getMessage());
        }
    }
}
