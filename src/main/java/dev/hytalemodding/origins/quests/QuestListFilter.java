package dev.hytalemodding.origins.quests;

/**
 * Filter options for the quest list dropdown.
 */
public enum QuestListFilter {
    ALPHABETICAL("Alphabetical"),
    BY_LENGTH("By Length"),
    BY_DIFFICULTY("By Difficulty");
    
    private final String displayName;
    
    QuestListFilter(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }

    public static QuestListFilter fromString(String value, QuestListFilter fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.trim();
        for (QuestListFilter filter : values()) {
            if (filter.name().equalsIgnoreCase(normalized)) {
                return filter;
            }
            if (filter.displayName.equalsIgnoreCase(normalized)) {
                return filter;
            }
        }
        if ("HIDE_COMPLETED".equalsIgnoreCase(normalized) || "HIDE_UNAVAILABLE".equalsIgnoreCase(normalized)) {
            return fallback;
        }
        return fallback;
    }
}
