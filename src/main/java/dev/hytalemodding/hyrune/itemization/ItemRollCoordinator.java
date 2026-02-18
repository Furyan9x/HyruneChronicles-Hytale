package dev.hytalemodding.hyrune.itemization;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import dev.hytalemodding.hyrune.config.HyruneConfigManager;
import dev.hytalemodding.hyrune.util.PlayerEntityAccess;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.List;

/**
 * Centralized queue for pending item generation rolls, applied on inventory updates.
 */
public final class ItemRollCoordinator {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final class PendingCraftRoll {
        int quantity;
        String professionSkill;
        int professionLevel;
        int benchTier;
    }

    private static final Map<UUID, Map<String, PendingCraftRoll>> PENDING_CRAFT_ROLLS = new ConcurrentHashMap<>();

    private ItemRollCoordinator() {
    }

    public static void queueCraftedRoll(UUID playerUuid, String itemId, int quantity) {
        queueCraftedRoll(playerUuid, itemId, quantity, null, 0, 1);
    }

    public static void queueCraftedRoll(UUID playerUuid,
                                        String itemId,
                                        int quantity,
                                        String professionSkill,
                                        int professionLevel,
                                        int benchTier) {
        if (playerUuid == null || itemId == null || itemId.isBlank() || quantity <= 0) {
            return;
        }
        String normalized = itemId.toLowerCase(Locale.ROOT);
        if (!ItemizationEligibilityService.isEligibleItemId(normalized)) {
            return;
        }
        PENDING_CRAFT_ROLLS.compute(playerUuid, (id, pending) -> {
            Map<String, PendingCraftRoll> map = pending == null ? new ConcurrentHashMap<>() : pending;
            PendingCraftRoll roll = map.get(normalized);
            if (roll == null) {
                roll = new PendingCraftRoll();
                map.put(normalized, roll);
            }
            roll.quantity += quantity;
            if (professionSkill != null && !professionSkill.isBlank()) {
                roll.professionSkill = professionSkill;
            }
            roll.professionLevel = Math.max(roll.professionLevel, professionLevel);
            roll.benchTier = Math.max(roll.benchTier, benchTier);
            return map;
        });
        if (HyruneConfigManager.getConfig().itemizationDebugLogging) {
            PendingCraftRoll pendingRoll = PENDING_CRAFT_ROLLS.getOrDefault(playerUuid, Map.of()).get(normalized);
            int pendingQty = pendingRoll == null ? 0 : pendingRoll.quantity;
            LOGGER.at(Level.INFO).log("[Itemization][Queue] +" + quantity
                + " item=" + normalized
                + ", p=" + shortUuid(playerUuid)
                + ", pending=" + pendingQty
                + ", prof=" + safeSkill(pendingRoll)
                + ", bench=" + safeBench(pendingRoll));
        }
    }

    public static int applyPendingCraftRolls(Player player) {
        if (player == null) {
            return 0;
        }
        UUID playerUuid = PlayerEntityAccess.getPlayerUuid(player);
        if (playerUuid == null) {
            return 0;
        }
        Map<String, PendingCraftRoll> pending = PENDING_CRAFT_ROLLS.get(playerUuid);
        if (pending == null || pending.isEmpty()) {
            return 0;
        }
        boolean debug = HyruneConfigManager.getConfig().itemizationDebugLogging;
        if (debug) {
            LOGGER.at(Level.INFO).log("[Itemization][QueueApply] start p=" + shortUuid(playerUuid)
                + ", entries=" + pending.size()
                + ", totalPending=" + totalPendingQuantity(pending));
        }

        Inventory inventory = player.getInventory();
        if (inventory == null) {
            return 0;
        }
        ItemContainer container = inventory.getCombinedEverything();
        if (container == null) {
            return 0;
        }

        short capacity = container.getCapacity();
        int appliedStacks = 0;
        List<String> appliedItems = debug ? new ArrayList<>() : List.of();
        for (short slot = 0; slot < capacity; slot++) {
            ItemStack current = container.getItemStack(slot);
            if (current == null || current.isEmpty() || current.getItemId() == null) {
                continue;
            }
            String itemId = current.getItemId().toLowerCase(Locale.ROOT);
            PendingCraftRoll pendingRoll = pending.get(itemId);
            int needed = pendingRoll == null ? 0 : pendingRoll.quantity;
            if (pendingRoll == null || needed <= 0) {
                continue;
            }
            if (!ItemizationEligibilityService.isEligible(current)) {
                continue;
            }
            if (current.getFromMetadataOrNull(ItemInstanceMetadata.KEYED_CODEC) != null) {
                continue;
            }

            ItemStack rolled = ItemGenerationService.rollIfEligible(
                current,
                ItemRollSource.CRAFTED,
                ItemRarityRollModel.GenerationContext.crafting(
                    "craft_queue_apply",
                    playerUuid.toString(),
                    itemId,
                    pendingRoll.professionSkill,
                    pendingRoll.professionLevel,
                    pendingRoll.benchTier
                )
            );
            container.setItemStackForSlot(slot, rolled);
            appliedStacks++;
            if (debug) {
                appliedItems.add(current.getItemId() + "x" + current.getQuantity() + "@s" + slot);
            }

            int consumed = Math.max(1, current.getQuantity());
            int remaining = needed - consumed;
            if (remaining > 0) {
                pendingRoll.quantity = remaining;
                pending.put(itemId, pendingRoll);
            } else {
                pending.remove(itemId);
            }
        }

        if (pending.isEmpty()) {
            PENDING_CRAFT_ROLLS.remove(playerUuid);
        }
        if (debug) {
            LOGGER.at(Level.INFO).log("[Itemization][QueueApply] done p=" + shortUuid(playerUuid)
                + ", appliedStacks=" + appliedStacks
                + ", pendingAfter=" + totalPendingQuantity(pending)
                + (appliedItems.isEmpty() ? "" : ", rolled=" + appliedItems));
        }
        return appliedStacks;
    }

    private static int totalPendingQuantity(Map<String, PendingCraftRoll> pending) {
        int total = 0;
        if (pending == null || pending.isEmpty()) {
            return 0;
        }
        for (PendingCraftRoll roll : pending.values()) {
            if (roll != null && roll.quantity > 0) {
                total += roll.quantity;
            }
        }
        return total;
    }

    private static String shortUuid(UUID uuid) {
        if (uuid == null) {
            return "null";
        }
        String raw = uuid.toString();
        return raw.length() <= 8 ? raw : raw.substring(0, 8);
    }

    private static String safeSkill(PendingCraftRoll roll) {
        if (roll == null || roll.professionSkill == null || roll.professionSkill.isBlank()) {
            return "none";
        }
        return roll.professionSkill + ":" + Math.max(0, roll.professionLevel);
    }

    private static int safeBench(PendingCraftRoll roll) {
        return roll == null ? 1 : Math.max(1, roll.benchTier);
    }
}
