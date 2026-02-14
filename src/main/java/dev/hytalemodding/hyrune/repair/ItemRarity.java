package dev.hytalemodding.hyrune.repair;

import java.util.Locale;

/**
 * Item rarity scale used by repair and durability balancing.
 */
public enum ItemRarity {
    COMMON(1.00, 0.00),
    UNCOMMON(1.15, 0.10),
    RARE(1.30, 0.20),
    EPIC(1.50, 0.30),
    VOCATIONAL(1.50, 0.30),
    LEGENDARY(1.75, 0.40),
    MYTHIC(2.00, 0.50);

    private final double repairCostMultiplier;
    private final double maxDurabilityBonus;

    ItemRarity(double repairCostMultiplier, double maxDurabilityBonus) {
        this.repairCostMultiplier = repairCostMultiplier;
        this.maxDurabilityBonus = maxDurabilityBonus;
    }

    public double getRepairCostMultiplier() {
        return repairCostMultiplier;
    }

    public double getMaxDurabilityBonus() {
        return maxDurabilityBonus;
    }

    public static ItemRarity fromItemId(String itemId) {
        if (itemId == null) {
            return COMMON;
        }
        String normalized = itemId.toLowerCase(Locale.ROOT);
        if (normalized.contains("mythic")) {
            return MYTHIC;
        }
        if (normalized.contains("legendary")) {
            return LEGENDARY;
        }
        if (normalized.contains("epic")) {
            return EPIC;
        }
        if (normalized.contains("vocational")) {
            return VOCATIONAL;
        }
        if (normalized.contains("rare")) {
            return RARE;
        }
        if (normalized.contains("uncommon")) {
            return UNCOMMON;
        }
        if (normalized.contains("common")) {
            return COMMON;
        }
        return COMMON;
    }
}
