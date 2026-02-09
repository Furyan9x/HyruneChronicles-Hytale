package dev.hytalemodding.origins.events;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.DrainPlayerFromWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.origins.bonus.SkillStatBonusApplier;
import dev.hytalemodding.origins.level.LevelingService;
import dev.hytalemodding.origins.quests.QuestManager;
import dev.hytalemodding.origins.slayer.SlayerService;
import dev.hytalemodding.origins.tradepack.TradePackManager;
import dev.hytalemodding.origins.util.NameplateManager;

import java.util.UUID;
import java.util.logging.Level;

/**
 * Handles player join/leave events to manage data persistence.
 */
public class PlayerJoinListener {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final LevelingService service;
    private final SlayerService slayerService;
    private final QuestManager questManager;

    public PlayerJoinListener(LevelingService service, SlayerService slayerService, QuestManager questManager) {
        this.service = service;
        this.slayerService = slayerService;
        this.questManager = questManager;
    }

    /**
     * Preloads player data and updates nameplate on join.
     */
    public void onPlayerJoin(AddPlayerToWorldEvent event) {
        var holder = event.getHolder();
        Player playerComp = holder.getComponent(Player.getComponentType());

        if (playerComp != null) {
            UUID uuid = playerComp.getUuid();
            service.load(uuid);
            slayerService.load(uuid);
            questManager.load(uuid);
            NameplateManager.update(uuid);
            SkillStatBonusApplier.apply(holder, uuid);
            PlayerRef playerRef = holder.getComponent(PlayerRef.getComponentType());
            if (playerRef != null) {
                SkillStatBonusApplier.applyMovementSpeed(playerRef);
            }
            TradePackManager.sync(playerComp);
        }
    }

    public void onPlayerLeave(DrainPlayerFromWorldEvent event) {
        handlePlayerDataSave(event.getHolder());
    }

    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        PlayerRef playerRef = event.getPlayerRef();
        UUID uuid = playerRef.getUuid();
        persistAll(uuid);
        safelyRun(uuid, "TradePackManager", () -> TradePackManager.clear(uuid));
    }

    private void handlePlayerDataSave(Holder<EntityStore> holder) {
        Player playerComp = holder.getComponent(Player.getComponentType());

        if (playerComp != null) {
            persistAll(playerComp.getUuid());
            safelyRun(playerComp.getUuid(), "TradePackManager", () -> TradePackManager.clear(playerComp.getUuid()));
        }
    }

    private void persistAll(UUID uuid) {
        safelyRun(uuid, "LevelingService", () -> service.unload(uuid));
        safelyRun(uuid, "SlayerService", () -> slayerService.unload(uuid));
        safelyRun(uuid, "QuestManager", () -> questManager.unload(uuid));
    }

    private void safelyRun(UUID uuid, String label, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            LOGGER.at(Level.WARNING)
                .log(label + " failed for " + uuid + ": " + e.getMessage());
        }
    }
}
