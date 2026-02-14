package dev.hytalemodding.hyrune.itemization;

import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import dev.hytalemodding.hyrune.config.HyruneConfig;
import dev.hytalemodding.hyrune.config.HyruneConfigManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Applies catalyst affinity to rolled item instances.
 */
public final class CatalystApplicationService {
    private CatalystApplicationService() {
    }

    public static Result applyCatalystToSlot(Inventory inventory,
                                             short targetSlot,
                                             String catalystItemId,
                                             CatalystAffinity catalystAffinity) {
        // Stage 1: validate inventory/target/catalyst inputs.
        if (inventory == null) {
            return Result.fail("Inventory is unavailable.");
        }
        ItemContainer container = inventory.getCombinedEverything();
        if (container == null) {
            return Result.fail("Inventory container is unavailable.");
        }
        if (targetSlot < 0 || targetSlot >= container.getCapacity()) {
            return Result.fail("Invalid receiver slot.");
        }
        if (catalystAffinity == null || catalystAffinity == CatalystAffinity.NONE) {
            return Result.fail("Invalid catalyst.");
        }

        ItemStack receiver = container.getItemStack(targetSlot);
        if (receiver == null || receiver.isEmpty() || receiver.getItemId() == null) {
            return Result.fail("No receiver item selected.");
        }
        if (!ItemizationEligibilityService.isEligible(receiver)) {
            return Result.fail("Receiver item is not eligible for itemization.");
        }

        // Stage 2: read/migrate metadata and enforce policy decisions.
        ItemInstanceMetadata metadata = receiver.getFromMetadataOrNull(ItemInstanceMetadata.KEYED_CODEC);
        if (metadata == null) {
            return Result.fail("Receiver item has no roll metadata. Generate the item first.");
        }
        metadata = ItemInstanceMetadataMigration.migrateToCurrent(metadata);

        CatalystAffinity previous = metadata.getCatalyst();
        if (previous == catalystAffinity) {
            return Result.fail("Receiver already has this catalyst.");
        }
        boolean isReimbue = previous != null && previous != CatalystAffinity.NONE;

        HyruneConfig.CatalystConfig cfg = resolveCatalystConfig();
        if (isReimbue && !cfg.allowOverwrite) {
            return Result.fail("Overwrite is disabled for already-imbued items.");
        }
        if (isReimbue && cfg.enableReimbueCost && cfg.enforceCurrencyBalance) {
            if (!tryConsumeReimbueCurrency(inventory, cfg.reimbueCurrencyCost, cfg.reimbueCurrencyName)) {
                return Result.fail("Need " + cfg.reimbueCurrencyCost + " " + cfg.reimbueCurrencyName + " to re-imbue.");
            }
        }

        if (catalystItemId == null || catalystItemId.isBlank()) {
            return Result.fail("Catalyst item is missing.");
        }
        String normalizedCatalystId = catalystItemId.trim();
        var tx = container.removeItemStack(new ItemStack(normalizedCatalystId, 1));
        if (tx == null || !tx.succeeded()) {
            return Result.fail("You need 1x " + normalizedCatalystId + " to imbue.");
        }

        // Stage 3: mutate metadata and atomically replace the receiver stack.
        metadata.setCatalyst(catalystAffinity); // explicit overwrite policy
        ItemStack updated = receiver.withMetadata(ItemInstanceMetadata.KEYED_CODEC, metadata);
        container.replaceItemStackInSlot(targetSlot, receiver, updated);
        String resolvedName = CatalystNamingResolver.resolveDisplayName(receiver.getItemId(), catalystAffinity);
        if (isReimbue && cfg.enableReimbueCost) {
            return Result.success("Imbued: " + resolvedName + " (" + catalystAffinity.name() + "). Re-imbue cost: "
                + cfg.reimbueCurrencyCost + " " + cfg.reimbueCurrencyName + (cfg.enforceCurrencyBalance ? "." : " (stubbed)."));
        }
        return Result.success("Imbued: " + resolvedName + " (" + catalystAffinity.name() + ").");
    }

    public static List<ReceiverEntry> findEligibleReceiverEntries(Inventory inventory) {
        List<ReceiverEntry> out = new ArrayList<>();
        if (inventory == null) {
            return out;
        }

        ItemContainer container = inventory.getCombinedEverything();
        if (container == null) {
            return out;
        }

        for (short slot = 0; slot < container.getCapacity(); slot++) {
            ItemStack stack = container.getItemStack(slot);
            if (stack == null || stack.isEmpty() || stack.getItemId() == null) {
                continue;
            }
            if (!ItemizationEligibilityService.isEligible(stack)) {
                continue;
            }
            ItemInstanceMetadata metadata = stack.getFromMetadataOrNull(ItemInstanceMetadata.KEYED_CODEC);
            if (metadata == null) {
                continue;
            }
            metadata = ItemInstanceMetadataMigration.migrateToCurrent(metadata);
            out.add(new ReceiverEntry(slot, stack, metadata));
        }

        return out;
    }

    public record ReceiverEntry(short slot, ItemStack stack, ItemInstanceMetadata metadata) {
    }

    public record Result(boolean success, String message) {
        public static Result success(String message) {
            return new Result(true, message);
        }

        public static Result fail(String message) {
            return new Result(false, message);
        }
    }

    private static HyruneConfig.CatalystConfig resolveCatalystConfig() {
        HyruneConfig cfg = HyruneConfigManager.getConfig();
        return cfg != null && cfg.catalyst != null ? cfg.catalyst : new HyruneConfig.CatalystConfig();
    }

    private static boolean tryConsumeReimbueCurrency(Inventory inventory, int amount, String currencyName) {
        // Currency is not wired yet. Keep hook in place for milestone follow-up.
        return false;
    }
}
