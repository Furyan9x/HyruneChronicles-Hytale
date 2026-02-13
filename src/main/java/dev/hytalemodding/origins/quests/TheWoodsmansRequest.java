package dev.hytalemodding.origins.quests;

import dev.hytalemodding.origins.level.LevelingService;
import dev.hytalemodding.origins.playerdata.QuestProgress;
import dev.hytalemodding.origins.playerdata.QuestStatus;
import dev.hytalemodding.origins.skills.SkillType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Example quest: Player talks to NPC, crafts a crude axe, and returns it.
 */
public class TheWoodsmansRequest extends Quest {
    
    private static final String QUEST_ID = "woodsmans_request";
    private static final String NPC_NAME = "Old Woodsman";
    
    public enum Stage {
        NOT_STARTED,
        TALKED_TO_WOODSMAN,
        CRAFTED_AXE,
        COMPLETED;
        
        public String getJournalText() {
            switch (this) {
                case NOT_STARTED:
                    return "I should speak to " + NPC_NAME + " in the village.";
                case TALKED_TO_WOODSMAN:
                    return "I spoke to " + NPC_NAME + ". He asked me to craft a crude axe for him.";
                case CRAFTED_AXE:
                    return "I have crafted a crude axe. I need to return it to " + NPC_NAME + ".";
                case COMPLETED:
                    return "I gave the axe to " + NPC_NAME + ". He thanked me and gave me some supplies.";
                default:
                    return "";
            }
        }
    }
    
    public TheWoodsmansRequest() {
        super(
            QUEST_ID,
            "The Woodsman's Request",
            "A weary woodsman needs a replacement axe. Perhaps I can help him?",
            QuestLength.SHORT,
            QuestDifficulty.TUTORIAL,
            1 // Quest points
        );
    }
    
    @Override
    public String getJournalText(QuestProgress progress) {
        if (progress.getStatus() == QuestStatus.NOT_STARTED) {
            return "I should speak to " + NPC_NAME + " in the village.";
        }
        
        try {
            Stage stage = Stage.valueOf(progress.getCurrentStage());
            return stage.getJournalText();
        } catch (IllegalArgumentException e) {
            return "Unknown quest stage.";
        }
    }
    
    @Override
    public List<StageInfo> getStageList(QuestProgress progress) {
        List<StageInfo> stages = new ArrayList<>();
        
        Stage currentStage = Stage.NOT_STARTED;
        if (progress.getStatus() != QuestStatus.NOT_STARTED) {
            try {
                currentStage = Stage.valueOf(progress.getCurrentStage());
            } catch (IllegalArgumentException e) {
                currentStage = Stage.NOT_STARTED;
            }
        }
        
        // Stage 1: Speak to NPC
        boolean stage1Complete = currentStage.ordinal() > Stage.NOT_STARTED.ordinal();
        stages.add(new StageInfo(
            "I should speak to " + NPC_NAME + " in the village.",
            stage1Complete
        ));
        
        // Stage 2: Talked to NPC
        boolean stage2Complete = currentStage.ordinal() > Stage.TALKED_TO_WOODSMAN.ordinal();
        stages.add(new StageInfo(
            "I spoke to " + NPC_NAME + ". He asked me to craft a crude axe for him.",
            stage2Complete
        ));
        
        // Stage 3: Craft axe
        boolean stage3Complete = currentStage.ordinal() > Stage.CRAFTED_AXE.ordinal();
        stages.add(new StageInfo(
            "I have crafted a crude axe. I need to return it to " + NPC_NAME + ".",
            stage3Complete
        ));
        
        // Stage 4: Return to NPC
        boolean stage4Complete = currentStage == Stage.COMPLETED;
        stages.add(new StageInfo(
            "I gave the axe to " + NPC_NAME + ". He thanked me and gave me some supplies.",
            stage4Complete
        ));
        
        return stages;
    }
    
    @Override
    public List<QuestRequirement.LevelRequirement> getLevelRequirements() {
        List<QuestRequirement.LevelRequirement> reqs = new ArrayList<>();
        reqs.add(new QuestRequirement.LevelRequirement(SkillType.WOODCUTTING, 5));
        reqs.add(new QuestRequirement.LevelRequirement(SkillType.WEAPONSMITHING, 1));
        return reqs;
    }
    
    @Override
    public List<QuestRequirement.ItemRequirement> getItemRequirements() {
        // No item requirements to START the quest
        return new ArrayList<>();
    }
    
    @Override
    public List<String> getPrerequisiteQuests() {
        // No prerequisite quests
        return new ArrayList<>();
    }
    
    @Override
    public List<QuestReward> getRewards() {
        List<QuestReward> rewards = new ArrayList<>();
        rewards.add(new QuestReward.XpReward(SkillType.WOODCUTTING, 250));
        rewards.add(new QuestReward.XpReward(SkillType.WEAPONSMITHING, 150));
        rewards.add(new QuestReward.ItemReward("copper_coins", 50, "Copper Coins"));
        return rewards;
    }
    
    @Override
    public boolean meetsRequirements(UUID playerId) {
        LevelingService service = LevelingService.get();
        
        // Check level requirements
        for (QuestRequirement.LevelRequirement req : getLevelRequirements()) {
            if (service.getSkillLevel(playerId, req.getSkill()) < req.getLevel()) {
                return false;
            }
        }
        
        // Check prerequisite quests
        QuestManager manager = QuestManager.get();
        for (String questId : getPrerequisiteQuests()) {
            QuestProgress prereqProgress = manager.getQuestProgress(playerId, questId);
            if (prereqProgress == null || prereqProgress.getStatus() != QuestStatus.COMPLETED) {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public boolean tryAdvanceStage(UUID playerId, QuestProgress progress, String eventType, Object eventData) {
        Stage currentStage;
        try {
            currentStage = Stage.valueOf(progress.getCurrentStage());
        } catch (IllegalArgumentException e) {
            currentStage = Stage.NOT_STARTED;
        }
        
        switch (currentStage) {
            case NOT_STARTED:
                // Event: Talk to NPC
                if ("TALK_TO_NPC".equals(eventType) && NPC_NAME.equals(eventData)) {
                    progress.setCurrentStage(Stage.TALKED_TO_WOODSMAN.name());
                    progress.setStatus(QuestStatus.IN_PROGRESS);
                    return true;
                }
                break;
                
            case TALKED_TO_WOODSMAN:
                // Event: Craft crude axe
                if ("CRAFT_ITEM".equals(eventType) && "crude_axe".equals(eventData)) {
                    progress.setCurrentStage(Stage.CRAFTED_AXE.name());
                    return true;
                }
                break;
                
            case CRAFTED_AXE:
                // Event: Return to NPC
                if ("TALK_TO_NPC".equals(eventType) && NPC_NAME.equals(eventData)) {
                    progress.setCurrentStage(Stage.COMPLETED.name());
                    progress.setStatus(QuestStatus.COMPLETED);
                    progress.setCompletedAt(System.currentTimeMillis());
                    
                    // Grant rewards
                    for (QuestReward reward : getRewards()) {
                        reward.grant(playerId);
                    }
                    
                    // Award quest points
                    QuestManager.get().addQuestPoints(playerId, getQuestPoints());
                    
                    return true;
                }
                break;
                
            default:
                break;
        }
        
        return false;
    }
}
