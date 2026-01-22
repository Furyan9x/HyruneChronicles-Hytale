package dev.hytalemodding.origins.playerdata;

import java.util.UUID;

public class PlayerAttributes {
    private final UUID playerUuid;

    // The 5 Core Attributes (Initialize at 0 or a base value like 5)
    private int strength = 5;
    private int constitution = 5;
    private int intellect = 5;
    private int agility = 5;
    private int wisdom = 5;

    public PlayerAttributes(UUID uuid) {
        this.playerUuid = uuid;
    }

    // --- Core Setters (Used by the Manager) ---
    public void addStrength(int amount) { this.strength += amount; }
    public void addConstitution(int amount) { this.constitution += amount; }
    public void addIntellect(int amount) { this.intellect += amount; }
    public void addAgility(int amount) { this.agility += amount; }
    public void addWisdom(int amount) { this.wisdom += amount; }

    // --- Core Getters ---
    public int getStrength() { return strength; }
    public int getConstitution() { return constitution; }
    public int getIntellect() { return intellect; }
    public int getAgility() { return agility; }
    public int getWisdom() { return wisdom; }

    // --- The "Bonus" System (1 Modifier Per Stat) ---

    // Strength -> Melee Damage
    public float getMeleeDamageBonus() {
        return strength * 1.0f;
    }

    // Constitution -> Max HP
    public float getMaxHealthBonus() {
        return constitution * 1.0f;
    }

    // Intellect -> Max Mana
    public float getMaxManaBonus() {
        return intellect * 1.0f;
    }

    // Agility -> Crit Damage
    public float getCritDamageBonus() {
        return agility * 1.0f;
    }

    // Wisdom -> Mana Regen
    public float getManaRegenBonus() {
        return wisdom * 1.0f;
    }
}