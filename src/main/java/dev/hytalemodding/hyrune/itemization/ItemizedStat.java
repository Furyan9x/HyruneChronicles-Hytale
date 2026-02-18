package dev.hytalemodding.hyrune.itemization;

import java.util.Locale;

/**
 * Specialized itemized stat identifiers for the deep RNG system.
 */
public enum ItemizedStat {
    PHYSICAL_DAMAGE("physical_damage", "Physical Damage", false, 8.0),
    MAGICAL_DAMAGE("magical_damage", "Magical Damage", false, 8.0),
    PHYSICAL_CRIT_CHANCE("physical_crit_chance", "Physical Crit Chance", true, 0.05),
    MAGICAL_CRIT_CHANCE("magical_crit_chance", "Magical Crit Chance", true, 0.05),
    CRIT_BONUS("crit_bonus", "Crit Bonus", true, 0.15),
    PHYSICAL_PENETRATION("physical_penetration", "Physical Penetration", true, 0.06),
    MAGICAL_PENETRATION("magical_penetration", "Magical Penetration", true, 0.06),

    PHYSICAL_DEFENCE("physical_defence", "Physical Defence", false, 6.0),
    MAGICAL_DEFENCE("magical_defence", "Magical Defence", false, 6.0),
    BLOCK_EFFICIENCY("block_efficiency", "Block Efficiency", true, 0.06),
    REFLECT_DAMAGE("reflect_damage", "Reflect Damage", true, 0.04),
    CRIT_REDUCTION("crit_reduction", "Crit Reduction", true, 0.05),
    MAX_HP("max_hp", "Max HP", false, 10.0),
    HP_REGEN("hp_regen", "HP Regen", false, 0.08),

    HEALING_POWER("healing_power", "Healing Power", false, 0.10),
    HEALING_CRIT_CHANCE("healing_crit_chance", "Healing Crit Chance", true, 0.05),
    HEALING_CRIT_BONUS("healing_crit_bonus", "Healing Crit Bonus", true, 0.10),
    MANA_COST_REDUCTION("mana_cost_reduction", "Mana Cost Reduction", true, 0.06),

    MANA_REGEN("mana_regen", "Mana Regen", false, 0.08),
    MOVEMENT_SPEED("movement_speed", "Movement Speed", true, 0.04),
    ATTACK_SPEED("attack_speed", "Attack Speed", true, 0.04),
    CAST_SPEED("cast_speed", "Cast Speed", true, 0.04),
    BLOCK_BREAK_SPEED("block_break_speed", "Block Break Speed", true, 0.05),
    RARE_DROP_CHANCE("rare_drop_chance", "Rare Drop Chance", true, 0.04),
    DOUBLE_DROP_CHANCE("double_drop_chance", "Double Drop Chance", true, 0.05);

    private final String id;
    private final String displayName;
    private final boolean percentPrimary;
    private final double flatReference;

    ItemizedStat(String id, String displayName, boolean percentPrimary, double flatReference) {
        this.id = id;
        this.displayName = displayName;
        this.percentPrimary = percentPrimary;
        this.flatReference = flatReference;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isPercentPrimary() {
        return percentPrimary;
    }

    public double getFlatReference() {
        return flatReference;
    }

    public static ItemizedStat fromId(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        for (ItemizedStat stat : values()) {
            if (stat.id.equals(normalized)) {
                return stat;
            }
        }
        return null;
    }
}
