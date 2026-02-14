package dev.hytalemodding.hyrune.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.Hyrune;
import dev.hytalemodding.hyrune.config.HyruneConfigManager;
import dev.hytalemodding.hyrune.level.LevelingService;
import dev.hytalemodding.hyrune.registry.ToolRequirementRegistry;
import dev.hytalemodding.hyrune.skills.SkillType;
import dev.hytalemodding.hyrune.util.MiningUtils;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Enforces tool/block compatibility and prevents out-of-level ore damage.
 */
public class ToolTypeEnforcementSystem extends EntityEventSystem<EntityStore, DamageBlockEvent> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final long WARNING_COOLDOWN_MS = 1500L;
    private static final Map<UUID, Long> LAST_WARNING = new ConcurrentHashMap<>();

    public ToolTypeEnforcementSystem() {
        super(DamageBlockEvent.class);
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
                       @Nonnull DamageBlockEvent event) {
        if (event.isCancelled()) {
            return;
        }

        PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
        if (playerRef == null || playerRef.getUuid() == null) {
            return;
        }
        boolean debug = HyruneConfigManager.getConfig().durabilityDebugLogging;

        BlockType blockType = event.getBlockType();
        ItemStack itemInHand = event.getItemInHand();
        if (blockType == null || itemInHand == null || itemInHand.getItemId() == null) {
            return;
        }

        boolean isWoodBlock = MiningUtils.isWoodBlock(blockType);
        boolean isStoneOrOreBlock = MiningUtils.isStoneOrOreBlock(blockType);
        boolean usingPickaxe = MiningUtils.isPickaxe(itemInHand);
        boolean usingAxe = MiningUtils.isAxe(itemInHand);

        if (isWoodBlock && usingPickaxe) {
            cancelDamage(event);
            warn(playerRef, "You need an axe to chop this.");
            if (debug) {
                LOGGER.at(Level.INFO).log("[ToolGate] blocked: pickaxe on wood");
            }
            return;
        }

        if (isStoneOrOreBlock && usingAxe) {
            cancelDamage(event);
            warn(playerRef, "You need a pickaxe to mine this.");
            if (debug) {
                LOGGER.at(Level.INFO).log("[ToolGate] blocked: axe on stone/ore");
            }
            return;
        }

        LevelingService service = Hyrune.getService();
        if (service == null) {
            return;
        }

        if (usingPickaxe && isStoneOrOreBlock) {
            Integer toolLevelReq = ToolRequirementRegistry.getRequiredLevel(itemInHand.getItemId());
            int miningLevel = service.getSkillLevel(playerRef.getUuid(), SkillType.MINING);
            if (toolLevelReq != null && miningLevel < toolLevelReq) {
                cancelDamage(event);
                warn(playerRef, "You need Mining level " + toolLevelReq + " to use this pickaxe.");
                if (debug) {
                    LOGGER.at(Level.INFO).log("[ToolGate] blocked: mining level " + miningLevel + " < tool req " + toolLevelReq
                        + " for item=" + itemInHand.getItemId());
                }
                return;
            }

            String blockId = blockType.getId() == null ? null : blockType.getId().toLowerCase(java.util.Locale.ROOT);
            GatheringXpSystem.Reward miningReward = GatheringXpSystem.findMiningReward(blockId);
            if (miningReward != null && miningLevel < miningReward.minLevel) {
                cancelDamage(event);
                warn(playerRef, "You need Mining level " + miningReward.minLevel + " to mine this.");
                if (debug) {
                    LOGGER.at(Level.INFO).log("[ToolGate] blocked: mining level " + miningLevel + " < block req " + miningReward.minLevel
                        + " for block=" + blockId);
                }
            } else if (debug && miningReward == null && blockId != null && blockId.contains("ore")) {
                LOGGER.at(Level.INFO).log("[ToolGate] no custom mining reward rule for ore block=" + blockId + ", not blocked by Hyrune.");
            }
            return;
        }

        if (usingAxe && isWoodBlock) {
            Integer toolLevelReq = ToolRequirementRegistry.getRequiredLevel(itemInHand.getItemId());
            if (toolLevelReq == null) {
                return;
            }
            int woodcuttingLevel = service.getSkillLevel(playerRef.getUuid(), SkillType.WOODCUTTING);
            if (woodcuttingLevel < toolLevelReq) {
                cancelDamage(event);
                warn(playerRef, "You need Woodcutting level " + toolLevelReq + " to use this axe.");
                if (debug) {
                    LOGGER.at(Level.INFO).log("[ToolGate] blocked: woodcutting level " + woodcuttingLevel + " < tool req " + toolLevelReq
                        + " for item=" + itemInHand.getItemId());
                }
            }
        }
    }

    private static void cancelDamage(DamageBlockEvent event) {
        event.setDamage(0f);
        event.setCancelled(true);
    }

    private static void warn(PlayerRef playerRef, String message) {
        UUID uuid = playerRef.getUuid();
        if (uuid == null) {
            return;
        }
        long now = System.currentTimeMillis();
        Long previous = LAST_WARNING.get(uuid);
        if (previous != null && now - previous < WARNING_COOLDOWN_MS) {
            return;
        }
        LAST_WARNING.put(uuid, now);
        playerRef.sendMessage(Message.raw(message));
    }
}
