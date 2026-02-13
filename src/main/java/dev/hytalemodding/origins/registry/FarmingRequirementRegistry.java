package dev.hytalemodding.origins.registry;

import dev.hytalemodding.origins.config.OriginsConfig;
import dev.hytalemodding.origins.config.OriginsConfigManager;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Map;

/**
 * Registry facade for farming seed and animal level requirements.
 */
public final class FarmingRequirementRegistry {
    private FarmingRequirementRegistry() {
    }

    @Nullable
    public static Integer getSeedRequiredLevel(@Nullable String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return null;
        }

        String id = itemId.toLowerCase(Locale.ROOT);
        OriginsConfig config = OriginsConfigManager.getConfig();
        if (config.farmingSeedLevelRequirements == null) {
            return null;
        }

        for (Map.Entry<String, Integer> entry : config.farmingSeedLevelRequirements.entrySet()) {
            String token = entry.getKey();
            Integer level = entry.getValue();
            if (token == null || token.isBlank() || level == null) {
                continue;
            }
            if (id.contains(token.toLowerCase(Locale.ROOT))) {
                return level;
            }
        }
        return null;
    }

    @Nullable
    public static Integer getAnimalRequiredLevel(@Nullable String targetId) {
        if (targetId == null || targetId.isBlank()) {
            return null;
        }

        String id = targetId.toLowerCase(Locale.ROOT);
        OriginsConfig config = OriginsConfigManager.getConfig();
        if (config.farmingAnimalLevelRequirements == null) {
            return null;
        }

        for (Map.Entry<String, Integer> entry : config.farmingAnimalLevelRequirements.entrySet()) {
            String token = entry.getKey();
            Integer level = entry.getValue();
            if (token == null || token.isBlank() || level == null) {
                continue;
            }
            if (id.contains(token.toLowerCase(Locale.ROOT))) {
                return level;
            }
        }
        return null;
    }

    public static boolean isAnimalHusbandryGatingEnabled() {
        return OriginsConfigManager.getConfig().enableAnimalHusbandryGating;
    }
}
