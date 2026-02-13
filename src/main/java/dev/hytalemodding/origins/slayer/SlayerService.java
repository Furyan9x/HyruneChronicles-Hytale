package dev.hytalemodding.origins.slayer;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.origins.database.SlayerRepository;
import dev.hytalemodding.origins.level.LevelingService;
import dev.hytalemodding.origins.npc.NpcLevelService;
import dev.hytalemodding.origins.playerdata.SlayerPlayerData;
import dev.hytalemodding.origins.skills.SkillType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages Slayer assignments, rewards, and persistence.
 */
public class SlayerService {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final int BASE_POINTS_AWARDED = 5;
    private static final int POINTS_KILL_DIVISOR = 5;
    private static final long XP_PER_KILL = 25L;
    private static final double ITEM_REWARD_CHANCE = 0.1;

    private final SlayerRepository repository;
    private final SlayerTaskRegistry taskRegistry;
    private final Map<UUID, SlayerPlayerData> cache = new ConcurrentHashMap<>();
    private final NpcLevelService npcService;

    private final List<ShopItem> shopItems = new ArrayList<>();

    public SlayerService(SlayerRepository repository, SlayerTaskRegistry taskRegistry, NpcLevelService npcService) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.taskRegistry = Objects.requireNonNull(taskRegistry, "taskRegistry");
        this.npcService = Objects.requireNonNull(npcService, "npcService");
        initShop();
    }

    public List<ShopItem> getShopItems() {
        return Collections.unmodifiableList(shopItems);
    }

    public void load(UUID uuid) {
        if (uuid == null) {
            return;
        }
        cache.computeIfAbsent(uuid, this::loadOrCreate);
    }

    /**
     * Attempts to purchase an item.
     * 1. Checks points.
     * 2. Checks inventory space using transaction.
     * 3. Deducts points only if item fits.
     */
    public boolean attemptPurchase(Player player, String itemId, Ref<EntityStore> playerRef, Store<EntityStore> store) {
        if (player == null || itemId == null || playerRef == null || store == null) {
            return false;
        }

        ShopItem shopItem = findShopItem(itemId);
        if (shopItem == null) {
            return false;
        }

        SlayerPlayerData data = getPlayerData(player.getUuid());
        if (data.getSlayerPoints() < shopItem.cost()) {
            return false;
        }

        Inventory inventory = player.getInventory();
        if (inventory == null) {
            return false;
        }

        CombinedItemContainer container = inventory.getCombinedHotbarFirst();
        ItemStack stackToAdd = new ItemStack(shopItem.id(), 1);

        ItemStackTransaction transaction = container.addItemStack(stackToAdd);
        ItemStack remainder = transaction.getRemainder();
        if (remainder != null && !remainder.isEmpty()) {
            return false;
        }

        data.removeSlayerPoints(shopItem.cost());
        persist(data);

        player.notifyPickupItem(playerRef, stackToAdd, null, store);
        return true;
    }

    public SlayerPlayerData getPlayerData(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        return cache.computeIfAbsent(uuid, this::loadOrCreate);
    }

    public SlayerTaskAssignment getAssignment(UUID uuid) {
        SlayerPlayerData data = getPlayerData(uuid);
        return normalizeAssignment(data);
    }

    public SlayerTaskAssignment assignTask(UUID uuid, int slayerLevel) {
        if (uuid == null) {
            return null;
        }
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
        persist(data);
        return assignment;
    }

    public boolean onKill(UUID killerUuid, String npcTypeId, long xpAmount) {
        if (killerUuid == null || npcTypeId == null || xpAmount <= 0) {
            return false;
        }
        SlayerPlayerData data = getPlayerData(killerUuid);
        SlayerTaskAssignment assignment = normalizeAssignment(data);
        if (assignment == null) {
            return false;
        }
        if (assignment.getState() == SlayerTaskState.COMPLETED) {
            return false;
        }
        if (!matchesAssignmentTarget(assignment, npcTypeId)) {
            return false;
        }

        assignment.decrement();
        persist(data);

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
        SlayerTaskAssignment assignment = normalizeAssignment(data);
        if (assignment == null || assignment.getState() != SlayerTaskState.COMPLETED) {
            return null;
        }

        int pointsAwarded = BASE_POINTS_AWARDED + Math.max(1, assignment.getTotalKills() / POINTS_KILL_DIVISOR);
        long slayerXpAwarded = XP_PER_KILL * assignment.getTotalKills();
        boolean itemRewarded = Math.random() < ITEM_REWARD_CHANCE;

        data.addSlayerPoints(pointsAwarded);
        data.incrementCompletedTasks();
        data.setAssignment(null);
        persist(data);

        LevelingService levelingService = LevelingService.get();
        if (levelingService != null) {
            levelingService.addSkillXp(uuid, SkillType.SLAYER, slayerXpAwarded);
        }

        return new SlayerTurnInResult(pointsAwarded, slayerXpAwarded, itemRewarded);
    }

    public void unload(UUID uuid) {
        if (uuid == null) {
            return;
        }
        SlayerPlayerData data = cache.remove(uuid);
        if (data != null) {
            persist(data);
        }
    }

    private void initShop() {
        shopItems.add(new ShopItem("Ingredient_Bar_Copper", "Copper Bar", 5));
        shopItems.add(new ShopItem("Ingredient_Bar_Iron", "Iron Bar", 10));
        shopItems.add(new ShopItem("Ingredient_Bar_Thorium", "Thorium Bar", 50));
    }

    private ShopItem findShopItem(String itemId) {
        for (ShopItem item : shopItems) {
            if (item.id().equalsIgnoreCase(itemId)) {
                return item;
            }
        }
        return null;
    }

    private SlayerTaskAssignment normalizeAssignment(SlayerPlayerData data) {
        SlayerTaskAssignment assignment = data.getAssignment();
        if (assignment == null) {
            return null;
        }
        boolean changed = false;
        if (assignment.getState() == null) {
            assignment.setState(SlayerTaskState.ACCEPTED);
            changed = true;
        }
        if (assignment.getRemainingKills() <= 0 && assignment.getTotalKills() > 0
            && assignment.getState() != SlayerTaskState.COMPLETED) {
            assignment.setState(SlayerTaskState.COMPLETED);
            changed = true;
        }
        if (changed) {
            persist(data);
        }
        return assignment;
    }

    private boolean matchesAssignmentTarget(SlayerTaskAssignment assignment, String npcTypeId) {
        if (assignment == null || npcTypeId == null) {
            return false;
        }
        String targetId = assignment.getTargetNpcTypeId();
        if (targetId != null && targetId.equalsIgnoreCase(npcTypeId)) {
            return true;
        }

        SlayerTaskDefinition task = taskRegistry.getTaskById(assignment.getTaskId());
        if (task != null && npcService.isNpcInGroup(npcTypeId, task.getTargetGroupId())) {
            return true;
        }

        if (targetId == null || targetId.isBlank()) {
            return false;
        }
        String normalizedNpc = npcTypeId.toLowerCase(Locale.ROOT);
        String normalizedTarget = targetId.toLowerCase(Locale.ROOT);
        return normalizedNpc.contains(normalizedTarget) || normalizedTarget.contains(normalizedNpc);
    }

    private SlayerPlayerData loadOrCreate(UUID uuid) {
        SlayerPlayerData stored = repository.load(uuid);
        return stored != null ? stored : new SlayerPlayerData(uuid);
    }

    private void persist(SlayerPlayerData data) {
        if (data == null) {
            return;
        }
        try {
            repository.save(data);
        } catch (RuntimeException e) {
            LOGGER.at(Level.WARNING)
                .log("Failed to save Slayer data for " + (data != null ? data.getUuid() : "unknown") + ": " + e.getMessage());
        }
    }
}
