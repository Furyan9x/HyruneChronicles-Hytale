package dev.hytalemodding.origins.classes;

public enum StatGrowth {
    // Format: Strength, Constitution, Intellect, Agility, Wisdom
    WARRIOR(3, 2, 0, 1, 0),
    MAGE(0, 1, 3, 0, 2),
    RANGER(0, 2, 0, 3, 1),
    CLERIC(0, 1, 2, 0, 3);

    private final int strGrowth;
    private final int conGrowth;
    private final int intGrowth;
    private final int agiGrowth;
    private final int wisGrowth;

    StatGrowth(int str, int con, int intel, int agi, int wis) {
        this.strGrowth = str;
        this.conGrowth = con;
        this.intGrowth = intel;
        this.agiGrowth = agi;
        this.wisGrowth = wis;
    }

    public int getStrGrowth() { return strGrowth; }
    public int getConGrowth() { return conGrowth; }
    public int getIntGrowth() { return intGrowth; }
    public int getAgiGrowth() { return agiGrowth; }
    public int getWisGrowth() { return wisGrowth; }
}