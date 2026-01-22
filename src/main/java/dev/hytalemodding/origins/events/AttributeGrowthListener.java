package dev.hytalemodding.origins.events;

import dev.hytalemodding.origins.classes.StatGrowth;
import dev.hytalemodding.origins.util.AttributeManager;

import java.util.UUID;

public class AttributeGrowthListener implements LevelUpListener {

    @Override
    public void onLevelUp(UUID playerId, int newLevel, String sourceName) {
        // 1. Check if the source is a Combat Class
        // We try to match the source name (e.g. "Mage") to our RPGClass enum.
        try {
            StatGrowth combatClass = StatGrowth.valueOf(sourceName.toUpperCase());

            // 2. If successful, it IS a combat class. Apply the growth.
            AttributeManager.getInstance().applyLevelUpGrowth(playerId, combatClass);

            // Debug message (optional)
            System.out.println("Valid Combat Level Up detected! Stats applied for " + sourceName);

        } catch (IllegalArgumentException e) {
            // 3. If it fails, it means 'sourceName' was "Mining", "Global", or "Woodcutting".
            // We intentionally do NOTHING here.

        }
    }
}