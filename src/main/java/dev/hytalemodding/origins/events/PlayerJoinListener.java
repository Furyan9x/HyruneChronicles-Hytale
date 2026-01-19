package dev.hytalemodding.origins.events;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import dev.hytalemodding.origins.level.LevelingService;
import dev.hytalemodding.origins.util.NameplateManager;
import java.util.UUID;

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
            NameplateManager.updateNameplate(uuid, service);
        }
    }
}