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

public class NpcLevelConfigRepository {
    private static final String CONFIG_FILE = "npc_levels.json";
    private static final List<String> DEFAULT_EXCLUDES = List.of(
        "Tier1_Slayer_Master",
        "Tier1_Slayer_Master_Static"
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

        groups.add(buildGroup("critters", 2, 2, CombatStyle.MELEE, false,
            List.of("rat", "rabbit", "chicken", "duck", "frog")));
        groups.add(buildGroup("beasts", 6, 4, CombatStyle.RANGED, false,
            List.of("wolf", "bear", "boar", "spider", "bat")));
        groups.add(buildGroup("undead", 10, 3, CombatStyle.MAGIC, false,
            List.of("undead", "skeleton", "zombie", "ghost")));
        groups.add(buildGroup("bandits", 12, 2, CombatStyle.MELEE, false,
            List.of("bandit", "raider", "brigand")));
        groups.add(buildGroup("elites", 20, 4, CombatStyle.MAGIC, true,
            List.of("elite", "boss", "champion")));

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
