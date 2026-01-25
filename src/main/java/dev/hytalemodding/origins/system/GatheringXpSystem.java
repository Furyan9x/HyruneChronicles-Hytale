package dev.hytalemodding.origins.system;

import com.hypixel.hytale.builtin.adventure.farming.states.FarmingBlock;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.farming.FarmingData;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.farming.FarmingStageData;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import dev.hytalemodding.origins.level.LevelingService;
import dev.hytalemodding.origins.skills.SkillType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class GatheringXpSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {

    private static final Map<String, Reward> MINING_BLOCKS = new HashMap<>();
    private static final Map<String, Reward> MINING_KEYWORDS = new HashMap<>();
    private static final Map<String, Reward> WOODCUTTING_BLOCKS = new HashMap<>();
    private static final Map<String, Reward> WOODCUTTING_KEYWORDS = new HashMap<>();
    private static final Map<String, Reward> FARMING_BLOCKS = new HashMap<>();
    private static final Map<String, Reward> FARMING_KEYWORDS = new HashMap<>();
    private static final Map<String, Integer> TOOL_LEVELS = new HashMap<>();

    static {
        // Mining examples.
        MINING_KEYWORDS.put("copper", new Reward(1, 5));
        MINING_KEYWORDS.put("iron", new Reward(1, 8));
        MINING_KEYWORDS.put("gold", new Reward(10, 20));
        MINING_KEYWORDS.put("diamond", new Reward(30, 60));

        // Woodcutting examples.
        WOODCUTTING_KEYWORDS.put("oak", new Reward(1, 8));
        WOODCUTTING_KEYWORDS.put("pine", new Reward(5, 12));

        // Farming example (mature crops only).
        FARMING_KEYWORDS.put("wheat", new Reward(1, 6));

        // Tool level requirements (material -> min level).
        TOOL_LEVELS.put("crude", 1);
        TOOL_LEVELS.put("copper", 10);
        TOOL_LEVELS.put("iron", 15);
    }

    public GatheringXpSystem() {
        super(BreakBlockEvent.class);
    }

    @Override
    @Nullable
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }

    @Override
    public void handle(int index,
                       @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull BreakBlockEvent event) {

        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (player == null || playerRef == null) {
            return;
        }

        BlockType blockType = event.getBlockType();
        if (blockType == null) {
            return;
        }

        String blockId = blockType.getId().toLowerCase(Locale.ROOT);
        String itemId = getHeldItemId(event);

        Reward miningReward = findReward(blockId, MINING_BLOCKS, MINING_KEYWORDS);
        if (miningReward != null) {
            if (!isPickaxe(itemId)) {
                return;
            }
            if (isToolRestricted(playerRef, SkillType.MINING, itemId, event)) {
                return;
            }
            if (isRestricted(playerRef, SkillType.MINING, miningReward, event)) {
                return;
            }
            LevelingService.get().addSkillXp(playerRef.getUuid(), SkillType.MINING, miningReward.xp);
            return;
        }

        Reward woodcuttingReward = findReward(blockId, WOODCUTTING_BLOCKS, WOODCUTTING_KEYWORDS);
        if (woodcuttingReward != null) {
            if (!isAxe(itemId)) {
                return;
            }
            if (isToolRestricted(playerRef, SkillType.WOODCUTTING, itemId, event)) {
                return;
            }
            if (isRestricted(playerRef, SkillType.WOODCUTTING, woodcuttingReward, event)) {
                return;
            }
            LevelingService.get().addSkillXp(playerRef.getUuid(), SkillType.WOODCUTTING, woodcuttingReward.xp);
            return;
        }

        Reward farmingReward = findReward(blockId, FARMING_BLOCKS, FARMING_KEYWORDS);
        if (farmingReward != null) {
            if (!isMatureCrop(store, blockType, event)) {
                return;
            }
            if (isSickle(itemId) && isToolRestricted(playerRef, SkillType.FARMING, itemId, event)) {
                return;
            }
            if (isRestricted(playerRef, SkillType.FARMING, farmingReward, event)) {
                return;
            }
            long xp = farmingReward.xp;
            if (isSickle(itemId)) {
                xp = Math.round(xp * 1.25);
            }
            LevelingService.get().addSkillXp(playerRef.getUuid(), SkillType.FARMING, xp);
        }
    }

    private static Reward findReward(String blockId, Map<String, Reward> exact, Map<String, Reward> keywords) {
        Reward reward = exact.get(blockId);
        if (reward != null) {
            return reward;
        }

        for (Map.Entry<String, Reward> entry : keywords.entrySet()) {
            if (blockId.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return null;
    }

    private static boolean isToolRestricted(PlayerRef playerRef, SkillType skill, @Nullable String itemId, BreakBlockEvent event) {
        if (itemId == null) {
            return false;
        }

        Integer requiredLevel = getToolRequiredLevel(itemId);
        if (requiredLevel == null) {
            return false;
        }

        LevelingService service = LevelingService.get();
        if (service == null) {
            return true;
        }

        int level = service.getSkillLevel(playerRef.getUuid(), skill);
        if (level < requiredLevel) {
            event.setCancelled(true);
            return true;
        }

        return false;
    }

    private static boolean isRestricted(PlayerRef playerRef, SkillType skill, Reward reward, BreakBlockEvent event) {
        LevelingService service = LevelingService.get();
        if (service == null) {
            return true;
        }

        int level = service.getSkillLevel(playerRef.getUuid(), skill);
        if (level < reward.minLevel) {
            event.setCancelled(true);
            return true;
        }

        return false;
    }

    private static boolean isAxe(@Nullable String itemId) {
        if (itemId == null) {
            return false;
        }
        String id = itemId.toLowerCase(Locale.ROOT);
        return (id.contains("axe") && !id.contains("pickaxe")) || id.contains("hatchet");
    }

    private static boolean isPickaxe(@Nullable String itemId) {
        if (itemId == null) {
            return false;
        }
        String id = itemId.toLowerCase(Locale.ROOT);
        return id.contains("pickaxe") || id.contains("pick") && id.contains("axe");
    }

    @Nullable
    private static Integer getToolRequiredLevel(String itemId) {
        String id = itemId.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, Integer> entry : TOOL_LEVELS.entrySet()) {
            if (id.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static boolean isSickle(@Nullable String itemId) {
        if (itemId == null) {
            return false;
        }
        return itemId.toLowerCase(Locale.ROOT).contains("sickle");
    }

    private static String getHeldItemId(BreakBlockEvent event) {
        if (event.getItemInHand() == null || event.getItemInHand().getItemId() == null) {
            return null;
        }
        return event.getItemInHand().getItemId().toLowerCase(Locale.ROOT);
    }

    /**
     * Returns true when the farming block is at its final growth stage.
     */
    private static boolean isMatureCrop(Store<EntityStore> store, BlockType blockType, BreakBlockEvent event) {
        FarmingData farmingData = blockType.getFarming();
        if (farmingData == null || farmingData.getStages() == null) {
            return false;
        }

        World world = store.getExternalData().getWorld();
        if (world == null || event.getTargetBlock() == null) {
            return false;
        }

        var target = event.getTargetBlock();
        ChunkStore chunkStore = world.getChunkStore();
        Ref<ChunkStore> chunkRef = chunkStore.getChunkReference(ChunkUtil.indexChunkFromBlock(target.x, target.z));
        if (chunkRef == null || !chunkRef.isValid()) {
            return false;
        }

        BlockChunk blockChunk = chunkStore.getStore().getComponent(chunkRef, BlockChunk.getComponentType());
        if (blockChunk == null) {
            return false;
        }

        BlockSection section = blockChunk.getSectionAtBlockY(target.y);
        if (section == null) {
            return false;
        }

        BlockComponentChunk blockComponentChunk = chunkStore.getStore().getComponent(chunkRef, BlockComponentChunk.getComponentType());
        if (blockComponentChunk == null) {
            return false;
        }

        int blockIndexColumn = ChunkUtil.indexBlockInColumn(target.x, target.y, target.z);
        Ref<ChunkStore> blockRef = blockComponentChunk.getEntityReference(blockIndexColumn);
        if (blockRef == null || !blockRef.isValid()) {
            return false;
        }

        FarmingBlock farmingBlock = chunkStore.getStore().getComponent(blockRef, FarmingBlock.getComponentType());
        if (farmingBlock == null) {
            return false;
        }

        String stageSet = farmingBlock.getCurrentStageSet();
        if (stageSet == null) {
            stageSet = farmingData.getStartingStageSet();
        }
        if (stageSet == null) {
            return false;
        }

        FarmingStageData[] stages = farmingData.getStages().get(stageSet);
        if (stages == null || stages.length == 0) {
            return false;
        }

        int currentStage = (int) farmingBlock.getGrowthProgress();
        return currentStage >= stages.length - 1;
    }

    private static final class Reward {
        private final int minLevel;
        private final long xp;

        private Reward(int minLevel, long xp) {
            this.minLevel = minLevel;
            this.xp = xp;
        }
    }
}
