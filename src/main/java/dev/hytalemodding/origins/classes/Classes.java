package dev.hytalemodding.origins.classes;


/**
 * Enumeration of available character classes in the Origins mod.
 */
public enum Classes {

    /** Tier 1 Classes (Unlock at Level 10) */
    WARRIOR("Warrior", null, 10),
    RANGER("Ranger", null, 10),
    MAGE("Mage", null, 10),
    HEALER("Healer", null, 10);

    // Future Tier 2 (Elite)
    // PALADIN("Paladin", WARRIOR, 40),
    // BERSERKER("Berserker", WARRIOR, 40);

    private final String displayName;
    private final Classes parent;      // The class required to switch to this one
    private final int requiredLevel;   // Level required in the parent class

    Classes(String displayName, Classes parent, int requiredLevel) {
        this.displayName = displayName;
        this.parent = parent;
        this.requiredLevel = requiredLevel;
    }

    /**
     * @return The lowercase string identifier for the class.
     */
    public String getId() {
        return this.name().toLowerCase();
    }

    public String getDisplayName() {
        return displayName;
    }

    public Classes getParent() {
        return parent;
    }

    public int getRequiredLevel() {
        return requiredLevel;
    }

    /**
     * Resolves a class enum from its string ID.
     * 
     * @param id The class ID (case-insensitive).
     * @return The matching Classes enum, or null if not found.
     */
    public static Classes fromId(String id) {
        for (Classes c : values()) {
            if (c.getId().equalsIgnoreCase(id)) return c;
        }
        return null;
    }
}