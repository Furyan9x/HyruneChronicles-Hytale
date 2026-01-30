package dev.hytalemodding.origins.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
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
import dev.hytalemodding.origins.level.LevelingService;
import dev.hytalemodding.origins.skills.SkillType;
import dev.hytalemodding.origins.util.MiningUtils;

import javax.annotation.Nonnull;

public class WoodcuttingDurabilitySystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {
    public static final float WOODCUTTING_DURABILITY_REDUCTION_PER_LEVEL = 0.30f / 99.0f;
    public static final float WOODCUTTING_DURABILITY_REDUCTION_CAP = 0.30f;

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
        var holder = EntityUtils.toHolder(index, archetypeChunk);
        UUIDComponent uuidComponent = holder.getComponent(UUIDComponent.getComponentType());
        if (uuidComponent == null) {
            return;
        }

        BlockType blockType = event.getBlockType();
        if (MiningUtils.isStoneOrOreBlock(blockType)) {
            return;
        }

        ItemStack itemInHand = event.getItemInHand();
        if (!MiningUtils.isAxe(itemInHand)) {
            return;
        }

        LevelingService service = Origins.getService();
        int level = service.getSkillLevel(uuidComponent.getUuid(), SkillType.WOODCUTTING);
        if (level <= 0) {
            return;
        }

        float reduction = Math.min(WOODCUTTING_DURABILITY_REDUCTION_CAP,
            level * WOODCUTTING_DURABILITY_REDUCTION_PER_LEVEL);
        if (reduction <= 0f) {
            return;
        }

        double durabilityUse = itemInHand.getItem() == null
            ? 0d
            : BlockHarvestUtils.calculateDurabilityUse(itemInHand.getItem(), blockType);
        if (durabilityUse <= 0d) {
            return;
        }

        double restoreAmount = durabilityUse * reduction;
        if (restoreAmount <= 0d) {
            return;
        }

        var player = holder.getComponent(com.hypixel.hytale.server.core.entity.entities.Player.getComponentType());
        if (player == null) {
            return;
        }

        Inventory inventory = player.getInventory();
        if (inventory == null) {
            return;
        }

        byte activeSlot = inventory.getActiveHotbarSlot();
        if (activeSlot < 0 || activeSlot > 8) {
            return;
        }

        ItemContainer hotbar = inventory.getHotbar();
        if (hotbar == null) {
            return;
        }

        ItemStack current = hotbar.getItemStack(activeSlot);
        if (current == null || current.isEmpty()) {
            return;
        }

        ItemStack restored = current.withIncreasedDurability(restoreAmount);
        hotbar.setItemStackForSlot((short) activeSlot, restored);
    }
}
