package dev.hytalemodding.hyrune.slayer;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds Slayer task registry from data-driven task/master configs.
 */
public class SlayerDataInitializer {

    public static SlayerTaskRegistry buildRegistry(SlayerTasksConfig tasksConfig, SlayerMastersConfig mastersConfig) {
        SlayerTaskRegistry registry = new SlayerTaskRegistry();
        registerTasks(registry, tasksConfig);
        registerMasters(registry, mastersConfig);
        return registry;
    }

    private static void registerTasks(SlayerTaskRegistry registry, SlayerTasksConfig tasksConfig) {
        if (registry == null || tasksConfig == null || tasksConfig.tasks == null) {
            return;
        }
        for (SlayerTasksConfig.Task task : tasksConfig.tasks) {
            if (task == null || task.id == null || task.id.isBlank()
                || task.targetFamilyId == null || task.targetFamilyId.isBlank()) {
                continue;
            }
            registry.registerTask(new SlayerTaskDefinition(
                task.id,
                task.targetFamilyId,
                Math.max(1, task.requiredSlayerLevel),
                Math.max(1, task.requiredCombatLevel)
            ));
        }
    }

    private static void registerMasters(SlayerTaskRegistry registry, SlayerMastersConfig mastersConfig) {
        if (registry == null || mastersConfig == null || mastersConfig.masters == null) {
            return;
        }
        for (SlayerMastersConfig.Master master : mastersConfig.masters) {
            if (master == null || master.id == null || master.id.isBlank()) {
                continue;
            }
            List<SlayerMasterDefinition.TaskEntry> taskEntries = new ArrayList<>();
            if (master.taskEntries != null) {
                for (SlayerMastersConfig.TaskEntry entry : master.taskEntries) {
                    if (entry == null || entry.taskId == null || entry.taskId.isBlank()) {
                        continue;
                    }
                    taskEntries.add(new SlayerMasterDefinition.TaskEntry(
                        entry.taskId,
                        Math.max(1, entry.weight),
                        Math.max(1, entry.minCount),
                        Math.max(entry.minCount, entry.maxCount)
                    ));
                }
            }
            registry.registerMaster(new SlayerMasterDefinition(
                master.id,
                master.displayName == null || master.displayName.isBlank() ? master.id : master.displayName,
                Math.max(1, master.minSlayerLevel),
                Math.max(1, master.minCombatLevel),
                Math.max(1, master.basePoints),
                Math.max(1, master.streakMilestoneInterval),
                Math.max(0, master.streakMilestoneBonusPoints),
                taskEntries
            ));
        }
    }
}
