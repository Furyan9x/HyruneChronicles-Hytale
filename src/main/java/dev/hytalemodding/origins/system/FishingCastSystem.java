package dev.hytalemodding.origins.system;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.origins.interaction.FishingInteraction;
import dev.hytalemodding.origins.registry.FishingRegistry;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FishingCastSystem extends TickingSystem<EntityStore> {
    private static final Map<UUID, PendingCast> PENDING_CASTS = new ConcurrentHashMap<>();
    private static final int MAX_CAST_DISTANCE = 10;

    public static boolean queueCast(UUID playerId,
                                    UUID worldId,
                                    Vector3i target,
                                    FishingRegistry.BaitDefinition bait,
                                    int fishingLevel,
                                    int delayTicks) {
        if (playerId == null || worldId == null || target == null || bait == null || delayTicks < 0) {
            return false;
        }
        return PENDING_CASTS.putIfAbsent(
            playerId,
            new PendingCast(worldId, target, bait, fishingLevel, delayTicks)
        ) == null;
    }

    public static boolean hasPendingCast(UUID playerId) {
        return playerId != null && PENDING_CASTS.containsKey(playerId);
    }

    public static void cancelPendingCast(UUID playerId) {
        if (playerId != null) {
            PENDING_CASTS.remove(playerId);
        }
    }

    @Override
    public void tick(float deltaTime, int tick, @Nonnull Store<EntityStore> store) {
        // Delay bobber spawning until the cast animation finishes, then validate state.
        if (PENDING_CASTS.isEmpty()) {
            return;
        }

        EntityStore entityStore = (EntityStore) store.getExternalData();
        World world = entityStore != null ? entityStore.getWorld() : null;
        if (world == null) {
            return;
        }

        Iterator<Map.Entry<UUID, PendingCast>> iterator = PENDING_CASTS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PendingCast> entry = iterator.next();
            PendingCast cast = entry.getValue();
            if (cast == null) {
                iterator.remove();
                continue;
            }

            World castWorld = Universe.get().getWorld(cast.worldId);
            if (castWorld != world) {
                continue;
            }

            if (cast.ticksRemaining > 0) {
                if (!isCastStillValid(store, entry.getKey(), cast)) {
                    iterator.remove();
                    continue;
                }
                cast.ticksRemaining -= 1;
                continue;
            }

            PlayerRef playerRef = Universe.get().getPlayer(entry.getKey());
            if (playerRef == null
                || playerRef.getReference() == null
                || !cast.worldId.equals(playerRef.getWorldUuid())) {
                iterator.remove();
                continue;
            }

            Player player = store.getComponent(playerRef.getReference(), Player.getComponentType());
            Inventory inventory = player != null ? player.getInventory() : null;
            ItemStack active = inventory != null ? inventory.getActiveHotbarItem() : null;
            if (!FishingRegistry.isFishingRod(active) || !withinCastDistance(store, playerRef, cast.target)) {
                iterator.remove();
                continue;
            }

            UUID bobberId = FishingInteraction.spawnBobber(
                store,
                cast.target,
                playerRef,
                cast.bait,
                cast.fishingLevel
            );
            if (bobberId != null) {
                FishingInteraction.updateRodMetadata(inventory, bobberId);
                FishingInteraction.playCastSound(playerRef);
            }

            iterator.remove();
        }
    }

    private static boolean isCastStillValid(Store<EntityStore> store, UUID playerId, PendingCast cast) {
        PlayerRef playerRef = Universe.get().getPlayer(playerId);
        if (playerRef == null
            || playerRef.getReference() == null
            || !cast.worldId.equals(playerRef.getWorldUuid())) {
            return false;
        }

        Player player = store.getComponent(playerRef.getReference(), Player.getComponentType());
        Inventory inventory = player != null ? player.getInventory() : null;
        ItemStack active = inventory != null ? inventory.getActiveHotbarItem() : null;
        return FishingRegistry.isFishingRod(active) && withinCastDistance(store, playerRef, cast.target);
    }

    private static boolean withinCastDistance(Store<EntityStore> store, PlayerRef playerRef, Vector3i target) {
        TransformComponent transform = store.getComponent(playerRef.getReference(), TransformComponent.getComponentType());
        if (transform == null) {
            return false;
        }
        double dx = transform.getPosition().x - (target.x + 0.5);
        double dy = transform.getPosition().y - (target.y + 0.5);
        double dz = transform.getPosition().z - (target.z + 0.5);
        return (dx * dx + dy * dy + dz * dz) <= (MAX_CAST_DISTANCE * MAX_CAST_DISTANCE);
    }

    private static final class PendingCast {
        private final UUID worldId;
        private final Vector3i target;
        private final FishingRegistry.BaitDefinition bait;
        private final int fishingLevel;
        private int ticksRemaining;

        private PendingCast(UUID worldId,
                            Vector3i target,
                            FishingRegistry.BaitDefinition bait,
                            int fishingLevel,
                            int ticksRemaining) {
            this.worldId = worldId;
            this.target = target;
            this.bait = bait;
            this.fishingLevel = fishingLevel;
            this.ticksRemaining = ticksRemaining;
        }
    }
}
