package dev.hytalemodding.origins.skills;



public enum SkillType {
    // Combat
    ATTACK("Attack", "Determines weapon accuracy and tier."),
    STRENGTH("Strength", "Increases melee damage and max hit."),
    DEFENCE("Defence", "Increases damage mitigation and armor tier."),
    RANGED("Ranged", "Increases bow/crossbow damage and accuracy."),
    MAGIC("Magic", "Increases spell damage and mana pool."),
    DIVINITY("Spirit", "Increases healing potency and mana regen."), // Your "Healer" equivalent
    CONSTITUTION("Constitution", "Determines maximum health points."),

    // Gathering (Examples for the grid)
    MINING("Mining", "Extract ores from rocks."),
    WOODCUTTING("Woodcutting", "Chop trees for logs."),
    FISHING("Fishing", "Catch fish from water sources."),

    // Artisan
    SMITHING("Smithing", "Forge weapons and armor."),
    COOKING("Cooking", "Prepare food for healing."),
    ALCHEMY("Alchemy", "Brew potions and transmutations.");

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

    // Helper to get a 2-letter code for the UI icon (e.g. "AT", "ST")
    public String getIconCode() {
        return displayName.substring(0, 2).toUpperCase();
    }
}