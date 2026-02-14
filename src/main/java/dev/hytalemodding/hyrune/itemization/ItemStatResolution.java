package dev.hytalemodding.hyrune.itemization;

/**
 * Detailed specialized stat resolution output for one item instance.
 */
public final class ItemStatResolution {
    private final String itemId;
    private final ItemArchetype archetype;
    private final ItemizedStatBlock baseSpecializedStats;
    private final ItemizedStatBlock resolvedSpecializedStats;
    private final EffectiveItemStats baseStats;
    private final EffectiveItemStats resolvedStats;
    private final double rarityScalar;
    private final double droppedPenaltyKeep;

    public ItemStatResolution(String itemId,
                              ItemArchetype archetype,
                              ItemizedStatBlock baseSpecializedStats,
                              ItemizedStatBlock resolvedSpecializedStats,
                              EffectiveItemStats baseStats,
                              EffectiveItemStats resolvedStats,
                              double rarityScalar,
                              double droppedPenaltyKeep) {
        this.itemId = itemId;
        this.archetype = archetype;
        this.baseSpecializedStats = baseSpecializedStats;
        this.resolvedSpecializedStats = resolvedSpecializedStats;
        this.baseStats = baseStats;
        this.resolvedStats = resolvedStats;
        this.rarityScalar = rarityScalar;
        this.droppedPenaltyKeep = droppedPenaltyKeep;
    }

    public String getItemId() {
        return itemId;
    }

    public ItemArchetype getArchetype() {
        return archetype;
    }

    public ItemizedStatBlock getBaseSpecializedStats() {
        return baseSpecializedStats;
    }

    public ItemizedStatBlock getResolvedSpecializedStats() {
        return resolvedSpecializedStats;
    }

    public EffectiveItemStats getBaseStats() {
        return baseStats;
    }

    public EffectiveItemStats getResolvedStats() {
        return resolvedStats;
    }

    public double getRarityScalar() {
        return rarityScalar;
    }

    public double getDroppedPenaltyKeep() {
        return droppedPenaltyKeep;
    }
}
