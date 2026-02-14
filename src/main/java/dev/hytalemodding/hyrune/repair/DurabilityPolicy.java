package dev.hytalemodding.hyrune.repair;

/**
 * Shared durability policy for cap and future max durability upgrades/restores.
 */
public final class DurabilityPolicy {
    public static final double RARITY_STEP_MAX_DURABILITY_BONUS = 0.10;
    public static final double HARD_MAX_DURABILITY_MULTIPLIER = 2.00;

    // Placeholder IDs for future max-durability systems.
    public static final String MAX_DURABILITY_RESTORE_ITEM_ID = "Item_MaxDurabilityRestore_Placeholder";
    public static final String MAX_DURABILITY_INCREASE_ITEM_ID = "Item_MaxDurabilityIncrease_Placeholder";

    private DurabilityPolicy() {
    }

    public static double getRarityAdjustedBaseMax(double baseMaxDurability, ItemRarity rarity) {
        if (baseMaxDurability <= 0d) {
            return 0d;
        }
        ItemRarity effectiveRarity = rarity == null ? ItemRarity.COMMON : rarity;
        double bonus = effectiveRarity.ordinal() * RARITY_STEP_MAX_DURABILITY_BONUS;
        return baseMaxDurability * (1.0 + bonus);
    }

    public static double getHardCap(double baseMaxDurability) {
        return Math.max(0d, baseMaxDurability) * HARD_MAX_DURABILITY_MULTIPLIER;
    }
}
