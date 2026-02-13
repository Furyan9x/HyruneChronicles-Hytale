package dev.hytalemodding.origins.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.Origins;
import dev.hytalemodding.origins.level.LevelingService;
import dev.hytalemodding.origins.registry.FarmingRequirementRegistry;
import dev.hytalemodding.origins.skills.SkillType;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enforces farming level requirements for planting seeds.
 */
public class FarmingPlantingRestrictionSystem extends EntityEventSystem<EntityStore, PlaceBlockEvent> {
    private static final long WARNING_COOLDOWN_MS = 2000L;
    private static final Map<UUID, Long> LAST_WARNING_MS = new ConcurrentHashMap<>();

    public FarmingPlantingRestrictionSystem() {
        super(PlaceBlockEvent.class);
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }

    @Override
    public void handle(int index,
                       @Nonnull ArchetypeChunk<EntityStore> chunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull PlaceBlockEvent event) {
        if (event.isCancelled() || event.getItemInHand() == null || event.getItemInHand().getItemId() == null) {
            return;
        }

        PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
        if (playerRef == null || playerRef.getUuid() == null) {
            return;
        }

        String itemId = event.getItemInHand().getItemId();
        Integer requiredLevel = FarmingRequirementRegistry.getSeedRequiredLevel(itemId);
        if (requiredLevel == null) {
            return;
        }

        LevelingService service = Origins.getService();
        if (service == null) {
            return;
        }

        int farmingLevel = service.getSkillLevel(playerRef.getUuid(), SkillType.FARMING);
        if (farmingLevel >= requiredLevel) {
            return;
        }

        event.setCancelled(true);
        sendRateLimitedMessage(playerRef, requiredLevel);
    }

    private void sendRateLimitedMessage(PlayerRef playerRef, int requiredLevel) {
        UUID uuid = playerRef.getUuid();
        if (uuid == null) {
            return;
        }

        long now = System.currentTimeMillis();
        Long last = LAST_WARNING_MS.get(uuid);
        if (last != null && now - last < WARNING_COOLDOWN_MS) {
            return;
        }
        LAST_WARNING_MS.put(uuid, now);
        playerRef.sendMessage(Message.raw("You need Farming level " + requiredLevel + " to plant this."));
    }
}
