package dev.hytalemodding.hyrune.npc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.hytalemodding.Hyrune;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

/**
 * 
 */
public class NpcLevelConfigRepository {
    private static final String CONFIG_FILE = "npc_levels.json";
    private static final String DEFAULT_ARCHETYPE = "DPS";
    private static final List<String> DEFAULT_EXCLUDES = List.of(
        "Tier1_Slayer_Master",
        "Slayer_Master",
        "Hans",
        "Master_Hans"
    );

    private final Gson gson;
    private final File configFile;

    public NpcLevelConfigRepository(String rootPath) {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        File folder = new File(rootPath);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        this.configFile = new File(folder, CONFIG_FILE);
    }

    public NpcLevelConfig loadOrCreate() {
        if (!configFile.exists()) {
            NpcLevelConfig defaults = buildDefaults();
            save(defaults);
            return defaults;
        }

        try (FileReader reader = new FileReader(configFile)) {
            NpcLevelConfig config = gson.fromJson(reader, NpcLevelConfig.class);
            if (config == null) {
                return buildDefaults();
            }
            boolean changed = ensureDefaults(config);
            if (changed) {
                save(config);
            }
            return config;
        } catch (IOException e) {
            Hyrune.LOGGER.at(Level.WARNING).log("Failed to load NPC level config: " + e.getMessage());
            return buildDefaults();
        }
    }

    public void save(NpcLevelConfig config) {
        try (FileWriter writer = new FileWriter(configFile)) {
            gson.toJson(config, writer);
        } catch (IOException e) {
            Hyrune.LOGGER.at(Level.WARNING).log("Failed to save NPC level config: " + e.getMessage());
        }
    }

    private static NpcLevelConfig buildDefaults() {
        NpcLevelConfig config = new NpcLevelConfig();
        config.getExcludedNpcIds().addAll(DEFAULT_EXCLUDES);
        config.getArchetypeProfiles().addAll(defaultArchetypeProfiles());
        List<NpcLevelConfig.NpcLevelGroup> groups = new ArrayList<>();

        // --- TIER 1 MOBS ---
        // Level 1-5 (The "Rat" phase)
        groups.add(buildGroup("vermin", 3, 3, CombatStyle.MELEE, "DPS", false,
                List.of("rat", "scorpion", "snake", "spider", "bunny", "rabbit", "chicken", "turkey",
                        "mouse", "frog", "gecko", "meerkat", "squirrel" )));

        // Level 6-12 (Introduction to combat)
        groups.add(buildGroup("beasts", 8, 4, CombatStyle.MELEE, "DPS", false,
                List.of("bison", "boar", "cow", "pig", "sheep", "mouflon", "goat", "camel", "deer", "fox", "hyena",
                        "toad", "deer", "moose", "horse", "ram", "skrill", "warthog" )));

        // --- TIER 2 MOBS ---
        // Level 15-22 (Wilderness dangers)
        groups.add(buildGroup("predators", 18, 4, CombatStyle.RANGED, "DPS", false,
                List.of("wolf", "crocodile", "leapard_snow", "tiger_sabertooth" )));

        // Level 20-28 (Humanoid enemies)
        groups.add(buildGroup("outlaws", 24, 4, CombatStyle.MELEE, "DPS", false,
                List.of("goblin", "bandit", "thief", "poacher", "outlander", "trork", "scarak")));

        // --- TIER 3 MOBS ---
        // Level 30-40
        groups.add(buildGroup("undead", 35, 5, CombatStyle.MAGIC, "MAGE", false,
                List.of("zombie", "skeleton", "undead", "ghost", "void", "spawn_void")));

        // Level 40-55 (Heavy hitters)
        groups.add(buildGroup("brutes", 45, 10, CombatStyle.RANGED, "TANK", true, // 'true' for Elite?
                List.of("bear", "trork", "troll", "yeti", "fen_stalker", "emberwulf", "raptor_cave", "rex_cave")));

        // --- BOSSES ---
        groups.add(buildGroup("bosses", 60, 5, CombatStyle.MAGIC, "TANK", true,
                List.of("boss", "chief", "leader", "titan")));

        config.getGroups().addAll(groups);
        return config;
    }

    private static boolean ensureDefaults(NpcLevelConfig config) {
        boolean changed = false;
        if (config.getExcludedNpcIds() == null) {
            config.setExcludedNpcIds(new ArrayList<>());
            changed = true;
        }
        if (config.getGroups() == null) {
            config.setGroups(new ArrayList<>());
            changed = true;
        }
        if (config.getOverrides() == null) {
            config.setOverrides(new ArrayList<>());
            changed = true;
        }
        if (config.getArchetypeProfiles() == null) {
            config.setArchetypeProfiles(new ArrayList<>());
            changed = true;
        }

        List<String> excludes = config.getExcludedNpcIds();
        for (String required : DEFAULT_EXCLUDES) {
            boolean found = false;
            for (String existing : excludes) {
                if (existing != null && existing.equalsIgnoreCase(required)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                excludes.add(required);
                changed = true;
            }
        }

        for (NpcLevelConfig.NpcArchetypeProfile profile : defaultArchetypeProfiles()) {
            if (!hasProfile(config.getArchetypeProfiles(), profile.getId())) {
                config.getArchetypeProfiles().add(profile);
                changed = true;
            }
        }

        String defaultArchetype = normalizeArchetype(config.getDefaultArchetype());
        if (!hasProfile(config.getArchetypeProfiles(), defaultArchetype)) {
            config.setDefaultArchetype(DEFAULT_ARCHETYPE);
            defaultArchetype = DEFAULT_ARCHETYPE;
            changed = true;
        } else if (!defaultArchetype.equals(config.getDefaultArchetype())) {
            config.setDefaultArchetype(defaultArchetype);
            changed = true;
        }

        for (NpcLevelConfig.NpcLevelGroup group : config.getGroups()) {
            if (group == null) {
                continue;
            }
            String normalized = normalizeArchetype(group.getArchetype());
            if (!hasProfile(config.getArchetypeProfiles(), normalized)) {
                String inferred = inferArchetype(group.getId(), defaultArchetype);
                group.setArchetype(inferred);
                changed = true;
            } else if (!normalized.equals(group.getArchetype())) {
                group.setArchetype(normalized);
                changed = true;
            }
        }

        for (NpcLevelConfig.NpcLevelOverride override : config.getOverrides()) {
            if (override == null) {
                continue;
            }
            String normalized = normalizeArchetype(override.getArchetype());
            if (!hasProfile(config.getArchetypeProfiles(), normalized)) {
                String inferred = inferArchetype(override.getTypeId(), defaultArchetype);
                override.setArchetype(inferred);
                changed = true;
            } else if (!normalized.equals(override.getArchetype())) {
                override.setArchetype(normalized);
                changed = true;
            }
        }
        return changed;
    }

    private static NpcLevelConfig.NpcLevelGroup buildGroup(String id,
                                                           int baseLevel,
                                                           int variance,
                                                           CombatStyle weakness,
                                                           String archetype,
                                                           boolean elite,
                                                           List<String> contains) {
        NpcLevelConfig.NpcLevelGroup group = new NpcLevelConfig.NpcLevelGroup();
        NpcLevelConfig.NpcLevelMatch match = new NpcLevelConfig.NpcLevelMatch();
        match.getContains().addAll(contains);
        group.setId(id);
        group.setMatch(match);
        group.setBaseLevel(baseLevel);
        group.setVariance(variance);
        group.setWeakness(weakness.name());
        group.setArchetype(archetype);
        group.setElite(elite);
        return group;
    }

    private static List<NpcLevelConfig.NpcArchetypeProfile> defaultArchetypeProfiles() {
        List<NpcLevelConfig.NpcArchetypeProfile> defaults = new ArrayList<>();
        defaults.add(buildProfile(
            "TANK",
            1.00, 0.012,
            0.06, 0.0036, 0.82,
            0.02, 0.0011, 0.20,
            1.30, 0.0030, 1.95,
            1.08, 0.92, 0.88
        ));
        defaults.add(buildProfile(
            "DPS",
            1.05, 0.018,
            0.02, 0.0022, 0.58,
            0.05, 0.0025, 0.35,
            1.45, 0.0040, 2.25,
            1.00, 1.06, 0.98
        ));
        defaults.add(buildProfile(
            "MAGE",
            1.03, 0.017,
            0.03, 0.0020, 0.62,
            0.04, 0.0022, 0.32,
            1.50, 0.0044, 2.30,
            0.92, 0.98, 1.12
        ));
        return defaults;
    }

    private static NpcLevelConfig.NpcArchetypeProfile buildProfile(String id,
                                                                    double baseDamage,
                                                                    double damagePerLevel,
                                                                    double baseDefenceReduction,
                                                                    double defencePerLevel,
                                                                    double defenceCap,
                                                                    double baseCritChance,
                                                                    double critChancePerLevel,
                                                                    double critChanceCap,
                                                                    double baseCritMultiplier,
                                                                    double critMultiplierPerLevel,
                                                                    double critMultiplierCap,
                                                                    double meleeBias,
                                                                    double rangedBias,
                                                                    double magicBias) {
        NpcLevelConfig.NpcArchetypeProfile profile = new NpcLevelConfig.NpcArchetypeProfile();
        profile.setId(id);
        profile.setBaseDamage(baseDamage);
        profile.setDamagePerLevel(damagePerLevel);
        profile.setBaseDefenceReduction(baseDefenceReduction);
        profile.setDefencePerLevel(defencePerLevel);
        profile.setDefenceCap(defenceCap);
        profile.setBaseCritChance(baseCritChance);
        profile.setCritChancePerLevel(critChancePerLevel);
        profile.setCritChanceCap(critChanceCap);
        profile.setBaseCritMultiplier(baseCritMultiplier);
        profile.setCritMultiplierPerLevel(critMultiplierPerLevel);
        profile.setCritMultiplierCap(critMultiplierCap);
        profile.setMeleeBias(meleeBias);
        profile.setRangedBias(rangedBias);
        profile.setMagicBias(magicBias);
        return profile;
    }

    private static boolean hasProfile(List<NpcLevelConfig.NpcArchetypeProfile> profiles, String profileId) {
        if (profiles == null || profileId == null || profileId.isBlank()) {
            return false;
        }
        for (NpcLevelConfig.NpcArchetypeProfile profile : profiles) {
            if (profile == null || profile.getId() == null) {
                continue;
            }
            if (profile.getId().trim().equalsIgnoreCase(profileId.trim())) {
                return true;
            }
        }
        return false;
    }

    private static String inferArchetype(String source, String fallback) {
        String normalized = source == null ? "" : source.toLowerCase(Locale.ROOT);
        if (normalized.contains("boss")
            || normalized.contains("brute")
            || normalized.contains("tank")
            || normalized.contains("guardian")
            || normalized.contains("juggernaut")) {
            return "TANK";
        }
        if (normalized.contains("undead")
            || normalized.contains("mage")
            || normalized.contains("caster")
            || normalized.contains("shaman")
            || normalized.contains("witch")
            || normalized.contains("void")
            || normalized.contains("sorcerer")) {
            return "MAGE";
        }
        return fallback == null || fallback.isBlank() ? DEFAULT_ARCHETYPE : fallback;
    }

    private static String normalizeArchetype(String archetype) {
        if (archetype == null || archetype.isBlank()) {
            return DEFAULT_ARCHETYPE;
        }
        return archetype.trim().toUpperCase(Locale.ROOT);
    }
}
