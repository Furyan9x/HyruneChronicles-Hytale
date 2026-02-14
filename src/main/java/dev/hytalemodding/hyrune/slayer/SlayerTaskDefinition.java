package dev.hytalemodding.hyrune.slayer;

/**
 * Definition for slayer task.
 */
public class SlayerTaskDefinition {
    private final String id;
    private final String targetGroupId;
    private final int minCount;
    private final int maxCount;

    public SlayerTaskDefinition(String id, String targetGroupId, int minCount, int maxCount) {
        this.id = id;
        this.targetGroupId = targetGroupId;
        this.minCount = minCount;
        this.maxCount = maxCount;
    }

    public String getId() {
        return id;
    }

    public String getTargetGroupId() {
        return targetGroupId;
    }

    public int getMinCount() {
        return minCount;
    }

    public int getMaxCount() {
        return maxCount;
    }
}
