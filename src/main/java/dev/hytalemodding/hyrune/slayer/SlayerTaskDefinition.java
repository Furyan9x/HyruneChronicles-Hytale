package dev.hytalemodding.hyrune.slayer;

/**
 * Definition for slayer task.
 */
public class SlayerTaskDefinition {
    private final String id;
    private final String targetGroupId;
    private final int requiredSlayerLevel;
    private final int requiredCombatLevel;

    public SlayerTaskDefinition(String id, String targetGroupId, int requiredSlayerLevel, int requiredCombatLevel) {
        this.id = id;
        this.targetGroupId = targetGroupId;
        this.requiredSlayerLevel = Math.max(1, requiredSlayerLevel);
        this.requiredCombatLevel = Math.max(1, requiredCombatLevel);
    }

    public String getId() {
        return id;
    }

    public String getTargetGroupId() {
        return targetGroupId;
    }

    public int getRequiredSlayerLevel() {
        return requiredSlayerLevel;
    }

    public int getRequiredCombatLevel() {
        return requiredCombatLevel;
    }
}
