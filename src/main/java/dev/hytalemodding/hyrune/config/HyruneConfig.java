package dev.hytalemodding.hyrune.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runtime config for Hyrune gameplay switches and requirement maps.
 */
public class HyruneConfig {
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
}
