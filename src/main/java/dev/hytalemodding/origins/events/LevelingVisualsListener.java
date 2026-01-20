package dev.hytalemodding.origins.events;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import dev.hytalemodding.origins.classes.Classes;
import dev.hytalemodding.origins.level.LevelingService;
import dev.hytalemodding.origins.util.NameplateManager;

import java.util.UUID;

/**
 * Handles visual feedback for level-up events including titles, messages, and nameplate updates.
 */
public class LevelingVisualsListener implements LevelUpListener {

    private final LevelingService service;

    public LevelingVisualsListener(LevelingService service) {
        this.service = service;
    }

    @Override
    public void onLevelUp(UUID uuid, int newLevel, String source) {
        PlayerRef player = Universe.get().getPlayer(uuid);
        if (player == null) return;

        boolean isGlobal = source.equalsIgnoreCase("Global");

        if (isGlobal) {
            handleGlobalLevelUp(player, uuid, newLevel);
        } else {
            handleClassLevelUp(player, newLevel, source);
        }
    }

    private void handleGlobalLevelUp(PlayerRef player, UUID uuid, int newLevel) {
        String title = "Level Up!";
        String subtitle = "Character reached Level " + newLevel;
        String chat = "[Origins] Congratulations! Your Character is now Level " + newLevel + "!";

        EventTitleUtil.showEventTitleToPlayer(player, Message.raw(title), Message.raw(subtitle), true);
        player.sendMessage(Message.raw(chat));

        // Immediate update
        NameplateManager.update(uuid);
    }

    private void handleClassLevelUp(PlayerRef player, int newLevel, String source) {
        Classes rpgClass = Classes.fromId(source);
        String displayName = (rpgClass != null) ? rpgClass.getDisplayName() : source;

        String chatText = "[Origins] " + displayName + " reached Level " + newLevel + "!";
        player.sendMessage(Message.raw(chatText));
    }
}