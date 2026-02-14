package dev.hytalemodding.hyrune.itemization;

import java.util.Locale;
import java.util.Map;

/**
 * Placeholder catalyst definitions.
 * Replace item IDs here when your final catalyst items are ready.
 */
public final class CatalystCatalog {
    private static final Map<String, CatalystAffinity> CATALYST_BY_ITEM_ID = Map.of(
        "ingredient_fire_essence", CatalystAffinity.FIRE,
        "ingredient_water_essence", CatalystAffinity.WATER,
        "ingredient_lightning_essence", CatalystAffinity.AIR,
        "ingredient_life_essence", CatalystAffinity.EARTH
    );

    private CatalystCatalog() {
    }

    public static CatalystAffinity resolveAffinity(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return CatalystAffinity.NONE;
        }
        return CATALYST_BY_ITEM_ID.getOrDefault(itemId.trim().toLowerCase(Locale.ROOT), CatalystAffinity.NONE);
    }

    public static boolean isCatalystItem(String itemId) {
        return resolveAffinity(itemId) != CatalystAffinity.NONE;
    }
}

