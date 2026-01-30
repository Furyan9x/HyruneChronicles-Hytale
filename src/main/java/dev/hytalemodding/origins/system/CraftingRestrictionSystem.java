package dev.hytalemodding.origins.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.entity.EntityUtils;
import com.hypixel.hytale.server.core.event.events.ecs.CraftRecipeEvent;
import com.hypixel.hytale.server.core.inventory.MaterialQuantity;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.origins.level.LevelingService;
import dev.hytalemodding.origins.registry.CraftingSkillRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;

public class CraftingRestrictionSystem extends EntityEventSystem<EntityStore, CraftRecipeEvent.Pre> {

    public CraftingRestrictionSystem() {
        super(CraftRecipeEvent.Pre.class);
    }

    @Override
    @Nullable
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public void handle(int index,
                       @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull CraftRecipeEvent.Pre event) {

        var holder = EntityUtils.toHolder(index, archetypeChunk);
        PlayerRef playerRef = holder.getComponent(PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }

        CraftingRecipe recipe = event.getCraftedRecipe();
        if (recipe == null) {
            return;
        }

        MaterialQuantity output = recipe.getPrimaryOutput();
        if (output == null || output.getItemId() == null) {
            return;
        }

        String itemId = output.getItemId().toLowerCase(Locale.ROOT);
        CraftingSkillRegistry.SkillReward match = CraftingSkillRegistry.findByItemId(itemId);
        if (match == null) {
            return;
        }

        LevelingService service = LevelingService.get();
        if (service == null) {
            return;
        }

        int level = service.getSkillLevel(playerRef.getUuid(), match.skill);
        if (level < match.reward.minLevel) {
            event.setCancelled(true);
        }
    }
}
