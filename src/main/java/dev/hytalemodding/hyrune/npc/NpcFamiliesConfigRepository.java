package dev.hytalemodding.hyrune.npc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import dev.hytalemodding.Hyrune;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

/**
 * Repository for strict, non-legacy NPC family/profile/rank config.
 */
public class NpcFamiliesConfigRepository {
    private static final String CONFIG_FILE = "npc_families.json";
    private static final String BUNDLED_SEED_RESOURCE = "/hyrune_defaults/npc_families.json";
    private static final String DEFAULT_ARCHETYPE = "DPS";
    private static final List<String> DEFAULT_EXCLUDES = List.of(
        "Tier1_Slayer_Master",
        "Tier2_Slayer_Master",
        "Slayer_Master",
        "Hans",
        "Master_Hans"
    );

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File configFile;

    public NpcFamiliesConfigRepository(String dataRootPath, String npcRolesRootPath) {
        File folder = new File(dataRootPath);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        this.configFile = new File(folder, CONFIG_FILE);
        // npcRolesRootPath kept for constructor compatibility with existing plugin wiring.
    }

    public NpcFamiliesConfig loadOrCreate() {
        if (!configFile.exists()) {
            NpcFamiliesConfig defaults = buildDefaults();
            save(defaults);
            return defaults;
        }

        try (FileReader reader = new FileReader(configFile)) {
            NpcFamiliesConfig loaded = gson.fromJson(reader, NpcFamiliesConfig.class);
            if (loaded == null) {
                loaded = new NpcFamiliesConfig();
            }
            if (ensureDefaults(loaded)) {
                save(loaded);
            }
            return loaded;
        } catch (IOException | JsonParseException e) {
            Hyrune.LOGGER.at(Level.WARNING).log("Failed to load npc families config: " + e.getMessage());
            backupCorruptConfig();
            NpcFamiliesConfig fallback = buildDefaults();
            save(fallback);
            return fallback;
        }
    }

    public void save(NpcFamiliesConfig config) {
        if (config == null) {
            return;
        }
        try (FileWriter writer = new FileWriter(configFile)) {
            JsonObject root = gson.toJsonTree(config).getAsJsonObject();
            if (root.has("families") && root.get("families").isJsonArray()) {
                for (JsonElement familyElem : root.getAsJsonArray("families")) {
                    if (!familyElem.isJsonObject()) {
                        continue;
                    }
                    JsonObject family = familyElem.getAsJsonObject();
                    if (family.has("weakness") && "MELEE".equalsIgnoreCase(family.get("weakness").getAsString())) {
                        family.remove("weakness");
                    }
                    if (family.has("archetype") && "DPS".equalsIgnoreCase(family.get("archetype").getAsString())) {
                        family.remove("archetype");
                    }
                    if (family.has("rank") && "NORMAL".equalsIgnoreCase(family.get("rank").getAsString())) {
                        family.remove("rank");
                    }
                    if (family.has("elite") && !family.get("elite").getAsBoolean()) {
                        family.remove("elite");
                    }
                }
            }
            gson.toJson(root, writer);
        } catch (IOException e) {
            Hyrune.LOGGER.at(Level.WARNING).log("Failed to save npc families config: " + e.getMessage());
        }
    }

    private NpcFamiliesConfig buildDefaults() {
        NpcFamiliesConfig out = loadBundledSeed();
        if (out == null) {
            out = new NpcFamiliesConfig();
        }
        ensureDefaults(out);
        return out;
    }

    private boolean ensureDefaults(NpcFamiliesConfig config) {
        boolean changed = false;
        if (config.defaultWeakness == null || config.defaultWeakness.isBlank()) {
            config.defaultWeakness = CombatStyle.MELEE.name();
            changed = true;
        }
        if (config.defaultLevel <= 0 || config.defaultLevel > 99) {
            config.defaultLevel = 1;
            changed = true;
        }
        if (config.defaultVariance < 0 || config.defaultVariance > 20) {
            config.defaultVariance = 3;
            changed = true;
        }
        if (config.defaultArchetype == null || config.defaultArchetype.isBlank()) {
            config.defaultArchetype = DEFAULT_ARCHETYPE;
            changed = true;
        } else {
            String normalized = normalizeArchetype(config.defaultArchetype);
            if (!normalized.equals(config.defaultArchetype)) {
                config.defaultArchetype = normalized;
                changed = true;
            }
        }
        if (config.weaknessMultiplier <= 0.0) {
            config.weaknessMultiplier = 1.20;
            changed = true;
        }
        if (config.resistanceMultiplier <= 0.0) {
            config.resistanceMultiplier = 0.80;
            changed = true;
        }

        if (config.excludedNpcIds == null) {
            config.excludedNpcIds = new ArrayList<>();
            changed = true;
        }
        for (String required : DEFAULT_EXCLUDES) {
            if (!containsIgnoreCase(config.excludedNpcIds, required)) {
                config.excludedNpcIds.add(required);
                changed = true;
            }
        }

        if (config.archetypeProfiles == null) {
            config.archetypeProfiles = new ArrayList<>();
            changed = true;
        }
        for (NpcFamiliesConfig.NpcArchetypeProfile profile : defaultArchetypeProfiles()) {
            if (!hasArchetypeProfile(config.archetypeProfiles, profile.id)) {
                config.archetypeProfiles.add(profile);
                changed = true;
            }
        }
        for (NpcFamiliesConfig.NpcArchetypeProfile profile : config.archetypeProfiles) {
            if (profile == null) {
                continue;
            }
            if (profile.id == null || profile.id.isBlank()) {
                profile.id = DEFAULT_ARCHETYPE;
                changed = true;
            } else {
                String normalized = normalizeArchetype(profile.id);
                if (!normalized.equals(profile.id)) {
                    profile.id = normalized;
                    changed = true;
                }
            }
            if (profile.damageGrowthRate <= 0.0) {
                profile.damageGrowthRate = 1.0;
                changed = true;
            }
            if (profile.defenceGrowthRate <= 0.0) {
                profile.defenceGrowthRate = 1.0;
                changed = true;
            }
            if (profile.critGrowthRate <= 0.0) {
                profile.critGrowthRate = 1.0;
                changed = true;
            }
        }

        if (config.rankProfiles == null) {
            config.rankProfiles = new ArrayList<>();
            changed = true;
        }
        for (NpcFamiliesConfig.NpcRankProfile rank : defaultRankProfiles()) {
            if (!hasRank(config.rankProfiles, rank.id)) {
                config.rankProfiles.add(rank);
                changed = true;
            }
        }
        for (NpcFamiliesConfig.NpcRankProfile rank : config.rankProfiles) {
            if (rank == null) {
                continue;
            }
            if (rank.id == null || rank.id.isBlank()) {
                rank.id = NpcRank.NORMAL.name();
                changed = true;
            } else {
                String normalized = rank.id.trim().toUpperCase(Locale.ROOT);
                if (!normalized.equals(rank.id)) {
                    rank.id = normalized;
                    changed = true;
                }
            }
            if (rank.statMultiplier <= 0.0) {
                rank.statMultiplier = 1.0;
                changed = true;
            }
        }

        if (config.families == null) {
            config.families = new ArrayList<>();
            changed = true;
        }
        for (NpcFamiliesConfig.NpcFamilyDefinition family : config.families) {
            if (family == null) {
                continue;
            }
            if (family.tags == null) {
                family.tags = new ArrayList<>();
                changed = true;
            }
            if (family.typeIds == null) {
                family.typeIds = new ArrayList<>();
                changed = true;
            }
            if (family.rolePaths == null) {
                family.rolePaths = new ArrayList<>();
                changed = true;
            }

            int clampedBase = Math.max(1, Math.min(99, family.baseLevel));
            if (clampedBase != family.baseLevel) {
                family.baseLevel = clampedBase;
                changed = true;
            }
            int clampedVariance = Math.max(0, Math.min(20, family.variance));
            if (clampedVariance != family.variance) {
                family.variance = clampedVariance;
                changed = true;
            }
            if (family.weakness == null || family.weakness.isBlank()) {
                family.weakness = CombatStyle.MELEE.name();
                changed = true;
            }
            String normalizedArchetype = normalizeArchetype(family.archetype);
            if (!normalizedArchetype.equals(family.archetype)) {
                family.archetype = normalizedArchetype;
                changed = true;
            }
            String normalizedRank = NpcRank.fromString(family.rank, NpcRank.NORMAL).name();
            if (!normalizedRank.equals(family.rank)) {
                family.rank = normalizedRank;
                changed = true;
            }
        }

        config.families.sort(Comparator.comparing(f -> f.id == null ? "" : f.id.toLowerCase(Locale.ROOT)));
        return changed;
    }

    private static List<NpcFamiliesConfig.NpcRankProfile> defaultRankProfiles() {
        return List.of(
            rank(NpcRank.NORMAL, 0, 1.0),
            rank(NpcRank.STRONG, 4, 1.35),
            rank(NpcRank.ELITE, 7, 1.8),
            rank(NpcRank.HERO, 11, 2.5),
            rank(NpcRank.WORLD_BOSS, 16, 4.0)
        );
    }

    private static List<NpcFamiliesConfig.NpcArchetypeProfile> defaultArchetypeProfiles() {
        return List.of(
            buildArchetypeProfile("TANK", 1.00, 1.022, 0.06, 1.032, 0.82, 0.02, 1.012, 0.20, 1.30, 0.0030, 1.95, 1.00, 1.08, 0.92, 0.88),
            buildArchetypeProfile("DPS", 1.05, 1.035, 0.02, 1.020, 0.58, 0.05, 1.020, 0.35, 1.45, 0.0040, 2.25, 1.00, 1.00, 1.06, 0.98),
            buildArchetypeProfile("MAGE", 1.03, 1.030, 0.03, 1.024, 0.62, 0.04, 1.018, 0.32, 1.50, 0.0044, 2.30, 1.00, 0.92, 0.98, 1.12),
            buildArchetypeProfile("BOSS", 1.10, 1.030, 0.05, 1.030, 0.85, 0.03, 1.015, 0.45, 1.55, 0.0048, 2.60, 5.00, 1.04, 1.00, 1.02)
        );
    }

    private static NpcFamiliesConfig.NpcArchetypeProfile buildArchetypeProfile(String id,
                                                                                double baseDamage,
                                                                                double damageGrowthRate,
                                                                                double baseDefenceReduction,
                                                                                double defenceGrowthRate,
                                                                                double defenceCap,
                                                                                double baseCritChance,
                                                                                double critGrowthRate,
                                                                                double critChanceCap,
                                                                                double baseCritMultiplier,
                                                                                double critMultiplierPerLevel,
                                                                                double critMultiplierCap,
                                                                                double baseHealthMultiplier,
                                                                                double meleeBias,
                                                                                double rangedBias,
                                                                                double magicBias) {
        NpcFamiliesConfig.NpcArchetypeProfile profile = new NpcFamiliesConfig.NpcArchetypeProfile();
        profile.id = id;
        profile.baseDamage = baseDamage;
        profile.damageGrowthRate = damageGrowthRate;
        profile.baseDefenceReduction = baseDefenceReduction;
        profile.defenceGrowthRate = defenceGrowthRate;
        profile.defenceCap = defenceCap;
        profile.baseCritChance = baseCritChance;
        profile.critGrowthRate = critGrowthRate;
        profile.critChanceCap = critChanceCap;
        profile.baseCritMultiplier = baseCritMultiplier;
        profile.critMultiplierPerLevel = critMultiplierPerLevel;
        profile.critMultiplierCap = critMultiplierCap;
        profile.baseHealthMultiplier = baseHealthMultiplier;
        profile.meleeBias = meleeBias;
        profile.rangedBias = rangedBias;
        profile.magicBias = magicBias;
        return profile;
    }

    private static NpcFamiliesConfig.NpcRankProfile rank(NpcRank rank, int levelOffset, double statMultiplier) {
        NpcFamiliesConfig.NpcRankProfile profile = new NpcFamiliesConfig.NpcRankProfile();
        profile.id = rank.name();
        profile.levelOffset = levelOffset;
        profile.statMultiplier = statMultiplier;
        return profile;
    }

    private static boolean hasRank(List<NpcFamiliesConfig.NpcRankProfile> profiles, String id) {
        if (profiles == null || id == null) {
            return false;
        }
        for (NpcFamiliesConfig.NpcRankProfile profile : profiles) {
            if (profile != null && profile.id != null && profile.id.equalsIgnoreCase(id)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasArchetypeProfile(List<NpcFamiliesConfig.NpcArchetypeProfile> profiles, String id) {
        if (profiles == null || id == null) {
            return false;
        }
        for (NpcFamiliesConfig.NpcArchetypeProfile profile : profiles) {
            if (profile != null && profile.id != null && profile.id.equalsIgnoreCase(id)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsIgnoreCase(List<String> values, String value) {
        if (values == null || value == null) {
            return false;
        }
        for (String existing : values) {
            if (existing != null && existing.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeArchetype(String archetype) {
        if (archetype == null || archetype.isBlank()) {
            return DEFAULT_ARCHETYPE;
        }
        return archetype.trim().toUpperCase(Locale.ROOT);
    }

    private NpcFamiliesConfig loadBundledSeed() {
        try (InputStream in = NpcFamiliesConfigRepository.class.getResourceAsStream(BUNDLED_SEED_RESOURCE)) {
            if (in == null) {
                return null;
            }
            try (InputStreamReader reader = new InputStreamReader(in)) {
                return gson.fromJson(reader, NpcFamiliesConfig.class);
            }
        } catch (IOException | JsonParseException e) {
            Hyrune.LOGGER.at(Level.WARNING).log("Failed to load bundled npc families seed: " + e.getMessage());
            return null;
        }
    }

    private void backupCorruptConfig() {
        if (!configFile.exists() || !configFile.isFile()) {
            return;
        }
        File backup = new File(configFile.getParentFile(),
            "npc_families.corrupt." + System.currentTimeMillis() + ".json");
        if (configFile.renameTo(backup)) {
            Hyrune.LOGGER.at(Level.WARNING).log("Backed up corrupt npc families config to: " + backup.getAbsolutePath());
        }
    }
}
