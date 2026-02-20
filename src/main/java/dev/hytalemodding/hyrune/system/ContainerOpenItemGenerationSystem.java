package dev.hytalemodding.hyrune.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk;
import com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.hyrune.itemization.ItemGenerationService;
import dev.hytalemodding.hyrune.itemization.ItemInstanceMetadata;
import dev.hytalemodding.hyrune.itemization.ItemRarityRollModel;
import dev.hytalemodding.hyrune.itemization.ItemRollSource;
import dev.hytalemodding.hyrune.itemization.ItemizationEligibilityService;

import javax.annotation.Nonnull;

/**
 * Rolls container loot before a block container window opens, so players see final rolled items immediately.
 */
public class ContainerOpenItemGenerationSystem extends EntityEventSystem<EntityStore, UseBlockEvent.Pre> {
    public ContainerOpenItemGenerationSystem() {
        super(UseBlockEvent.Pre.class);
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
                       @Nonnull UseBlockEvent.Pre event) {
        World world = store.getExternalData().getWorld();
        if (world == null || event.getTargetBlock() == null) {
            return;
        }

        Vector3i pos = event.getTargetBlock();
        Ref<ChunkStore> chunkRef = world.getChunkStore().getChunkReference(ChunkUtil.indexChunkFromBlock(pos.x, pos.z));
        if (chunkRef == null || !chunkRef.isValid()) {
            return;
        }
        BlockComponentChunk blockComponentChunk = world.getChunkStore().getStore().getComponent(chunkRef, BlockComponentChunk.getComponentType());
        if (blockComponentChunk == null) {
            return;
        }
        int blockIndex = ChunkUtil.indexBlockInColumn(pos.x, pos.y, pos.z);
        Ref<ChunkStore> blockRef = blockComponentChunk.getEntityReference(blockIndex);
        if (blockRef == null || !blockRef.isValid()) {
            return;
        }

        Store<ChunkStore> chunkStore = world.getChunkStore().getStore();
        ComponentType<ChunkStore, ItemContainerState> itemContainerComponentType = findItemContainerStateComponentType(chunkStore, blockRef);
        if (itemContainerComponentType == null) {
            return;
        }
        ItemContainerState itemContainerState = chunkStore.getComponent(blockRef, itemContainerComponentType);
        if (itemContainerState == null) {
            return;
        }

        ItemContainer container = itemContainerState.getItemContainer();
        if (container == null) {
            return;
        }

        short capacity = container.getCapacity();
        for (short slot = 0; slot < capacity; slot++) {
            ItemStack current = container.getItemStack(slot);
            if (current == null || current.isEmpty()) {
                continue;
            }
            if (!ItemizationEligibilityService.isEligible(current)) {
                continue;
            }
            if (current.getFromMetadataOrNull(ItemInstanceMetadata.KEYED_CODEC) != null) {
                continue;
            }

            ItemRarityRollModel.GenerationContext context = ItemRarityRollModel.GenerationContext.of("container_open_block");
            ItemStack rolled = ItemGenerationService.rollIfEligible(
                current,
                ItemRollSource.CONTAINER_LOOT,
                context
            );
            container.replaceItemStackInSlot(slot, current, rolled);
        }
    }

    @SuppressWarnings("unchecked")
    private static ComponentType<ChunkStore, ItemContainerState> findItemContainerStateComponentType(Store<ChunkStore> chunkStore, Ref<ChunkStore> blockRef) {
        var archetype = chunkStore.getArchetype(blockRef);
        if (archetype == null) {
            return null;
        }
        for (int i = archetype.getMinIndex(); i < archetype.length(); i++) {
            ComponentType<ChunkStore, ? extends Component<ChunkStore>> type =
                (ComponentType<ChunkStore, ? extends Component<ChunkStore>>) archetype.get(i);
            if (type != null && ItemContainerState.class.isAssignableFrom(type.getTypeClass())) {
                return (ComponentType<ChunkStore, ItemContainerState>) type;
            }
        }
        return null;
    }
}
