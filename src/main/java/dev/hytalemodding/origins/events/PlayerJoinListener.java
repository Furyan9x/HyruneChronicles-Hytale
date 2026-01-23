package dev.hytalemodding.origins.events;

// Verified Import
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.DrainPlayerFromWorldEvent;
import dev.hytalemodding.origins.level.LevelingService;
import dev.hytalemodding.origins.util.NameplateManager;

import java.util.UUID;

/**
 * Handles player join/leave events to manage data persistence.
 */
public class PlayerJoinListener {

    private final LevelingService service;

    public PlayerJoinListener(LevelingService service) {
        this.service = service;
    }

    public void onPlayerJoin(AddPlayerToWorldEvent event) {
        var holder = event.getHolder();
        Player playerComp = holder.getComponent(Player.getComponentType());

        if (playerComp != null) {
            UUID uuid = playerComp.getUuid();

            // 1. Force Load Data
            // Calling a getter triggers the "computeIfAbsent" loading logic in LevelingService
            service.getCombatLevel(uuid);

            // 2. Update Nameplate
            // Now that data is loaded, we can display the correct level
            NameplateManager.update(uuid);
        }
    }

    public void onPlayerLeave(DrainPlayerFromWorldEvent event) {
        var holder = event.getHolder();
        Player playerComp = holder.getComponent(Player.getComponentType());

        if (playerComp != null) {
            UUID uuid = playerComp.getUuid();

            // 3. Save & Clear Cache
            // Prevents memory leaks and ensures data is written to disk
            service.unload(uuid);
        }
    }
}