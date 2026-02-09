package dev.hytalemodding.origins.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.InteractivelyPickupItemEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.origins.level.LevelingService;
import dev.hytalemodding.origins.skills.SkillType;
import dev.hytalemodding.origins.util.FarmingHarvestTracker;

import javax.annotation.Nonnull;
import java.util.Locale;

/**
 * ECS system for farming harvest pickup.
 */
    public class FarmingHarvestPickupSystem extends EntityEventSystem<EntityStore, InteractivelyPickupItemEvent> {
        private static final long BREAK_SUPPRESSION_WINDOW_MS = 5000;
        public static final double SICKLE_XP_BONUS = 1.25;
        public static final double MAX_YIELD_BONUS = 0.50;

    public FarmingHarvestPickupSystem() {
        super(InteractivelyPickupItemEvent.class);
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }

    @Override
    public void handle(int index,
                       @Nonnull ArchetypeChunk<EntityStore> chunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull InteractivelyPickupItemEvent event) {

        PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }

        Player player = chunk.getComponent(index, Player.getComponentType());
        if (player == null) {
            return;
        }

        ItemStack itemStack = event.getItemStack();
        if (itemStack == null || itemStack.getItemId() == null) {
            return;
        }

        String itemId = itemStack.getItemId().toLowerCase(Locale.ROOT);
        if (!itemId.contains("crop") && !itemId.contains("wheat")) {
            return;
        }

        GatheringXpSystem.Reward reward = GatheringXpSystem.findFarmingHarvestReward(itemId);
        if (reward == null) {
            return;
        }

        int baseQty = itemStack.getQuantity();
        int boostedQty = applyYieldBonus(baseQty, playerRef.getUuid());
        if (boostedQty > baseQty) {
            event.setItemStack(itemStack.withQuantity(boostedQty));
        }

        if (FarmingHarvestTracker.wasRecentBreak(playerRef.getUuid(), BREAK_SUPPRESSION_WINDOW_MS)) {
            return;
        }

        LevelingService service = LevelingService.get();
        if (service == null) {
            return;
        }

        int level = service.getSkillLevel(playerRef.getUuid(), SkillType.FARMING);
        if (level < reward.minLevel) {
            return;
        }

        long xp = reward.xp;
        if (itemStack.getQuantity() > 1) {
            xp *= itemStack.getQuantity();
        }
        if (isHoldingSickle(player)) {
            xp = Math.round(xp * SICKLE_XP_BONUS);
        }

        service.addSkillXp(playerRef.getUuid(), SkillType.FARMING, xp);
    }

    private static boolean isHoldingSickle(Player player) {
        if (player == null || player.getInventory() == null) {
            return false;
        }
        try {
            ItemStack held = player.getInventory().getItemInHand();
            if (held == null || held.getItemId() == null) {
                return false;
            }
            return GatheringXpSystem.isSickleItemId(held.getItemId());
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static int applyYieldBonus(int baseQty, java.util.UUID uuid) {
        if (baseQty <= 0 || uuid == null) {
            return baseQty;
        }

        LevelingService service = LevelingService.get();
        if (service == null) {
            return baseQty;
        }

        int level = service.getSkillLevel(uuid, SkillType.FARMING);
        if (level <= 0) {
            return baseQty;
        }

        double bonusPercent = MAX_YIELD_BONUS * (Math.min(level, 99) / 99.0);
        double total = baseQty * (1.0 + bonusPercent);
        int newQty = (int) Math.floor(total);
        double remainder = total - newQty;
        if (Math.random() < remainder) {
            newQty += 1;
        }
        return Math.max(baseQty, newQty);
    }
}

