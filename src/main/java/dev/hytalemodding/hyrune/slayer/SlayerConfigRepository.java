package dev.hytalemodding.hyrune.slayer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import dev.hytalemodding.Hyrune;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

/**
 * Loads/saves Slayer task and master configs from runtime data folder, with bundled fallback.
 */
public class SlayerConfigRepository {
    private static final String TASKS_FILE = "slayer_tasks.json";
    private static final String MASTERS_FILE = "slayer_masters.json";
    private static final String TASKS_RESOURCE = "/hyrune_defaults/slayer_tasks.json";
    private static final String MASTERS_RESOURCE = "/hyrune_defaults/slayer_masters.json";

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File tasksFile;
    private final File mastersFile;

    public SlayerConfigRepository(String dataRootPath) {
        File folder = new File(dataRootPath);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        this.tasksFile = new File(folder, TASKS_FILE);
        this.mastersFile = new File(folder, MASTERS_FILE);
    }

    public SlayerTaskRegistry loadOrCreateRegistry() {
        SlayerTasksConfig tasks = loadOrCreateTasks();
        SlayerMastersConfig masters = loadOrCreateMasters();
        return SlayerDataInitializer.buildRegistry(tasks, masters);
    }

    private SlayerTasksConfig loadOrCreateTasks() {
        if (!tasksFile.exists()) {
            SlayerTasksConfig defaults = loadBundled(TASKS_RESOURCE, SlayerTasksConfig.class);
            if (defaults == null) {
                defaults = new SlayerTasksConfig();
            }
            ensureTaskDefaults(defaults);
            save(tasksFile, defaults);
            return defaults;
        }
        try (FileReader reader = new FileReader(tasksFile, StandardCharsets.UTF_8)) {
            SlayerTasksConfig loaded = gson.fromJson(reader, SlayerTasksConfig.class);
            if (loaded == null) {
                loaded = new SlayerTasksConfig();
            }
            if (ensureTaskDefaults(loaded)) {
                save(tasksFile, loaded);
            }
            return loaded;
        } catch (IOException | JsonParseException e) {
            Hyrune.LOGGER.at(Level.WARNING).log("Failed to load slayer tasks config: " + e.getMessage());
            backupCorrupt(tasksFile, "slayer_tasks");
            SlayerTasksConfig fallback = loadBundled(TASKS_RESOURCE, SlayerTasksConfig.class);
            if (fallback == null) {
                fallback = new SlayerTasksConfig();
            }
            ensureTaskDefaults(fallback);
            save(tasksFile, fallback);
            return fallback;
        }
    }

    private SlayerMastersConfig loadOrCreateMasters() {
        if (!mastersFile.exists()) {
            SlayerMastersConfig defaults = loadBundled(MASTERS_RESOURCE, SlayerMastersConfig.class);
            if (defaults == null) {
                defaults = new SlayerMastersConfig();
            }
            ensureMasterDefaults(defaults);
            save(mastersFile, defaults);
            return defaults;
        }
        try (FileReader reader = new FileReader(mastersFile, StandardCharsets.UTF_8)) {
            SlayerMastersConfig loaded = gson.fromJson(reader, SlayerMastersConfig.class);
            if (loaded == null) {
                loaded = new SlayerMastersConfig();
            }
            if (ensureMasterDefaults(loaded)) {
                save(mastersFile, loaded);
            }
            return loaded;
        } catch (IOException | JsonParseException e) {
            Hyrune.LOGGER.at(Level.WARNING).log("Failed to load slayer masters config: " + e.getMessage());
            backupCorrupt(mastersFile, "slayer_masters");
            SlayerMastersConfig fallback = loadBundled(MASTERS_RESOURCE, SlayerMastersConfig.class);
            if (fallback == null) {
                fallback = new SlayerMastersConfig();
            }
            ensureMasterDefaults(fallback);
            save(mastersFile, fallback);
            return fallback;
        }
    }

    private boolean ensureTaskDefaults(SlayerTasksConfig cfg) {
        boolean changed = false;
        if (cfg.tasks == null) {
            cfg.tasks = new ArrayList<>();
            changed = true;
        }
        for (SlayerTasksConfig.Task task : cfg.tasks) {
            if (task == null) {
                continue;
            }
            if (task.requiredSlayerLevel <= 0) {
                task.requiredSlayerLevel = 1;
                changed = true;
            }
            if (task.requiredCombatLevel <= 0) {
                task.requiredCombatLevel = 1;
                changed = true;
            }
        }
        return changed;
    }

    private boolean ensureMasterDefaults(SlayerMastersConfig cfg) {
        boolean changed = false;
        if (cfg.masters == null) {
            cfg.masters = new ArrayList<>();
            changed = true;
        }
        for (SlayerMastersConfig.Master master : cfg.masters) {
            if (master == null) {
                continue;
            }
            if (master.minSlayerLevel <= 0) {
                master.minSlayerLevel = 1;
                changed = true;
            }
            if (master.minCombatLevel <= 0) {
                master.minCombatLevel = 1;
                changed = true;
            }
            if (master.basePoints <= 0) {
                master.basePoints = 1;
                changed = true;
            }
            if (master.streakMilestoneInterval <= 0) {
                master.streakMilestoneInterval = 10;
                changed = true;
            }
            if (master.streakMilestoneBonusPoints < 0) {
                master.streakMilestoneBonusPoints = 0;
                changed = true;
            }
            if (master.taskEntries == null) {
                master.taskEntries = new ArrayList<>();
                changed = true;
                continue;
            }
            for (SlayerMastersConfig.TaskEntry entry : master.taskEntries) {
                if (entry == null) {
                    continue;
                }
                if (entry.weight <= 0) {
                    entry.weight = 1;
                    changed = true;
                }
                if (entry.minCount <= 0) {
                    entry.minCount = 1;
                    changed = true;
                }
                if (entry.maxCount < entry.minCount) {
                    entry.maxCount = entry.minCount;
                    changed = true;
                }
            }
        }
        return changed;
    }

    private <T> T loadBundled(String resourcePath, Class<T> type) {
        try (InputStream in = SlayerConfigRepository.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                return null;
            }
            try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return gson.fromJson(reader, type);
            }
        } catch (IOException e) {
            Hyrune.LOGGER.at(Level.WARNING).log("Failed to load bundled slayer config " + resourcePath + ": " + e.getMessage());
            return null;
        }
    }

    private void save(File file, Object config) {
        try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            gson.toJson(config, writer);
        } catch (IOException e) {
            Hyrune.LOGGER.at(Level.WARNING).log("Failed to save slayer config " + file.getName() + ": " + e.getMessage());
        }
    }

    private void backupCorrupt(File file, String prefix) {
        if (file == null || !file.exists() || !file.isFile()) {
            return;
        }
        File backup = new File(file.getParentFile(),
            prefix + ".corrupt." + System.currentTimeMillis() + ".json");
        if (file.renameTo(backup)) {
            Hyrune.LOGGER.at(Level.WARNING).log("Backed up corrupt slayer config to: " + backup.getAbsolutePath());
        }
    }
}

