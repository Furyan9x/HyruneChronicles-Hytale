package dev.hytalemodding.hyrune.itemization;

import dev.hytalemodding.hyrune.repair.ItemRarity;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

/**
 * In-memory counters for generation telemetry by source and rarity.
 */
public final class ItemGenerationDiagnostics {
    private static final EnumMap<ItemRollSource, LongAdder> ATTEMPTS_BY_SOURCE = new EnumMap<>(ItemRollSource.class);
    private static final EnumMap<ItemRollSource, LongAdder> INELIGIBLE_BY_SOURCE = new EnumMap<>(ItemRollSource.class);
    private static final EnumMap<ItemRollSource, LongAdder> ALREADY_ROLLED_BY_SOURCE = new EnumMap<>(ItemRollSource.class);
    private static final EnumMap<ItemRollSource, LongAdder> ROLLED_BY_SOURCE = new EnumMap<>(ItemRollSource.class);
    private static final EnumMap<ItemRollSource, EnumMap<ItemRarity, LongAdder>> ROLLED_BY_SOURCE_AND_RARITY = new EnumMap<>(ItemRollSource.class);

    static {
        for (ItemRollSource source : ItemRollSource.values()) {
            ATTEMPTS_BY_SOURCE.put(source, new LongAdder());
            INELIGIBLE_BY_SOURCE.put(source, new LongAdder());
            ALREADY_ROLLED_BY_SOURCE.put(source, new LongAdder());
            ROLLED_BY_SOURCE.put(source, new LongAdder());

            EnumMap<ItemRarity, LongAdder> perRarity = new EnumMap<>(ItemRarity.class);
            for (ItemRarity rarity : ItemRarity.values()) {
                perRarity.put(rarity, new LongAdder());
            }
            ROLLED_BY_SOURCE_AND_RARITY.put(source, perRarity);
        }
    }

    private ItemGenerationDiagnostics() {
    }

    public static void incrementAttempt(ItemRollSource source) {
        ATTEMPTS_BY_SOURCE.get(source).increment();
    }

    public static void incrementIneligible(ItemRollSource source) {
        INELIGIBLE_BY_SOURCE.get(source).increment();
    }

    public static void incrementAlreadyRolled(ItemRollSource source) {
        ALREADY_ROLLED_BY_SOURCE.get(source).increment();
    }

    public static void incrementRolled(ItemRollSource source, ItemRarity rarity) {
        ROLLED_BY_SOURCE.get(source).increment();
        if (rarity != null) {
            ROLLED_BY_SOURCE_AND_RARITY.get(source).get(rarity).increment();
        }
    }

    public static Map<ItemRollSource, Long> attemptsBySourceSnapshot() {
        return snapshotFlat(ATTEMPTS_BY_SOURCE);
    }

    public static Map<ItemRollSource, Long> ineligibleBySourceSnapshot() {
        return snapshotFlat(INELIGIBLE_BY_SOURCE);
    }

    public static Map<ItemRollSource, Long> alreadyRolledBySourceSnapshot() {
        return snapshotFlat(ALREADY_ROLLED_BY_SOURCE);
    }

    public static Map<ItemRollSource, Long> rolledBySourceSnapshot() {
        return snapshotFlat(ROLLED_BY_SOURCE);
    }

    public static Map<ItemRollSource, Map<ItemRarity, Long>> rolledBySourceAndRaritySnapshot() {
        EnumMap<ItemRollSource, Map<ItemRarity, Long>> snapshot = new EnumMap<>(ItemRollSource.class);
        for (ItemRollSource source : ItemRollSource.values()) {
            EnumMap<ItemRarity, Long> perRarity = new EnumMap<>(ItemRarity.class);
            for (ItemRarity rarity : ItemRarity.values()) {
                perRarity.put(rarity, ROLLED_BY_SOURCE_AND_RARITY.get(source).get(rarity).sum());
            }
            snapshot.put(source, perRarity);
        }
        return snapshot;
    }

    private static Map<ItemRollSource, Long> snapshotFlat(EnumMap<ItemRollSource, LongAdder> counters) {
        EnumMap<ItemRollSource, Long> snapshot = new EnumMap<>(ItemRollSource.class);
        for (ItemRollSource source : ItemRollSource.values()) {
            snapshot.put(source, counters.get(source).sum());
        }
        return snapshot;
    }
}
