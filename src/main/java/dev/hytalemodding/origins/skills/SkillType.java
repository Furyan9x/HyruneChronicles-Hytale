package dev.hytalemodding.origins.skills;

/**
 * Enumeration of skill types.
 */
public enum SkillType {
    // Combat
    ATTACK("Attack", "Determines weapon accuracy and tier."),
    STRENGTH("Strength", "Increases melee damage and max hit."),
    DEFENCE("Defence", "Increases damage mitigation and armor tier."),
    RANGED("Ranged", "Increases bow/crossbow damage and accuracy."),
    MAGIC("Magic", "Increases spell damage and mana pool."),
    RESTORATION("Restoration", "Increases healing potency and mana regen."), // Your "Healer" equivalent
    CONSTITUTION("Constitution", "Determines maximum health points."),
    SLAYER("Slayer", "Temporary Description."),
    //NECROMANCY - necromancy from Rs3 duh

    // Gathering (Examples for the grid)
    MINING("Mining", "Extract ores from rocks."),
    WOODCUTTING("Woodcutting", "Chop trees for logs."),
    FISHING("Fishing", "Catch fish from water sources."),
    FARMING("Farming", "Plant and harvest crops for food."),
    //ARCHAEOLOGY - source of gems for RNG crafting, other artifacts.
    // Artisan / Crafting
    SMELTING("Smelting", "Process materials in furnaces and salvage stations."),
    ARCANE_ENGINEERING("Arcane Engineering", "Craft magical devices at the Arcanist's Workbench."),
    ARMORSMITHING("Armorsmithing", "Forge and repair armor at the Armorer's Workbench."),
    WEAPONSMITHING("Weaponsmithing", "Forge weapons at the Blacksmith's Anvil."),
    COOKING("Cooking", "Prepare meals at campfires and stoves."),
    LEATHERWORKING("Leatherworking", "Cure hides at the Tanning Rack."),
    ARCHITECT("Architect", "Build structures at the Builder and Furniture workbenches."),
    ALCHEMY("Alchemy", "Brew potions and transmutations at the Alchemist's Workbench."),
    //FLETCHING - bows/crossbows/arrows/ranged weapon crafting.
    //HANDICRAFTING - Jewelry and accessories

    //Misc
    AGILITY("Agility", "Increased movement speed and jump height.");
    //INVENTION - Salvaging gear, adding sockets to gear, building and using contraptions(from some technology mod eventually)
    //COMMERCE - trading, buying/selling items, trade pack value, etc.

    private final String displayName;
    private final String description;

    SkillType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Returns a two-letter code for UI fallback icons.
     */
    public String getIconCode() {
        return displayName.substring(0, 2).toUpperCase();
    }
}
