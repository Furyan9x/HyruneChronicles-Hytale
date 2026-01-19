package dev.hytalemodding.origins.system;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.origins.util.NameplateManager;

import javax.annotation.Nonnull;

/**
 * ECS System that runs on the WorldThread to process pending nameplate updates.
 * This ensures all component modifications happen in the correct thread context.
 */
public class NameplateUpdateSystem extends TickingSystem<EntityStore> {

    @Override
    public void tick(float deltaTime, int tick, @Nonnull Store<EntityStore> store) {
        // Process all pending nameplate updates on the WorldThread
        NameplateManager.processPendingUpdates();
    }
}
