package dev.hytalemodding.hyrune.itemization;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.ItemUtils;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.hyrune.config.HyruneConfigManager;

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
            ItemGenerationContext.of("create_crafted_item")
        );
    }

    public static ItemStack createDroppedLootItem(String itemId, int quantity) {
        return rollIfEligible(
            new ItemStack(itemId, quantity),
            ItemRollSource.DROPPED,
            ItemGenerationContext.of("create_dropped_loot_item")
        );
    }

    public static ItemStack rollIfEligible(ItemStack stack, ItemRollSource source, ItemGenerationContext context) {
        if (stack == null || stack.isEmpty()) {
            return stack;
        }

        ItemRollSource safeSource = source == null ? ItemRollSource.CRAFTED : source;
        ItemGenerationDiagnostics.incrementAttempt(safeSource);

        if (!ItemizationEligibilityService.isEligible(stack)) {
            ItemGenerationDiagnostics.incrementIneligible(safeSource);
            return stack;
        }

        ItemInstanceMetadata existing = stack.getFromMetadataOrNull(ItemInstanceMetadata.KEYED_CODEC);
        if (existing != null) {
            long versionBefore = existing.getVersion();
            ItemInstanceMetadata migrated = ItemInstanceMetadataMigration.migrateToCurrent(existing);
            if (migrated.getVersion() != versionBefore) {
                stack = stack.withMetadata(ItemInstanceMetadata.KEYED_CODEC, migrated);
            }
            ItemGenerationDiagnostics.incrementAlreadyRolled(safeSource);
            return stack;
        }

        ItemStack rolled = ItemRollService.rollNewInstance(stack, safeSource, CatalystAffinity.NONE, context);
        ItemInstanceMetadata metadata = rolled.getFromMetadataOrNull(ItemInstanceMetadata.KEYED_CODEC);
        ItemGenerationDiagnostics.incrementRolled(safeSource, metadata == null ? null : metadata.getRarity());

        if (HyruneConfigManager.getConfig().itemizationDebugLogging) {
            LOGGER.at(Level.INFO).log("[Itemization] Generated item source=" + safeSource
                + ", item=" + stack.getItemId()
                + ", rarity=" + (metadata == null ? "unknown" : metadata.getRarity().name())
                + ", contextReason=" + (context == null ? "none" : context.reason())
                + ", actorId=" + (context == null ? "none" : context.actorId())
                + ", triggerId=" + (context == null ? "none" : context.triggerId())
                + ", professionSkill=" + (context == null ? "none" : context.professionSkill())
                + ", professionLevel=" + (context == null ? "none" : context.professionLevel())
                + ", benchTier=" + (context == null ? "none" : context.benchTier())
                + ", attemptsBySource=" + ItemGenerationDiagnostics.attemptsBySourceSnapshot()
                + ", rolledBySource=" + ItemGenerationDiagnostics.rolledBySourceSnapshot());
        }

        return rolled;
    }

    public static void dropGeneratedLoot(Ref<EntityStore> sourceRef, Store<EntityStore> store, String itemId, int quantity) {
        if (sourceRef == null || store == null || itemId == null || itemId.isBlank() || quantity <= 0) {
            return;
        }
        ItemStack rolled = createDroppedLootItem(itemId, quantity);
        ItemUtils.dropItem(sourceRef, rolled, store);
    }
}
