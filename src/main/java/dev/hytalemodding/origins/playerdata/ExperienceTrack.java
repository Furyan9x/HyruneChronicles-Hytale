package dev.hytalemodding.origins.playerdata;

public class ExperienceTrack {

    private int level;
    private long xp;

    public ExperienceTrack() {
        this.level = 1; // Default to Level 1
        this.xp = 0;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public long getXp() {
        return xp;
    }

    public void setXp(long xp) {
        this.xp = xp;
    }

    public void addXp(long amount) {
        this.xp += amount;
    }
}
