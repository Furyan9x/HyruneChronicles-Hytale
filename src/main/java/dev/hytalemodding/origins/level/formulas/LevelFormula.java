package dev.hytalemodding.origins.level.formulas;

public interface LevelFormula {
    int getLevelForXp(long xp);
    long getXpForLevel(int level);
}