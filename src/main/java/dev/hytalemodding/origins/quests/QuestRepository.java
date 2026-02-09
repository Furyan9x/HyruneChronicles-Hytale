package dev.hytalemodding.origins.quests;

import java.util.UUID;

public interface QuestRepository {
    QuestManager.PlayerQuestData load(UUID uuid);
    void save(QuestManager.PlayerQuestData data);
}
