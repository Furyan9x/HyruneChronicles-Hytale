package dev.hytalemodding.hyrune.itemization;

/**
 * Cached resolved itemization stats for one player's currently equipped gear.
 */
public final class PlayerItemizationStats {
    private final long equipmentFingerprint;
    private final EffectiveItemStats heldBaseStats;
    private final EffectiveItemStats heldResolvedStats;
    private final EffectiveItemStats armorBaseStats;
    private final EffectiveItemStats armorResolvedStats;
    private final EffectiveItemStats totalBaseStats;
    private final EffectiveItemStats totalResolvedStats;
    private final ItemizedStatBlock heldResolvedSpecialized;
    private final ItemizedStatBlock armorResolvedSpecialized;
    private final ItemizedStatBlock totalResolvedSpecialized;
    private final double physicalDamageMultiplier;
    private final double magicalDamageMultiplier;
    private final double physicalDefenceReductionBonus;
    private final double magicalDefenceReductionBonus;
    private final double physicalCritChanceBonus;
    private final double magicalCritChanceBonus;
    private final double critBonusMultiplier;
    private final double itemUtilityMoveSpeedBonus;
    private final double itemAttackSpeedBonus;
    private final double itemCastSpeedBonus;
    private final double itemManaRegenBonusPerSecond;
    private final double itemStaminaRegenBonusPerSecond;
    private final double itemHpRegenBonusPerSecond;
    private final double itemMaxHpBonus;
    private final double itemManaCostReduction;
    private final double itemReflectDamage;
    private final long computedAtEpochMs;

    public PlayerItemizationStats(long equipmentFingerprint,
                                  EffectiveItemStats heldBaseStats,
                                  EffectiveItemStats heldResolvedStats,
                                  EffectiveItemStats armorBaseStats,
                                  EffectiveItemStats armorResolvedStats,
                                  EffectiveItemStats totalBaseStats,
                                  EffectiveItemStats totalResolvedStats,
                                  ItemizedStatBlock heldResolvedSpecialized,
                                  ItemizedStatBlock armorResolvedSpecialized,
                                  ItemizedStatBlock totalResolvedSpecialized,
                                  double physicalDamageMultiplier,
                                  double magicalDamageMultiplier,
                                  double physicalDefenceReductionBonus,
                                  double magicalDefenceReductionBonus,
                                  double physicalCritChanceBonus,
                                  double magicalCritChanceBonus,
                                  double critBonusMultiplier,
                                  double itemUtilityMoveSpeedBonus,
                                  double itemAttackSpeedBonus,
                                  double itemCastSpeedBonus,
                                  double itemManaRegenBonusPerSecond,
                                  double itemStaminaRegenBonusPerSecond,
                                  double itemHpRegenBonusPerSecond,
                                  double itemMaxHpBonus,
                                  double itemManaCostReduction,
                                  double itemReflectDamage,
                                  long computedAtEpochMs) {
        this.equipmentFingerprint = equipmentFingerprint;
        this.heldBaseStats = heldBaseStats;
        this.heldResolvedStats = heldResolvedStats;
        this.armorBaseStats = armorBaseStats;
        this.armorResolvedStats = armorResolvedStats;
        this.totalBaseStats = totalBaseStats;
        this.totalResolvedStats = totalResolvedStats;
        this.heldResolvedSpecialized = heldResolvedSpecialized;
        this.armorResolvedSpecialized = armorResolvedSpecialized;
        this.totalResolvedSpecialized = totalResolvedSpecialized;
        this.physicalDamageMultiplier = physicalDamageMultiplier;
        this.magicalDamageMultiplier = magicalDamageMultiplier;
        this.physicalDefenceReductionBonus = physicalDefenceReductionBonus;
        this.magicalDefenceReductionBonus = magicalDefenceReductionBonus;
        this.physicalCritChanceBonus = physicalCritChanceBonus;
        this.magicalCritChanceBonus = magicalCritChanceBonus;
        this.critBonusMultiplier = critBonusMultiplier;
        this.itemUtilityMoveSpeedBonus = itemUtilityMoveSpeedBonus;
        this.itemAttackSpeedBonus = itemAttackSpeedBonus;
        this.itemCastSpeedBonus = itemCastSpeedBonus;
        this.itemManaRegenBonusPerSecond = itemManaRegenBonusPerSecond;
        this.itemStaminaRegenBonusPerSecond = itemStaminaRegenBonusPerSecond;
        this.itemHpRegenBonusPerSecond = itemHpRegenBonusPerSecond;
        this.itemMaxHpBonus = itemMaxHpBonus;
        this.itemManaCostReduction = itemManaCostReduction;
        this.itemReflectDamage = itemReflectDamage;
        this.computedAtEpochMs = computedAtEpochMs;
    }

    public long getEquipmentFingerprint() {
        return equipmentFingerprint;
    }

    public EffectiveItemStats getHeldBaseStats() {
        return heldBaseStats;
    }

    public EffectiveItemStats getHeldResolvedStats() {
        return heldResolvedStats;
    }

    public EffectiveItemStats getArmorBaseStats() {
        return armorBaseStats;
    }

    public EffectiveItemStats getArmorResolvedStats() {
        return armorResolvedStats;
    }

    public EffectiveItemStats getTotalBaseStats() {
        return totalBaseStats;
    }

    public EffectiveItemStats getTotalResolvedStats() {
        return totalResolvedStats;
    }

    public ItemizedStatBlock getHeldResolvedSpecialized() {
        return heldResolvedSpecialized;
    }

    public ItemizedStatBlock getArmorResolvedSpecialized() {
        return armorResolvedSpecialized;
    }

    public ItemizedStatBlock getTotalResolvedSpecialized() {
        return totalResolvedSpecialized;
    }

    public double getPhysicalDamageMultiplier() {
        return physicalDamageMultiplier;
    }

    public double getMagicalDamageMultiplier() {
        return magicalDamageMultiplier;
    }

    public double getPhysicalDefenceReductionBonus() {
        return physicalDefenceReductionBonus;
    }

    public double getMagicalDefenceReductionBonus() {
        return magicalDefenceReductionBonus;
    }

    public double getPhysicalCritChanceBonus() {
        return physicalCritChanceBonus;
    }

    public double getMagicalCritChanceBonus() {
        return magicalCritChanceBonus;
    }

    public double getCritBonusMultiplier() {
        return critBonusMultiplier;
    }

    public double getItemUtilityMoveSpeedBonus() {
        return itemUtilityMoveSpeedBonus;
    }

    public double getItemAttackSpeedBonus() {
        return itemAttackSpeedBonus;
    }

    public double getItemCastSpeedBonus() {
        return itemCastSpeedBonus;
    }

    public double getItemManaRegenBonusPerSecond() {
        return itemManaRegenBonusPerSecond;
    }

    public double getItemStaminaRegenBonusPerSecond() {
        return itemStaminaRegenBonusPerSecond;
    }

    public double getItemHpRegenBonusPerSecond() {
        return itemHpRegenBonusPerSecond;
    }

    public double getItemMaxHpBonus() {
        return itemMaxHpBonus;
    }

    public double getItemManaCostReduction() {
        return itemManaCostReduction;
    }

    public double getItemReflectDamage() {
        return itemReflectDamage;
    }

    public long getComputedAtEpochMs() {
        return computedAtEpochMs;
    }
}
