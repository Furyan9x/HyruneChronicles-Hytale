package dev.hytalemodding.origins.quests;

/**
 * Filter options for the quest list dropdown.
 */
public enum QuestListFilter {
    ALPHABETICAL("Alphabetical"),
    BY_LENGTH("By Length"),
    HIDE_COMPLETED("Hide Completed"),
    HIDE_UNAVAILABLE("Hide Unavailable");
    
    private final String displayName;
    
    QuestListFilter(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
