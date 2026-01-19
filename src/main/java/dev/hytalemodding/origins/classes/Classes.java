package dev.hytalemodding.origins.classes;


public enum Classes {


    // Tier 1 (Unlock at Level 10)
    WARRIOR("Warrior", null, 10),
    RANGER("Ranger", null, 10),
    MAGE("Mage", null, 10),
    HEALER("Healer", null, 10);

    // Future Tier 2 (Elite) - Commented out for now
    // PALADIN("Paladin", WARRIOR, 40),
    // BERSERKER("Berserker", WARRIOR, 40);

    private final String displayName;
    private final Classes parent; // The class you must BE to switch to this
    private final int requiredLevel; // Level required in the PARENT class

    Classes(String displayName, Classes parent, int requiredLevel) {
        this.displayName = displayName;
        this.parent = parent;
        this.requiredLevel = requiredLevel;
    }

    public String getId() {
        return this.name().toLowerCase(); // e.g., "warrior"
    }

    public String getDisplayName() { return displayName; }
    public Classes getParent() { return parent; }
    public int getRequiredLevel() { return requiredLevel; }

    /**
     * Helper to look up an Enum by string ID (useful for commands)
     */
    public static Classes fromId(String id) {
        for (Classes c : values()) {
            if (c.getId().equalsIgnoreCase(id)) return c;
        }
        return null;
    }
}