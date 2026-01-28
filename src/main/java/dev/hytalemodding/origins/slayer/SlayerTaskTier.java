package dev.hytalemodding.origins.slayer;

import java.util.List;

public class SlayerTaskTier {
    private final int minLevel;
    private final int maxLevel;
    private final List<SlayerTaskDefinition> tasks;

    public SlayerTaskTier(int minLevel, int maxLevel, List<SlayerTaskDefinition> tasks) {
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.tasks = tasks;
    }

    public int getMinLevel() {
        return minLevel;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public List<SlayerTaskDefinition> getTasks() {
        return tasks;
    }

    public boolean isInRange(int level) {
        return level >= minLevel && level <= maxLevel;
    }
}
