package dev.hytalemodding.origins.util;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import dev.hytalemodding.origins.classes.Classes;
import dev.hytalemodding.origins.level.LevelingService;

import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Manages player nameplate updates for the Origins leveling system.
 * Uses a thread-safe queue to defer component modifications to the WorldThread.
 */
public class NameplateManager {

    private static final Queue<UUID> pendingUpdates = new ConcurrentLinkedQueue<>();
    private static LevelingService serviceInstance;

    /**
     * Initialize the NameplateManager with the leveling service instance.
     */
    public static void init(LevelingService service) {
        serviceInstance = service;
    }

    /**
     * Schedule a nameplate update for the given player.
     * The update will be processed on the WorldThread during the next tick.
     */
    public static void scheduleUpdate(UUID uuid) {
        pendingUpdates.offer(uuid);
    }

    /**
     * Process all pending nameplate updates.
     * Called from NameplateUpdateSystem on the WorldThread.
     */
    public static void processPendingUpdates() {
        UUID uuid;
        while ((uuid = pendingUpdates.poll()) != null) {
            if (serviceInstance != null) {
                updateNameplateImmediate(uuid, serviceInstance);
            }
        }
    }

    /**
     * Immediately update a player's nameplate with their current level and class.
     * Must be called from the WorldThread.
     */
    private static void updateNameplateImmediate(UUID uuid, LevelingService service) {
        PlayerRef ref = Universe.get().getPlayer(uuid);
        if (ref == null) return;

        var entityRef = ref.getReference();
        if (entityRef == null) return;

        int globalLevel = service.getAdventurerLevel(uuid);
        String classId = service.getActiveClassId(uuid);
        String username = ref.getUsername();

        // Build nameplate format: [Lvl X ClassName] Username
        StringBuilder nameplate = new StringBuilder();
        nameplate.append("[Lvl ").append(globalLevel);

        if (classId != null) {
            Classes rpgClass = Classes.fromId(classId);
            String className = (rpgClass != null) ? rpgClass.getDisplayName() : "Hero";
            nameplate.append(" ").append(className);
        }

        nameplate.append("] ").append(username);

        // Apply the nameplate via DisplayNameComponent
        Message displayName = Message.raw(nameplate.toString());
        var store = entityRef.getStore();
        store.putComponent(entityRef, DisplayNameComponent.getComponentType(), new DisplayNameComponent(displayName));

        System.out.println("[Origins] Nameplate updated: " + username + " -> " + nameplate);
    }
}