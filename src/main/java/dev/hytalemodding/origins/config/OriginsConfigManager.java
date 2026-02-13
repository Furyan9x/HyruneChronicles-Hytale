package dev.hytalemodding.origins.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.logger.HytaleLogger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Loads and caches the Origins config from ./origins_data/gameplay_config.json.
 */
public final class OriginsConfigManager {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File ROOT_FOLDER = new File("./origins_data");
    private static final File CONFIG_FILE = new File(ROOT_FOLDER, "gameplay_config.json");
    private static volatile OriginsConfig config;

    private OriginsConfigManager() {
    }

    public static OriginsConfig getConfig() {
        OriginsConfig local = config;
        if (local == null) {
            return reload();
        }
        return local;
    }

    public static synchronized OriginsConfig reload() {
        ensureRootFolder();
        LOGGER.at(Level.INFO).log("Loading gameplay config from: " + CONFIG_FILE.getAbsolutePath());

        if (!CONFIG_FILE.exists()) {
            OriginsConfig defaults = new OriginsConfig();
            writeConfig(defaults);
            config = defaults;
            LOGGER.at(Level.INFO).log("Gameplay config not found. Wrote defaults.");
            LOGGER.at(Level.INFO).log("Gameplay toggles: durabilityDebugLogging=" + defaults.durabilityDebugLogging
                + ", enableAnimalHusbandryGating=" + defaults.enableAnimalHusbandryGating);
            return defaults;
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            OriginsConfig loaded = GSON.fromJson(reader, OriginsConfig.class);
            if (loaded == null) {
                loaded = new OriginsConfig();
            }
            config = loaded;
            LOGGER.at(Level.INFO).log("Gameplay config loaded. durabilityDebugLogging=" + loaded.durabilityDebugLogging
                + ", enableAnimalHusbandryGating=" + loaded.enableAnimalHusbandryGating);
            return loaded;
        } catch (IOException e) {
            LOGGER.at(Level.WARNING).log("Failed to read gameplay config, using defaults: " + e.getMessage());
            OriginsConfig fallback = new OriginsConfig();
            config = fallback;
            LOGGER.at(Level.INFO).log("Gameplay toggles: durabilityDebugLogging=" + fallback.durabilityDebugLogging
                + ", enableAnimalHusbandryGating=" + fallback.enableAnimalHusbandryGating);
            return fallback;
        }
    }

    private static void ensureRootFolder() {
        if (ROOT_FOLDER.exists()) {
            return;
        }
        if (!ROOT_FOLDER.mkdirs()) {
            LOGGER.at(Level.WARNING).log("Failed to create config directory: " + ROOT_FOLDER.getAbsolutePath());
        }
    }

    private static void writeConfig(OriginsConfig cfg) {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(cfg, writer);
        } catch (IOException e) {
            LOGGER.at(Level.WARNING).log("Failed to write gameplay config: " + e.getMessage());
        }
    }
}
