package dev.hytalemodding.hyrune.itemization;

import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;

import java.util.ArrayList;
import java.util.List;

/**
 * Applies gem sockets to rolled item instances.
 */
public final class GemSocketApplicationService {
    private GemSocketApplicationService() {
    }

    public static Result applyGemToSlot(Inventory inventory, short targetSlot, String gemItemId) {
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
        if (!GemSocketConfigHelper.isGemItemId(gemItemId)) {
            return Result.fail("Held item is not recognized as a gem.");
        }

        ItemStack receiver = container.getItemStack(targetSlot);
        if (receiver == null || receiver.isEmpty() || receiver.getItemId() == null) {
            return Result.fail("No receiver item selected.");
        }
        if (!ItemizationEligibilityService.isEligible(receiver)) {
            return Result.fail("Receiver item is not eligible for itemization.");
        }

        ItemInstanceMetadata metadata = receiver.getFromMetadataOrNull(ItemInstanceMetadata.KEYED_CODEC);
        if (metadata == null) {
            return Result.fail("Receiver item has no roll metadata. Generate the item first.");
        }
        metadata = ItemInstanceMetadataMigration.migrateToCurrent(metadata);
        if (metadata.getSocketCapacity() <= 0) {
            return Result.fail("Receiver has no sockets.");
        }
        if (metadata.getOpenSocketCount() <= 0) {
            return Result.fail("All sockets are already filled.");
        }

        if (gemItemId == null || gemItemId.isBlank()) {
            return Result.fail("Gem item is missing.");
        }
        String normalizedGemItemId = gemItemId.trim();
        var tx = container.removeItemStack(new ItemStack(normalizedGemItemId, 1));
        if (tx == null || !tx.succeeded()) {
            return Result.fail("You need 1x " + normalizedGemItemId + " to socket.");
        }

        if (!metadata.addSocketedGem(normalizedGemItemId)) {
            return Result.fail("Unable to socket gem.");
        }
        ItemStack updated = receiver.withMetadata(ItemInstanceMetadata.KEYED_CODEC, metadata);
        container.replaceItemStackInSlot(targetSlot, receiver, updated);
        String bonusSummary = GemSocketConfigHelper.describeGemBonusForItem(normalizedGemItemId, receiver.getItemId());
        return Result.success(
            "Socketed " + normalizedGemItemId
                + " into " + receiver.getItemId()
                + " (" + metadata.getSocketedGemCount() + "/" + metadata.getSocketCapacity() + ")."
                + " Gem bonus: " + bonusSummary + "."
        );
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

    public static Result removeAllGemsAndDestroySlot(Inventory inventory, short targetSlot, String removerItemId) {
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
        if (removerItemId == null || removerItemId.isBlank()) {
            return Result.fail("Gem remover item is missing.");
        }

        ItemStack receiver = container.getItemStack(targetSlot);
        if (receiver == null || receiver.isEmpty() || receiver.getItemId() == null) {
            return Result.fail("No receiver item selected.");
        }
        ItemInstanceMetadata metadata = receiver.getFromMetadataOrNull(ItemInstanceMetadata.KEYED_CODEC);
        if (metadata == null) {
            return Result.fail("Receiver item has no socket metadata.");
        }
        metadata = ItemInstanceMetadataMigration.migrateToCurrent(metadata);
        List<String> socketedGems = metadata.getSocketedGems();
        if (socketedGems.isEmpty()) {
            return Result.fail("Selected item has no socketed gems to remove.");
        }

        for (String gemItemId : socketedGems) {
            ItemStack gemStack = new ItemStack(gemItemId, 1);
            if (!container.canAddItemStack(gemStack)) {
                return Result.fail("Not enough inventory space to recover socketed gems.");
            }
        }

        var removerTx = container.removeItemStack(new ItemStack(removerItemId.trim(), 1));
        if (removerTx == null || !removerTx.succeeded()) {
            return Result.fail("You need 1x " + removerItemId.trim() + " to remove gems.");
        }

        var removeReceiverTx = container.removeItemStackFromSlot(targetSlot);
        if (removeReceiverTx == null || !removeReceiverTx.succeeded()) {
            return Result.fail("Unable to remove the selected item.");
        }

        int recovered = 0;
        for (String gemItemId : socketedGems) {
            var addTx = container.addItemStack(new ItemStack(gemItemId, 1));
            if (addTx == null || !addTx.succeeded() || (addTx.getRemainder() != null && !addTx.getRemainder().isEmpty())) {
                return Result.fail("Failed to return one or more gems to inventory.");
            }
            recovered++;
        }

        return Result.success(
            "The socketing ritual cracked the host item. "
                + recovered
                + " gem(s) were recovered, but the item was destroyed."
        );
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

}

