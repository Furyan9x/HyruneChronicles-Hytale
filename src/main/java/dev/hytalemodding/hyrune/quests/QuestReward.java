package dev.hytalemodding.hyrune.quests;

import dev.hytalemodding.hyrune.skills.SkillType;
import com.hypixel.hytale.logger.HytaleLogger;

import java.util.logging.Level;

/**
 * Base class for quest rewards.
 */
public abstract class QuestReward {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    
    public abstract String getDisplayText();
    
    public abstract void grant(java.util.UUID playerId);
    
    /**
     * XP reward for a specific skill.
     */
    public static class XpReward extends QuestReward {
        private final SkillType skill;
        private final long amount;
        
        public XpReward(SkillType skill, long amount) {
            this.skill = skill;
            this.amount = amount;
        }
        
        @Override
        public String getDisplayText() {
            return amount + " " + skill.getDisplayName() + " XP";
        }
        
        @Override
        public void grant(java.util.UUID playerId) {
            dev.hytalemodding.hyrune.level.LevelingService.get()
                .addSkillXp(playerId, skill, amount);
        }
        
        public SkillType getSkill() { return skill; }
        public long getAmount() { return amount; }
    }
    
    /**
     * Item reward.
     */
    public static class ItemReward extends QuestReward {
        private final String itemId;
        private final int count;
        private final String displayName;
        
        public ItemReward(String itemId, int count, String displayName) {
            this.itemId = itemId;
            this.count = count;
            this.displayName = displayName;
        }
        
        @Override
        public String getDisplayText() {
            return count + "x " + displayName;
        }
        
        @Override
        public void grant(java.util.UUID playerId) {
            // TODO: Implement item granting through inventory system
            LOGGER.at(Level.INFO).log("Granting " + count + "x " + itemId + " to player " + playerId);
        }
        
        public String getItemId() { return itemId; }
        public int getCount() { return count; }
        public String getDisplayName() { return displayName; }
    }
    
    /**
     * Generic text-based unlock/reward.
     */
    public static class UnlockReward extends QuestReward {
        private final String unlockText;
        
        public UnlockReward(String unlockText) {
            this.unlockText = unlockText;
        }
        
        @Override
        public String getDisplayText() {
            return unlockText;
        }
        
        @Override
        public void grant(java.util.UUID playerId) {
            // Unlocks are typically passive/text-only
            LOGGER.at(Level.INFO).log("Unlocked: " + unlockText + " for player " + playerId);
        }
        
        public String getUnlockText() { return unlockText; }
    }
}
