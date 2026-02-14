package dev.hytalemodding.hyrune.registry;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Registry for tool requirement.
 */
public final class ToolRequirementRegistry {
    private static final Map<String, Integer> TOOL_LEVELS = new HashMap<>();

    static {
        TOOL_LEVELS.put("crude", 1);
        TOOL_LEVELS.put("copper", 10);
        TOOL_LEVELS.put("iron", 15);
        TOOL_LEVELS.put("cobalt", 30);
        TOOL_LEVELS.put("adamantite", 40);
        TOOL_LEVELS.put("thorium", 50);
        TOOL_LEVELS.put("mithril", 60);
        TOOL_LEVELS.put("onyxium", 70);
    }

    private ToolRequirementRegistry() {
    }

    @Nullable
    public static Integer getRequiredLevel(@Nullable String itemId) {
        if (itemId == null) {
            return null;
        }
        String id = itemId.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, Integer> entry : TOOL_LEVELS.entrySet()) {
            if (id.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }
}
