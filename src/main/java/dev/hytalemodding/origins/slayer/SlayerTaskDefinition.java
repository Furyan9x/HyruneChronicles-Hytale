package dev.hytalemodding.origins.slayer;

public class SlayerTaskDefinition {
    private final String id;
    private final String targetNpcTypeId;
    private final int minCount;
    private final int maxCount;

    public SlayerTaskDefinition(String id, String targetNpcTypeId, int minCount, int maxCount) {
        this.id = id;
        this.targetNpcTypeId = targetNpcTypeId;
        this.minCount = minCount;
        this.maxCount = maxCount;
    }

    public String getId() {
        return id;
    }

    public String getTargetNpcTypeId() {
        return targetNpcTypeId;
    }

    public int getMinCount() {
        return minCount;
    }

    public int getMaxCount() {
        return maxCount;
    }
}
