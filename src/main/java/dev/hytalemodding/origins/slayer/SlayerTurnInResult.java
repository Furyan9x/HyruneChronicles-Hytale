package dev.hytalemodding.origins.slayer;

public class SlayerTurnInResult {
    private final int pointsAwarded;
    private final long slayerXpAwarded;
    private final boolean itemRewarded;

    public SlayerTurnInResult(int pointsAwarded, long slayerXpAwarded, boolean itemRewarded) {
        this.pointsAwarded = pointsAwarded;
        this.slayerXpAwarded = slayerXpAwarded;
        this.itemRewarded = itemRewarded;
    }

    public int getPointsAwarded() {
        return pointsAwarded;
    }

    public long getSlayerXpAwarded() {
        return slayerXpAwarded;
    }

    public boolean isItemRewarded() {
        return itemRewarded;
    }
}
