package dev.hytalemodding.hyrune.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.logger.HytaleLogger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Loads and caches the Hyrune config from ./hyrune_data/gameplay_config.json.
 */
public final class HyruneConfigManager {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File ROOT_FOLDER = new File("./hyrune_data");
    private static final File CONFIG_FILE = new File(ROOT_FOLDER, "gameplay_config.json");
    private static volatile HyruneConfig config;

    private HyruneConfigManager() {
    }

    public static HyruneConfig getConfig() {
        HyruneConfig local = config;
        if (local == null) {
            return reload();
        }
        return local;
    }

    public static synchronized HyruneConfig reload() {
        ensureRootFolder();
        LOGGER.at(Level.INFO).log("Loading gameplay config from: " + CONFIG_FILE.getAbsolutePath());

        if (!CONFIG_FILE.exists()) {
            HyruneConfig defaults = new HyruneConfig();
            writeConfig(defaults);
            config = defaults;
            LOGGER.at(Level.INFO).log("Gameplay config not found. Wrote defaults.");
            LOGGER.at(Level.INFO).log(summarizeConfig("Gameplay config defaults", defaults));
            return defaults;
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            HyruneConfig loaded = GSON.fromJson(reader, HyruneConfig.class);
            if (loaded == null) {
                loaded = new HyruneConfig();
            }
            config = loaded;
            LOGGER.at(Level.INFO).log(summarizeConfig("Gameplay config loaded", loaded));
            return loaded;
        } catch (IOException e) {
            LOGGER.at(Level.WARNING).log("Failed to read gameplay config, using defaults: " + e.getMessage());
            HyruneConfig fallback = new HyruneConfig();
            config = fallback;
            LOGGER.at(Level.INFO).log(summarizeConfig("Gameplay config fallback", fallback));
            return fallback;
        }
    }

    private static String summarizeConfig(String label, HyruneConfig cfg) {
        if (cfg == null) {
            return label + ": <null>";
        }
        int eligiblePrefixes = cfg.itemizationEligiblePrefixes == null ? 0 : cfg.itemizationEligiblePrefixes.size();
        int excludedPrefixes = cfg.itemizationExcludedPrefixes == null ? 0 : cfg.itemizationExcludedPrefixes.size();
        int excludedIds = cfg.itemizationExcludedIds == null ? 0 : cfg.itemizationExcludedIds.size();
        int raritySources = cfg.itemizationRarityModel == null || cfg.itemizationRarityModel.baseWeightsBySource == null
            ? 0
            : cfg.itemizationRarityModel.baseWeightsBySource.size();

        return label
            + ". debug={durability=" + cfg.durabilityDebugLogging
            + ", itemization=" + cfg.itemizationDebugLogging
            + ", tooltipCompose=" + cfg.dynamicTooltipComposeDebug
            + ", tooltipMap=" + cfg.dynamicTooltipMappingDebug
            + ", tooltipCache=" + cfg.dynamicTooltipCacheDebug
            + "} features={dynamicTooltips=" + cfg.enableDynamicItemTooltips
            + ", animalGating=" + cfg.enableAnimalHusbandryGating
            + "} itemization={eligiblePrefixes=" + eligiblePrefixes
            + ", excludedPrefixes=" + excludedPrefixes
            + ", excludedIds=" + excludedIds
            + ", raritySources=" + raritySources
            + "}";
    }

    private static void ensureRootFolder() {
        if (ROOT_FOLDER.exists()) {
            return;
        }
        if (!ROOT_FOLDER.mkdirs()) {
            LOGGER.at(Level.WARNING).log("Failed to create config directory: " + ROOT_FOLDER.getAbsolutePath());
        }
    }

    private static void writeConfig(HyruneConfig cfg) {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(cfg, writer);
        } catch (IOException e) {
            LOGGER.at(Level.WARNING).log("Failed to write gameplay config: " + e.getMessage());
        }
    }
}
