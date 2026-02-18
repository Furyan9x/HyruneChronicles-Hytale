package dev.hytalemodding.hyrune.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.entity.EntityUtils;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.CraftRecipeEvent;
import com.hypixel.hytale.server.core.inventory.MaterialQuantity;
import com.hypixel.hytale.server.core.entity.entities.player.windows.Window;
import com.hypixel.hytale.server.core.entity.entities.player.windows.WindowManager;
import com.hypixel.hytale.builtin.crafting.window.BenchWindow;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.hyrune.itemization.ItemRollCoordinator;
import dev.hytalemodding.hyrune.level.LevelingService;
import dev.hytalemodding.hyrune.registry.CraftingSkillRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.UUID;

/**
 * ECS system for crafting xp.
 */
public class CraftingXpSystem extends EntityEventSystem<EntityStore, CraftRecipeEvent.Post> {

    private static Method benchTierMethod;
    private static boolean benchTierReady;

    public CraftingXpSystem() {
        super(CraftRecipeEvent.Post.class);
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
                       @Nonnull CraftRecipeEvent.Post event) {

        var holder = EntityUtils.toHolder(index, archetypeChunk);
        Player player = holder.getComponent(Player.getComponentType());
        if (player == null) {
            return;
        }

        UUIDComponent uuidComponent = holder.getComponent(UUIDComponent.getComponentType());
        if (uuidComponent == null) {
            return;
        }

        UUID uuid = uuidComponent.getUuid();
        PlayerRef playerRef = Universe.get().getPlayer(uuid);
        if (playerRef == null) {
            return;
        }

        CraftingRecipe recipe = event.getCraftedRecipe();
        if (recipe == null) {
            return;
        }

        String recipeId = recipe.getId() != null ? recipe.getId().toLowerCase(Locale.ROOT) : null;
        CraftingSkillRegistry.SkillReward match = CraftingSkillRegistry.findByRecipeId(recipeId);
        if (match == null) {
            String itemId = getOutputItemId(recipe);
            match = CraftingSkillRegistry.findByItemId(itemId);
        }

        if (match == null || match.reward.xp <= 0) {
            return;
        }

        LevelingService service = LevelingService.get();
        if (service == null) {
            return;
        }

        String itemId = getOutputItemId(recipe);
        double materialMultiplier = CraftingSkillRegistry.getMaterialMultiplier(itemId);
        int benchTier = getBenchTier(player);
        double benchMultiplier = CraftingSkillRegistry.getBenchTierMultiplier(benchTier);

        long perItemXp = Math.round(match.reward.xp * materialMultiplier * benchMultiplier);
        if (perItemXp <= 0) {
            perItemXp = 1;
        }

        long xp = perItemXp * event.getQuantity();
        service.addSkillXp(uuid, match.skill, xp);
        int professionLevel = service.getSkillLevel(uuid, match.skill);

        if (itemId != null) {
            ItemRollCoordinator.queueCraftedRoll(
                uuid,
                itemId,
                event.getQuantity(),
                match.skill.name(),
                professionLevel,
                benchTier
            );
            // Attempt immediate apply in case output is already in inventory.
            ItemRollCoordinator.applyPendingCraftRolls(player);
        }
    }

    @Nullable
    private static String getOutputItemId(CraftingRecipe recipe) {
        MaterialQuantity output = recipe.getPrimaryOutput();
        if (output == null || output.getItemId() == null) {
            return null;
        }
        return output.getItemId().toLowerCase(Locale.ROOT);
    }

    private static int getBenchTier(Player player) {
        WindowManager windowManager = player.getWindowManager();
        if (windowManager == null) {
            return 1;
        }

        for (Window window : windowManager.getWindows()) {
            if (window instanceof BenchWindow) {
                return invokeBenchTier(window);
            }
        }

        return 1;
    }

    private static int invokeBenchTier(Window window) {
        if (!benchTierReady) {
            try {
                benchTierMethod = BenchWindow.class.getDeclaredMethod("getBenchTierLevel");
                benchTierMethod.setAccessible(true);
                benchTierReady = true;
            } catch (RuntimeException e) {
                benchTierReady = false;
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }

        if (!benchTierReady || benchTierMethod == null) {
            return 1;
        }

        try {
            Object result = benchTierMethod.invoke(window);
            if (result instanceof Integer) {
                return (Integer) result;
            }
        } catch (RuntimeException ignored) {
            // Fallback below.
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        return 1;
    }
}

