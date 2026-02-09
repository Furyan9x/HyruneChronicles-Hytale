package dev.hytalemodding.origins.system;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.origins.util.SyncManager;

import javax.annotation.Nonnull;

/**
 * ECS system for sync task.
 */
public class SyncTaskSystem extends TickingSystem<EntityStore> {
    @Override
    public void tick(float deltaTime, int tick, @Nonnull Store<EntityStore> store) {
        // Run all tasks queued by commands or async threads
        SyncManager.processQueue();
    }
}
