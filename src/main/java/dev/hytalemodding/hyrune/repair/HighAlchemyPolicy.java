package dev.hytalemodding.hyrune.repair;

import dev.hytalemodding.hyrune.itemization.ItemizationSpecializedStatConfigHelper;

/**
 * Baseline high-alchemy valuation model for item sink loops.
 */
public final class HighAlchemyPolicy {
    public static final String COIN_ITEM_ID = "copper_coins";

    private HighAlchemyPolicy() {
    }

    public static int estimateCoinReturn(String itemId, ItemRarity rarity) {
        ItemRarity safeRarity = rarity == null ? ItemRarity.COMMON : rarity;
        double tierScalar = Math.max(1.0, ItemizationSpecializedStatConfigHelper.tierScalar(itemId));
        double tierMultiplier = 0.75 + (0.25 * Math.sqrt(tierScalar));
        double rarityBase = baseByRarity(safeRarity);
        return Math.max(1, (int) Math.round(rarityBase * tierMultiplier));
    }

    private static double baseByRarity(ItemRarity rarity) {
        return switch (rarity) {
            case COMMON -> 20.0;
            case UNCOMMON -> 35.0;
            case RARE -> 60.0;
            case EPIC, VOCATIONAL -> 95.0;
            case LEGENDARY -> 145.0;
            case MYTHIC -> 220.0;
        };
    }
}

