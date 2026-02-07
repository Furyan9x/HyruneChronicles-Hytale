package dev.hytalemodding.origins.events;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import dev.hytalemodding.origins.util.NameplateManager;

import java.util.UUID;

/**
 * Handles visual feedback for level-up events including titles, messages, and nameplate updates.
 */
public class LevelingVisualsListener implements LevelUpListener {

    @Override
    public void onLevelUp(UUID uuid, int newLevel, String sourceName) {
        PlayerRef player = Universe.get().getPlayer(uuid);
        if (player == null) {
            return;
        }

        String title = "Level Up!";
        String subtitle = sourceName + " reached Level " + newLevel;
        String chat = "[Origins]" + sourceName + " is now Level " + newLevel + "!";

        EventTitleUtil.showEventTitleToPlayer(player, Message.raw(title), Message.raw(subtitle), true);
        player.sendMessage(Message.raw(chat));
        NameplateManager.update(uuid);
    }
}
