package dev.hytalemodding.origins.playerdata;

public class ExperienceTrack {
    private long xp;
    private int level;

    public ExperienceTrack() {
        this.xp = 0;
        this.level = 1; // Everything starts at level 1
    }

    // Constructor for loading specific values
    public ExperienceTrack(long xp, int level) {
        this.xp = xp;
        this.level = level;
    }

    public void addXp(long amount) {
        this.xp += amount;
    }

    public long getXp() { return xp; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
}