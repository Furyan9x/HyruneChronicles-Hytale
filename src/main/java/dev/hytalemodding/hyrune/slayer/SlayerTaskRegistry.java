package dev.hytalemodding.hyrune.slayer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Registry for Slayer tasks and masters.
 */
public class SlayerTaskRegistry {

    public record AssignmentRoll(SlayerTaskDefinition task, SlayerMasterDefinition master,
                                 SlayerMasterDefinition.TaskEntry taskEntry) {
    }

    private final Map<String, SlayerTaskDefinition> tasksById = new LinkedHashMap<>();
    private final Map<String, SlayerMasterDefinition> mastersById = new LinkedHashMap<>();
    private final Random random = new Random();

    public SlayerTaskRegistry() {
    }

    public void registerTask(SlayerTaskDefinition task) {
        if (task == null || task.getId() == null || task.getId().isBlank()) {
            return;
        }
        tasksById.put(task.getId().toLowerCase(), task);
    }

    public void registerMaster(SlayerMasterDefinition master) {
        if (master == null || master.getId() == null || master.getId().isBlank()) {
            return;
        }
        mastersById.put(master.getId().toLowerCase(), master);
    }

    public SlayerMasterDefinition getMasterById(String masterId) {
        if (masterId == null || masterId.isBlank()) {
            return null;
        }
        return mastersById.get(masterId.toLowerCase());
    }

    public AssignmentRoll getRandomTaskForMaster(String masterId, int slayerLevel, int combatLevel) {
        SlayerMasterDefinition master = getMasterById(masterId);
        if (master == null) {
            return null;
        }
        if (slayerLevel < master.getMinSlayerLevel() || combatLevel < master.getMinCombatLevel()) {
            return null;
        }

        List<WeightedEntry> eligible = new ArrayList<>();
        int totalWeight = 0;
        for (SlayerMasterDefinition.TaskEntry taskEntry : master.getTaskEntries()) {
            if (taskEntry == null || taskEntry.getTaskId() == null || taskEntry.getTaskId().isBlank()) {
                continue;
            }
            SlayerTaskDefinition task = tasksById.get(taskEntry.getTaskId().toLowerCase());
            if (task == null) {
                continue;
            }
            if (slayerLevel < task.getRequiredSlayerLevel() || combatLevel < task.getRequiredCombatLevel()) {
                continue;
            }
            int weight = Math.max(1, taskEntry.getWeight());
            eligible.add(new WeightedEntry(task, master, taskEntry, weight));
            totalWeight += weight;
        }
        if (eligible.isEmpty() || totalWeight <= 0) {
            return null;
        }

        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        for (WeightedEntry entry : eligible) {
            cumulative += entry.weight;
            if (roll < cumulative) {
                return new AssignmentRoll(entry.task, entry.master, entry.taskEntry);
            }
        }
        WeightedEntry fallback = eligible.get(eligible.size() - 1);
        return new AssignmentRoll(fallback.task, fallback.master, fallback.taskEntry);
    }

    public int rollKillCount(SlayerMasterDefinition.TaskEntry taskEntry) {
        if (taskEntry == null) {
            return 0;
        }
        int min = Math.max(1, taskEntry.getMinCount());
        int max = Math.max(min, taskEntry.getMaxCount());
        return min + random.nextInt(max - min + 1);
    }

    public SlayerTaskDefinition getTaskById(String taskId) {
        if (taskId == null || taskId.isBlank()) {
            return null;
        }
        return tasksById.get(taskId.toLowerCase());
    }

    public List<String> validate() {
        List<String> issues = new ArrayList<>();
        if (tasksById.isEmpty()) {
            issues.add("No Slayer tasks registered.");
        }
        if (mastersById.isEmpty()) {
            issues.add("No Slayer masters registered.");
            return issues;
        }

        for (SlayerMasterDefinition master : mastersById.values()) {
            if (master.getTaskEntries().isEmpty()) {
                issues.add("Master " + master.getId() + " has no task entries.");
            }
            int totalWeight = 0;
            for (SlayerMasterDefinition.TaskEntry entry : master.getTaskEntries()) {
                if (entry.getTaskId() == null || entry.getTaskId().isBlank()) {
                    issues.add("Master " + master.getId() + " has a task entry with empty taskId.");
                    continue;
                }
                SlayerTaskDefinition task = tasksById.get(entry.getTaskId().toLowerCase());
                if (task == null) {
                    issues.add("Master " + master.getId() + " references unknown task: " + entry.getTaskId());
                    continue;
                }
                if (task.getTargetGroupId() == null || task.getTargetGroupId().isBlank()) {
                    issues.add("Task " + task.getId() + " has empty target family id.");
                }
                if (entry.getMinCount() <= 0) {
                    issues.add("Task entry " + entry.getTaskId() + " has non-positive min count.");
                }
                if (entry.getMaxCount() < entry.getMinCount()) {
                    issues.add("Task entry " + entry.getTaskId() + " has max count lower than min count.");
                }
                totalWeight += Math.max(1, entry.getWeight());
            }
            if (totalWeight <= 0) {
                issues.add("Master " + master.getId() + " has non-positive total task weight.");
            }
        }

        return issues;
    }

    private record WeightedEntry(SlayerTaskDefinition task, SlayerMasterDefinition master,
                                 SlayerMasterDefinition.TaskEntry taskEntry, int weight) {
    }
}
