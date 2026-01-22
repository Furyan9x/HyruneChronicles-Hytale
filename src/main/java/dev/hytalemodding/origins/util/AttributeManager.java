package dev.hytalemodding.origins.util;

import dev.hytalemodding.origins.classes.StatGrowth;
import dev.hytalemodding.origins.playerdata.PlayerAttributes;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AttributeManager {
    private static AttributeManager instance;
    private final Map<UUID, PlayerAttributes> attributeCache = new HashMap<>();

    private AttributeManager() {}

    public static AttributeManager getInstance() {
        if (instance == null) {
            instance = new AttributeManager();
        }
        return instance;
    }

    public PlayerAttributes getPlayerData(UUID uuid) {
        // Create new data if it doesn't exist (Replace with DB load later)
        return attributeCache.computeIfAbsent(uuid, PlayerAttributes::new);
    }

    /**
     * Call this method whenever your LevelingService detects a Level Up.
     */
    public void applyLevelUpGrowth(UUID uuid, StatGrowth playerClass) {
        PlayerAttributes stats = getPlayerData(uuid);

        // Apply the growth values defined in the Enum
        stats.addStrength(playerClass.getStrGrowth());
        stats.addConstitution(playerClass.getConGrowth());
        stats.addIntellect(playerClass.getIntGrowth());
        stats.addAgility(playerClass.getAgiGrowth());
        stats.addWisdom(playerClass.getWisGrowth());

        // Optional: Log it for debugging
        System.out.println("Leveled up " + uuid + " as " + playerClass.name());
        System.out.println("New Strength: " + stats.getStrength());
    }
}