package dev.hytalemodding.origins.events;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.DrainPlayerFromWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.origins.bonus.SkillStatBonusApplier;
import dev.hytalemodding.origins.level.LevelingService;
import dev.hytalemodding.origins.quests.QuestManager;

import dev.hytalemodding.origins.util.NameplateManager;
import dev.hytalemodding.origins.slayer.SlayerService;
import dev.hytalemodding.origins.tradepack.TradePackManager;


import java.util.UUID;

/**
 * Handles player join/leave events to manage data persistence.
 */
public class PlayerJoinListener {
static {System.err.println("========== PlayerJoinListener CLASS LOADED ==========");}

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
            service.getCombatLevel(uuid);
            NameplateManager.update(uuid);
            SkillStatBonusApplier.apply(holder, uuid);
            PlayerRef playerRef = holder.getComponent(PlayerRef.getComponentType());
            SkillStatBonusApplier.applyMovementSpeed(playerRef);
            slayerService.getPlayerData(uuid);
            questManager.load(uuid);
            TradePackManager.sync(playerComp);
            System.err.println("========== PlayerJoinListener onplayerJoin worked ==========");
        }
    }

    public void onPlayerLeave(DrainPlayerFromWorldEvent event) {
        System.err.println("========== DRAIN FROM WORLD (world change?) ==========");
        handlePlayerDataSave(event.getHolder());
    }

    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        System.err.println("========== PLAYER DISCONNECT EVENT ==========");

        PlayerRef playerRef = event.getPlayerRef();
        UUID uuid = playerRef.getUuid();

        System.err.println("Saving data for player: " + uuid + " (" + playerRef.getUsername() + ")");

        // Save all player data
        try {
            service.unload(uuid);
            System.err.println("✓ LevelingService data saved");
        } catch (Exception e) {
            System.err.println("✗ LevelingService save failed");
            e.printStackTrace();
        }

        try {
            slayerService.unload(uuid);
            System.err.println("✓ SlayerService data saved");
        } catch (Exception e) {
            System.err.println("✗ SlayerService save failed");
            e.printStackTrace();
        }

        try {
            questManager.unload(uuid);
            System.err.println("✓ QuestManager data saved");
        } catch (Exception e) {
            System.err.println("✗ QuestManager save failed");
            e.printStackTrace();
        }

        try {
            TradePackManager.clear(uuid);
            System.err.println("✓ TradePackManager cleared");
        } catch (Exception e) {
            System.err.println("✗ TradePackManager clear failed");
            e.printStackTrace();
        }

        System.err.println("========== Player data save complete ==========");
    }

    private void handlePlayerDataSave(Holder<EntityStore> holder) {
        Player playerComp = holder.getComponent(Player.getComponentType());

        if (playerComp != null) {
            UUID uuid = playerComp.getUuid();

            try {
                service.unload(uuid);
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                slayerService.unload(uuid);
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                questManager.unload(uuid);
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                TradePackManager.clear(uuid);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
