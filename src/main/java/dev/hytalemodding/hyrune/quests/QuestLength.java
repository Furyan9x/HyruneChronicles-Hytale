package dev.hytalemodding.hyrune.quests;

/**
 * Quest length categories for filtering and sorting.
 */
public enum QuestLength {
    SHORT("Short", 1),      // 1-2 objectives, <10 minutes
    MEDIUM("Medium", 2),    // 3-5 objectives, 10-30 minutes
    LONG("Long", 3),        // 6-10 objectives, 30-60 minutes
    EPIC("Epic", 4);        // 11+ objectives, 60+ minutes
    
    private final String displayName;
    private final int sortOrder;
    
    QuestLength(String displayName, int sortOrder) {
        this.displayName = displayName;
        this.sortOrder = sortOrder;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public int getSortOrder() {
        return sortOrder;
    }
}
