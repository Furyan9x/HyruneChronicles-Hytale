package dev.hytalemodding.origins.util;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.origins.ui.XPDropOverlay;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager for x p drop.
 */
public class XPDropManager {
    private static final XPDropManager instance = new XPDropManager();

    // Keying by PlayerRef since that is what the Overlay holds and returns on close
    private final Map<PlayerRef, XPDropOverlay> activeOverlays = new ConcurrentHashMap<>();

    public static XPDropManager get() { return instance; }

    public void handleXpGain(UUID playerUuid, String skillName, long amount, float currentLevelProgress) {
        // 1. Get PlayerRef from Universe
        if ("Constitution".equalsIgnoreCase(skillName)) {
            return;
        }

        PlayerRef playerRef = Universe.get().getPlayer(playerUuid);
        if (playerRef == null) {
            return; // Player is offline
        }

        // 2. Get the entity reference directly from PlayerRef
        Ref<EntityStore> playerEntityRef = playerRef.getReference();
        if (playerEntityRef == null || !playerEntityRef.isValid()) {
            return; // Player not in world
        }

        // 3. Get the store from the reference
        Store<EntityStore> store = playerEntityRef.getStore();

        // 4. Get the Player component
        Player player = store.getComponent(playerEntityRef, Player.getComponentType());
        if (player == null) {
            return;
        }
        XPDropOverlay existingOverlay = activeOverlays.get(playerRef);
        if (existingOverlay != null) {
            if (!existingOverlay.getSkillName().equals(skillName)) {
                // Remove the old overlay
                UICommandBuilder emptyBuilder = new UICommandBuilder();
                existingOverlay.update(true, emptyBuilder);
                activeOverlays.remove(playerRef);
                existingOverlay = null;
            } else {
                existingOverlay.updateData(amount, currentLevelProgress);
                return;
            }
        }

        // Create new overlay if none exists or skill changed
        XPDropOverlay newOverlay = new XPDropOverlay(playerRef, skillName, amount, currentLevelProgress);
        activeOverlays.put(playerRef, newOverlay);
        player.getHudManager().setCustomHud(playerRef, newOverlay);
    }

    // Called by XPDropOverlay.onClose()
    public void onOverlayClosed(PlayerRef playerRef) {
        if (playerRef != null) {
            activeOverlays.remove(playerRef);
        }
    }
}
