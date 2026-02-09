package dev.hytalemodding.origins.playerdata;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Aggregates quest progress and quest points for a player.
 */
public class PlayerQuestData implements PlayerData {
    private UUID uuid;
    private Map<String, QuestProgress> questProgress = new HashMap<>();
    private int questPoints;

    public PlayerQuestData() {
    }

    public PlayerQuestData(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public Map<String, QuestProgress> getQuestProgress() {
        return questProgress;
    }

    public void setQuestProgress(Map<String, QuestProgress> questProgress) {
        this.questProgress = questProgress != null ? questProgress : new HashMap<>();
    }

    public int getQuestPoints() {
        return questPoints;
    }

    public void setQuestPoints(int questPoints) {
        this.questPoints = questPoints;
    }
}
