package dev.hytalemodding.hyrune.system;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.AnimationUtils;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.hyrune.registry.FishingRegistry;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ECS system for fishing rod idle.
 */
public class FishingRodIdleSystem extends TickingSystem<EntityStore> {
    private static final String IDLE_ANIMATION_NAME = "Idle";
    private static final Set<UUID> IDLE_PLAYERS = ConcurrentHashMap.newKeySet();

    @Override
    public void tick(float deltaTime, int tick, @Nonnull Store<EntityStore> store) {
        // Keep the rod idle pose active while the player is holding a fishing rod.
        EntityStore entityStore = (EntityStore) store.getExternalData();
        World world = entityStore != null ? entityStore.getWorld() : null;
        if (world == null) {
            return;
        }

        for (PlayerRef playerRef : world.getPlayerRefs()) {
            if (playerRef == null || playerRef.getReference() == null) {
                continue;
            }

            UUID playerId = playerRef.getUuid();
            if (FishingCastSystem.hasPendingCast(playerId)) {
                continue;
            }

            Player player = store.getComponent(playerRef.getReference(), Player.getComponentType());
            Inventory inventory = player != null ? player.getInventory() : null;
            ItemStack active = inventory != null ? inventory.getActiveHotbarItem() : null;

            if (FishingRegistry.isFishingRod(active)) {
                if (IDLE_PLAYERS.add(playerId)) {
                    playIdleAnimation(playerRef, store, active);
                }
            } else if (IDLE_PLAYERS.remove(playerId)) {
                AnimationUtils.stopAnimation(playerRef.getReference(), AnimationSlot.Action, store);
            }
        }
    }

    private void playIdleAnimation(PlayerRef playerRef, Store<EntityStore> store, ItemStack active) {
        Item item = active != null ? active.getItem() : null;
        String animationsId = item != null ? item.getPlayerAnimationsId() : null;
        if (animationsId != null && !animationsId.isEmpty()) {
            AnimationUtils.playAnimation(
                playerRef.getReference(),
                AnimationSlot.Action,
                animationsId,
                IDLE_ANIMATION_NAME,
                true,
                store
            );
            return;
        }

        AnimationUtils.playAnimation(
            playerRef.getReference(),
            AnimationSlot.Action,
            IDLE_ANIMATION_NAME,
            true,
            store
        );
    }
}
