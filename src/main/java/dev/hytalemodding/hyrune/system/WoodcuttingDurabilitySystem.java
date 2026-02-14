package dev.hytalemodding.hyrune.system;

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
import dev.hytalemodding.Hyrune;
import dev.hytalemodding.hyrune.config.HyruneConfigManager;
import dev.hytalemodding.hyrune.level.LevelingService;
import dev.hytalemodding.hyrune.skills.SkillType;
import dev.hytalemodding.hyrune.util.MiningUtils;

import javax.annotation.Nonnull;
import java.util.logging.Level;

/**
 * ECS system for woodcutting durability.
 */
public class WoodcuttingDurabilitySystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final float WOODCUTTING_DURABILITY_REDUCTION_PER_LEVEL = 0.30f / 99.0f;
    public static final float WOODCUTTING_DURABILITY_REDUCTION_CAP = 0.30f;
    private static final double WOODCUTTING_DURABILITY_USE_FALLBACK = 0.25d;

    public WoodcuttingDurabilitySystem() {
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
        boolean debug = HyruneConfigManager.getConfig().durabilityDebugLogging;
        var holder = EntityUtils.toHolder(index, archetypeChunk);
        UUIDComponent uuidComponent = holder.getComponent(UUIDComponent.getComponentType());
        if (uuidComponent == null) {
            if (debug) {
                LOGGER.at(Level.INFO).log("[DurabilityDebug][Woodcutting] skipped: missing UUID component");
            }
            return;
        }

        BlockType blockType = event.getBlockType();
        if (MiningUtils.isStoneOrOreBlock(blockType)) {
            if (debug) {
                LOGGER.at(Level.INFO).log("[DurabilityDebug][Woodcutting] skipped: stone/ore block");
            }
            return;
        }

        ItemStack itemInHand = event.getItemInHand();
        if (!MiningUtils.isAxe(itemInHand)) {
            if (debug) {
                LOGGER.at(Level.INFO).log("[DurabilityDebug][Woodcutting] skipped: not axe");
            }
            return;
        }

        LevelingService service = Hyrune.getService();
        int level = service.getSkillLevel(uuidComponent.getUuid(), SkillType.WOODCUTTING);
        if (level <= 0) {
            if (debug) {
                LOGGER.at(Level.INFO).log("[DurabilityDebug][Woodcutting] skipped: level <= 0");
            }
            return;
        }

        float reduction = Math.min(WOODCUTTING_DURABILITY_REDUCTION_CAP,
            level * WOODCUTTING_DURABILITY_REDUCTION_PER_LEVEL);
        if (reduction <= 0f) {
            if (debug) {
                LOGGER.at(Level.INFO).log("[DurabilityDebug][Woodcutting] skipped: reduction <= 0");
            }
            return;
        }

        double durabilityUse = itemInHand.getItem() == null
            ? 0d
            : BlockHarvestUtils.calculateDurabilityUse(itemInHand.getItem(), blockType);
        boolean syntheticDurabilityUse = false;
        if (durabilityUse <= 0d && MiningUtils.isWoodBlock(blockType)) {
            durabilityUse = WOODCUTTING_DURABILITY_USE_FALLBACK;
            syntheticDurabilityUse = true;
            if (debug) {
                LOGGER.at(Level.INFO).log("[DurabilityDebug][Woodcutting] fallback durabilityUse applied: " + durabilityUse);
            }
        }
        if (durabilityUse <= 0d) {
            if (debug) {
                LOGGER.at(Level.INFO).log("[DurabilityDebug][Woodcutting] skipped: durabilityUse <= 0");
            }
            return;
        }

        double restoreAmount = durabilityUse * reduction;
        if (restoreAmount <= 0d) {
            if (debug) {
                LOGGER.at(Level.INFO).log("[DurabilityDebug][Woodcutting] skipped: restoreAmount <= 0");
            }
            return;
        }

        var player = holder.getComponent(com.hypixel.hytale.server.core.entity.entities.Player.getComponentType());
        if (player == null) {
            if (debug) {
                LOGGER.at(Level.INFO).log("[DurabilityDebug][Woodcutting] skipped: player missing");
            }
            return;
        }

        Inventory inventory = player.getInventory();
        if (inventory == null) {
            if (debug) {
                LOGGER.at(Level.INFO).log("[DurabilityDebug][Woodcutting] skipped: inventory missing");
            }
            return;
        }

        byte activeSlot = inventory.getActiveHotbarSlot();
        if (activeSlot < 0 || activeSlot > 8) {
            if (debug) {
                LOGGER.at(Level.INFO).log("[DurabilityDebug][Woodcutting] skipped: active slot out of range");
            }
            return;
        }

        ItemContainer hotbar = inventory.getHotbar();
        if (hotbar == null) {
            if (debug) {
                LOGGER.at(Level.INFO).log("[DurabilityDebug][Woodcutting] skipped: hotbar missing");
            }
            return;
        }

        ItemStack current = hotbar.getItemStack(activeSlot);
        if (current == null || current.isEmpty()) {
            if (debug) {
                LOGGER.at(Level.INFO).log("[DurabilityDebug][Woodcutting] skipped: current stack empty");
            }
            return;
        }

        ItemStack updated;
        if (syntheticDurabilityUse) {
            double netLoss = Math.max(0d, durabilityUse - restoreAmount);
            double newDurability = Math.max(0d, current.getDurability() - netLoss);
            updated = current.withDurability(newDurability);
        } else {
            updated = current.withIncreasedDurability(restoreAmount);
        }
        hotbar.setItemStackForSlot((short) activeSlot, updated);

        if (debug) {
            String blockId = blockType != null ? blockType.getId() : "unknown_block";
            String itemId = itemInHand.getItemId() != null ? itemInHand.getItemId() : "unknown_item";
            LOGGER.at(Level.INFO).log(
                "[DurabilityDebug][Woodcutting] player=" + uuidComponent.getUuid()
                    + ", item=" + itemId
                    + ", block=" + blockId
                    + ", level=" + level
                    + ", reduction=" + reduction
                    + ", durabilityUse=" + durabilityUse
                    + ", restoreAmount=" + restoreAmount
                    + ", syntheticUse=" + syntheticDurabilityUse
                    + ", before=" + current.getDurability()
                    + ", after=" + updated.getDurability()
            );
        }
    }
}
