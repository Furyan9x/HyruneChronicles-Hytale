package dev.hytalemodding.hyrune.itemization;

import java.util.Locale;

/**
 * Resolves item IDs to itemization archetypes used by roll pools and base stat profiles.
 */
public final class ItemArchetypeResolver {
    private ItemArchetypeResolver() {
    }

    public static ItemArchetype resolve(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return ItemArchetype.GENERIC;
        }
        String id = itemId.toLowerCase(Locale.ROOT);

        if (id.startsWith("weapon_")) {
            if (isMagicWeaponId(id)) {
                return ItemArchetype.WEAPON_MAGIC;
            }
            if (isRangedWeaponId(id)) {
                return ItemArchetype.WEAPON_RANGED;
            }
            return ItemArchetype.WEAPON_MELEE;
        }
        if (id.startsWith("armor_")) {
            if (containsAny(id, "robe", "cloth", "mage", "wizard")) {
                return ItemArchetype.ARMOR_MAGIC;
            }
            if (containsAny(id, "leather", "light")) {
                return ItemArchetype.ARMOR_LIGHT;
            }
            return ItemArchetype.ARMOR_HEAVY;
        }
        if (id.startsWith("tool_")) {
            return ItemArchetype.TOOL;
        }
        return ItemArchetype.GENERIC;
    }

    private static boolean isRangedWeaponId(String itemId) {
        return containsAny(itemId, "shortbow", "longbow", "crossbow", "gun", "sling");
    }

    private static boolean isMagicWeaponId(String itemId) {
        return containsAny(itemId, "staff", "wand", "spellbook", "scepter", "grimoire");
    }

    private static boolean containsAny(String value, String... terms) {
        if (value == null || terms == null) {
            return false;
        }
        for (String term : terms) {
            if (term != null && value.contains(term)) {
                return true;
            }
        }
        return false;
    }
}
