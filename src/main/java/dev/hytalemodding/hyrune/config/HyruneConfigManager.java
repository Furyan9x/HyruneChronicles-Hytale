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
            LOGGER.at(Level.INFO).log("Gameplay toggles: durabilityDebugLogging=" + defaults.durabilityDebugLogging
                + ", itemizationDebugLogging=" + defaults.itemizationDebugLogging
                + ", enableDynamicItemTooltips=" + defaults.enableDynamicItemTooltips
                + ", dynamicTooltipComposeDebug=" + defaults.dynamicTooltipComposeDebug
                + ", dynamicTooltipMappingDebug=" + defaults.dynamicTooltipMappingDebug
                + ", dynamicTooltipCacheDebug=" + defaults.dynamicTooltipCacheDebug
                + ", itemizationEligiblePrefixes=" + defaults.itemizationEligiblePrefixes.size()
                + ", itemizationExcludedPrefixes=" + defaults.itemizationExcludedPrefixes.size()
                + ", itemizationExcludedIds=" + defaults.itemizationExcludedIds.size()
                + ", itemizationRarityModelSources=" + defaults.itemizationRarityModel.baseWeightsBySource.size()
                + ", enableAnimalHusbandryGating=" + defaults.enableAnimalHusbandryGating);
            return defaults;
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            HyruneConfig loaded = GSON.fromJson(reader, HyruneConfig.class);
            if (loaded == null) {
                loaded = new HyruneConfig();
            }
            config = loaded;
            LOGGER.at(Level.INFO).log("Gameplay config loaded. durabilityDebugLogging=" + loaded.durabilityDebugLogging
                + ", itemizationDebugLogging=" + loaded.itemizationDebugLogging
                + ", enableDynamicItemTooltips=" + loaded.enableDynamicItemTooltips
                + ", dynamicTooltipComposeDebug=" + loaded.dynamicTooltipComposeDebug
                + ", dynamicTooltipMappingDebug=" + loaded.dynamicTooltipMappingDebug
                + ", dynamicTooltipCacheDebug=" + loaded.dynamicTooltipCacheDebug
                + ", itemizationEligiblePrefixes=" + (loaded.itemizationEligiblePrefixes == null ? 0 : loaded.itemizationEligiblePrefixes.size())
                + ", itemizationExcludedPrefixes=" + (loaded.itemizationExcludedPrefixes == null ? 0 : loaded.itemizationExcludedPrefixes.size())
                + ", itemizationExcludedIds=" + (loaded.itemizationExcludedIds == null ? 0 : loaded.itemizationExcludedIds.size())
                + ", itemizationRarityModelSources=" + (loaded.itemizationRarityModel == null || loaded.itemizationRarityModel.baseWeightsBySource == null ? 0 : loaded.itemizationRarityModel.baseWeightsBySource.size())
                + ", enableAnimalHusbandryGating=" + loaded.enableAnimalHusbandryGating);
            return loaded;
        } catch (IOException e) {
            LOGGER.at(Level.WARNING).log("Failed to read gameplay config, using defaults: " + e.getMessage());
            HyruneConfig fallback = new HyruneConfig();
            config = fallback;
            LOGGER.at(Level.INFO).log("Gameplay toggles: durabilityDebugLogging=" + fallback.durabilityDebugLogging
                + ", itemizationDebugLogging=" + fallback.itemizationDebugLogging
                + ", enableDynamicItemTooltips=" + fallback.enableDynamicItemTooltips
                + ", dynamicTooltipComposeDebug=" + fallback.dynamicTooltipComposeDebug
                + ", dynamicTooltipMappingDebug=" + fallback.dynamicTooltipMappingDebug
                + ", dynamicTooltipCacheDebug=" + fallback.dynamicTooltipCacheDebug
                + ", itemizationEligiblePrefixes=" + fallback.itemizationEligiblePrefixes.size()
                + ", itemizationExcludedPrefixes=" + fallback.itemizationExcludedPrefixes.size()
                + ", itemizationExcludedIds=" + fallback.itemizationExcludedIds.size()
                + ", itemizationRarityModelSources=" + fallback.itemizationRarityModel.baseWeightsBySource.size()
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

    private static void writeConfig(HyruneConfig cfg) {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(cfg, writer);
        } catch (IOException e) {
            LOGGER.at(Level.WARNING).log("Failed to write gameplay config: " + e.getMessage());
        }
    }
}
