package dev.hytalemodding.origins.quests;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all quest state and player progression.
 * Singleton service for quest operations.
 */
public class QuestManager {
    
    private static QuestManager instance;

    private QuestRepository repository;
    
    // All registered quests
    private final Map<String, Quest> registeredQuests = new LinkedHashMap<>();
    
    // Player quest progress: UUID -> (QuestID -> QuestProgress)
    private final Map<UUID, Map<String, QuestProgress>> playerQuestData = new ConcurrentHashMap<>();
    
    // Player quest points: UUID -> points
    private final Map<UUID, Integer> questPoints = new ConcurrentHashMap<>();
    
    private QuestManager() {
        registerDefaultQuests();
    }
    
    public static QuestManager get() {
        if (instance == null) {
            instance = new QuestManager();
        }
        return instance;
    }

    public void setRepository(QuestRepository repository) {
        this.repository = repository;
    }

    public QuestRepository getRepository() {
        return repository;
    }
    /**
     * Registers all default quests.
     */
    private void registerDefaultQuests() {
        registerQuest(new TheWoodsmansRequest());
        // Register more quests here as they're created
    }
    
    /**
     * Registers a quest with the manager.
     */
    public void registerQuest(Quest quest) {
        registeredQuests.put(quest.getId(), quest);
    }
    
    /**
     * Gets a quest by ID.
     */
    public Quest getQuest(String questId) {
        return registeredQuests.get(questId);
    }
    
    /**
     * Gets all registered quests.
     */
    public Collection<Quest> getAllQuests() {
        return registeredQuests.values();
    }
    
    /**
     * Gets a player's progress on a specific quest.
     * Returns null if player hasn't started the quest.
     */
    public QuestProgress getQuestProgress(UUID playerId, String questId) {
        Map<String, QuestProgress> playerQuests = playerQuestData.get(playerId);
        if (playerQuests == null) {
            return null;
        }
        return playerQuests.get(questId);
    }
    
    /**
     * Gets all quest progress for a player.
     */
    public Map<String, QuestProgress> getAllQuestProgress(UUID playerId) {
        return playerQuestData.getOrDefault(playerId, new HashMap<>());
    }

    /**
     * Starts a quest for a player.
     */
    public boolean startQuest(UUID playerId, String questId) {
        Quest quest = getQuest(questId);
        if (quest == null) {
            return false;
        }

        // Check if already started
        QuestProgress existing = getQuestProgress(playerId, questId);
        if (existing != null && existing.getStatus() != QuestStatus.NOT_STARTED) {
            return false;
        }

        // Check requirements
        if (!quest.meetsRequirements(playerId)) {
            return false;
        }

        // Create new progress
        QuestProgress progress = new QuestProgress(questId);
        progress.setStatus(QuestStatus.IN_PROGRESS);
        progress.setStartedAt(System.currentTimeMillis());

        // Store progress
        playerQuestData.computeIfAbsent(playerId, k -> new HashMap<>()).put(questId, progress);

        // ✅ SAVE IMMEDIATELY
        if (repository != null) {
            repository.save(savePlayerData(playerId));
        }

        return true;
    }

    /**
     * Attempts to advance a quest stage based on an event.
     */
    public boolean triggerQuestEvent(UUID playerId, String eventType, Object eventData) {
        Map<String, QuestProgress> playerQuests = playerQuestData.get(playerId);
        if (playerQuests == null) {
            return false;
        }

        boolean anyAdvanced = false;

        // Check all in-progress quests for stage advancement
        for (Map.Entry<String, QuestProgress> entry : playerQuests.entrySet()) {
            QuestProgress progress = entry.getValue();
            if (progress.getStatus() == QuestStatus.IN_PROGRESS) {
                Quest quest = getQuest(entry.getKey());
                if (quest != null) {
                    if (quest.tryAdvanceStage(playerId, progress, eventType, eventData)) {
                        anyAdvanced = true;
                    }
                }
            }
        }

        // ✅ SAVE IMMEDIATELY if anything changed
        if (anyAdvanced && repository != null) {
            repository.save(savePlayerData(playerId));
        }

        return anyAdvanced;
    }

    /**
     * Adds quest points to a player.
     */
    public void addQuestPoints(UUID playerId, int points) {
        questPoints.merge(playerId, points, Integer::sum);

        // ✅ SAVE IMMEDIATELY
        if (repository != null) {
            repository.save(savePlayerData(playerId));
        }
    }
    
    /**
     * Gets a player's total quest points.
     */
    public int getQuestPoints(UUID playerId) {
        return questPoints.getOrDefault(playerId, 0);
    }

    
    /**
     * Filters and sorts quests based on filter type.
     */
    public List<Quest> getFilteredQuests(UUID playerId, QuestListFilter filter) {
        List<Quest> quests = new ArrayList<>(getAllQuests());
        
        switch (filter) {
            case ALPHABETICAL:
                quests.sort(Comparator.comparing(Quest::getName));
                break;
                
            case BY_LENGTH:
                quests.sort(Comparator.comparing((Quest q) -> q.getLength().getSortOrder())
                    .thenComparing(Quest::getName));
                break;
                
            case HIDE_COMPLETED:
                quests.removeIf(q -> {
                    QuestProgress progress = getQuestProgress(playerId, q.getId());
                    return progress != null && progress.getStatus() == QuestStatus.COMPLETED;
                });
                quests.sort(Comparator.comparing(Quest::getName));
                break;
                
            case HIDE_UNAVAILABLE:
                quests.removeIf(q -> {
                    QuestProgress progress = getQuestProgress(playerId, q.getId());
                    boolean isCompleted = progress != null && progress.getStatus() == QuestStatus.COMPLETED;
                    return !q.meetsRequirements(playerId) && !isCompleted;
                });
                quests.sort(Comparator.comparing(Quest::getName));
                break;
        }
        
        return quests;
    }
    
    /**
     * Gets the status of a quest for a player.
     */
    public QuestStatus getQuestStatus(UUID playerId, String questId) {
        QuestProgress progress = getQuestProgress(playerId, questId);
        return progress != null ? progress.getStatus() : QuestStatus.NOT_STARTED;
    }
    
    /**
     * Codec for saving/loading player quest data.
     */
    public static class PlayerQuestData {
        private UUID uuid;  // ADD THIS
        private Map<String, QuestProgress> questProgress = new HashMap<>();
        private int questPoints = 0;

        // ADD THIS
        public UUID getUuid() {
            return uuid;
        }

        public void setUuid(UUID uuid) {
            this.uuid = uuid;
        }
    }
    
    /**
     * Loads player quest data from storage.
     */
    public void loadPlayerData(UUID playerId, PlayerQuestData data) {
        if (data.questProgress != null) {
            playerQuestData.put(playerId, new HashMap<>(data.questProgress));
        }
        if (data.questPoints > 0) {
            questPoints.put(playerId, data.questPoints);
        }
    }

    public void load(UUID playerId) {
        if (repository == null) {
            return;
        }
        PlayerQuestData data = repository.load(playerId);
        if (data != null) {
            loadPlayerData(playerId, data);
        }
    }
    
    /**
     * Saves player quest data to storage.
     */
    public PlayerQuestData savePlayerData(UUID playerId) {
        PlayerQuestData data = new PlayerQuestData();
        data.setUuid(playerId);  // ADD THIS
        data.questProgress = new HashMap<>(getAllQuestProgress(playerId));
        data.questPoints = getQuestPoints(playerId);
        return data;
    }

    public void unload(UUID playerId) {
        if (repository != null) {
            repository.save(savePlayerData(playerId));  // Remove UUID parameter
        }
        playerQuestData.remove(playerId);
        questPoints.remove(playerId);
    }
}
