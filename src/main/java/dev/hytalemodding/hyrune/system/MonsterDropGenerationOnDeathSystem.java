package dev.hytalemodding.hyrune.system;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.asset.type.gameplay.DeathConfig;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.item.ItemModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.systems.NPCDamageSystems;
import dev.hytalemodding.hyrune.itemization.ItemGenerationService;
import dev.hytalemodding.hyrune.itemization.ItemRarityRollModel;
import dev.hytalemodding.hyrune.itemization.ItemRollSource;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Set;

/**
 * Rolls NPC death-drop stacks before world spawn so world-space rarity VFX can reflect final rarity.
 */
public class MonsterDropGenerationOnDeathSystem extends DeathSystems.OnDeathSystem {
    private static final Query<EntityStore> QUERY = Query.and(
        NPCEntity.getComponentType(),
        TransformComponent.getComponentType(),
        HeadRotation.getComponentType()
    );

    private static final Set<Dependency<EntityStore>> DEPENDENCIES = Set.of(
        new SystemDependency<>(Order.BEFORE, NPCDamageSystems.DropDeathItems.class)
    );

    @Override
    public Query<EntityStore> getQuery() {
        return QUERY;
    }

    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return DEPENDENCIES;
    }

    @Override
    public void onComponentAdded(@Nonnull Ref<EntityStore> ref,
                                 @Nonnull DeathComponent component,
                                 @Nonnull Store<EntityStore> store,
                                 @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        if (component.getItemsLossMode() != DeathConfig.ItemsLossMode.ALL) {
            return;
        }

        NPCEntity npcComponent = commandBuffer.getComponent(ref, NPCEntity.getComponentType());
        if (npcComponent == null) {
            return;
        }

        Role role = npcComponent.getRole();
        if (role == null) {
            return;
        }

        List<ItemStack> itemsToDrop = new ObjectArrayList<>();
        if (role.isPickupDropOnDeath()) {
            Inventory inventory = npcComponent.getInventory();
            if (inventory != null && inventory.getStorage() != null) {
                itemsToDrop.addAll(inventory.getStorage().dropAllItemStacks());
            }
        }

        String dropListId = role.getDropListId();
        if (dropListId != null) {
            ItemModule itemModule = ItemModule.get();
            if (itemModule != null && itemModule.isEnabled()) {
                List<ItemStack> randomItemsToDrop = itemModule.getRandomItemDrops(dropListId);
                if (randomItemsToDrop != null && !randomItemsToDrop.isEmpty()) {
                    itemsToDrop.addAll(randomItemsToDrop);
                }
            }
        }

        if (itemsToDrop.isEmpty()) {
            return;
        }

        List<ItemStack> rolledDrops = new ObjectArrayList<>(itemsToDrop.size());
        for (ItemStack stack : itemsToDrop) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            ItemRarityRollModel.GenerationContext context = ItemRarityRollModel.GenerationContext.of("npc_death_droplist");
            ItemStack rolled = ItemGenerationService.rollIfEligible(
                stack,
                ItemRollSource.MONSTER_DROP,
                context
            );
            rolledDrops.add(rolled);
        }
        if (rolledDrops.isEmpty()) {
            return;
        }

        TransformComponent transformComponent = store.getComponent(ref, TransformComponent.getComponentType());
        HeadRotation headRotationComponent = store.getComponent(ref, HeadRotation.getComponentType());
        if (transformComponent == null || headRotationComponent == null) {
            return;
        }

        Vector3d position = transformComponent.getPosition();
        Vector3f headRotation = headRotationComponent.getRotation();
        Vector3d dropPosition = position.clone().add(0.0, 1.0, 0.0);

        Holder<EntityStore>[] drops = ItemComponent.generateItemDrops(store, rolledDrops, dropPosition, headRotation.clone());
        commandBuffer.addEntities(drops, AddReason.SPAWN);

        // Suppress core NPC drop-spawn system after we emit rolled drops.
        component.setItemsLossMode(DeathConfig.ItemsLossMode.NONE);
    }
}
