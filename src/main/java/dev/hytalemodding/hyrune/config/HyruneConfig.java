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
        public double flatRollMinScalar = 0.10;
        public double flatRollMaxScalar = 0.45;
        public double percentRollMin = 0.03;
        public double percentRollMax = 0.16;
        public double percentRollTierInfluence = 0.55;
        public double hybridFlatScalar = 0.80;
        public double hybridPercentScalar = 0.80;
        public Map<String, Double> rollTypeWeights = defaultRollTypeWeights();
        public Map<String, Double> tierScalarByKeyword = defaultTierScalarByKeyword();
        public RarityScalarConfig rarityScalar = new RarityScalarConfig();
        public Map<String, Integer> statsPerRarity = defaultStatsPerRarity();
        public Map<String, Double> baseStatWeights = defaultBaseStatWeights();
        public Map<String, List<String>> poolByArchetype = defaultPoolByArchetype();
        public Map<String, Map<String, Double>> catalystFamilyWeightBias = defaultCatalystFamilyWeightBias();
        public Map<String, Map<String, Double>> baseStatsByArchetype = defaultBaseStatsByArchetype();
    }

    public static class CatalystConfig {
        public boolean allowOverwrite = true;
        public boolean enableReimbueCost = true;
        public int reimbueCurrencyCost = 250;
        public String reimbueCurrencyName = "Gold";
        public boolean enforceCurrencyBalance = false;
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
    public CatalystConfig catalyst = new CatalystConfig();

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

    private static Map<String, Double> defaultRollTypeWeights() {
        Map<String, Double> out = new LinkedHashMap<>();
        out.put("flat", 0.45);
        out.put("percent", 0.35);
        out.put("hybrid", 0.20);
        return out;
    }

    private static Map<String, Double> defaultTierScalarByKeyword() {
        Map<String, Double> out = new LinkedHashMap<>();
        out.put("crude", 0.70);
        out.put("copper", 0.85);
        out.put("bronze", 0.95);
        out.put("iron", 1.00);
        out.put("steel", 1.15);
        out.put("wool", 0.82);
        out.put("linen", 0.95);
        out.put("leather", 1.00);
        out.put("silver", 1.25);
        out.put("silk", 1.20);
        out.put("raven", 1.35);
        out.put("gold", 1.35);
        out.put("shadoweave", 1.60);
        out.put("cobalt", 1.55);
        out.put("thorium", 1.75);
        out.put("adamantite", 2.00);
        out.put("mithril", 2.25);
        out.put("onyxium", 2.50);
        out.put("prisma", 2.85);
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
        weights.put("stamina_regen", 0.80);
        weights.put("movement_speed", 0.65);
        weights.put("attack_speed", 0.55);
        weights.put("cast_speed", 0.55);
        return weights;
    }

    private static Map<String, List<String>> defaultPoolByArchetype() {
        Map<String, List<String>> pools = new LinkedHashMap<>();
        pools.put("weapon_melee", List.of(
            "physical_damage",
            "physical_crit_chance",
            "crit_bonus",
            "physical_penetration",
            "attack_speed",
            "stamina_regen",
            "max_hp",
            "crit_reduction"
        ));
        pools.put("weapon_ranged", List.of(
            "physical_damage",
            "physical_crit_chance",
            "crit_bonus",
            "physical_penetration",
            "attack_speed",
            "stamina_regen",
            "movement_speed",
            "crit_reduction"
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
            "magical_defence",
            "max_hp",
            "block_efficiency",
            "crit_reduction",
            "reflect_damage",
            "hp_regen",
            "stamina_regen"
        ));
        pools.put("armor_light", List.of(
            "physical_defence",
            "magical_defence",
            "crit_reduction",
            "movement_speed",
            "attack_speed",
            "stamina_regen",
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
            "stamina_regen",
            "movement_speed",
            "attack_speed",
            "mana_regen"
        ));
        pools.put("generic", List.of(
            "physical_damage",
            "magical_damage",
            "physical_defence",
            "magical_defence",
            "max_hp",
            "mana_regen",
            "stamina_regen",
            "movement_speed"
        ));
        return pools;
    }

    private static Map<String, Map<String, Double>> defaultCatalystFamilyWeightBias() {
        Map<String, Map<String, Double>> out = new LinkedHashMap<>();

        Map<String, Double> none = new LinkedHashMap<>();
        none.put("offense_physical", 1.0);
        none.put("offense_magical", 1.0);
        none.put("defense_physical", 1.0);
        none.put("defense_magical", 1.0);
        none.put("defense_core", 1.0);
        none.put("healing", 1.0);
        none.put("utility", 1.0);
        out.put("NONE", none);

        Map<String, Double> fire = new LinkedHashMap<>(none);
        fire.put("offense_physical", 1.25);
        fire.put("offense_magical", 1.10);
        fire.put("defense_physical", 0.90);
        fire.put("healing", 0.90);
        out.put("FIRE", fire);

        Map<String, Double> water = new LinkedHashMap<>(none);
        water.put("healing", 1.30);
        water.put("utility", 1.15);
        water.put("offense_physical", 0.90);
        out.put("WATER", water);

        Map<String, Double> air = new LinkedHashMap<>(none);
        air.put("utility", 1.30);
        air.put("offense_magical", 1.10);
        air.put("defense_core", 0.92);
        out.put("AIR", air);

        Map<String, Double> earth = new LinkedHashMap<>(none);
        earth.put("defense_physical", 1.25);
        earth.put("defense_magical", 1.10);
        earth.put("offense_physical", 0.95);
        out.put("EARTH", earth);

        return out;
    }

    private static Map<String, Map<String, Double>> defaultBaseStatsByArchetype() {
        Map<String, Map<String, Double>> bases = new LinkedHashMap<>();
        bases.put("weapon_melee", statMap(
            "physical_damage", 8.0,
            "physical_crit_chance", 0.03,
            "crit_bonus", 0.18,
            "physical_penetration", 0.04,
            "attack_speed", 0.03
        ));
        bases.put("weapon_ranged", statMap(
            "physical_damage", 7.0,
            "physical_crit_chance", 0.04,
            "crit_bonus", 0.16,
            "physical_penetration", 0.03,
            "attack_speed", 0.04,
            "movement_speed", 0.01
        ));
        bases.put("weapon_magic", statMap(
            "magical_damage", 7.0,
            "magical_crit_chance", 0.04,
            "crit_bonus", 0.16,
            "magical_penetration", 0.04,
            "cast_speed", 0.04,
            "mana_regen", 0.03
        ));
        bases.put("armor_heavy", statMap(
            "physical_defence", 7.0,
            "magical_defence", 4.0,
            "max_hp", 10.0,
            "crit_reduction", 0.03,
            "block_efficiency", 0.04
        ));
        bases.put("armor_light", statMap(
            "physical_defence", 5.0,
            "magical_defence", 5.0,
            "max_hp", 7.0,
            "movement_speed", 0.02,
            "attack_speed", 0.02
        ));
        bases.put("armor_magic", statMap(
            "physical_defence", 4.0,
            "magical_defence", 7.0,
            "max_hp", 6.0,
            "mana_regen", 0.04,
            "cast_speed", 0.03,
            "healing_power", 0.05
        ));
        bases.put("tool", statMap(
            "stamina_regen", 0.05,
            "movement_speed", 0.01
        ));
        bases.put("generic", statMap(
            "physical_damage", 2.0,
            "physical_defence", 2.0,
            "max_hp", 2.0
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
}
