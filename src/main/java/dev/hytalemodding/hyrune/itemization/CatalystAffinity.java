package dev.hytalemodding.hyrune.itemization;

/**
 * Catalyst affinities used for item stat specialization.
 */
public enum CatalystAffinity {
    NONE,
    WATER,
    FIRE,
    AIR,
    EARTH;

    public static CatalystAffinity fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return NONE;
        }
        try {
            return CatalystAffinity.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return NONE;
        }
    }
}
