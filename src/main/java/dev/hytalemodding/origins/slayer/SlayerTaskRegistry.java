package dev.hytalemodding.origins.slayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SlayerTaskRegistry {

    private final List<SlayerTaskTier> tiers = new ArrayList<>();
    private final Random random = new Random();

    public SlayerTaskRegistry() {
    }

    public void registerTier(SlayerTaskTier tier) {
        if (tier != null) {
            tiers.add(tier);
        }
    }

    public SlayerTaskDefinition getRandomTaskForLevel(int slayerLevel) {
        SlayerTaskTier tier = getTierForLevel(slayerLevel);
        if (tier == null || tier.getTasks().isEmpty()) {
            return null;
        }
        int idx = random.nextInt(tier.getTasks().size());
        return tier.getTasks().get(idx);
    }

    public int rollKillCount(SlayerTaskDefinition task) {
        if (task == null) {
            return 0;
        }
        int min = Math.max(1, task.getMinCount());
        int max = Math.max(min, task.getMaxCount());
        return min + random.nextInt(max - min + 1);
    }

    public List<String> validate() {
        List<String> issues = new ArrayList<>();
        if (tiers.isEmpty()) {
            issues.add("No Slayer task tiers registered.");
            return issues;
        }

        for (int i = 0; i < tiers.size(); i++) {
            SlayerTaskTier tier = tiers.get(i);
            if (tier.getMinLevel() > tier.getMaxLevel()) {
                issues.add("Tier has invalid level range: " + tier.getMinLevel() + " > " + tier.getMaxLevel());
            }
            if (tier.getTasks().isEmpty()) {
                issues.add("Tier " + tier.getMinLevel() + "-" + tier.getMaxLevel() + " has no tasks.");
            }
            for (SlayerTaskDefinition task : tier.getTasks()) {
                if (task.getId() == null || task.getId().isBlank()) {
                    issues.add("Tier " + tier.getMinLevel() + "-" + tier.getMaxLevel() + " has a task with empty id.");
                }
                if (task.getTargetNpcTypeId() == null || task.getTargetNpcTypeId().isBlank()) {
                    issues.add("Task " + task.getId() + " has empty target NPC id.");
                }
                if (task.getMinCount() <= 0) {
                    issues.add("Task " + task.getId() + " has non-positive min count.");
                }
                if (task.getMaxCount() < task.getMinCount()) {
                    issues.add("Task " + task.getId() + " has max count lower than min count.");
                }
            }
        }

        for (int i = 0; i < tiers.size(); i++) {
            SlayerTaskTier a = tiers.get(i);
            for (int j = i + 1; j < tiers.size(); j++) {
                SlayerTaskTier b = tiers.get(j);
                if (rangesOverlap(a.getMinLevel(), a.getMaxLevel(), b.getMinLevel(), b.getMaxLevel())) {
                    issues.add("Tier ranges overlap: " + a.getMinLevel() + "-" + a.getMaxLevel()
                        + " with " + b.getMinLevel() + "-" + b.getMaxLevel());
                }
            }
        }

        return issues;
    }

    private SlayerTaskTier getTierForLevel(int slayerLevel) {
        for (SlayerTaskTier tier : tiers) {
            if (tier.isInRange(slayerLevel)) {
                return tier;
            }
        }
        return null;
    }

    private boolean rangesOverlap(int minA, int maxA, int minB, int maxB) {
        return minA <= maxB && minB <= maxA;
    }
}
