package dev.hytalemodding.origins.quests;

import dev.hytalemodding.origins.skills.SkillType;

/**
 * Container class for various quest requirement types.
 */
public class QuestRequirement {
    
    /**
     * Skill level requirement (e.g., Woodcutting Level 10).
     */
    public static class LevelRequirement {
        private final SkillType skill;
        private final int level;
        
        public LevelRequirement(SkillType skill, int level) {
            this.skill = skill;
            this.level = level;
        }
        
        public SkillType getSkill() { return skill; }
        public int getLevel() { return level; }
        
        public String getDisplayText() {
            return skill.getDisplayName() + " Level " + level;
        }
    }

    /**
         * Item requirement (e.g., 5x Iron Ore).
         */
        public record ItemRequirement(String itemId, int count, String displayName) {

        public String getDisplayText() {
                return count + "x " + displayName;
            }
        }
}
