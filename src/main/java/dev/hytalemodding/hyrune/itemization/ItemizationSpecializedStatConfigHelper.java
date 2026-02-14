package dev.hytalemodding.hyrune.itemization;

import dev.hytalemodding.hyrune.config.HyruneConfig;
import dev.hytalemodding.hyrune.config.HyruneConfigManager;
import dev.hytalemodding.hyrune.repair.ItemRarity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Central helper for specialized stat configuration lookups.
 */
public final class ItemizationSpecializedStatConfigHelper {
    public enum RollType {
        FLAT,
        PERCENT,
        HYBRID
    }

    private ItemizationSpecializedStatConfigHelper() {
    }

    public static double flatRollMinScalar() {
        return config().flatRollMinScalar;
    }

    public static double flatRollMaxScalar() {
        return config().flatRollMaxScalar;
    }

    public static double percentRollMin() {
        return config().percentRollMin;
    }

    public static double percentRollMax() {
        return config().percentRollMax;
    }

    public static double hybridFlatScalar() {
        return config().hybridFlatScalar;
    }

    public static double hybridPercentScalar() {
        return config().hybridPercentScalar;
    }

    public static double percentRollTierInfluence() {
        return config().percentRollTierInfluence;
    }

    public static int statsForRarity(ItemRarity rarity) {
        HyruneConfig.ItemizationSpecializedStatsConfig cfg = config();
        Map<String, Integer> map = cfg.statsPerRarity;
        if (map == null || map.isEmpty()) {
            return 1;
        }
        String key = rarity == null ? ItemRarity.COMMON.name() : rarity.name();
        Integer explicit = map.get(key);
        if (explicit != null) {
            return Math.max(1, explicit);
        }
        Integer fallback = map.get(ItemRarity.COMMON.name());
        return Math.max(1, fallback == null ? 1 : fallback);
    }

    public static double rarityScalar(ItemRarity rarity) {
        HyruneConfig.RarityScalarConfig values = config().rarityScalar == null
            ? new HyruneConfig.RarityScalarConfig()
            : config().rarityScalar;
        if (rarity == null) {
            return values.common;
        }
        return switch (rarity) {
            case UNCOMMON -> values.uncommon;
            case RARE -> values.rare;
            case EPIC -> values.epic;
            case VOCATIONAL -> values.vocational;
            case LEGENDARY -> values.legendary;
            case MYTHIC -> values.mythic;
            case COMMON -> values.common;
        };
    }

    public static List<ItemizedStat> poolForArchetype(ItemArchetype archetype) {
        HyruneConfig.ItemizationSpecializedStatsConfig cfg = config();
        Map<String, List<String>> pools = cfg.poolByArchetype;
        if (pools == null || pools.isEmpty()) {
            return List.of();
        }
        String key = archetype == null ? ItemArchetype.GENERIC.getId() : archetype.getId();
        List<String> raw = pools.get(key);
        if (raw == null || raw.isEmpty()) {
            raw = pools.get(ItemArchetype.GENERIC.getId());
        }
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<ItemizedStat> out = new ArrayList<>();
        for (String id : raw) {
            ItemizedStat stat = ItemizedStat.fromId(id);
            if (stat != null && !out.contains(stat)) {
                out.add(stat);
            }
        }
        return out;
    }

    public static Map<ItemizedStat, Double> baseStatsForArchetype(ItemArchetype archetype) {
        HyruneConfig.ItemizationSpecializedStatsConfig cfg = config();
        Map<String, Map<String, Double>> all = cfg.baseStatsByArchetype;
        if (all == null || all.isEmpty()) {
            return Map.of();
        }
        String key = archetype == null ? ItemArchetype.GENERIC.getId() : archetype.getId();
        Map<String, Double> raw = all.get(key);
        if (raw == null || raw.isEmpty()) {
            raw = all.get(ItemArchetype.GENERIC.getId());
        }
        if (raw == null || raw.isEmpty()) {
            return Map.of();
        }
        Map<ItemizedStat, Double> out = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : raw.entrySet()) {
            ItemizedStat stat = ItemizedStat.fromId(entry.getKey());
            if (stat == null || entry.getValue() == null) {
                continue;
            }
            out.put(stat, Math.max(0.0, entry.getValue()));
        }
        return out;
    }

    public static double statWeight(ItemizedStat stat) {
        if (stat == null) {
            return 1.0;
        }
        HyruneConfig.ItemizationSpecializedStatsConfig cfg = config();
        if (cfg.baseStatWeights == null || cfg.baseStatWeights.isEmpty()) {
            return 1.0;
        }
        Double raw = cfg.baseStatWeights.get(stat.getId());
        return raw == null ? 1.0 : Math.max(0.0, raw);
    }

    public static double catalystFamilyBias(CatalystAffinity catalyst, ItemizedStatFamily family) {
        if (family == null) {
            return 1.0;
        }
        HyruneConfig.ItemizationSpecializedStatsConfig cfg = config();
        if (cfg.catalystFamilyWeightBias == null || cfg.catalystFamilyWeightBias.isEmpty()) {
            return 1.0;
        }
        String catalystKey = catalyst == null ? CatalystAffinity.NONE.name() : catalyst.name();
        Map<String, Double> familyMap = cfg.catalystFamilyWeightBias.get(catalystKey);
        if ((familyMap == null || familyMap.isEmpty()) && catalyst != CatalystAffinity.NONE) {
            familyMap = cfg.catalystFamilyWeightBias.get(CatalystAffinity.NONE.name());
        }
        if (familyMap == null || familyMap.isEmpty()) {
            return 1.0;
        }
        Double bias = familyMap.get(family.getId());
        return bias == null ? 1.0 : Math.max(0.0, bias);
    }

    public static double rollTypeWeight(RollType type) {
        HyruneConfig.ItemizationSpecializedStatsConfig cfg = config();
        if (cfg.rollTypeWeights == null || cfg.rollTypeWeights.isEmpty() || type == null) {
            return 1.0;
        }
        String key = switch (type) {
            case FLAT -> "flat";
            case PERCENT -> "percent";
            case HYBRID -> "hybrid";
        };
        Double raw = cfg.rollTypeWeights.get(key);
        return raw == null ? 1.0 : Math.max(0.0, raw);
    }

    public static double baseValueForArchetypeStat(ItemArchetype archetype, ItemizedStat stat) {
        if (stat == null) {
            return 0.0;
        }
        Map<ItemizedStat, Double> configured = baseStatsForArchetype(archetype);
        double base = configured.getOrDefault(stat, 0.0);
        if (base > 0.0) {
            return base;
        }
        return Math.max(0.0001, stat.getFlatReference());
    }

    public static double tierScalar(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return 1.0;
        }
        HyruneConfig.ItemizationSpecializedStatsConfig cfg = config();
        Map<String, Double> map = cfg.tierScalarByKeyword;
        if (map == null || map.isEmpty()) {
            return 1.0;
        }
        String normalized = itemId.toLowerCase(Locale.ROOT);
        double best = 1.0;
        for (Map.Entry<String, Double> entry : map.entrySet()) {
            String key = entry.getKey();
            Double raw = entry.getValue();
            if (key == null || key.isBlank() || raw == null || raw <= 0.0 || Double.isNaN(raw) || Double.isInfinite(raw)) {
                continue;
            }
            if (normalized.contains(key.toLowerCase(Locale.ROOT))) {
                best = Math.max(best, raw);
            }
        }
        return clamp(best, 0.25, 5.0);
    }

    private static double clamp(double value, double min, double max) {
        if (Double.isNaN(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    private static HyruneConfig.ItemizationSpecializedStatsConfig config() {
        HyruneConfig cfg = HyruneConfigManager.getConfig();
        if (cfg == null || cfg.itemizationSpecializedStats == null) {
            return new HyruneConfig.ItemizationSpecializedStatsConfig();
        }
        return cfg.itemizationSpecializedStats;
    }

}
