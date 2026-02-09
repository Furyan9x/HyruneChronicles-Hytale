package dev.hytalemodding.origins.util;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import dev.hytalemodding.origins.level.LevelingService;

import java.util.UUID;
import java.util.logging.Level;

/**
 * Utility class to manage player nameplate updates.
 * Updates are applied directly via the ECS.
 */
public class NameplateManager {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static LevelingService serviceInstance;

    public static void init(LevelingService service) {
        serviceInstance = service;
    }

    /**
     * Updates the nameplate for a specific player immediately.
     * Ensures the update runs on the main server thread.
     *
     * @param uuid The UUID of the player to update.
     */
    public static void update(UUID uuid) {
        SyncManager.runSync(() -> performSafeUpdate(uuid));
    }

    /**
     * Core logic for updating a player's nameplate based on their Combat Level.
     */
    private static void performSafeUpdate(UUID uuid) {
        if (serviceInstance == null) {
            serviceInstance = LevelingService.get();
            if (serviceInstance == null) {
                LOGGER.at(Level.WARNING).log("Cannot update nameplate: LevelingService not initialized.");
                return;
            }
        }

        PlayerRef ref = Universe.get().getPlayer(uuid);
        if (ref == null) {
            return;
        }

        var entityRef = ref.getReference();
        if (entityRef == null) {
            return;
        }

        int combatLevel = serviceInstance.getCombatLevel(uuid);
        String username = ref.getUsername();
        String formattedName = formatNameplate(username, combatLevel);

        var store = entityRef.getStore();
        store.putComponent(entityRef, DisplayNameComponent.getComponentType(), new DisplayNameComponent(Message.raw(formattedName)));
    }

    /**
     * Constructs a formatted nameplate string.
     *
     * @param username The player's name.
     * @param level The player's combat level.
     * @return A formatted string (e.g., "[126] Username").
     */
    private static String formatNameplate(String username, int level) {
        // Simple, clean format: [126] Username.
        // You can add colors here if you want, e.g. "\u00A7e[" + level + "] \u00A7r" + username.
        return "[" + level + "] " + username;
    }
}
