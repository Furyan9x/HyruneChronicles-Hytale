package dev.hytalemodding.hyrune.slayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Config file model for Slayer master definitions and weighted pools.
 */
public class SlayerMastersConfig {
    public int schemaVersion = 1;
    public List<Master> masters = new ArrayList<>();

    public static class Master {
        public String id;
        public String displayName;
        public int minSlayerLevel = 1;
        public int minCombatLevel = 1;
        public int basePoints = 5;
        public int streakMilestoneInterval = 10;
        public int streakMilestoneBonusPoints = 15;
        public List<TaskEntry> taskEntries = new ArrayList<>();
    }

    public static class TaskEntry {
        public String taskId;
        public int weight = 1;
        public int minCount = 8;
        public int maxCount = 14;
    }
}

