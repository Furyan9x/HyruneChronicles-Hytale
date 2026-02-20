package dev.hytalemodding.hyrune.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.ItemUtils;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

final class ItemSinkCommandSupport {
    private ItemSinkCommandSupport() {
    }

    static boolean consumeOneFromHand(Inventory inventory, ItemStack held) {
        if (inventory == null || held == null || held.isEmpty()) {
            return false;
        }
        if (tryConsumeOne(inventory.getHotbar(), inventory.getActiveHotbarSlot(), held)) {
            return true;
        }
        return tryConsumeOne(inventory.getTools(), inventory.getActiveToolsSlot(), held);
    }

    static void payoutOrDrop(Ref<EntityStore> playerRef,
                             Store<EntityStore> store,
                             ItemContainer target,
                             ItemStack payout) {
        if (playerRef == null || store == null || target == null || payout == null || payout.isEmpty()) {
            return;
        }
        ItemStackTransaction tx = target.addItemStack(payout);
        ItemStack remainder = tx == null ? payout : tx.getRemainder();
        if (remainder != null && !remainder.isEmpty()) {
            ItemUtils.dropItem(playerRef, remainder, store);
        }
    }

    static ItemContainer combinedContainer(Player player) {
        if (player == null || player.getInventory() == null) {
            return null;
        }
        return player.getInventory().getCombinedEverything();
    }

    private static boolean tryConsumeOne(ItemContainer container, byte slot, ItemStack held) {
        if (container == null || slot < 0 || slot >= container.getCapacity()) {
            return false;
        }
        ItemStack inSlot = container.getItemStack(slot);
        if (inSlot == null || inSlot.isEmpty() || !inSlot.isEquivalentType(held) || inSlot.getQuantity() < 1) {
            return false;
        }
        ItemStack after = inSlot.withQuantity(inSlot.getQuantity() - 1);
        container.replaceItemStackInSlot(slot, inSlot, after);
        return true;
    }
}

