package dev.hytalemodding.hyrune.quests;

import com.hypixel.hytale.logger.HytaleLogger;
import dev.hytalemodding.hyrune.database.QuestRepository;
import dev.hytalemodding.hyrune.playerdata.PlayerQuestData;
import dev.hytalemodding.hyrune.playerdata.QuestProgress;
import dev.hytalemodding.hyrune.playerdata.QuestStatus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages all quest state and player progression.
 */
public class QuestManager {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final QuestManager INSTANCE = new QuestManager();

    private QuestRepository repository;

    // All registered quests
    private final Map<String, Quest> registeredQuests = new LinkedHashMap<>();

    // Player quest data: UUID -> PlayerQuestData
    private final Map<UUID, PlayerQuestData> playerData = new ConcurrentHashMap<>();

    private QuestManager() {
        registerDefaultQuests();
    }

    public static QuestManager get() {
        return INSTANCE;
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
    }

    /**
     * Registers a quest with the manager.
     */
    public void registerQuest(Quest quest) {
        Objects.requireNonNull(quest, "quest");
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
     * Returns null if the player hasn't started the quest.
     */
    public QuestProgress getQuestProgress(UUID playerId, String questId) {
        if (playerId == null || questId == null) {
            return null;
        }
        return getOrCreatePlayerData(playerId).getQuestProgress().get(questId);
    }

    /**
     * Gets all quest progress for a player.
     */
    public Map<String, QuestProgress> getAllQuestProgress(UUID playerId) {
        if (playerId == null) {
            return new HashMap<>();
        }
        return new HashMap<>(getOrCreatePlayerData(playerId).getQuestProgress());
    }

    /**
     * Starts a quest for a player.
     */
    public boolean startQuest(UUID playerId, String questId) {
        if (playerId == null || questId == null) {
            return false;
        }
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
        PlayerQuestData data = getOrCreatePlayerData(playerId);
        data.getQuestProgress().put(questId, progress);
        persist(data);

        return true;
    }

    /**
     * Attempts to advance a quest stage based on an event.
     */
    public boolean triggerQuestEvent(UUID playerId, String eventType, Object eventData) {
        if (playerId == null) {
            return false;
        }

        PlayerQuestData data = playerData.get(playerId);
        if (data == null || data.getQuestProgress().isEmpty()) {
            return false;
        }

        boolean anyAdvanced = false;
        for (Map.Entry<String, QuestProgress> entry : data.getQuestProgress().entrySet()) {
            QuestProgress progress = entry.getValue();
            if (progress.getStatus() == QuestStatus.IN_PROGRESS) {
                Quest quest = getQuest(entry.getKey());
                if (quest != null && quest.tryAdvanceStage(playerId, progress, eventType, eventData)) {
                    anyAdvanced = true;
                }
            }
        }

        if (anyAdvanced) {
            persist(data);
        }

        return anyAdvanced;
    }

    /**
     * Adds quest points to a player.
     */
    public void addQuestPoints(UUID playerId, int points) {
        if (playerId == null || points <= 0) {
            return;
        }
        PlayerQuestData data = getOrCreatePlayerData(playerId);
        data.setQuestPoints(data.getQuestPoints() + points);
        persist(data);
    }

    /**
     * Gets a player's total quest points.
     */
    public int getQuestPoints(UUID playerId) {
        if (playerId == null) {
            return 0;
        }
        return getOrCreatePlayerData(playerId).getQuestPoints();
    }

    /**
     * Filters and sorts quests based on filter type.
     */
    public List<Quest> getFilteredQuests(UUID playerId, QuestListFilter filter) {
        if (filter == null) {
            filter = QuestListFilter.ALPHABETICAL;
        }
        List<Quest> quests = new ArrayList<>(getAllQuests());

        switch (filter) {
            case ALPHABETICAL:
                quests.sort(Comparator.comparing(Quest::getName));
                break;
            case BY_LENGTH:
                quests.sort(Comparator.comparing((Quest q) -> q.getLength().getSortOrder())
                    .thenComparing(Quest::getName));
                break;
            case BY_DIFFICULTY:
                quests.sort(Comparator.comparing((Quest q) -> q.getDifficulty().getSortOrder())
                    .thenComparing(Quest::getName));
                break;
        }

        return quests;
    }

    public QuestListFilter getQuestListFilter(UUID playerId) {
        if (playerId == null) {
            return QuestListFilter.ALPHABETICAL;
        }
        PlayerQuestData data = getOrCreatePlayerData(playerId);
        return QuestListFilter.fromString(data.getQuestListFilter(), QuestListFilter.ALPHABETICAL);
    }

    public void setQuestListFilter(UUID playerId, QuestListFilter filter) {
        if (playerId == null) {
            return;
        }
        PlayerQuestData data = getOrCreatePlayerData(playerId);
        data.setQuestListFilter(filter != null ? filter.name() : null);
        persist(data);
    }

    public boolean isHideCompleted(UUID playerId) {
        if (playerId == null) {
            return false;
        }
        return getOrCreatePlayerData(playerId).isHideCompleted();
    }

    public void setHideCompleted(UUID playerId, boolean hideCompleted) {
        if (playerId == null) {
            return;
        }
        PlayerQuestData data = getOrCreatePlayerData(playerId);
        data.setHideCompleted(hideCompleted);
        persist(data);
    }

    public boolean isHideUnavailable(UUID playerId) {
        if (playerId == null) {
            return false;
        }
        return getOrCreatePlayerData(playerId).isHideUnavailable();
    }

    public void setHideUnavailable(UUID playerId, boolean hideUnavailable) {
        if (playerId == null) {
            return;
        }
        PlayerQuestData data = getOrCreatePlayerData(playerId);
        data.setHideUnavailable(hideUnavailable);
        persist(data);
    }

    /**
     * Gets the status of a quest for a player.
     */
    public QuestStatus getQuestStatus(UUID playerId, String questId) {
        if (playerId == null || questId == null) {
            return QuestStatus.NOT_STARTED;
        }
        QuestProgress progress = getQuestProgress(playerId, questId);
        return progress != null ? progress.getStatus() : QuestStatus.NOT_STARTED;
    }

    /**
     * Loads player quest data from storage.
     */
    public void load(UUID playerId) {
        if (playerId == null || repository == null) {
            return;
        }
        PlayerQuestData data = repository.load(playerId);
        if (data == null) {
            data = new PlayerQuestData(playerId);
        }
        if (data.getUuid() == null) {
            data.setUuid(playerId);
        }
        if (data.getQuestProgress() == null) {
            data.setQuestProgress(new HashMap<>());
        }
        playerData.put(playerId, data);
    }

    public void unload(UUID playerId) {
        if (playerId == null) {
            return;
        }
        PlayerQuestData data = playerData.remove(playerId);
        if (data != null) {
            persist(data);
        }
    }

    private PlayerQuestData getOrCreatePlayerData(UUID playerId) {
        return playerData.computeIfAbsent(playerId, this::loadOrCreate);
    }

    private PlayerQuestData loadOrCreate(UUID playerId) {
        if (repository != null) {
            PlayerQuestData stored = repository.load(playerId);
            if (stored != null) {
                if (stored.getUuid() == null) {
                    stored.setUuid(playerId);
                }
                if (stored.getQuestProgress() == null) {
                    stored.setQuestProgress(new HashMap<>());
                }
                return stored;
            }
        }
        return new PlayerQuestData(playerId);
    }

    private void persist(PlayerQuestData data) {
        if (repository == null || data == null) {
            return;
        }
        if (data.getUuid() == null) {
            LOGGER.at(Level.WARNING).log("Skipping quest save for missing UUID.");
            return;
        }
        try {
            repository.save(data);
        } catch (RuntimeException e) {
            LOGGER.at(Level.WARNING)
                .log("Failed to save quest data for " + data.getUuid() + ": " + e.getMessage());
        }
    }
}
