package dev.hytalemodding.hyrune.events;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.LivingEntity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.entity.LivingEntityInventoryChangeEvent;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ListTransaction;
import com.hypixel.hytale.server.core.inventory.transaction.MoveTransaction;
import com.hypixel.hytale.server.core.inventory.transaction.MoveType;
import com.hypixel.hytale.server.core.inventory.transaction.Transaction;
import com.hypixel.hytale.server.core.universe.world.World;
import dev.hytalemodding.hyrune.config.HyruneConfigManager;
import dev.hytalemodding.hyrune.itemization.ItemGenerationService;
import dev.hytalemodding.hyrune.itemization.ItemInstanceMetadata;
import dev.hytalemodding.hyrune.itemization.ItemRarityRollModel;
import dev.hytalemodding.hyrune.itemization.ItemRollSource;
import dev.hytalemodding.hyrune.itemization.ItemizationEligibilityService;
import dev.hytalemodding.hyrune.util.PlayerEntityAccess;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Applies explicit container-loot generation when items are moved into player inventory from external containers.
 */
public class ContainerLootItemizationListener {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final Set<UUID> ACTIVE = ConcurrentHashMap.newKeySet();

    public void onInventoryChange(LivingEntityInventoryChangeEvent event) {
        if (event == null) {
            return;
        }
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Player player)) {
            return;
        }

        Transaction tx = event.getTransaction();
        if (!isExternalMoveToSelf(tx, player)) {
            return;
        }

        UUID uuid = PlayerEntityAccess.getPlayerUuid(player);
        if (uuid == null || !ACTIVE.add(uuid)) {
            return;
        }

        ItemContainer container = event.getItemContainer();
        if (container == null) {
            ACTIVE.remove(uuid);
            return;
        }

        World world = player.getReference() != null ? player.getReference().getStore().getExternalData().getWorld() : null;
        if (world == null) {
            ACTIVE.remove(uuid);
            return;
        }

        world.execute(() -> {
            int rolledCount = 0;
            try {
                short capacity = container.getCapacity();
                for (short slot = 0; slot < capacity; slot++) {
                    if (tx == null || !tx.wasSlotModified(slot)) {
                        continue;
                    }
                    ItemStack current = container.getItemStack(slot);
                    if (current == null || current.isEmpty()) {
                        continue;
                    }
                    if (!ItemizationEligibilityService.isEligible(current)) {
                        continue;
                    }
                    if (current.getFromMetadataOrNull(ItemInstanceMetadata.KEYED_CODEC) != null) {
                        continue;
                    }

                    ItemRarityRollModel.GenerationContext context = ItemRarityRollModel.GenerationContext.of("container_move_to_player");
                    ItemStack rolled = ItemGenerationService.rollIfEligible(
                        current,
                        ItemRollSource.CONTAINER_LOOT,
                        context
                    );
                    container.replaceItemStackInSlot(slot, current, rolled);
                    rolledCount++;
                }

                if (rolledCount > 0 && HyruneConfigManager.getConfig().itemizationDebugLogging) {
                    LOGGER.at(Level.INFO).log("[Itemization][ContainerLoot] p=" + shortUuid(uuid) + ", rolledStacks=" + rolledCount);
                }
            } finally {
                ACTIVE.remove(uuid);
            }
        });
    }

    private static boolean isExternalMoveToSelf(Transaction tx, Player player) {
        if (player == null || tx == null) {
            return false;
        }
        if (tx instanceof MoveTransaction<?> move) {
            return isExternalMoveToSelf(move, player);
        }
        if (tx instanceof ListTransaction<?> list) {
            for (Object entry : list.getList()) {
                if (entry instanceof MoveTransaction<?> move && isExternalMoveToSelf(move, player)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isExternalMoveToSelf(MoveTransaction<?> move, Player player) {
        if (move == null || move.getMoveType() != MoveType.MOVE_TO_SELF) {
            return false;
        }
        Inventory inventory = player.getInventory();
        if (inventory == null || inventory.getCombinedEverything() == null) {
            return false;
        }
        ItemContainer other = move.getOtherContainer();
        if (other == null) {
            return false;
        }
        return !inventory.getCombinedEverything().containsContainer(other);
    }

    private static String shortUuid(UUID uuid) {
        if (uuid == null) {
            return "null";
        }
        String raw = uuid.toString();
        return raw.length() <= 8 ? raw : raw.substring(0, 8);
    }
}
