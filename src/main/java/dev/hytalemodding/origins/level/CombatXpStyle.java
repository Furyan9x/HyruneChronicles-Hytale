package dev.hytalemodding.origins.level;

import java.util.Locale;

public enum CombatXpStyle {
    ATTACK("Attack"),
    STRENGTH("Strength"),
    DEFENCE("Defence"),
    SHARED("Shared"),
    RANGED("Ranged"),
    MAGIC("Magic");

    private final String displayName;

    CombatXpStyle(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static CombatXpStyle fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return CombatXpStyle.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
