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
import dev.hytalemodding.origins.registry.ToolRequirementRegistry;
import dev.hytalemodding.origins.util.FarmingHarvestTracker;

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
    private static final Reward DEFAULT_FARMING_REWARD = new Reward(1, 10);
    private static final String[] FARMING_KEYWORDS_LIST = {
            "crop",
            "mushroom",
            "wheat",
            "turnip",
            "tomato",
            "rice",
            "pumpkin",
            "potato",
            "onion",
            "lettuce",
            "cotton",
            "corn",
            "chilli",
            "cauliflower",
            "carrot",
            "berry",
            "aubergine",
            "apple",
            "flower",
            "fiber",
            "cactus",
            "coral",
            "fruit",
            "fern",
            "hay"
    };
    static {
        // Mining examples.
        MINING_KEYWORDS.put("copper", new Reward(1, 5));
        MINING_KEYWORDS.put("iron", new Reward(1, 8));
        MINING_KEYWORDS.put("gold", new Reward(10, 20));
        MINING_KEYWORDS.put("diamond", new Reward(30, 60));
        MINING_KEYWORDS.put("adamantite", new Reward(20, 30));
        MINING_KEYWORDS.put("cobalt", new Reward(20, 30));
        MINING_KEYWORDS.put("mithril", new Reward(30, 40));
        MINING_KEYWORDS.put("onyxium", new Reward(40, 50));
        MINING_KEYWORDS.put("silver", new Reward(10, 18));
        MINING_KEYWORDS.put("thorium", new Reward(35, 45));
        MINING_KEYWORDS.put("rock", new Reward(1, 2));

        // Woodcutting examples.
        WOODCUTTING_KEYWORDS.put("oak", new Reward(1, 8));
        WOODCUTTING_KEYWORDS.put("pine", new Reward(5, 12));
        WOODCUTTING_KEYWORDS.put("wisteria", new Reward(1, 8));
        WOODCUTTING_KEYWORDS.put("windwillow", new Reward(1, 8));
        WOODCUTTING_KEYWORDS.put("stormbark", new Reward(1, 8));
        WOODCUTTING_KEYWORDS.put("spiral", new Reward(1, 8));
        WOODCUTTING_KEYWORDS.put("sallow", new Reward(1, 8));
        WOODCUTTING_KEYWORDS.put("redwood", new Reward(1, 8));
        WOODCUTTING_KEYWORDS.put("poisoned", new Reward(1, 8));
        WOODCUTTING_KEYWORDS.put("petrified", new Reward(1, 8));
        WOODCUTTING_KEYWORDS.put("palo", new Reward(1, 8));
        WOODCUTTING_KEYWORDS.put("palm", new Reward(1, 8));
        WOODCUTTING_KEYWORDS.put("maple", new Reward(1, 8));
        WOODCUTTING_KEYWORDS.put("jungle", new Reward(1, 8));
        WOODCUTTING_KEYWORDS.put("ice", new Reward(1, 8));
        WOODCUTTING_KEYWORDS.put("gumboab", new Reward(1, 8));
        WOODCUTTING_KEYWORDS.put("gnarled", new Reward(1, 8));
        WOODCUTTING_KEYWORDS.put("fire", new Reward(1, 8));
        WOODCUTTING_KEYWORDS.put("fir", new Reward(1, 8));
        WOODCUTTING_KEYWORDS.put("fig_blue", new Reward(1, 8));
        WOODCUTTING_KEYWORDS.put("dry", new Reward(1, 8));
        WOODCUTTING_KEYWORDS.put("crystal", new Reward(1, 8));
        WOODCUTTING_KEYWORDS.put("cedar", new Reward(1, 8));
        WOODCUTTING_KEYWORDS.put("camphor", new Reward(1, 8));
        WOODCUTTING_KEYWORDS.put("burnt", new Reward(1, 8));
        WOODCUTTING_KEYWORDS.put("bottletree", new Reward(1, 8));
        WOODCUTTING_KEYWORDS.put("birch", new Reward(1, 8));
        WOODCUTTING_KEYWORDS.put("beech", new Reward(1, 8));
        WOODCUTTING_KEYWORDS.put("banyan", new Reward(1, 8));
        WOODCUTTING_KEYWORDS.put("bamboo", new Reward(1, 8));
        WOODCUTTING_KEYWORDS.put("azure", new Reward(1, 8));
        WOODCUTTING_KEYWORDS.put("aspen", new Reward(1, 8));
        WOODCUTTING_KEYWORDS.put("ash", new Reward(1, 8));
        WOODCUTTING_KEYWORDS.put("amber", new Reward(1, 8));

        // Farming examples (mature crops only).
        for (String keyword : FARMING_KEYWORDS_LIST) {
            FARMING_KEYWORDS.put(keyword, DEFAULT_FARMING_REWARD);
        }

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

        Reward farmingReward = findFarmingReward(blockId);
        if (farmingReward != null) {
            World world = store.getExternalData().getWorld();
            if (!isMatureCrop(world, blockType, event.getTargetBlock())) {
                return;
            }
            if (isSickleItemId(itemId) && isToolRestricted(playerRef, SkillType.FARMING, itemId, event)) {
                return;
            }
            if (isRestricted(playerRef, SkillType.FARMING, farmingReward, event)) {
                return;
            }
            long xp = farmingReward.xp;
            if (isSickleItemId(itemId)) {
                xp = Math.round(xp * 1.25);
            }
            FarmingHarvestTracker.recordBreak(playerRef.getUuid());
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

        Integer requiredLevel = ToolRequirementRegistry.getRequiredLevel(itemId);
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

    public static boolean isSickleItemId(@Nullable String itemId) {
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
    public static boolean isMatureCrop(World world, BlockType blockType, com.hypixel.hytale.math.vector.Vector3i target) {
        FarmingData farmingData = blockType.getFarming();
        if (farmingData == null || farmingData.getStages() == null) {
            return false;
        }

        String blockId = blockType.getId();
        if (blockId != null && blockId.toLowerCase(Locale.ROOT).contains("stagefinal")) {
            return true;
        }

        if (world == null || target == null) {
            return false;
        }

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

        int localX = ChunkUtil.localCoordinate(target.x);
        int localZ = ChunkUtil.localCoordinate(target.z);
        int blockIndexColumn = ChunkUtil.indexBlockInColumn(localX, target.y, localZ);
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

    public static Reward findFarmingReward(String blockId) {
        return findReward(blockId, FARMING_BLOCKS, FARMING_KEYWORDS);
    }

    static Reward findFarmingHarvestReward(String itemId) {
        if (itemId == null) {
            return null;
        }
        return findReward(itemId.toLowerCase(Locale.ROOT), FARMING_BLOCKS, FARMING_KEYWORDS);
    }

    public static final class Reward {
        public final int minLevel;
        public final long xp;

        private Reward(int minLevel, long xp) {
            this.minLevel = minLevel;
            this.xp = xp;
        }
    }
}
