package dev.hytalemodding.hyrune.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.ItemUtils;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.InteractivelyPickupItemEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.hyrune.itemization.GatheringUtilityDropService;
import dev.hytalemodding.hyrune.itemization.PlayerItemizationStatsService;
import dev.hytalemodding.hyrune.level.LevelingService;
import dev.hytalemodding.hyrune.skills.SkillType;

import javax.annotation.Nonnull;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * ECS system for farming harvest pickup.
 */
    public class FarmingHarvestPickupSystem extends EntityEventSystem<EntityStore, InteractivelyPickupItemEvent> {
        public static final double SICKLE_XP_BONUS = 1.25;

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
        if (!itemId.contains("crop")) {
            return;
        }

        GatheringXpSystem.Reward reward = GatheringXpSystem.findFarmingHarvestReward(itemId);
        if (reward == null) {
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

        var stats = PlayerItemizationStatsService.getOrRecompute(player);
        if (GatheringUtilityDropService.shouldDoubleDropForGathering(stats.getItemDoubleDropChanceBonus(), ThreadLocalRandom.current())) {
            itemStack = itemStack.withQuantity(GatheringUtilityDropService.doubledQuantity(itemStack.getQuantity()));
            event.setItemStack(itemStack);
        }

        long xp = reward.xp;
        if (itemStack.getQuantity() > 1) {
            xp *= itemStack.getQuantity();
        }
        if (isHoldingSickle(player)) {
            xp = Math.round(xp * SICKLE_XP_BONUS);
        }

        service.addSkillXp(playerRef.getUuid(), SkillType.FARMING, xp);

        if (playerRef.getReference() == null) {
            return;
        }
        for (ItemStack rare : GatheringUtilityDropService.resolveRareDrops(SkillType.FARMING, stats, ThreadLocalRandom.current())) {
            if (rare == null || rare.isEmpty()) {
                continue;
            }
            ItemUtils.dropItem(playerRef.getReference(), rare, store);
        }
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

}

