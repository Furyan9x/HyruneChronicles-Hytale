package dev.hytalemodding.origins.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.EntityUtils;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.interaction.BlockHarvestUtils;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.Origins;
import dev.hytalemodding.origins.config.OriginsConfigManager;
import dev.hytalemodding.origins.level.LevelingService;
import dev.hytalemodding.origins.skills.SkillType;
import dev.hytalemodding.origins.util.MiningUtils;

import javax.annotation.Nonnull;
import java.util.logging.Level;

/**
 * ECS system for mining durability.
 */
public class MiningDurabilitySystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final float MINING_DURABILITY_REDUCTION_PER_LEVEL = 0.30f / 99.0f;
    public static final float MINING_DURABILITY_REDUCTION_CAP = 0.30f;

    public MiningDurabilitySystem() {
        super(BreakBlockEvent.class);
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public void handle(int index,
                       @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull BreakBlockEvent event) {
        boolean debug = OriginsConfigManager.getConfig().durabilityDebugLogging;
        var holder = EntityUtils.toHolder(index, archetypeChunk);
        UUIDComponent uuidComponent = holder.getComponent(UUIDComponent.getComponentType());
        if (uuidComponent == null) {
            if (debug) {
                LOGGER.at(Level.INFO).log("[DurabilityDebug][Mining] skipped: missing UUID component");
            }
            return;
        }

        BlockType blockType = event.getBlockType();
        if (MiningUtils.isWoodBlock(blockType)) {
            if (debug) {
                LOGGER.at(Level.INFO).log("[DurabilityDebug][Mining] skipped: wood block");
            }
            return;
        }

        ItemStack itemInHand = event.getItemInHand();
        if (!MiningUtils.isPickaxe(itemInHand)) {
            if (debug) {
                LOGGER.at(Level.INFO).log("[DurabilityDebug][Mining] skipped: not pickaxe");
            }
            return;
        }

        LevelingService service = Origins.getService();
        int level = service.getSkillLevel(uuidComponent.getUuid(), SkillType.MINING);
        if (level <= 0) {
            if (debug) {
                LOGGER.at(Level.INFO).log("[DurabilityDebug][Mining] skipped: level <= 0");
            }
            return;
        }

        float reduction = Math.min(MINING_DURABILITY_REDUCTION_CAP,
            level * MINING_DURABILITY_REDUCTION_PER_LEVEL);
        if (reduction <= 0f) {
            if (debug) {
                LOGGER.at(Level.INFO).log("[DurabilityDebug][Mining] skipped: reduction <= 0");
            }
            return;
        }

        double durabilityUse = itemInHand.getItem() == null
            ? 0d
            : BlockHarvestUtils.calculateDurabilityUse(itemInHand.getItem(), blockType);
        if (durabilityUse <= 0d) {
            if (debug) {
                LOGGER.at(Level.INFO).log("[DurabilityDebug][Mining] skipped: durabilityUse <= 0");
            }
            return;
        }

        double restoreAmount = durabilityUse * reduction;
        if (restoreAmount <= 0d) {
            if (debug) {
                LOGGER.at(Level.INFO).log("[DurabilityDebug][Mining] skipped: restoreAmount <= 0");
            }
            return;
        }

        var player = holder.getComponent(com.hypixel.hytale.server.core.entity.entities.Player.getComponentType());
        if (player == null) {
            if (debug) {
                LOGGER.at(Level.INFO).log("[DurabilityDebug][Mining] skipped: player missing");
            }
            return;
        }

        Inventory inventory = player.getInventory();
        if (inventory == null) {
            if (debug) {
                LOGGER.at(Level.INFO).log("[DurabilityDebug][Mining] skipped: inventory missing");
            }
            return;
        }

        byte activeSlot = inventory.getActiveHotbarSlot();
        if (activeSlot < 0 || activeSlot > 8) {
            if (debug) {
                LOGGER.at(Level.INFO).log("[DurabilityDebug][Mining] skipped: active slot out of range");
            }
            return;
        }

        ItemContainer hotbar = inventory.getHotbar();
        if (hotbar == null) {
            if (debug) {
                LOGGER.at(Level.INFO).log("[DurabilityDebug][Mining] skipped: hotbar missing");
            }
            return;
        }

        ItemStack current = hotbar.getItemStack(activeSlot);
        if (current == null || current.isEmpty()) {
            if (debug) {
                LOGGER.at(Level.INFO).log("[DurabilityDebug][Mining] skipped: current stack empty");
            }
            return;
        }

        ItemStack restored = current.withIncreasedDurability(restoreAmount);
        hotbar.setItemStackForSlot((short) activeSlot, restored);

        if (debug) {
            String blockId = blockType != null ? blockType.getId() : "unknown_block";
            String itemId = itemInHand.getItemId() != null ? itemInHand.getItemId() : "unknown_item";
            LOGGER.at(Level.INFO).log(
                "[DurabilityDebug][Mining] player=" + uuidComponent.getUuid()
                    + ", item=" + itemId
                    + ", block=" + blockId
                    + ", level=" + level
                    + ", reduction=" + reduction
                    + ", durabilityUse=" + durabilityUse
                    + ", restoreAmount=" + restoreAmount
                    + ", before=" + current.getDurability()
                    + ", after=" + restored.getDurability()
            );
        }
    }
}
