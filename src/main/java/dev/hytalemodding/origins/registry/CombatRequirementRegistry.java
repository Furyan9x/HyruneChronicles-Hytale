package dev.hytalemodding.origins.registry;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class CombatRequirementRegistry {
    private static final Map<String, Integer> LEVELS = new LinkedHashMap<>();

    static {
        LEVELS.put("crude", 1);
        LEVELS.put("wooden", 1);
        LEVELS.put("wood", 1);
        LEVELS.put("scrap", 1);
        LEVELS.put("bone", 10);
        LEVELS.put("stone", 10);
        LEVELS.put("iron", 10);
        LEVELS.put("cobalt", 30);
        LEVELS.put("thorium", 40);
        LEVELS.put("adamantite", 50);
        LEVELS.put("doomed", 50);
        LEVELS.put("mithril", 60);
        LEVELS.put("onyxium", 70);
    }

    private CombatRequirementRegistry() {
    }

    @Nullable
    public static Integer getRequiredLevel(@Nullable String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return null;
        }
        String id = itemId.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, Integer> entry : LEVELS.entrySet()) {
            if (id.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }
}
