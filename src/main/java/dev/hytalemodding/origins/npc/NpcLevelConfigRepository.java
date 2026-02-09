package dev.hytalemodding.origins.npc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.hytalemodding.Origins;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * 
 */
public class NpcLevelConfigRepository {
    private static final String CONFIG_FILE = "npc_levels.json";
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
            Origins.LOGGER.at(Level.WARNING).log("Failed to load NPC level config: " + e.getMessage());
            return buildDefaults();
        }
    }

    public void save(NpcLevelConfig config) {
        try (FileWriter writer = new FileWriter(configFile)) {
            gson.toJson(config, writer);
        } catch (IOException e) {
            Origins.LOGGER.at(Level.WARNING).log("Failed to save NPC level config: " + e.getMessage());
        }
    }

    private static NpcLevelConfig buildDefaults() {
        NpcLevelConfig config = new NpcLevelConfig();
        config.getExcludedNpcIds().addAll(DEFAULT_EXCLUDES);
        List<NpcLevelConfig.NpcLevelGroup> groups = new ArrayList<>();

        // --- TIER 1 MOBS ---
        // Level 1-5 (The "Rat" phase)
        groups.add(buildGroup("vermin", 3, 3, CombatStyle.MELEE, false,
                List.of("rat", "scorpion", "snake", "spider", "bunny", "rabbit", "chicken", "turkey",
                        "mouse", "frog", "gecko", "meerkat", "squirrel" )));

        // Level 6-12 (Introduction to combat)
        groups.add(buildGroup("beasts", 8, 4, CombatStyle.MELEE, false,
                List.of("bison", "boar", "cow", "pig", "sheep", "mouflon", "goat", "camel", "deer", "fox", "hyena",
                        "toad", "deer", "moose", "horse", "ram", "skrill", "warthog" )));

        // --- TIER 2 MOBS ---
        // Level 15-22 (Wilderness dangers)
        groups.add(buildGroup("predators", 18, 4, CombatStyle.RANGED, false,
                List.of("wolf", "crocodile", "leapard_snow", "tiger_sabertooth" )));

        // Level 20-28 (Humanoid enemies)
        groups.add(buildGroup("outlaws", 24, 4, CombatStyle.MELEE, false,
                List.of("goblin", "bandit", "thief", "poacher", "outlander", "trork", "scarak")));

        // --- TIER 3 MOBS ---
        // Level 30-40
        groups.add(buildGroup("undead", 35, 5, CombatStyle.MAGIC, false,
                List.of("zombie", "skeleton", "undead", "ghost", "void", "spawn_void")));

        // Level 40-55 (Heavy hitters)
        groups.add(buildGroup("brutes", 45, 10, CombatStyle.RANGED, true, // 'true' for Elite?
                List.of("bear", "trork", "troll", "yeti", "fen_stalker", "emberwulf", "raptor_cave", "rex_cave")));

        // --- BOSSES ---
        groups.add(buildGroup("bosses", 60, 5, CombatStyle.MAGIC, true,
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
        return changed;
    }

    private static NpcLevelConfig.NpcLevelGroup buildGroup(String id,
                                                           int baseLevel,
                                                           int variance,
                                                           CombatStyle weakness,
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
        group.setElite(elite);
        return group;
    }
}
