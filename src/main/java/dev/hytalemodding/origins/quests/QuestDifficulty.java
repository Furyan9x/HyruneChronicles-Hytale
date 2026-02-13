package dev.hytalemodding.origins.quests;

public enum QuestDifficulty {
    TUTORIAL("Tutorial", 1),
    EASY("Easy", 2),
    MEDIUM("Medium", 3),
    HARD("Hard", 4),
    ELITE("Elite", 5);

    private final String displayName;
    private final int sortOrder;

    QuestDifficulty(String displayName, int sortOrder) {
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
