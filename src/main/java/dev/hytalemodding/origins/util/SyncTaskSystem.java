package dev.hytalemodding.origins.util;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class SyncTaskSystem extends TickingSystem<EntityStore> {
    @Override
    public void tick(float deltaTime, int tick, @Nonnull Store<EntityStore> store) {
        // Run all tasks queued by commands or async threads
        SyncManager.processQueue();
    }
}