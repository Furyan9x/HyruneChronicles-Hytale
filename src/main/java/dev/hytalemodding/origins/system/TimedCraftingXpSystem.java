package dev.hytalemodding.origins.system;

import com.hypixel.hytale.builtin.crafting.component.CraftingManager;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.entity.ItemUtils;
import com.hypixel.hytale.server.core.entity.EntityUtils;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.MaterialQuantity;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.entity.entities.player.windows.Window;
import com.hypixel.hytale.server.core.entity.entities.player.windows.WindowManager;
import com.hypixel.hytale.builtin.crafting.window.BenchWindow;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.origins.level.LevelingService;
import dev.hytalemodding.origins.skills.SkillType;
import dev.hytalemodding.origins.registry.CraftingSkillRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;

public class TimedCraftingXpSystem extends EntityTickingSystem<EntityStore> {
    public static final float DOUBLE_PROC_CHANCE_PER_LEVEL = 0.20f / 99.0f;
    public static final float DOUBLE_PROC_CHANCE_CAP = 0.20f;

    private static Class<?> craftingManagerClass;
    private static Class<?> craftingJobClass;
    private static Field queuedJobsField;
    private static Field jobRecipeField;
    private static Field jobQuantityCompletedField;
    private static boolean reflectionInitialized;
    private static Method benchTierMethod;
    private static boolean benchTierReady;

    private final Map<UUID, Map<Object, Integer>> playerJobStates = new HashMap<>();

    public TimedCraftingXpSystem() {
        initializeReflection();
    }

    public void removePlayer(UUID uuid) {
        playerJobStates.remove(uuid);
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public boolean isParallel(int tick, int index) {
        return false;
    }

    @Override
    public void tick(float deltaTime,
                     int index,
                     @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                     @Nonnull Store<EntityStore> store,
                     @Nonnull CommandBuffer<EntityStore> commandBuffer) {

        if (!reflectionInitialized) {
            return;
        }

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
        CraftingManager craftingManager = holder.getComponent(CraftingManager.getComponentType());
        if (craftingManager == null) {
            return;
        }

        try {
            @SuppressWarnings("unchecked")
            BlockingQueue<Object> queuedJobs = (BlockingQueue<Object>) queuedJobsField.get(craftingManager);
            if (queuedJobs == null) {
                return;
            }

            Map<Object, Integer> jobStates = playerJobStates.computeIfAbsent(uuid, id -> new IdentityHashMap<>());
            Set<Object> liveJobs = Collections.newSetFromMap(new IdentityHashMap<>());

            for (Object job : queuedJobs) {
                liveJobs.add(job);
                jobStates.putIfAbsent(job, 0);
            }

            var iterator = jobStates.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Object, Integer> entry = iterator.next();
                Object job = entry.getKey();
                int lastCompleted = entry.getValue();

                int completed;
                try {
                    completed = jobQuantityCompletedField.getInt(job);
                } catch (Exception e) {
                    iterator.remove();
                    continue;
                }

                if (completed > lastCompleted) {
                    int delta = completed - lastCompleted;
                    CraftingRecipe recipe = (CraftingRecipe) jobRecipeField.get(job);
                    grantCraftingXp(player, uuid, recipe, delta);
                    applyDoubleCraft(player, uuid, recipe, delta, store);
                    entry.setValue(completed);
                }

                if (!liveJobs.contains(job)) {
                    iterator.remove();
                }
            }
        } catch (Exception e) {
            // Ignore reflection failures once initialized.
        }
    }

    private void grantCraftingXp(Player player, UUID uuid, @Nullable CraftingRecipe recipe, int quantity) {
        if (recipe == null || quantity <= 0) {
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

        PlayerRef playerRef = Universe.get().getPlayer(uuid);
        if (playerRef == null) {
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

        service.addSkillXp(uuid, match.skill, perItemXp * quantity);
    }

    private void applyDoubleCraft(Player player,
                                  UUID uuid,
                                  @Nullable CraftingRecipe recipe,
                                  int quantity,
                                  Store<EntityStore> store) {
        if (recipe == null || quantity <= 0 || player == null) {
            return;
        }

        MaterialQuantity output = recipe.getPrimaryOutput();
        if (output == null || output.getItemId() == null) {
            return;
        }

        CraftingSkillRegistry.SkillReward match = CraftingSkillRegistry.findByItemId(output.getItemId());
        if (match == null) {
            return;
        }

        if (match.skill != SkillType.COOKING && match.skill != SkillType.ALCHEMY) {
            return;
        }

        LevelingService service = LevelingService.get();
        if (service == null) {
            return;
        }

        int level = service.getSkillLevel(uuid, match.skill);
        if (level <= 0) {
            return;
        }

        float chance = Math.min(DOUBLE_PROC_CHANCE_CAP,
            level * DOUBLE_PROC_CHANCE_PER_LEVEL);
        if (java.util.concurrent.ThreadLocalRandom.current().nextFloat() >= chance) {
            return;
        }

        Inventory inventory = player.getInventory();
        if (inventory == null) {
            return;
        }

        ItemStack baseStack = output.toItemStack();
        if (baseStack == null) {
            return;
        }

        int extraQuantity = baseStack.getQuantity() * quantity;
        if (extraQuantity <= 0) {
            return;
        }

        ItemContainer storage = inventory.getStorage();
        if (storage == null) {
            return;
        }

        ItemStack extra = new ItemStack(baseStack.getItemId(), extraQuantity);
        var transaction = storage.addItemStack(extra);
        ItemStack remainder = transaction.getRemainder();
        if (remainder != null && remainder.getQuantity() > 0) {
            PlayerRef playerRef = Universe.get().getPlayer(uuid);
            if (playerRef != null && playerRef.getReference() != null) {
                ItemUtils.dropItem(playerRef.getReference(), remainder, store);
                player.sendMessage(Message.raw(
                    "Inventory full - extra items dropped at your feet."
                ));
            }
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

    private static void initializeReflection() {
        if (reflectionInitialized) {
            return;
        }

        try {
            craftingManagerClass = Class.forName("com.hypixel.hytale.builtin.crafting.component.CraftingManager");
            craftingJobClass = Class.forName("com.hypixel.hytale.builtin.crafting.component.CraftingManager$CraftingJob");

            queuedJobsField = craftingManagerClass.getDeclaredField("queuedCraftingJobs");
            queuedJobsField.setAccessible(true);

            jobRecipeField = craftingJobClass.getDeclaredField("recipe");
            jobRecipeField.setAccessible(true);

            jobQuantityCompletedField = craftingJobClass.getDeclaredField("quantityCompleted");
            jobQuantityCompletedField.setAccessible(true);

            reflectionInitialized = true;
        } catch (Exception e) {
            reflectionInitialized = false;
        }
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
            } catch (Exception e) {
                benchTierReady = false;
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
        } catch (Exception ignored) {
            // Fallback below.
        }

        return 1;
    }
}
