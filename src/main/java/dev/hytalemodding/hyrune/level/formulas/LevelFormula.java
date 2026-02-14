package dev.hytalemodding.hyrune.level.formulas;

/**
 * 
 */
public class LevelFormula {

    // Standard "Runescape-like" curve constants
    // Level 1 = 0 XP
    // Level 99 ~ 13,000,000 XP
    public long getXpForLevel(int level) {
        if (level <= 1) {
            return 0;
        }
        long total = 0;
        for (int i = 1; i < level; i++) {
            total += (long) Math.floor(i + 300 * Math.pow(2, i / 7.0));
        }
        return (long) Math.floor(total / 4.0);
    }

    public int getLevelForXp(long xp) {
        // Cap at 120 to keep the loop bounded.
        for (int i = 1; i < 120; i++) {
            if (xp < getXpForLevel(i + 1)) {
                return i;
            }
        }
        return 120;
    }
}
