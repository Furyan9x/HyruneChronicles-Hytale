package dev.hytalemodding.hyrune.quests;

import com.hypixel.hytale.server.core.entity.ItemUtils;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import dev.hytalemodding.hyrune.skills.SkillType;
import com.hypixel.hytale.logger.HytaleLogger;
import dev.hytalemodding.hyrune.itemization.ItemGenerationService;
import dev.hytalemodding.hyrune.itemization.ItemRarityRollModel;
import dev.hytalemodding.hyrune.itemization.ItemRollSource;

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
            if (playerId == null || itemId == null || itemId.isBlank() || count <= 0) {
                return;
            }

            PlayerRef playerRef = Universe.get().getPlayer(playerId);
            if (playerRef == null || playerRef.getReference() == null) {
                LOGGER.at(Level.INFO).log("Skipping item reward for offline player " + playerId + ": " + count + "x " + itemId);
                return;
            }

            var entityRef = playerRef.getReference();
            var store = entityRef.getStore();
            if (store == null) {
                return;
            }

            Player player = store.getComponent(entityRef, Player.getComponentType());
            if (player == null) {
                return;
            }

            Inventory inventory = player.getInventory();
            if (inventory == null) {
                return;
            }

            ItemContainer container = inventory.getCombinedHotbarFirst();
            if (container == null) {
                return;
            }

            ItemStack reward = ItemGenerationService.rollIfEligible(
                new ItemStack(itemId, count),
                ItemRollSource.QUEST_REWARD,
                ItemRarityRollModel.GenerationContext.of("quest_reward_item")
            );

            var tx = container.addItemStack(reward);
            ItemStack remainder = tx == null ? reward : tx.getRemainder();
            if (remainder != null && !remainder.isEmpty()) {
                ItemUtils.dropItem(entityRef, remainder, store);
            }

            LOGGER.at(Level.INFO).log("Granted " + count + "x " + itemId + " to player " + playerId);
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
