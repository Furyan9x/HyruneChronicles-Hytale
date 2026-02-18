package dev.hytalemodding.hyrune.events;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.ItemUtils;
import com.hypixel.hytale.server.core.entity.LivingEntity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.entity.LivingEntityInventoryChangeEvent;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.Hyrune;
import dev.hytalemodding.hyrune.level.LevelingService;
import dev.hytalemodding.hyrune.skills.SkillType;
import dev.hytalemodding.hyrune.registry.CombatRequirementRegistry;
import dev.hytalemodding.hyrune.util.PlayerEntityAccess;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Event listener for armor requirement.
 */
public class ArmorRequirementListener {
    private static final long ARMOR_WARNING_COOLDOWN_MS = 2000;
    private static final Map<UUID, Long> LAST_ARMOR_WARNING = new ConcurrentHashMap<>();
    private static final Set<UUID> ACTIVE = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, ItemStack[]> LAST_VALID_ARMOR = new ConcurrentHashMap<>();

    public void onInventoryChange(LivingEntityInventoryChangeEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Player player)) {
            return;
        }

        UUID uuid = PlayerEntityAccess.getPlayerUuid(player);
        if (uuid == null) {
            return;
        }
        if (!ACTIVE.add(uuid)) {
            return;
        }

        World world = player.getReference() != null ? player.getReference().getStore().getExternalData().getWorld() : null;
        if (world == null) {
            ACTIVE.remove(uuid);
            return;
        }

        world.execute(() -> {
            try {
                handleArmorChange(player);
            } finally {
                ACTIVE.remove(uuid);
            }
        });
    }

    private void handleArmorChange(Player player) {
        Inventory inventory = player.getInventory();
        if (inventory == null) {
            return;
        }

        ItemContainer armor = inventory.getArmor();
        if (armor == null) {
            return;
        }

        PlayerRef playerRef = PlayerEntityAccess.getPlayerRef(player);
        if (playerRef == null) {
            return;
        }
        UUID playerUuid = playerRef.getUuid();

        LevelingService service = Hyrune.getService();
        int defenceLevel = service.getSkillLevel(playerRef.getUuid(), SkillType.DEFENCE);

        ItemStack[] cached = getOrInitLastValidArmor(playerUuid, armor);
        boolean warned = false;
        short capacity = armor.getCapacity();
        for (short slot = 0; slot < capacity; slot++) {
            ItemStack stack = armor.getItemStack(slot);
            if (stack == null || stack.isEmpty() || stack.getItemId() == null) {
                cached[slot] = null;
                continue;
            }

            Integer required = CombatRequirementRegistry.getRequiredLevel(stack.getItemId());
            if (required != null && defenceLevel < required) {
                ItemStack previous = cached[slot];
                armor.removeItemStackFromSlot(slot);
                moveOrDrop(playerRef, inventory, stack);
                if (previous != null && !previous.isEmpty()) {
                    restorePreviousArmor(inventory, armor, slot, previous);
                }
                if (!warned) {
                    sendArmorWarning(player, playerUuid, required);
                    warned = true;
                }
                continue;
            }

            cached[slot] = stack;
        }
    }

    private ItemStack[] getOrInitLastValidArmor(UUID uuid, ItemContainer armor) {
        return LAST_VALID_ARMOR.compute(uuid, (key, existing) -> {
            short capacity = armor.getCapacity();
            if (existing == null || existing.length != capacity) {
                ItemStack[] snapshot = new ItemStack[capacity];
                for (short slot = 0; slot < capacity; slot++) {
                    snapshot[slot] = armor.getItemStack(slot);
                }
                return snapshot;
            }
            return existing;
        });
    }

    private void restorePreviousArmor(Inventory inventory,
                                      ItemContainer armor,
                                      short slot,
                                      ItemStack previous) {
        if (previous == null || previous.isEmpty() || previous.getItemId() == null) {
            return;
        }

        ItemContainer target = inventory.getCombinedBackpackStorageHotbar();
        if (target != null) {
            ItemStack restored = takeMatchingItem(target, previous);
            if (restored != null && !restored.isEmpty()) {
                armor.setItemStackForSlot(slot, restored);
                return;
            }
        }

        armor.setItemStackForSlot(slot, previous);
    }

    private ItemStack takeMatchingItem(ItemContainer container, ItemStack desired) {
        short capacity = container.getCapacity();
        for (short slot = 0; slot < capacity; slot++) {
            ItemStack stack = container.getItemStack(slot);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            if (!ItemStack.isEquivalentType(stack, desired)) {
                continue;
            }
            int quantity = Math.min(desired.getQuantity(), stack.getQuantity());
            var removed = container.removeItemStackFromSlot(slot, quantity);
            if (removed != null && removed.getOutput() != null && !removed.getOutput().isEmpty()) {
                return removed.getOutput();
            }
        }
        return null;
    }

    private void moveOrDrop(PlayerRef playerRef, Inventory inventory, ItemStack stack) {
        ItemContainer target = inventory.getCombinedBackpackStorageHotbar();
        if (target != null) {
            ItemStack remainder = target.addItemStack(stack).getRemainder();
            if (remainder == null || remainder.isEmpty()) {
                return;
            }
            stack = remainder;
        }

        if (playerRef.getReference() != null) {
            ItemUtils.dropItem(playerRef.getReference(), stack, playerRef.getReference().getStore());
        }
    }

    private void sendArmorWarning(Player player, UUID playerUuid, int requiredLevel) {
        long now = System.currentTimeMillis();
        Long last = LAST_ARMOR_WARNING.get(playerUuid);
        if (last != null && now - last < ARMOR_WARNING_COOLDOWN_MS) {
            return;
        }
        LAST_ARMOR_WARNING.put(playerUuid, now);
        player.sendMessage(Message.raw(
            "You need Defence level " + requiredLevel + " to equip this armor."
        ));
    }
}

