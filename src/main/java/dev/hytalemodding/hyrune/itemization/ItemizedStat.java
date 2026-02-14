package dev.hytalemodding.hyrune.itemization;

import java.util.Locale;

/**
 * Specialized itemized stat identifiers for the deep RNG system.
 */
public enum ItemizedStat {
    PHYSICAL_DAMAGE("physical_damage", "Physical Damage", ItemizedStatFamily.OFFENSE_PHYSICAL, false, 8.0),
    MAGICAL_DAMAGE("magical_damage", "Magical Damage", ItemizedStatFamily.OFFENSE_MAGICAL, false, 8.0),
    PHYSICAL_CRIT_CHANCE("physical_crit_chance", "Physical Crit Chance", ItemizedStatFamily.OFFENSE_PHYSICAL, true, 0.05),
    MAGICAL_CRIT_CHANCE("magical_crit_chance", "Magical Crit Chance", ItemizedStatFamily.OFFENSE_MAGICAL, true, 0.05),
    CRIT_BONUS("crit_bonus", "Crit Bonus", ItemizedStatFamily.OFFENSE_PHYSICAL, true, 0.15),
    PHYSICAL_PENETRATION("physical_penetration", "Physical Penetration", ItemizedStatFamily.OFFENSE_PHYSICAL, true, 0.06),
    MAGICAL_PENETRATION("magical_penetration", "Magical Penetration", ItemizedStatFamily.OFFENSE_MAGICAL, true, 0.06),

    PHYSICAL_DEFENCE("physical_defence", "Physical Defence", ItemizedStatFamily.DEFENSE_PHYSICAL, false, 6.0),
    MAGICAL_DEFENCE("magical_defence", "Magical Defence", ItemizedStatFamily.DEFENSE_MAGICAL, false, 6.0),
    BLOCK_EFFICIENCY("block_efficiency", "Block Efficiency", ItemizedStatFamily.DEFENSE_CORE, true, 0.06),
    REFLECT_DAMAGE("reflect_damage", "Reflect Damage", ItemizedStatFamily.DEFENSE_CORE, true, 0.04),
    CRIT_REDUCTION("crit_reduction", "Crit Reduction", ItemizedStatFamily.DEFENSE_CORE, true, 0.05),
    MAX_HP("max_hp", "Max HP", ItemizedStatFamily.DEFENSE_CORE, false, 10.0),
    HP_REGEN("hp_regen", "HP Regen", ItemizedStatFamily.DEFENSE_CORE, false, 0.08),

    HEALING_POWER("healing_power", "Healing Power", ItemizedStatFamily.HEALING, false, 0.10),
    HEALING_CRIT_CHANCE("healing_crit_chance", "Healing Crit Chance", ItemizedStatFamily.HEALING, true, 0.05),
    HEALING_CRIT_BONUS("healing_crit_bonus", "Healing Crit Bonus", ItemizedStatFamily.HEALING, true, 0.10),
    MANA_COST_REDUCTION("mana_cost_reduction", "Mana Cost Reduction", ItemizedStatFamily.HEALING, true, 0.06),

    MANA_REGEN("mana_regen", "Mana Regen", ItemizedStatFamily.UTILITY, false, 0.08),
    STAMINA_REGEN("stamina_regen", "Stamina Regen", ItemizedStatFamily.UTILITY, false, 0.08),
    MOVEMENT_SPEED("movement_speed", "Movement Speed", ItemizedStatFamily.UTILITY, true, 0.04),
    ATTACK_SPEED("attack_speed", "Attack Speed", ItemizedStatFamily.UTILITY, true, 0.04),
    CAST_SPEED("cast_speed", "Cast Speed", ItemizedStatFamily.UTILITY, true, 0.04);

    private final String id;
    private final String displayName;
    private final ItemizedStatFamily family;
    private final boolean percentPrimary;
    private final double flatReference;

    ItemizedStat(String id, String displayName, ItemizedStatFamily family, boolean percentPrimary, double flatReference) {
        this.id = id;
        this.displayName = displayName;
        this.family = family;
        this.percentPrimary = percentPrimary;
        this.flatReference = flatReference;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ItemizedStatFamily getFamily() {
        return family;
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
