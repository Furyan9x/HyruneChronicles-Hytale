package dev.hytalemodding.origins.npc;

import java.util.Locale;

/**
 * Enumeration of combat style.
 */
public enum CombatStyle {
    MELEE,
    RANGED,
    MAGIC;

    public static CombatStyle fromString(String value, CombatStyle fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (CombatStyle style : values()) {
            if (style.name().equals(normalized)) {
                return style;
            }
        }
        return fallback;
    }
}
