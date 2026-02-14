package dev.hytalemodding.hyrune.system;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.hyrune.registry.FishingRegistry;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * ECS system for fishing.
 */
public class FishingSystem extends TickingSystem<EntityStore> {
    static final int MIN_BITE_TICKS = 120;
    static final int MAX_BITE_TICKS = 240;
    static final int MIN_BITE_WINDOW_TICKS = 20;
    static final int MAX_BITE_WINDOW_TICKS = 40;

    private static final Map<UUID, FishingSession> SESSIONS = new ConcurrentHashMap<>();
    private static int currentTick;

    @Override
    public void tick(float deltaTime, int tick, @Nonnull Store<EntityStore> store) {
        currentTick = tick;
        for (var entry : SESSIONS.entrySet()) {
            FishingSession session = entry.getValue();
            if (session == null) {
                continue;
            }

            if (session.state == FishingState.WAITING && tick >= session.nextBiteTick) {
                session.state = FishingState.BITE;
                session.biteEndTick = tick + session.biteWindowTicks;
                notifyPlayer(session.playerId, "A fish is biting!");
                continue;
            }

            if (session.state == FishingState.BITE && tick > session.biteEndTick) {
                SESSIONS.remove(entry.getKey());
                notifyPlayer(session.playerId, "The fish got away.");
            }
        }
    }

    static FishingSession getSession(UUID uuid) {
        return SESSIONS.get(uuid);
    }

    static void cancelSession(UUID uuid) {
        if (uuid != null) {
            SESSIONS.remove(uuid);
        }
    }

    static boolean startSession(UUID uuid,
                                UUID worldId,
                                Vector3i target,
                                FishingRegistry.BaitDefinition bait,
                                int fishingLevel) {
        if (uuid == null || worldId == null || target == null || bait == null) {
            return false;
        }

        if (SESSIONS.containsKey(uuid)) {
            return false;
        }

        int baseWait = ThreadLocalRandom.current().nextInt(MIN_BITE_TICKS, MAX_BITE_TICKS + 1);
        double levelBonus = Math.min(1.0, fishingLevel / 99.0) * FishingRegistry.BITE_SPEED_MAX_BONUS;
        double waitTicks = baseWait * bait.speedMultiplier * (1.0 - levelBonus);
        int nextBite = currentTick + Math.max(20, (int) Math.round(waitTicks));

        int biteWindow = ThreadLocalRandom.current().nextInt(MIN_BITE_WINDOW_TICKS, MAX_BITE_WINDOW_TICKS + 1);

        FishingSession session = new FishingSession(uuid, worldId, target, bait, FishingState.WAITING);
        session.nextBiteTick = nextBite;
        session.biteWindowTicks = biteWindow;
        SESSIONS.put(uuid, session);
        return true;
    }

    static void markCaught(UUID uuid) {
        if (uuid != null) {
            SESSIONS.remove(uuid);
        }
    }

    private static void notifyPlayer(UUID uuid, String message) {
        PlayerRef playerRef = Universe.get().getPlayer(uuid);
        if (playerRef == null) {
            return;
        }
        playerRef.sendMessage(Message.raw(message));
    }

    enum FishingState {
        WAITING,
        BITE
    }

    static final class FishingSession {
        final UUID playerId;
        final UUID worldId;
        final Vector3i target;
        final FishingRegistry.BaitDefinition bait;
        FishingState state;
        int nextBiteTick;
        int biteEndTick;
        int biteWindowTicks;

        FishingSession(UUID playerId,
                       UUID worldId,
                       Vector3i target,
                       FishingRegistry.BaitDefinition bait,
                       FishingState state) {
            this.playerId = playerId;
            this.worldId = worldId;
            this.target = target;
            this.bait = bait;
            this.state = state;
        }
    }
}
