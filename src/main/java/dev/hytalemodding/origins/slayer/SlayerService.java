package dev.hytalemodding.origins.slayer;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.origins.level.LevelingService;
import dev.hytalemodding.origins.npc.NpcLevelService;
import dev.hytalemodding.origins.skills.SkillType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SlayerService {

    private final SlayerRepository repository;
    private final SlayerTaskRegistry taskRegistry;
    private final Map<UUID, SlayerPlayerData> cache = new ConcurrentHashMap<>();
    private final NpcLevelService npcService;

    private final List<ShopItem> shopItems = new ArrayList<>();

    public SlayerService(SlayerRepository repository, SlayerTaskRegistry taskRegistry, NpcLevelService npcService) {
        this.repository = repository;
        this.taskRegistry = taskRegistry;
        this.npcService = npcService;

        initShop();
    }
    private void initShop() {
        // ID, Display Name, Cost
        shopItems.add(new ShopItem("Ingredient_Bar_Copper", "Copper Bar", 5));
        shopItems.add(new ShopItem("Ingredient_Bar_Iron", "Iron Bar", 10));
        shopItems.add(new ShopItem("Ingredient_Bar_Thorium", "Thorium Bar", 50));
    }

    public List<ShopItem> getShopItems() {
        return Collections.unmodifiableList(shopItems);
    }

    /**
     * Attempts to purchase an item.
     * 1. Checks Points.
     * 2. Checks Inventory Space using Transaction.
     * 3. Deducts Points only if item fits.
     */
    public boolean attemptPurchase(Player player, String itemId, Ref<EntityStore> playerRef, Store<EntityStore> store) {
        if (player == null) return false;

        // 1. Validate Item
        ShopItem shopItem = shopItems.stream()
                .filter(i -> i.getId().equalsIgnoreCase(itemId))
                .findFirst()
                .orElse(null);

        if (shopItem == null) return false;

        // 2. Validate Points
        SlayerPlayerData data = getPlayerData(player.getUuid());
        if (data.getSlayerPoints() < shopItem.getCost()) {
            return false; // Not enough points
        }

        // 3. Handle Inventory Logic
        Inventory inventory = player.getInventory();
        if (inventory == null) return false;

        // Get the container that represents Hotbar + Main Inventory
        CombinedItemContainer container = inventory.getCombinedHotbarFirst();

        // Create the stack we want to give
        ItemStack stackToAdd = new ItemStack(shopItem.getId(), 1);

        // Try to add it. This calculates if it fits.
        ItemStackTransaction transaction = container.addItemStack(stackToAdd);
        ItemStack remainder = transaction.getRemainder();

        // If there is a remainder, it means the item didn't fit.
        // Since we are adding 1, any remainder means 0 were added.
        if (remainder != null && !remainder.isEmpty()) {
            return false; // Inventory Full
        }

        // 4. Success! Deduct Points & Save
        data.removeSlayerPoints(shopItem.getCost());
        repository.save(data);

        // 5. Visual Feedback (Item Pickup Notification)
        // This makes the item pop up on the right side of the screen
        player.notifyPickupItem(playerRef, stackToAdd, null, store);

        return true;
    }

    public SlayerPlayerData getPlayerData(UUID uuid) {
        return cache.computeIfAbsent(uuid, id -> {
            SlayerPlayerData stored = repository.load(id);
            return stored != null ? stored : new SlayerPlayerData(id);
        });
    }

    public SlayerTaskAssignment getAssignment(UUID uuid) {
        SlayerTaskAssignment assignment = getPlayerData(uuid).getAssignment();
        return normalizeAssignment(assignment);
    }

    public SlayerTaskAssignment assignTask(UUID uuid, int slayerLevel) {
        SlayerPlayerData data = getPlayerData(uuid);
        SlayerTaskAssignment current = data.getAssignment();
        if (current != null && current.getState() != SlayerTaskState.COMPLETED) {
            return current;
        }

        SlayerTaskDefinition task = taskRegistry.getRandomTaskForLevel(slayerLevel);
        if (task == null) {
            return null;
        }
        String specificTarget = npcService.getRandomIdFromGroup(task.getTargetGroupId());
        int count = taskRegistry.rollKillCount(task);
        SlayerTaskAssignment assignment = new SlayerTaskAssignment(task.getId(), specificTarget, count);
        data.setAssignment(assignment);
        repository.save(data);
        return assignment;
    }

    public boolean onKill(UUID killerUuid, String npcTypeId, long xpAmount) {
        SlayerPlayerData data = getPlayerData(killerUuid);
        SlayerTaskAssignment assignment = normalizeAssignment(data.getAssignment());
        if (assignment == null) {
            return false;
        }
        if (assignment.getState() == SlayerTaskState.COMPLETED) {
            return false;
        }
        if (!assignment.getTargetNpcTypeId().equalsIgnoreCase(npcTypeId)) {
            return false;
        }

        assignment.decrement();
        repository.save(data);

        LevelingService leveling = LevelingService.get();
        if (leveling != null) {
            leveling.addSkillXp(killerUuid, SkillType.SLAYER, xpAmount);
        }
        return true;
    }

    public SlayerTurnInResult turnInTask(Player player) {
        if (player == null) {
            return null;
        }
        UUID uuid = player.getUuid();
        SlayerPlayerData data = getPlayerData(uuid);
        SlayerTaskAssignment assignment = normalizeAssignment(data.getAssignment());
        if (assignment == null || assignment.getState() != SlayerTaskState.COMPLETED) {
            return null;
        }

        int pointsAwarded = 5 + Math.max(1, assignment.getTotalKills() / 5);
        long slayerXpAwarded = 25L * assignment.getTotalKills();
        boolean itemRewarded = Math.random() < 0.1;

        data.addSlayerPoints(pointsAwarded);
        data.incrementCompletedTasks();
        data.setAssignment(null);
        repository.save(data);

        LevelingService levelingService = LevelingService.get();
        if (levelingService != null) {
            levelingService.addSkillXp(uuid, SkillType.SLAYER, slayerXpAwarded);
        }

        // TODO: Hook into item reward system when available.
        return new SlayerTurnInResult(pointsAwarded, slayerXpAwarded, itemRewarded);
    }

    private SlayerTaskAssignment normalizeAssignment(SlayerTaskAssignment assignment) {
        if (assignment == null) {
            return null;
        }
        if (assignment.getState() == null) {
            assignment.setState(SlayerTaskState.ACCEPTED);
        }
        if (assignment.getRemainingKills() <= 0 && assignment.getTotalKills() > 0) {
            assignment.setState(SlayerTaskState.COMPLETED);
        }
        return assignment;
    }

    public void unload(UUID uuid) {
        SlayerPlayerData data = cache.remove(uuid);
        if (data != null) {
            repository.save(data);
        }
    }
}
