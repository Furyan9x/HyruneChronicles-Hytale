package dev.hytalemodding.hyrune.itemization;

/**
 * Roll-pool archetypes for slot/item-specific stat generation.
 */
public enum ItemArchetype {
    WEAPON_SHIELD("weapon_shield"),
    WEAPON_MELEE("weapon_melee"),
    WEAPON_RANGED("weapon_ranged"),
    WEAPON_MAGIC("weapon_magic"),
    ARMOR_HEAVY("armor_heavy"),
    ARMOR_LIGHT("armor_light"),
    ARMOR_MAGIC("armor_magic"),
    TOOL("tool"),
    GENERIC("generic");

    private final String id;

    ItemArchetype(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
