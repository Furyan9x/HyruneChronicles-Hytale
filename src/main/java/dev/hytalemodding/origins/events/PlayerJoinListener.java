package dev.hytalemodding.origins.events;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.DrainPlayerFromWorldEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import dev.hytalemodding.origins.bonus.SkillStatBonusApplier;
import dev.hytalemodding.origins.level.LevelingService;
import dev.hytalemodding.origins.util.NameplateManager;
import dev.hytalemodding.origins.slayer.SlayerService;

import java.util.UUID;

/**
 * Handles player join/leave events to manage data persistence.
 */
public class PlayerJoinListener {

    private final LevelingService service;
    private final SlayerService slayerService;

    public PlayerJoinListener(LevelingService service, SlayerService slayerService) {
        this.service = service;
        this.slayerService = slayerService;
    }

    /**
     * Preloads player data and updates nameplate on join.
     */
    public void onPlayerJoin(AddPlayerToWorldEvent event) {
        var holder = event.getHolder();
        Player playerComp = holder.getComponent(Player.getComponentType());

        if (playerComp != null) {
            UUID uuid = playerComp.getUuid();
            service.getCombatLevel(uuid);
            NameplateManager.update(uuid);
            SkillStatBonusApplier.apply(holder, uuid);
            PlayerRef playerRef = holder.getComponent(PlayerRef.getComponentType());
            SkillStatBonusApplier.applyMovementSpeed(playerRef);
            slayerService.getPlayerData(uuid);
        }
    }

    /**
     * Saves and unloads player data on leave.
     */
    public void onPlayerLeave(DrainPlayerFromWorldEvent event) {
        var holder = event.getHolder();
        Player playerComp = holder.getComponent(Player.getComponentType());

        if (playerComp != null) {
            UUID uuid = playerComp.getUuid();
            service.unload(uuid);
            slayerService.unload(uuid);
        }
    }
}
