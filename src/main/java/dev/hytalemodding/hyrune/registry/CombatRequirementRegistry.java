package dev.hytalemodding.hyrune.registry;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Registry for combat requirement.
 */
public final class CombatRequirementRegistry {
    private static final Map<String, Integer> LEVELS = new LinkedHashMap<>();

    static {
        LEVELS.put("crude", 1);
        LEVELS.put("soft", 1);
        LEVELS.put("linen", 1);
        LEVELS.put("wooden", 1);
        LEVELS.put("wood", 1);
        LEVELS.put("scrap", 1);
        LEVELS.put("bone", 10);
        LEVELS.put("cotton", 10);
        LEVELS.put("stone", 10);
        LEVELS.put("iron", 10);
        LEVELS.put("light", 10);
        LEVELS.put("copper", 20);
        LEVELS.put("medium", 20);
        LEVELS.put("silk", 20);
        LEVELS.put("bronze", 25);
        LEVELS.put("cobalt", 30);
        LEVELS.put("heavy", 30);
        LEVELS.put("cindercloth", 30);
        LEVELS.put("thorium", 40);
        LEVELS.put("frost", 45);
        LEVELS.put("nexus", 45);
        LEVELS.put("void", 45);
        LEVELS.put("adamantite", 50);
        LEVELS.put("doomed", 55);
        LEVELS.put("mithril", 60);
        LEVELS.put("onyxium", 70);
        LEVELS.put("prisma", 80);
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
