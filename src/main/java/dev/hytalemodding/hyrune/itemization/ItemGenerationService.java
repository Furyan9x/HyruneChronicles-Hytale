package dev.hytalemodding.hyrune.itemization;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.ItemUtils;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.hyrune.config.HyruneConfigManager;
import dev.hytalemodding.hyrune.repair.ItemRarity;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;
import java.util.logging.Level;

/**
 * Entry points for creating brand-new rolled item instances.
 * Use this for chest generation, mob/boss loot, and similar systems.
 */
public final class ItemGenerationService {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private ItemGenerationService() {
    }

    public static ItemStack createCraftedItem(String itemId, int quantity) {
        return rollIfEligible(
            new ItemStack(itemId, quantity),
            ItemRollSource.CRAFTED,
            ItemRarityRollModel.GenerationContext.of("create_crafted_item")
        );
    }

    public static ItemStack createDroppedLootItem(String itemId, int quantity) {
        return rollIfEligible(
            new ItemStack(itemId, quantity),
            ItemRollSource.DROPPED,
            ItemRarityRollModel.GenerationContext.of("create_dropped_loot_item")
        );
    }

    public static ItemStack createGeneratedItem(String itemId,
                                                int quantity,
                                                ItemRollSource source,
                                                ItemRarityRollModel.GenerationContext context) {
        return rollIfEligible(new ItemStack(itemId, quantity), source, context);
    }

    public static ItemStack rollIfEligible(ItemStack stack,
                                           ItemRollSource source,
                                           ItemRarityRollModel.GenerationContext context) {
        if (stack == null || stack.isEmpty()) {
            return stack;
        }

        ItemRollSource safeSource = source == null ? ItemRollSource.CRAFTED : source;
        Diagnostics.incrementAttempt(safeSource);

        if (!ItemizationEligibilityService.isEligible(stack)) {
            Diagnostics.incrementIneligible(safeSource);
            return stack;
        }

        ItemInstanceMetadata existing = stack.getFromMetadataOrNull(ItemInstanceMetadata.KEYED_CODEC);
        if (existing != null) {
            long versionBefore = existing.getVersion();
            ItemInstanceMetadata migrated = ItemInstanceMetadataMigration.migrateToCurrent(existing);
            if (migrated.getVersion() != versionBefore) {
                stack = stack.withMetadata(ItemInstanceMetadata.KEYED_CODEC, migrated);
            }
            Diagnostics.incrementAlreadyRolled(safeSource);
            return stack;
        }

        String prefixWord = safeSource == ItemRollSource.CRAFTED
            ? ItemPrefixService.rollRandomPrefix()
            : "";
        ItemStack rolled = ItemRollService.rollNewInstance(
            stack,
            safeSource,
            prefixWord,
            context
        );
        ItemInstanceMetadata metadata = rolled.getFromMetadataOrNull(ItemInstanceMetadata.KEYED_CODEC);
        Diagnostics.incrementRolled(safeSource, metadata == null ? null : metadata.getRarity());

        if (HyruneConfigManager.getConfig().itemizationDebugLogging) {
            LOGGER.at(Level.INFO).log("[Itemization][Gen] src=" + safeSource
                + ", item=" + stack.getItemId()
                + ", rarity=" + (metadata == null ? "unknown" : metadata.getRarity().name())
                + ", reason=" + (context == null ? "none" : context.reason())
                + ", trigger=" + (context == null ? "none" : context.triggerId())
                + ", prof=" + (context == null ? "none" : context.professionSkill())
                + ":" + (context == null ? "0" : context.professionLevel())
                + ", bench=" + (context == null ? "1" : context.benchTier()));
        }

        return rolled;
    }

    public static void dropGeneratedLoot(Ref<EntityStore> sourceRef, Store<EntityStore> store, String itemId, int quantity) {
        dropGeneratedLoot(sourceRef, store, itemId, quantity, ItemRollSource.DROPPED, ItemRarityRollModel.GenerationContext.of("drop_generated_loot"));
    }

    public static void dropGeneratedLoot(Ref<EntityStore> sourceRef,
                                         Store<EntityStore> store,
                                         String itemId,
                                         int quantity,
                                         ItemRollSource source,
                                         ItemRarityRollModel.GenerationContext context) {
        if (sourceRef == null || store == null || itemId == null || itemId.isBlank() || quantity <= 0) {
            return;
        }
        ItemStack rolled = createGeneratedItem(itemId, quantity, source, context);
        ItemUtils.dropItem(sourceRef, rolled, store);
    }

    /**
     * In-memory generation counters for telemetry by source and rarity.
     */
    public static final class Diagnostics {
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

        private Diagnostics() {
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
}

