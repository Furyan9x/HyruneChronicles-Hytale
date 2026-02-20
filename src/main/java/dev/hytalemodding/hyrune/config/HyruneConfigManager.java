package dev.hytalemodding.hyrune.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hypixel.hytale.logger.HytaleLogger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Loads and caches the Hyrune config from ./hyrune_data/gameplay_config.json.
 */
public final class HyruneConfigManager {
    private static final int CURRENT_SCHEMA_VERSION = 2;
    private static final String LEGACY_ROOT_PREFIX = "Root";
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
            NormalizedConfig normalized = normalizeConfig(new HyruneConfig());
            writeConfig(normalized.config());
            config = normalized.config();
            LOGGER.at(Level.INFO).log("Gameplay config not found. Wrote defaults.");
            LOGGER.at(Level.INFO).log(summarizeConfig("Gameplay config defaults", normalized.config()));
            return normalized.config();
        }

        try (FileReader reader = new FileReader(CONFIG_FILE, StandardCharsets.UTF_8)) {
            HyruneConfig loaded = GSON.fromJson(reader, HyruneConfig.class);
            NormalizedConfig normalized = normalizeConfig(loaded);
            if (normalized.changed()) {
                writeConfig(normalized.config());
            }
            config = normalized.config();
            LOGGER.at(Level.INFO).log(summarizeConfig("Gameplay config loaded", normalized.config()));
            return normalized.config();
        } catch (IOException | RuntimeException e) {
            LOGGER.at(Level.WARNING).log("Failed to read gameplay config, using defaults: " + e.getMessage());
            NormalizedConfig fallback = normalizeConfig(new HyruneConfig());
            writeConfig(fallback.config());
            config = fallback.config();
            LOGGER.at(Level.INFO).log(summarizeConfig("Gameplay config fallback", fallback.config()));
            return fallback.config();
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
            + "} schema={version=" + cfg.configSchemaVersion
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

    private static NormalizedConfig normalizeConfig(HyruneConfig cfg) {
        JsonObject normalizedTree = cfg == null ? new JsonObject() : asJsonObject(cfg);
        JsonObject defaultsTree = asJsonObject(new HyruneConfig());

        boolean changed = cfg == null;
        int loadedSchemaVersion = readSchemaVersion(normalizedTree);
        if (loadedSchemaVersion < 2) {
            changed |= migrateLegacyRootPrefix(normalizedTree);
        }

        changed |= mergeMissingFields(normalizedTree, defaultsTree);
        if (readSchemaVersion(normalizedTree) != CURRENT_SCHEMA_VERSION) {
            normalizedTree.addProperty("configSchemaVersion", CURRENT_SCHEMA_VERSION);
            changed = true;
        }

        HyruneConfig normalizedConfig = GSON.fromJson(normalizedTree, HyruneConfig.class);
        if (normalizedConfig == null) {
            normalizedConfig = new HyruneConfig();
            changed = true;
        }

        if (normalizedConfig.gatheringUtilityDrops == null) {
            normalizedConfig.gatheringUtilityDrops = new HyruneConfig.GatheringUtilityDropConfig();
            changed = true;
        }
        if (normalizedConfig.gatheringUtilityDrops.rareDropsBySkill == null
            || normalizedConfig.gatheringUtilityDrops.rareDropsBySkill.isEmpty()) {
            normalizedConfig.gatheringUtilityDrops.rareDropsBySkill =
                new HyruneConfig.GatheringUtilityDropConfig().rareDropsBySkill;
            changed = true;
        }

        return new NormalizedConfig(normalizedConfig, changed);
    }

    private static JsonObject asJsonObject(Object value) {
        JsonElement tree = GSON.toJsonTree(value);
        if (tree == null || !tree.isJsonObject()) {
            return new JsonObject();
        }
        return tree.getAsJsonObject();
    }

    private static int readSchemaVersion(JsonObject cfgTree) {
        if (cfgTree == null) {
            return 0;
        }
        JsonElement raw = cfgTree.get("configSchemaVersion");
        if (raw == null || raw.isJsonNull()) {
            return 0;
        }
        try {
            return raw.getAsInt();
        } catch (RuntimeException ignored) {
            return 0;
        }
    }

    private static boolean mergeMissingFields(JsonObject target, JsonObject defaults) {
        boolean changed = false;
        if (target == null || defaults == null) {
            return false;
        }
        for (Map.Entry<String, JsonElement> entry : defaults.entrySet()) {
            String key = entry.getKey();
            JsonElement defaultValue = entry.getValue();
            if (!target.has(key) || target.get(key) == null || target.get(key).isJsonNull()) {
                target.add(key, defaultValue == null ? null : defaultValue.deepCopy());
                changed = true;
                continue;
            }
            JsonElement existing = target.get(key);
            if (existing != null && existing.isJsonObject() && defaultValue != null && defaultValue.isJsonObject()) {
                changed |= mergeMissingFields(existing.getAsJsonObject(), defaultValue.getAsJsonObject());
            }
        }
        return changed;
    }

    private static boolean migrateLegacyRootPrefix(JsonObject cfgTree) {
        if (cfgTree == null) {
            return false;
        }
        boolean changed = false;

        JsonObject prefixes = getObject(cfgTree, "prefixes");
        if (prefixes != null) {
            JsonArray rollableWords = getArray(prefixes, "rollableWords");
            if (rollableWords != null) {
                for (int i = rollableWords.size() - 1; i >= 0; i--) {
                    JsonElement entry = rollableWords.get(i);
                    if (entry == null || entry.isJsonNull()) {
                        continue;
                    }
                    String value = entry.getAsString();
                    if (value != null && value.trim().equalsIgnoreCase(LEGACY_ROOT_PREFIX)) {
                        rollableWords.remove(i);
                        changed = true;
                    }
                }
            }
        }

        JsonObject specializedStats = getObject(cfgTree, "itemizationSpecializedStats");
        JsonObject poolByPrefix = specializedStats == null ? null : getObject(specializedStats, "poolByPrefix");
        if (poolByPrefix != null) {
            List<String> keysToRemove = new ArrayList<>();
            for (Map.Entry<String, JsonElement> entry : poolByPrefix.entrySet()) {
                String key = entry.getKey();
                if (key != null && key.trim().equalsIgnoreCase(LEGACY_ROOT_PREFIX)) {
                    keysToRemove.add(key);
                }
            }
            for (String key : keysToRemove) {
                poolByPrefix.remove(key);
                changed = true;
            }
        }

        return changed;
    }

    private static JsonObject getObject(JsonObject root, String key) {
        if (root == null || key == null || key.isBlank()) {
            return null;
        }
        JsonElement value = root.get(key);
        if (value == null || !value.isJsonObject()) {
            return null;
        }
        return value.getAsJsonObject();
    }

    private static JsonArray getArray(JsonObject root, String key) {
        if (root == null || key == null || key.isBlank()) {
            return null;
        }
        JsonElement value = root.get(key);
        if (value == null || !value.isJsonArray()) {
            return null;
        }
        return value.getAsJsonArray();
    }

    private static void writeConfig(HyruneConfig cfg) {
        try (FileWriter writer = new FileWriter(CONFIG_FILE, StandardCharsets.UTF_8)) {
            GSON.toJson(cfg, writer);
        } catch (IOException e) {
            LOGGER.at(Level.WARNING).log("Failed to write gameplay config: " + e.getMessage());
        }
    }

    private record NormalizedConfig(HyruneConfig config, boolean changed) {
    }
}
