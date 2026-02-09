package dev.hytalemodding.origins.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.origins.level.LevelingService;
import dev.hytalemodding.origins.skills.SkillType;
import dev.hytalemodding.origins.system.GatheringXpSystem;

import javax.annotation.Nullable;
import java.util.Locale;

/**
 * Event listener for farming harvest.
 */
public class FarmingHarvestListener {

    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event == null || event.isCancelled()) {
            return;
        }

        var target = event.getTargetBlock();
        if (target == null) {
            return;
        }

        Player player = event.getPlayer();
        if (player == null) {
            return;
        }

        Ref<EntityStore> playerEntityRef = player.getReference();
        if (playerEntityRef == null || !playerEntityRef.isValid()) {
            return;
        }

        Store<EntityStore> store = playerEntityRef.getStore();
        PlayerRef playerRef = store.getComponent(playerEntityRef, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }

        World world = Universe.get().getWorld(playerRef.getWorldUuid());
        if (world == null) {
            return;
        }

        InteractionType action = event.getActionType();
        ItemStack itemInHand = event.getItemInHand();
        String itemId = itemInHand != null ? itemInHand.getItemId() : null;
        boolean isSickle = GatheringXpSystem.isSickleItemId(itemId);

        BlockType blockType = getBlockType(world, target);
        if (blockType == null || blockType.getId() == null) {
            return;
        }

        String blockId = blockType.getId().toLowerCase(Locale.ROOT);
        if (action != InteractionType.Use
                && action != InteractionType.Primary
                && action != InteractionType.Secondary
                && !isSickle) {
            return;
        }

        GatheringXpSystem.Reward farmingReward = GatheringXpSystem.findFarmingReward(blockId);
        if (farmingReward == null) {
            return;
        }

        if (!GatheringXpSystem.isMatureCrop(world, blockType, target)) {
            return;
        }

        LevelingService service = LevelingService.get();
        if (service == null) {
            return;
        }

        int level = service.getSkillLevel(playerRef.getUuid(), SkillType.FARMING);
        if (level < farmingReward.minLevel) {
            return;
        }

        long xp = farmingReward.xp;
        if (isSickle) {
            xp = Math.round(xp * 1.25);
        }
        service.addSkillXp(playerRef.getUuid(), SkillType.FARMING, xp);
    }

    @Nullable
    private static BlockType getBlockType(World world, com.hypixel.hytale.math.vector.Vector3i target) {
        ChunkStore chunkStore = world.getChunkStore();
        var chunkRef = chunkStore.getChunkReference(ChunkUtil.indexChunkFromBlock(target.x, target.z));
        if (chunkRef == null || !chunkRef.isValid()) {
            return null;
        }

        WorldChunk worldChunk = chunkStore.getStore().getComponent(chunkRef, WorldChunk.getComponentType());
        if (worldChunk == null) {
            return null;
        }
        return worldChunk.getBlockType(target);
    }
}
