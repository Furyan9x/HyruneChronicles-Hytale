package dev.hytalemodding.origins.events;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
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
    public void onLevelUp(UUID uuid, int newLevel, String sourceName) {
        // sourceName comes from the Service (e.g., "Strength", "Mining", "Combat")
        PlayerRef player = Universe.get().getPlayer(uuid);
        if (player == null) return;

        // 1. Build Messages
        String title = "Level Up!";
        String subtitle = sourceName + " reached Level " + newLevel;
        String chat = "§6[Origins] §e" + sourceName + "§6 is now Level §e" + newLevel + "§6!";

        // 2. Display Title (Big text on screen)
        // Note: Check if your API version uses Message.raw() or just String for titles
        EventTitleUtil.showEventTitleToPlayer(player, Message.raw(title), Message.raw(subtitle), true);

        // 3. Send Chat
        player.sendMessage(Message.raw(chat));

        // 4. Immediate Nameplate Update
        // (Since gaining a skill level likely increased Combat/Total Level)
        NameplateManager.update(uuid);
    }
}