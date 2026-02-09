package dev.hytalemodding.origins.quests;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks a player's progress on a specific quest.
 * Stores only the current stage name (enum string) and simple data.
 */
public class QuestProgress {
    
    public static final BuilderCodec<QuestProgress> CODEC = BuilderCodec.builder(QuestProgress.class, QuestProgress::new)
        .addField(new KeyedCodec<>("QuestId", Codec.STRING), (p, s) -> p.questId = s, p -> p.questId)
        .addField(new KeyedCodec<>("Status", Codec.STRING), (p, s) -> p.status = QuestStatus.valueOf(s), p -> p.status.name())
        .addField(new KeyedCodec<>("CurrentStage", Codec.STRING), (p, s) -> p.currentStage = s, p -> p.currentStage)
        .addField(new KeyedCodec<>("StageData", MapCodec.STRING_HASH_MAP_CODEC), (p, m) -> p.stageData = (Map<String, String>)m, p -> p.stageData)
        .addField(new KeyedCodec<>("StartedAt", Codec.LONG), (p, l) -> p.startedAt = l, p -> p.startedAt)
        .addField(new KeyedCodec<>("CompletedAt", Codec.LONG), (p, l) -> p.completedAt = l, p -> p.completedAt)
        .build();
    
    private String questId;
    private QuestStatus status;
    private String currentStage; // Stores the enum.name() as a string
    private Map<String, String> stageData; // Flexible storage for stage-specific data
    private long startedAt;
    private long completedAt;
    
    public QuestProgress() {
        this.stageData = new HashMap<>();
        this.status = QuestStatus.NOT_STARTED;
        this.currentStage = "";
        this.startedAt = 0L;
        this.completedAt = 0L;
    }
    
    public QuestProgress(String questId) {
        this();
        this.questId = questId;
    }
    
    // Getters and Setters
    public String getQuestId() { return questId; }
    public void setQuestId(String questId) { this.questId = questId; }
    
    public QuestStatus getStatus() { return status; }
    public void setStatus(QuestStatus status) { this.status = status; }
    
    public String getCurrentStage() { return currentStage; }
    public void setCurrentStage(String currentStage) { this.currentStage = currentStage; }
    
    public Map<String, String> getStageData() { return stageData; }
    public void setStageData(Map<String, String> stageData) { this.stageData = stageData; }
    
    public long getStartedAt() { return startedAt; }
    public void setStartedAt(long startedAt) { this.startedAt = startedAt; }
    
    public long getCompletedAt() { return completedAt; }
    public void setCompletedAt(long completedAt) { this.completedAt = completedAt; }
    
    // Convenience methods
    public void setStageDataValue(String key, String value) {
        this.stageData.put(key, value);
    }
    
    public String getStageDataValue(String key) {
        return this.stageData.get(key);
    }
    
    public boolean hasStageData(String key) {
        return this.stageData.containsKey(key);
    }
}
