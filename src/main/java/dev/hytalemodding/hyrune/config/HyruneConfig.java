package dev.hytalemodding.hyrune.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runtime config for Hyrune gameplay switches and requirement maps.
 */
public class HyruneConfig {
    public static class RarityScalarConfig {
        public double common = 1.00;
        public double uncommon = 1.10;
        public double rare = 1.22;
        public double epic = 1.36;
        public double vocational = 1.36;
        public double legendary = 1.52;
        public double mythic = 1.70;
    }

    public static class ItemizationSpecializedStatsConfig {
        public double flatRollMinScalar = 0.20;
        public double flatRollMaxScalar = 0.80;
        // Deprecated fallback. Prefer statDefinitions[*].baseMin/baseMax.
        public double percentRollMin = 0.02;
        // Deprecated fallback. Prefer statDefinitions[*].baseMin/baseMax.
        public double percentRollMax = 0.14;
        public double percentRollTierInfluence = 0.25;
        public double legendaryRollStatMultiplier = 1.10;
        public double mythicRollStatMultiplier = 1.35;
        public double durabilityTierInfluence = 1.00;
        public Map<String, Double> rollTypeWeights = defaultRollTypeWeights();
        public Map<String, String> rollTypeConstraintByStat = defaultRollTypeConstraintByStat();
        public Map<String, StatDefinitionConfig> statDefinitions = defaultStatDefinitions();
        public Map<String, Double> tierScalarByKeyword = defaultTierScalarByKeyword();
        public RarityScalarConfig rarityScalar = new RarityScalarConfig();
        public Map<String, Integer> statsPerRarity = defaultStatsPerRarity();
        public Map<String, Double> baseStatWeights = defaultBaseStatWeights();
        public Map<String, List<String>> poolByArchetype = defaultPoolByArchetype();
        public Map<String, List<String>> poolByPrefix = defaultPoolByPrefix();
        public Map<Integer, Double> prefixPriorityWeightByRank = defaultPrefixPriorityWeightByRank();
        public Map<String, Map<String, Double>> baseStatsByArchetype = defaultBaseStatsByArchetype();
        public int uiDisplayFlatDecimals = 0;
        public int uiDisplayPercentDecimals = 1;
    }

    public static class StatDefinitionConfig {
        public double baseMin = 0.01;
        public double baseMax = 0.03;
        public double scalingWeight = 0.20;

        public StatDefinitionConfig() {
        }

        public StatDefinitionConfig(double baseMin, double baseMax, double scalingWeight) {
            this.baseMin = baseMin;
            this.baseMax = baseMax;
            this.scalingWeight = scalingWeight;
        }
    }

    public static class PrefixConfig {
        public List<String> rollableWords = defaultPrefixWords();
    }

    public static class GemSocketConfig {
        public boolean enabled = true;
        public double maxHpPerSocketedGem = 1.0;
        public Map<String, Integer> socketsPerRarity = defaultSocketsPerRarity();
        public List<String> gemItemIds = defaultGemItemIds();
        public Map<String, Map<String, Map<String, Double>>> bonusesByGemPatternAndArchetype = defaultGemBonusesByPatternAndArchetype();
    }

    public static class RarityWeights {
        public double common = 0.700;
        public double uncommon = 0.180;
        public double rare = 0.075;
        public double epic = 0.030;
        public double legendary = 0.010;
        public double mythic = 0.005;
    }

    public static class ItemizationRarityModelConfig {
        public Map<String, RarityWeights> baseWeightsBySource = defaultRarityWeightsBySource();
        public Map<String, Double> professionBonusPerLevel = defaultProfessionBonusPerLevel();
        public Map<Integer, Double> benchTierBonus = defaultBenchTierBonus();
        public double rarityShiftStrength = 0.22;
        public double minRarityScore = -1.5;
        public double maxRarityScore = 1.5;
        public int maxProfessionLevel = 99;
    }

    public boolean durabilityDebugLogging = true;
    public boolean itemizationDebugLogging = true;
    public boolean enableDynamicItemTooltips = true;
    public boolean dynamicTooltipComposeDebug = true;
    public boolean dynamicTooltipMappingDebug = true;
    public boolean dynamicTooltipCacheDebug = true;
    public List<String> itemizationEligiblePrefixes = defaultItemizationEligiblePrefixes();
    public List<String> itemizationExcludedPrefixes = defaultItemizationExcludedPrefixes();
    public List<String> itemizationExcludedIds = List.of();
    public ItemizationRarityModelConfig itemizationRarityModel = new ItemizationRarityModelConfig();
    public ItemizationSpecializedStatsConfig itemizationSpecializedStats = new ItemizationSpecializedStatsConfig();
    public boolean enableAnimalHusbandryGating = true;
    public Map<String, Integer> farmingSeedLevelRequirements = defaultSeedRequirements();
    public Map<String, Integer> farmingAnimalLevelRequirements = defaultAnimalRequirements();
    public Map<String, String> npcNameOverrides = new LinkedHashMap<>();
    public PrefixConfig prefixes = new PrefixConfig();
    public GemSocketConfig gemSockets = new GemSocketConfig();

    private static Map<String, Integer> defaultSeedRequirements() {
        Map<String, Integer> defaults = new LinkedHashMap<>();
        defaults.put("Plant_Seeds_Wheat", 1);
        defaults.put("Plant_Seeds_Lettuce", 10);
        defaults.put("Plant_Seeds_Carrots", 20);
        return defaults;
    }

    private static Map<String, Integer> defaultAnimalRequirements() {
        Map<String, Integer> defaults = new LinkedHashMap<>();
        defaults.put("chicken", 1);
        defaults.put("pig", 10);
        defaults.put("cow", 20);
        return defaults;
    }

    private static List<String> defaultItemizationEligiblePrefixes() {
        return List.of("weapon_", "armor_", "tool_");
    }

    private static List<String> defaultItemizationExcludedPrefixes() {
        return List.of(
            "weapon_bomb_",
            "weapon_grenade_",
            "weapon_poison_flask_",
            "weapon_dev_",
            "armor_dev_",
            "tool_dev_",
            "test_",
            "special_"
        );
    }

    private static Map<String, RarityWeights> defaultRarityWeightsBySource() {
        Map<String, RarityWeights> defaults = new LinkedHashMap<>();

        RarityWeights crafted = new RarityWeights();
        crafted.common = 0.700;
        crafted.uncommon = 0.180;
        crafted.rare = 0.075;
        crafted.epic = 0.030;
        crafted.legendary = 0.010;
        crafted.mythic = 0.005;
        defaults.put("crafted", crafted);

        RarityWeights dropped = new RarityWeights();
        dropped.common = 0.760;
        dropped.uncommon = 0.160;
        dropped.rare = 0.055;
        dropped.epic = 0.018;
        dropped.legendary = 0.006;
        dropped.mythic = 0.001;
        defaults.put("dropped", dropped);
        return defaults;
    }

    private static Map<String, Double> defaultProfessionBonusPerLevel() {
        Map<String, Double> defaults = new LinkedHashMap<>();
        defaults.put("WEAPONSMITHING", 0.0040);
        defaults.put("ARMORSMITHING", 0.0040);
        defaults.put("ARCANE_ENGINEERING", 0.0035);
        defaults.put("LEATHERWORKING", 0.0030);
        defaults.put("SMELTING", 0.0025);
        defaults.put("ARCHITECT", 0.0020);
        return defaults;
    }

    private static Map<Integer, Double> defaultBenchTierBonus() {
        Map<Integer, Double> defaults = new LinkedHashMap<>();
        defaults.put(1, 0.0);
        defaults.put(2, 0.08);
        defaults.put(3, 0.16);
        defaults.put(4, 0.24);
        defaults.put(5, 0.32);
        defaults.put(6, 0.40);
        return defaults;
    }

    private static Map<String, Integer> defaultStatsPerRarity() {
        Map<String, Integer> defaults = new LinkedHashMap<>();
        defaults.put("COMMON", 1);
        defaults.put("UNCOMMON", 2);
        defaults.put("RARE", 3);
        defaults.put("EPIC", 4);
        defaults.put("VOCATIONAL", 4);
        defaults.put("LEGENDARY", 5);
        defaults.put("MYTHIC", 6);
        return defaults;
    }

    private static Map<String, Integer> defaultSocketsPerRarity() {
        Map<String, Integer> defaults = new LinkedHashMap<>();
        defaults.put("COMMON", 1);
        defaults.put("UNCOMMON", 1);
        defaults.put("RARE", 2);
        defaults.put("EPIC", 2);
        defaults.put("VOCATIONAL", 2);
        defaults.put("LEGENDARY", 3);
        defaults.put("MYTHIC", 3);
        return defaults;
    }

    private static List<String> defaultGemItemIds() {
        return List.of(
            "Rock_Gem_*"
        );
    }

    private static List<String> defaultPrefixWords() {
        return List.of(
            "Flame",
            "Desert",
            "Ember",
            "Blaze",
            "Wave",
            "Ocean",
            "Tide",
            "Gale",
            "Squall",
            "Zephyr",
            "Stone",
            "Crag",
            "Root"
        );
    }

    private static Map<Integer, Double> defaultPrefixPriorityWeightByRank() {
        Map<Integer, Double> out = new LinkedHashMap<>();
        out.put(1, 1.50);
        out.put(2, 1.25);
        out.put(3, 1.00);
        out.put(4, 0.80);
        out.put(5, 0.65);
        out.put(6, 0.55);
        return out;
    }

    private static Map<String, Map<String, Map<String, Double>>> defaultGemBonusesByPatternAndArchetype() {
        Map<String, Map<String, Map<String, Double>>> out = new LinkedHashMap<>();
        out.put("Rock_Gem_Ruby", archetypeBonusMap(
            "weapon_melee", statMap("physical_damage", 1.25),
            "weapon_ranged", statMap("physical_damage", 1.10),
            "weapon_magic", statMap("crit_bonus", 0.02),
            "armor_heavy", statMap("max_hp", 1.25),
            "armor_light", statMap("attack_speed", 0.01),
            "armor_magic", statMap("healing_power", 0.03),
            "tool", statMap("block_break_speed", 0.02),
            "generic", statMap("max_hp", 1.0)
        ));
        out.put("Rock_Gem_Sapphire", archetypeBonusMap(
            "weapon_melee", statMap("physical_crit_chance", 0.01),
            "weapon_ranged", statMap("physical_crit_chance", 0.012),
            "weapon_magic", statMap("magical_damage", 1.20),
            "armor_heavy", statMap("magical_defence", 0.9),
            "armor_light", statMap("movement_speed", 0.01),
            "armor_magic", statMap("mana_regen", 0.02),
            "tool", statMap("rare_drop_chance", 0.01),
            "generic", statMap("mana_regen", 0.01)
        ));
        out.put("Rock_Gem_Emerald", archetypeBonusMap(
            "weapon_melee", statMap("max_hp", 0.8),
            "weapon_ranged", statMap("movement_speed", 0.01),
            "weapon_magic", statMap("mana_cost_reduction", 0.01),
            "armor_heavy", statMap("physical_defence", 1.0),
            "armor_light", statMap("hp_regen", 0.05),
            "armor_magic", statMap("healing_power", 0.04),
            "tool", statMap("double_drop_chance", 0.01),
            "generic", statMap("max_hp", 1.0)
        ));
        out.put("Rock_Gem_Diamond", archetypeBonusMap(
            "weapon_melee", statMap("crit_bonus", 0.02),
            "weapon_ranged", statMap("physical_penetration", 0.01),
            "weapon_magic", statMap("magical_penetration", 0.01),
            "armor_heavy", statMap("physical_defence", 1.2),
            "armor_light", statMap("magical_defence", 1.0),
            "armor_magic", statMap("magical_defence", 1.2),
            "tool", statMap("block_break_speed", 0.02),
            "generic", statMap("max_hp", 1.0)
        ));
        out.put("Rock_Gem_Topaz", archetypeBonusMap(
            "weapon_melee", statMap("attack_speed", 0.015),
            "weapon_ranged", statMap("attack_speed", 0.015),
            "weapon_magic", statMap("cast_speed", 0.015),
            "armor_heavy", statMap("crit_reduction", 0.01),
            "armor_light", statMap("movement_speed", 0.012),
            "armor_magic", statMap("mana_cost_reduction", 0.01),
            "tool", statMap("block_break_speed", 0.03),
            "generic", statMap("max_hp", 1.0)
        ));
        out.put("Rock_Gem_Voidstone", archetypeBonusMap(
            "weapon_melee", statMap("physical_penetration", 0.012),
            "weapon_ranged", statMap("physical_penetration", 0.012),
            "weapon_magic", statMap("magical_penetration", 0.012),
            "armor_heavy", statMap("reflect_damage", 0.01),
            "armor_light", statMap("crit_reduction", 0.01),
            "armor_magic", statMap("crit_reduction", 0.01),
            "tool", statMap("rare_drop_chance", 0.012),
            "generic", statMap("max_hp", 1.0)
        ));
        out.put("Rock_Gem_Zephyr", archetypeBonusMap(
            "weapon_melee", statMap("movement_speed", 0.01),
            "weapon_ranged", statMap("movement_speed", 0.012),
            "weapon_magic", statMap("cast_speed", 0.012),
            "armor_heavy", statMap("hp_regen", 0.05),
            "armor_light", statMap("movement_speed", 0.015),
            "armor_magic", statMap("mana_regen", 0.02),
            "tool", statMap("double_drop_chance", 0.015),
            "generic", statMap("movement_speed", 0.01)
        ));
        return out;
    }

    private static Map<String, Map<String, Double>> archetypeBonusMap(Object... kv) {
        Map<String, Map<String, Double>> out = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            String key = (String) kv[i];
            @SuppressWarnings("unchecked")
            Map<String, Double> value = (Map<String, Double>) kv[i + 1];
            out.put(key, value);
        }
        return out;
    }

    private static Map<String, Double> defaultRollTypeWeights() {
        Map<String, Double> out = new LinkedHashMap<>();
        out.put("flat", 0.55);
        out.put("percent", 0.45);
        return out;
    }

    private static Map<String, String> defaultRollTypeConstraintByStat() {
        Map<String, String> out = new LinkedHashMap<>();
        out.put("movement_speed", "percent_only");
        out.put("crit_chance", "percent_only");
        out.put("crit_reduction", "percent_only");
        return out;
    }

    private static Map<String, StatDefinitionConfig> defaultStatDefinitions() {
        Map<String, StatDefinitionConfig> out = new LinkedHashMap<>();

        // Crit stats: 1-3%, weight 0.2
        StatDefinitionConfig crit = statDef(0.01, 0.03, 0.20);
        out.put("physical_crit_chance", crit);
        out.put("magical_crit_chance", statDef(0.01, 0.03, 0.20));
        out.put("healing_crit_chance", statDef(0.01, 0.03, 0.20));
        out.put("crit_bonus", statDef(0.01, 0.03, 0.20));
        out.put("healing_crit_bonus", statDef(0.01, 0.03, 0.20));
        out.put("crit_reduction", statDef(0.01, 0.03, 0.20));

        // Resource stats: 5-10%, weight 0.6
        out.put("mana_regen", statDef(0.05, 0.10, 0.60));
        out.put("hp_regen", statDef(0.05, 0.10, 0.60));
        out.put("mana_cost_reduction", statDef(0.05, 0.10, 0.60));
        out.put("max_hp", statDef(0.05, 0.10, 0.60));
        out.put("healing_power", statDef(0.05, 0.10, 0.60));

        // Defense stats: 3-6%, weight 0.4
        out.put("physical_defence", statDef(0.03, 0.06, 0.40));
        out.put("magical_defence", statDef(0.03, 0.06, 0.40));
        out.put("block_efficiency", statDef(0.03, 0.06, 0.40));
        out.put("physical_penetration", statDef(0.03, 0.06, 0.40));
        out.put("magical_penetration", statDef(0.03, 0.06, 0.40));

        // Reflect / utility stats: 0.5-2%, weight 0.15
        out.put("reflect_damage", statDef(0.005, 0.02, 0.15));
        out.put("movement_speed", statDef(0.005, 0.02, 0.15));
        out.put("attack_speed", statDef(0.005, 0.02, 0.15));
        out.put("cast_speed", statDef(0.005, 0.02, 0.15));
        out.put("block_break_speed", statDef(0.005, 0.02, 0.15));
        out.put("rare_drop_chance", statDef(0.005, 0.02, 0.15));
        out.put("double_drop_chance", statDef(0.005, 0.02, 0.15));
        out.put("physical_damage", statDef(0.005, 0.02, 0.15));
        out.put("magical_damage", statDef(0.005, 0.02, 0.15));

        return out;
    }

    private static Map<String, Double> defaultTierScalarByKeyword() {
        Map<String, Double> out = new LinkedHashMap<>();
        // Combat tiers with exponential 30% growth per tier.
        out.put("crude", 1.00);
        out.put("copper", 1.30);
        out.put("bronze", 1.69);
        out.put("iron", 2.197);
        out.put("steel", 2.8561);
        out.put("cobalt", 3.7129);
        out.put("thorium", 4.8268);
        out.put("adamantite", 6.2749);
        out.put("mithril", 8.1573);
        out.put("onyxium", 10.6045);
        out.put("prisma", 13.7858);

        // Vocational / cloth / leather tiers mapped onto the same curve.
        out.put("wool", 1.00);
        out.put("linen", 1.69);
        out.put("leather", 2.197);
        out.put("silk", 3.7129);
        out.put("shadoweave", 8.1573);
        out.put("raven", 13.7858);
        return out;
    }

    private static Map<String, Double> defaultBaseStatWeights() {
        Map<String, Double> weights = new LinkedHashMap<>();
        weights.put("physical_damage", 1.00);
        weights.put("magical_damage", 1.00);
        weights.put("physical_crit_chance", 0.75);
        weights.put("magical_crit_chance", 0.75);
        weights.put("crit_bonus", 0.65);
        weights.put("physical_penetration", 0.55);
        weights.put("magical_penetration", 0.55);
        weights.put("physical_defence", 1.00);
        weights.put("magical_defence", 1.00);
        weights.put("block_efficiency", 0.50);
        weights.put("reflect_damage", 0.35);
        weights.put("crit_reduction", 0.50);
        weights.put("max_hp", 0.80);
        weights.put("hp_regen", 0.45);
        weights.put("healing_power", 0.70);
        weights.put("healing_crit_chance", 0.45);
        weights.put("healing_crit_bonus", 0.45);
        weights.put("mana_cost_reduction", 0.55);
        weights.put("mana_regen", 0.80);
        weights.put("movement_speed", 0.65);
        weights.put("attack_speed", 0.55);
        weights.put("cast_speed", 0.55);
        weights.put("block_break_speed", 0.90);
        weights.put("rare_drop_chance", 0.45);
        weights.put("double_drop_chance", 0.40);
        return weights;
    }

    private static Map<String, List<String>> defaultPoolByPrefix() {
    Map<String, List<String>> out = new LinkedHashMap<>();

    // MELEE FOCUS
    out.put("Flame", List.of("physical_damage", "physical_crit_chance", "crit_bonus", "attack_speed"));
    out.put("Blaze", List.of("attack_speed", "physical_crit_chance", "physical_damage", "crit_bonus"));
    out.put("Desert", List.of("physical_defence", "max_hp", "block_efficiency", "reflect_damage"));
    out.put("Ember", List.of("physical_crit_chance", "attack_speed", "physical_damage", "crit_bonus"));

    // MAGIC FOCUS
    out.put("Wave", List.of("magical_damage", "magical_crit_chance", "cast_speed", "mana_regen"));
    out.put("Ocean", List.of("healing_power", "mana_regen", "mana_cost_reduction", "healing_crit_chance"));
    out.put("Tide", List.of("magical_damage", "magical_penetration", "mana_regen", "max_hp"));

    // RANGED / UTILITY FOCUS
    out.put("Gale", List.of("physical_damage", "physical_penetration", "movement_speed", "attack_speed"));
    out.put("Squall", List.of("physical_crit_chance", "attack_speed", "movement_speed", "crit_bonus"));
    out.put("Zephyr", List.of("movement_speed", "attack_speed", "cast_speed", "mana_regen"));

    // TANK / SURVIVABILITY
    out.put("Stone", List.of("physical_defence", "magical_defence", "max_hp", "crit_reduction"));
    out.put("Crag", List.of("physical_defence", "block_efficiency", "reflect_damage", "hp_regen"));
    out.put("Root", List.of("max_hp", "hp_regen", "magical_defence", "physical_defence"));

    return out;
}

    private static Map<String, List<String>> defaultPoolByArchetype() {
        Map<String, List<String>> pools = new LinkedHashMap<>();
        pools.put("weapon_shield", List.of(
            "block_efficiency",
            "physical_defence",
            "max_hp",
            "reflect_damage"
        ));
        pools.put("weapon_melee", List.of(
            "physical_damage",
            "physical_crit_chance",
            "crit_bonus",
            "physical_penetration",
            "attack_speed",
            "max_hp",
            "movement_speed"
        ));
        pools.put("weapon_ranged", List.of(
            "physical_damage",
            "physical_crit_chance",
            "crit_bonus",
            "physical_penetration",
            "attack_speed",
            "movement_speed",
            "max_hp"
        ));
        pools.put("weapon_magic", List.of(
            "magical_damage",
            "magical_crit_chance",
            "crit_bonus",
            "magical_penetration",
            "cast_speed",
            "mana_regen",
            "mana_cost_reduction",
            "healing_power"
        ));
        pools.put("armor_heavy", List.of(
            "physical_defence",
            "max_hp",
            "crit_reduction",
            "reflect_damage",
            "hp_regen",
            "magical_defence"
        ));
        pools.put("armor_light", List.of(
            "physical_defence",
            "magical_defence",
            "crit_reduction",
            "movement_speed",
            "attack_speed",
            "max_hp",
            "hp_regen"
        ));
        pools.put("armor_magic", List.of(
            "magical_defence",
            "physical_defence",
            "mana_regen",
            "cast_speed",
            "mana_cost_reduction",
            "healing_power",
            "healing_crit_chance",
            "max_hp"
        ));
        pools.put("tool", List.of(
            "movement_speed",
            "block_break_speed",
            "rare_drop_chance",
            "double_drop_chance"
        ));
        pools.put("generic", List.of(
            "physical_damage",
            "magical_damage",
            "physical_defence",
            "magical_defence",
            "max_hp",
            "mana_regen",
            "movement_speed"
        ));
        return pools;
    }

    private static Map<String, Map<String, Double>> defaultBaseStatsByArchetype() {
        Map<String, Map<String, Double>> bases = new LinkedHashMap<>();
        bases.put("weapon_shield", statMap(
            "physical_defence", 12.0,
            "block_efficiency", 0.07,
            "max_hp", 20.0
        ));
        bases.put("weapon_melee", statMap(
            "physical_damage", 15.0,
            "physical_crit_chance", 0.02,
            "crit_bonus", 0.12,
            "physical_penetration", 0.03,
            "attack_speed", 0.02
        ));
        bases.put("weapon_ranged", statMap(
            "physical_damage", 13.0,
            "physical_crit_chance", 0.025,
            "crit_bonus", 0.11,
            "physical_penetration", 0.025,
            "attack_speed", 0.025,
            "movement_speed", 0.008
        ));
        bases.put("weapon_magic", statMap(
            "magical_damage", 13.0,
            "magical_crit_chance", 0.025,
            "crit_bonus", 0.11,
            "magical_penetration", 0.03,
            "cast_speed", 0.025,
            "mana_regen", 0.05
        ));
        bases.put("armor_heavy", statMap(
            "physical_defence", 14.0,
            "magical_defence", 6.0,
            "max_hp", 22.0,
            "crit_reduction", 0.03
        ));
        bases.put("armor_light", statMap(
            "physical_defence", 10.0,
            "magical_defence", 10.0,
            "max_hp", 16.0,
            "movement_speed", 0.012,
            "attack_speed", 0.015,
            "crit_reduction", 0.02
        ));
        bases.put("armor_magic", statMap(
            "physical_defence", 6.0,
            "magical_defence", 14.0,
            "max_hp", 14.0,
            "mana_regen", 0.06,
            "cast_speed", 0.02,
            "healing_power", 0.08
        ));
        bases.put("tool", statMap(
            "movement_speed", 0.005,
            "block_break_speed", 0.03
        ));
        bases.put("generic", statMap(
            "physical_damage", 4.0,
            "physical_defence", 4.0,
            "max_hp", 6.0
        ));
        return bases;
    }

    private static Map<String, Double> statMap(Object... kv) {
        Map<String, Double> out = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            String key = (String) kv[i];
            Double value = (Double) kv[i + 1];
            out.put(key, value);
        }
        return out;
    }

    private static StatDefinitionConfig statDef(double baseMin, double baseMax, double scalingWeight) {
        return new StatDefinitionConfig(baseMin, baseMax, scalingWeight);
    }
}
