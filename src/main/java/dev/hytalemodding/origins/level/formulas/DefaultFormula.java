package dev.hytalemodding.origins.level.formulas;

public class DefaultFormula implements LevelFormula {

    // Example: Level = sqrt(XP / 100)
    // Meaning: Level 1 = 100 XP, Level 2 = 400 XP, Level 3 = 900 XP...

    @Override
    public int getLevelForXp(long xp) {
        if (xp <= 0) return 1;
        return (int) Math.sqrt(xp / 100.0) + 1;
    }

    @Override
    public long getXpForLevel(int level) {
        if (level <= 1) return 0;
        return (long) (Math.pow(level - 1, 2) * 100);
    }
}