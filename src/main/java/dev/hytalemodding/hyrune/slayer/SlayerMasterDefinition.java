package dev.hytalemodding.hyrune.slayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines a slayer master and its weighted task pool.
 */
public class SlayerMasterDefinition {
    private final String id;
    private final String displayName;
    private final int minSlayerLevel;
    private final int minCombatLevel;
    private final int basePoints;
    private final int streakMilestoneInterval;
    private final int streakMilestoneBonusPoints;
    private final List<TaskEntry> taskEntries;

    public SlayerMasterDefinition(String id,
                                  String displayName,
                                  int minSlayerLevel,
                                  int minCombatLevel,
                                  int basePoints,
                                  int streakMilestoneInterval,
                                  int streakMilestoneBonusPoints,
                                  List<TaskEntry> taskEntries) {
        this.id = id;
        this.displayName = displayName;
        this.minSlayerLevel = Math.max(1, minSlayerLevel);
        this.minCombatLevel = Math.max(1, minCombatLevel);
        this.basePoints = Math.max(1, basePoints);
        this.streakMilestoneInterval = Math.max(1, streakMilestoneInterval);
        this.streakMilestoneBonusPoints = Math.max(0, streakMilestoneBonusPoints);
        this.taskEntries = taskEntries == null ? List.of() : new ArrayList<>(taskEntries);
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMinSlayerLevel() {
        return minSlayerLevel;
    }

    public int getMinCombatLevel() {
        return minCombatLevel;
    }

    public int getBasePoints() {
        return basePoints;
    }

    public int getStreakMilestoneInterval() {
        return streakMilestoneInterval;
    }

    public int getStreakMilestoneBonusPoints() {
        return streakMilestoneBonusPoints;
    }

    public List<TaskEntry> getTaskEntries() {
        return taskEntries;
    }

    public static class TaskEntry {
        private final String taskId;
        private final int weight;
        private final int minCount;
        private final int maxCount;

        public TaskEntry(String taskId, int weight, int minCount, int maxCount) {
            this.taskId = taskId;
            this.weight = Math.max(1, weight);
            this.minCount = Math.max(1, minCount);
            this.maxCount = Math.max(this.minCount, maxCount);
        }

        public String getTaskId() {
            return taskId;
        }

        public int getWeight() {
            return weight;
        }

        public int getMinCount() {
            return minCount;
        }

        public int getMaxCount() {
            return maxCount;
        }
    }
}

