package dev.hytalemodding.origins.registry;

import dev.hytalemodding.origins.skills.SkillType;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CraftingSkillRegistry {

    public static final class Reward {
        public final int minLevel;
        public final long xp;

        private Reward(int minLevel, long xp) {
            this.minLevel = minLevel;
            this.xp = xp;
        }
    }

    public static final class SkillReward {
        public final SkillType skill;
        public final Reward reward;

        private SkillReward(SkillType skill, Reward reward) {
            this.skill = skill;
            this.reward = reward;
        }
    }

    private static final class SkillRuleSet {
        private final SkillType skill;
        private final Map<String, Reward> exact;
        private final Map<String, Reward> keywords;

        private SkillRuleSet(SkillType skill, Map<String, Reward> exact, Map<String, Reward> keywords) {
            this.skill = skill;
            this.exact = exact;
            this.keywords = keywords;
        }
    }

    private static final List<SkillRuleSet> RULES;
    private static final Map<String, Double> MATERIAL_MULTIPLIERS = new HashMap<>();
    private static final Map<Integer, Double> BENCH_TIER_MULTIPLIERS = new HashMap<>();

    static {
        List<SkillRuleSet> rules = new ArrayList<>();

        rules.add(new SkillRuleSet(
                SkillType.SMELTING,
                new HashMap<>(),
                keywordRule("bar", 1, 20, "ingot")
        ));
        rules.add(new SkillRuleSet(
                SkillType.ARCANE_ENGINEERING,
                new HashMap<>(),
                keywordRule("staff", 1, 20, "wand", "book", "grimoire", "totem", "robe", "focus", "enchantment", "portalkey", "teleporter", "portal", "cloth")
        ));
        rules.add(new SkillRuleSet(
                SkillType.WEAPONSMITHING,
                new HashMap<>(),
                keywordRule("weapon", 1, 20, "sword", "mace", "battleaxe", "dagger", "shortbow", "longbow", "crossbow")
        ));
        rules.add(new SkillRuleSet(
                SkillType.COOKING,
                new HashMap<>(),
                keywordRule("food", 1, 20, "salt", "flour", "spices")
        ));
        rules.add(new SkillRuleSet(
                SkillType.LEATHERWORKING,
                new HashMap<>(),
                keywordRule("leather", 1, 20)
        ));
        rules.add(new SkillRuleSet(
                SkillType.ARCHITECT,
                new HashMap<>(),
                keywordRule("furniture", 1, 20, "deco", "smooth", "bolt", "wool", "petals", "wall", "cauldron", "wood", "brick", "roof", "beam", "half", "stairs", "bench", "tool", "trap")
        ));
        rules.add(new SkillRuleSet(
                SkillType.ALCHEMY,
                new HashMap<>(),
                keywordRule("potion", 1, 20, "bomb", "bandage")
        ));
        rules.add(new SkillRuleSet(
                SkillType.FARMING,
                new HashMap<>(),
                keywordRule("fertilizer", 1, 20, "coop", "hoe", "capture", "shears", "bucket", "watering", "feedbag", "seeds", "sapling", "life_essence")
        ));
        rules.add(new SkillRuleSet(
                SkillType.ARMORSMITHING,
                new HashMap<>(),
                keywordRule("armor", 1, 20, "shield")
        ));

        RULES = Collections.unmodifiableList(rules);

        MATERIAL_MULTIPLIERS.put("crude", 0.8);
        MATERIAL_MULTIPLIERS.put("copper", 1.0);
        MATERIAL_MULTIPLIERS.put("iron", 1.2);
        MATERIAL_MULTIPLIERS.put("thorium", 1.4);
        MATERIAL_MULTIPLIERS.put("cobalt", 1.4);
        MATERIAL_MULTIPLIERS.put("adamantite", 1.4);
        MATERIAL_MULTIPLIERS.put("mithril", 1.4);

        BENCH_TIER_MULTIPLIERS.put(1, 1.0);
        BENCH_TIER_MULTIPLIERS.put(2, 1.5);
        BENCH_TIER_MULTIPLIERS.put(3, 2.5);
        BENCH_TIER_MULTIPLIERS.put(4, 4.0);
        BENCH_TIER_MULTIPLIERS.put(5, 6.0);
        BENCH_TIER_MULTIPLIERS.put(6, 7.5);
    }

    private CraftingSkillRegistry() {
    }

    @Nullable
    public static SkillReward findByRecipeId(@Nullable String recipeId) {
        return findMatch(recipeId);
    }

    @Nullable
    public static SkillReward findByItemId(@Nullable String itemId) {
        return findMatch(itemId);
    }

    @Nullable
    private static SkillReward findMatch(@Nullable String rawId) {
        if (rawId == null || rawId.isEmpty()) {
            return null;
        }

        String id = rawId.toLowerCase(Locale.ROOT);

        for (SkillRuleSet ruleSet : RULES) {
            Reward reward = ruleSet.exact.get(id);
            if (reward != null) {
                return new SkillReward(ruleSet.skill, reward);
            }
        }

        for (SkillRuleSet ruleSet : RULES) {
            Reward reward = findKeyword(ruleSet, id);
            if (reward != null) {
                return new SkillReward(ruleSet.skill, reward);
            }
        }

        return null;
    }

    @Nullable
    private static Reward findKeyword(SkillRuleSet ruleSet, String id) {
        for (Map.Entry<String, Reward> entry : ruleSet.keywords.entrySet()) {
            if (id.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static Map<String, Reward> keywordRule(String keyword, int minLevel, long xp) {
        Map<String, Reward> map = new HashMap<>();
        map.put(keyword.toLowerCase(Locale.ROOT), new Reward(minLevel, xp));
        return map;
    }

    private static Map<String, Reward> keywordRule(String keyword, int minLevel, long xp, String... extraKeywords) {
        Map<String, Reward> map = keywordRule(keyword, minLevel, xp);
        for (String extraKeyword : extraKeywords) {
            map.put(extraKeyword.toLowerCase(Locale.ROOT), new Reward(minLevel, xp));
        }
        return map;
    }

    public static double getMaterialMultiplier(@Nullable String rawId) {
        if (rawId == null || rawId.isEmpty()) {
            return 1.0;
        }

        String id = rawId.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, Double> entry : MATERIAL_MULTIPLIERS.entrySet()) {
            if (id.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return 1.0;
    }

    public static double getBenchTierMultiplier(int tierLevel) {
        return BENCH_TIER_MULTIPLIERS.getOrDefault(tierLevel, 1.0);
    }
}
