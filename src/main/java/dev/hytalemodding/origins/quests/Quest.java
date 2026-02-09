package dev.hytalemodding.origins.quests;

import java.util.List;

/**
 * Abstract base class for all quests.
 * Each quest implementation defines its own Stage enum and progression logic.
 */
public abstract class Quest {
    
    private final String id;
    private final String name;
    private final String description;
    private final QuestLength length;
    private final int questPoints;
    
    public Quest(String id, String name, String description, QuestLength length, int questPoints) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.length = length;
        this.questPoints = questPoints;
    }
    
    /**
     * Returns the current stage's journal text based on the player's progress.
     */
    public abstract String getJournalText(QuestProgress progress);
    
    /**
     * Returns the list of stages for display in the quest journal.
     * Each stage shows its text, grayed out if incomplete.
     */
    public abstract List<StageInfo> getStageList(QuestProgress progress);
    
    /**
     * Returns level requirements (skill -> level).
     */
    public abstract List<QuestRequirement.LevelRequirement> getLevelRequirements();
    
    /**
     * Returns item requirements (itemId -> count).
     */
    public abstract List<QuestRequirement.ItemRequirement> getItemRequirements();
    
    /**
     * Returns prerequisite quest IDs that must be completed.
     */
    public abstract List<String> getPrerequisiteQuests();
    
    /**
     * Returns quest rewards (XP, items, unlocks).
     */
    public abstract List<QuestReward> getRewards();
    
    /**
     * Checks if player meets all requirements to start this quest.
     */
    public abstract boolean meetsRequirements(java.util.UUID playerId);
    
    /**
     * Advances the quest to the next stage if conditions are met.
     * Returns true if progression occurred.
     */
    public abstract boolean tryAdvanceStage(java.util.UUID playerId, QuestProgress progress, String eventType, Object eventData);
    
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public QuestLength getLength() { return length; }
    public int getQuestPoints() { return questPoints; }
    
    /**
     * Represents a single stage in the quest journal.
     */
    public static class StageInfo {
        private final String text;
        private final boolean completed;
        
        public StageInfo(String text, boolean completed) {
            this.text = text;
            this.completed = completed;
        }
        
        public String getText() { return text; }
        public boolean isCompleted() { return completed; }
    }
}
