package dev.hytalemodding.hyrune.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.EntityUtils;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.Hyrune;
import dev.hytalemodding.hyrune.itemization.PlayerItemizationStatsService;
import dev.hytalemodding.hyrune.level.LevelingService;
import dev.hytalemodding.hyrune.skills.SkillType;
import dev.hytalemodding.hyrune.registry.ToolRequirementRegistry;
import dev.hytalemodding.hyrune.util.MiningUtils;

import javax.annotation.Nonnull;

/**
 * ECS system for mining speed.
 */
public class MiningSpeedSystem extends EntityEventSystem<EntityStore, DamageBlockEvent> {
    public static final float MINING_DAMAGE_PER_LEVEL = 0.02f;

    public MiningSpeedSystem() {
        super(DamageBlockEvent.class);
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
                       @Nonnull DamageBlockEvent event) {
        var holder = EntityUtils.toHolder(index, archetypeChunk);
        UUIDComponent uuidComponent = holder.getComponent(UUIDComponent.getComponentType());
        if (uuidComponent == null) {
            return;
        }

        BlockType blockType = event.getBlockType();
        if (MiningUtils.isWoodBlock(blockType)) {
            return;
        }

        ItemStack itemInHand = event.getItemInHand();
        if (!MiningUtils.isPickaxe(itemInHand)) {
            return;
        }

        LevelingService service = Hyrune.getService();
        int level = service.getSkillLevel(uuidComponent.getUuid(), SkillType.MINING);
        if (level <= 0) {
            return;
        }

        Integer requiredLevel = ToolRequirementRegistry.getRequiredLevel(itemInHand.getItemId());
        if (requiredLevel != null && level < requiredLevel) {
            event.setCancelled(true);
            return;
        }

        double itemBonus = PlayerItemizationStatsService.getCached(uuidComponent.getUuid()).getItemBlockBreakSpeedBonus();
        float multiplier = (1.0f + (level * MINING_DAMAGE_PER_LEVEL)) * (float) (1.0 + itemBonus);
        event.setDamage(event.getDamage() * multiplier);
    }

}
