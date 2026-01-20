package dev.hytalemodding.origins.util;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import dev.hytalemodding.origins.classes.Classes;
import dev.hytalemodding.origins.level.LevelingService;

import java.util.UUID;

/**
 * Utility class to manage player nameplate updates.
 * Updates are applied directly via the ECS.
 */
public class NameplateManager {

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
     * Core logic for updating a player's nameplate based on their current level and class.
     * 
     * @param uuid The UUID of the player.
     */
    private static void performSafeUpdate(UUID uuid) {
        if (serviceInstance == null) {
            System.err.println("[Origins] Cannot update nameplate: Service not initialized.");
            return;
        }

        // Retrieve the Player reference
        PlayerRef ref = Universe.get().getPlayer(uuid);
        if (ref == null) return;

        var entityRef = ref.getReference();
        if (entityRef == null) return;

        // Fetch current character data
        int globalLevel = serviceInstance.getAdventurerLevel(uuid);
        String classId = serviceInstance.getActiveClassId(uuid);
        String username = ref.getUsername();

        // Format the nameplate string
        String formattedName = formatNameplate(username, globalLevel, classId);

        // Apply the DisplayNameComponent to the player entity
        var store = entityRef.getStore();
        store.putComponent(entityRef, DisplayNameComponent.getComponentType(), new DisplayNameComponent(Message.raw(formattedName)));
    }

    /**
     * Constructs a formatted nameplate string.
     * 
     * @param username The player's name.
     * @param level The player's global level.
     * @param classId The ID of the player's active class.
     * @return A formatted string (e.g., "[Lvl 10 Warrior] Username").
     */
    private static String formatNameplate(String username, int level, String classId) {
        StringBuilder sb = new StringBuilder();
        sb.append("[Lvl ").append(level);

        if (classId != null) {
            Classes rpgClass = Classes.fromId(classId);
            String className = (rpgClass != null) ? rpgClass.getDisplayName() : "Hero";
            sb.append(" ").append(className);
        }

        sb.append("] ").append(username);
        return sb.toString();
    }
}