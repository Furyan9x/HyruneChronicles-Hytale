package dev.hytalemodding.hyrune.npc;

import java.util.Locale;

/**
 * Tiered NPC threat rank. Used for additive level offsets and multiplicative stat scaling.
 */
public enum NpcRank {
    NORMAL,
    STRONG,
    ELITE,
    HERO,
    WORLD_BOSS;

    public static NpcRank fromString(String raw, NpcRank fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return NpcRank.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
