package dev.hytalemodding.hyrune.repair;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Central resolver from item IDs to repair profiles.
 */
public final class RepairProfileRegistry {
    private static final List<Rule> RULES = new ArrayList<>();

    private static final RepairProfile DEFAULT_PROFILE = RepairProfile
        .builder("Ingredient_Bar_Iron", "Ingredient_Leather_Light")
        .baseCosts(6, 4)
        .rareGem("Ingredient_Essence_Void", 1)
        .build();

    static {
        resetToDefaults();
    }

    private RepairProfileRegistry() {
    }

    public static synchronized RepairProfile resolve(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return DEFAULT_PROFILE;
        }

        String normalized = itemId.toLowerCase(Locale.ROOT);
        Rule best = null;
        for (Rule rule : RULES) {
            if (!rule.matches(normalized)) {
                continue;
            }
            if (best == null || rule.isHigherPriorityThan(best)) {
                best = rule;
            }
        }
        return best != null ? best.profile : DEFAULT_PROFILE;
    }

    public static synchronized void resetToDefaults() {
        RULES.clear();
        applyDefinitions(getDefaultDefinitions());
    }

    public static synchronized void reloadFromConfig(RepairProfileConfig config) {
        RULES.clear();
        if (config == null || config.profiles == null || config.profiles.isEmpty()) {
            applyDefinitions(getDefaultDefinitions());
            return;
        }
        applyDefinitions(config.profiles);
        if (RULES.isEmpty()) {
            applyDefinitions(getDefaultDefinitions());
        }
    }

    public static List<RepairProfileDefinition> getDefaultDefinitions() {
        List<RepairProfileDefinition> defaults = new ArrayList<>();
        // Material/tier-specific weapon rules (higher priority than generic Weapon_ fallback).
        defaults.add(def("adamantite", "contains", 300, "Ingredient_Bar_Adamantite", "Ingredient_Leather_Heavy", "Ingredient_Essence_Fire", 10, 6, 2));
        defaults.add(def("mithril", "contains", 290, "Ingredient_Bar_Mithril", "Ingredient_Leather_Medium", "Ingredient_Essence_Fire", 9, 5, 2));
        defaults.add(def("steel", "contains", 280, "Ingredient_Bar_Steel", "Ingredient_Leather_Medium", "Ingredient_Essence_Fire", 8, 5, 1));
        defaults.add(def("iron", "contains", 270, "Ingredient_Bar_Iron", "Ingredient_Leather_Light", "Ingredient_Essence_Fire", 7, 4, 1));
        defaults.add(def("copper", "contains", 260, "Ingredient_Bar_Copper", "Ingredient_Leather_Light", "Ingredient_Essence_Fire", 6, 4, 1));

        // Weapon type tuning.
        defaults.add(def("weapon_shortbow_", "prefix", 240, "Ingredient_Plank_Oak", "Ingredient_Fabric_Scrap_Linen", "Ingredient_Essence_Fire", 6, 5, 1));
        defaults.add(def("weapon_staff_", "prefix", 235, "Ingredient_Plank_Oak", "Ingredient_Fabric_Scrap_Linen", "Ingredient_Essence_Void", 6, 5, 1));
        defaults.add(def("weapon_daggers_", "prefix", 230, "Ingredient_Bar_Iron", "Ingredient_Leather_Light", "Ingredient_Essence_Fire", 6, 4, 1));

        // Armor and cloth families.
        defaults.add(def("armor_", "prefix", 200, "Ingredient_Bar_Iron", "Ingredient_Leather_Light", "Ingredient_Essence_Void", 8, 5, 1));
        defaults.add(def("robe", "contains", 190, "Ingredient_Bolt_Linen", "Ingredient_Fabric_Scrap_Linen", "Ingredient_Essence_Void", 7, 4, 1));
        defaults.add(def("cloth", "contains", 180, "Ingredient_Bolt_Linen", "Ingredient_Fabric_Scrap_Linen", "Ingredient_Essence_Void", 6, 4, 1));

        // Gatherer tools.
        defaults.add(def("tool_pickaxe_", "prefix", 170, "Ingredient_Bar_Iron", "Ingredient_Plank_Oak", "Ingredient_Essence_Fire", 6, 3, 1));
        defaults.add(def("tool_hatchet_", "prefix", 160, "Ingredient_Bar_Iron", "Ingredient_Plank_Oak", "Ingredient_Essence_Fire", 6, 3, 1));
        defaults.add(def("fishing_rod", "contains", 150, "Ingredient_Plank_Oak", "Ingredient_Fabric_Scrap_Linen", "Ingredient_Essence_Void", 4, 4, 1));
        defaults.add(def("tool_", "prefix", 140, "Ingredient_Plank_Oak", "Ingredient_Block_Stone", "Ingredient_Essence_Fire", 5, 4, 1));

        // Generic fallbacks.
        defaults.add(def("weapon_", "prefix", 100, "Ingredient_Bar_Iron", "Ingredient_Leather_Light", "Ingredient_Essence_Fire", 7, 4, 1));
        defaults.add(def("armor", "contains", 90, "Ingredient_Bar_Iron", "Ingredient_Leather_Light", "Ingredient_Essence_Void", 8, 5, 1));
        return defaults;
    }

    private static RepairProfileDefinition def(String keyword,
                                               String matchType,
                                               int priority,
                                               String primary,
                                               String secondary,
                                               String gem,
                                               int primaryCost,
                                               int secondaryCost,
                                               int gemCost) {
        RepairProfileDefinition def = new RepairProfileDefinition();
        def.keyword = keyword;
        def.matchType = matchType;
        def.priority = priority;
        def.primaryMaterial = primary;
        def.secondaryMaterial = secondary;
        def.rareGemMaterial = gem;
        def.primaryBaseCost = primaryCost;
        def.secondaryBaseCost = secondaryCost;
        def.gemBaseCost = gemCost;
        return def;
    }

    private static void applyDefinitions(List<RepairProfileDefinition> definitions) {
        if (definitions == null) {
            return;
        }

        int order = 0;
        for (RepairProfileDefinition definition : definitions) {
            if (definition == null
                || isBlank(definition.keyword)
                || isBlank(definition.primaryMaterial)
                || isBlank(definition.secondaryMaterial)) {
                order++;
                continue;
            }

            RepairProfile.Builder builder = RepairProfile
                .builder(definition.primaryMaterial, definition.secondaryMaterial)
                .baseCosts(definition.primaryBaseCost, definition.secondaryBaseCost);

            if (!isBlank(definition.rareGemMaterial)) {
                builder.rareGem(definition.rareGemMaterial, definition.gemBaseCost);
            }

            MatchType matchType = MatchType.parse(definition.matchType);
            RULES.add(new Rule(
                definition.keyword.toLowerCase(Locale.ROOT),
                matchType,
                definition.priority,
                order,
                builder.build()
            ));
            order++;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private enum MatchType {
        PREFIX,
        CONTAINS,
        EXACT;

        private static MatchType parse(String raw) {
            if (raw == null || raw.isBlank()) {
                return CONTAINS;
            }
            switch (raw.toLowerCase(Locale.ROOT)) {
                case "exact":
                    return EXACT;
                case "prefix":
                    return PREFIX;
                case "contains":
                default:
                    return CONTAINS;
            }
        }
    }

    private static final class Rule {
        private final String keyword;
        private final MatchType matchType;
        private final int priority;
        private final int order;
        private final RepairProfile profile;

        private Rule(String keyword, MatchType matchType, int priority, int order, RepairProfile profile) {
            this.keyword = keyword;
            this.matchType = matchType;
            this.priority = priority;
            this.order = order;
            this.profile = profile;
        }

        private boolean matches(String itemId) {
            switch (matchType) {
                case EXACT:
                    return itemId.equals(keyword);
                case PREFIX:
                    return itemId.startsWith(keyword);
                case CONTAINS:
                default:
                    return itemId.contains(keyword);
            }
        }

        private boolean isHigherPriorityThan(Rule other) {
            if (this.priority != other.priority) {
                return this.priority > other.priority;
            }
            if (this.keyword.length() != other.keyword.length()) {
                return this.keyword.length() > other.keyword.length();
            }
            return this.order < other.order;
        }
    }
}

