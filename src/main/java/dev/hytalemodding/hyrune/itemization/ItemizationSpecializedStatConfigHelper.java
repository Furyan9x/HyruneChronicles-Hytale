package dev.hytalemodding.hyrune.itemization;

import dev.hytalemodding.hyrune.config.HyruneConfig;
import dev.hytalemodding.hyrune.config.HyruneConfigManager;
import dev.hytalemodding.hyrune.repair.ItemRarity;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Central helper for specialized stat configuration lookups.
 */
public final class ItemizationSpecializedStatConfigHelper {
    private static final EnumSet<ItemizedStat> FLAT_MIN_ONE_FLOOR_STATS = EnumSet.of(
        ItemizedStat.PHYSICAL_DAMAGE,
        ItemizedStat.PHYSICAL_PENETRATION,
        ItemizedStat.MAGICAL_PENETRATION,
        ItemizedStat.PHYSICAL_DEFENCE,
        ItemizedStat.BLOCK_EFFICIENCY,
        ItemizedStat.REFLECT_DAMAGE,
        ItemizedStat.HP_REGEN,
        ItemizedStat.HEALING_POWER,
        ItemizedStat.HEALING_CRIT_BONUS,
        ItemizedStat.MANA_COST_REDUCTION,
        ItemizedStat.MANA_REGEN
    );

    public enum RollType {
        FLAT,
        PERCENT
    }

    public enum RollConstraint {
        FLAT_ONLY,
        PERCENT_ONLY,
        EITHER
    }

    public record PercentRollDefinition(double baseMin, double baseMax, double scalingWeight) {
    }

    private ItemizationSpecializedStatConfigHelper() {
    }

    public static double flatRollMinScalar() {
        return config().flatRollMinScalar;
    }

    public static double flatRollMaxScalar() {
        return config().flatRollMaxScalar;
    }

    public static double rollStatRarityMultiplier(ItemRarity rarity) {
        if (rarity == null) {
            return 1.0;
        }
        HyruneConfig.ItemizationSpecializedStatsConfig cfg = config();
        return switch (rarity) {
            case LEGENDARY -> clamp(cfg.legendaryRollStatMultiplier, 0.0, 10.0);
            case MYTHIC -> clamp(cfg.mythicRollStatMultiplier, 0.0, 10.0);
            default -> 1.0;
        };
    }

    public static double flatRollMinimumFloor(ItemizedStat stat) {
        if (stat == null) {
            return 0.0;
        }
        return FLAT_MIN_ONE_FLOOR_STATS.contains(stat) ? 1.0 : 0.0;
    }

    public static double scaledFlatRollMinimumFloor(ItemizedStat stat, double tierScalar) {
        double baseFloor = flatRollMinimumFloor(stat);
        if (baseFloor <= 0.0) {
            return 0.0;
        }
        double effectiveTier = Math.max(1.0, tierScalar);
        double scaled = baseFloor * (1.0 + ((effectiveTier - 1.0) * 0.35));
        return Math.max(baseFloor, scaled);
    }

    public static double durabilityTierInfluence() {
        return clamp(config().durabilityTierInfluence, 0.0, 1.0);
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
            if (stat != null && isStatAllowedForArchetype(archetype, stat) && !out.contains(stat)) {
                out.add(stat);
            }
        }
        return out;
    }

    public static List<ItemizedStat> poolForPrefix(String prefixWord) {
        if (prefixWord == null || prefixWord.isBlank()) {
            return List.of();
        }
        HyruneConfig.ItemizationSpecializedStatsConfig cfg = config();
        Map<String, List<String>> pools = cfg.poolByPrefix;
        if (pools == null || pools.isEmpty()) {
            return List.of();
        }

        List<String> raw = null;
        String normalizedPrefix = prefixWord.trim();
        for (Map.Entry<String, List<String>> entry : pools.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            if (entry.getKey().trim().equalsIgnoreCase(normalizedPrefix)) {
                raw = entry.getValue();
                break;
            }
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

    public static List<ItemizedStat> intersectPools(ItemArchetype archetype, String prefixWord) {
        List<ItemizedStat> archetypePool = poolForArchetype(archetype);
        if (archetypePool.isEmpty()) {
            return List.of();
        }
        List<ItemizedStat> prefixPool = poolForPrefix(prefixWord);
        if (prefixPool.isEmpty()) {
            return new ArrayList<>(archetypePool);
        }
        Set<ItemizedStat> prefixSet = new HashSet<>(prefixPool);
        List<ItemizedStat> out = new ArrayList<>();
        for (ItemizedStat stat : archetypePool) {
            if (prefixSet.contains(stat)) {
                out.add(stat);
            }
        }
        return out;
    }

    public static double prefixPriorityWeight(String prefixWord, ItemizedStat stat) {
        if (prefixWord == null || prefixWord.isBlank() || stat == null) {
            return 1.0;
        }
        List<ItemizedStat> prefixPool = poolForPrefix(prefixWord);
        if (prefixPool.isEmpty()) {
            return 1.0;
        }
        int rank = indexOf(prefixPool, stat) + 1;
        if (rank <= 0) {
            return 1.0;
        }
        HyruneConfig.ItemizationSpecializedStatsConfig cfg = config();
        Map<Integer, Double> rankWeights = cfg.prefixPriorityWeightByRank;
        if (rankWeights == null || rankWeights.isEmpty()) {
            return 1.0;
        }
        Double direct = rankWeights.get(rank);
        if (direct != null) {
            return Math.max(0.0, direct);
        }
        int maxKnownRank = -1;
        double maxKnownWeight = 1.0;
        for (Map.Entry<Integer, Double> entry : rankWeights.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            int key = entry.getKey();
            if (key <= 0) {
                continue;
            }
            if (key > maxKnownRank) {
                maxKnownRank = key;
                maxKnownWeight = Math.max(0.0, entry.getValue());
            }
        }
        return maxKnownRank > 0 ? maxKnownWeight : 1.0;
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
            if (!isStatAllowedForArchetype(archetype, stat)) {
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

    public static double rollTypeWeight(RollType type) {
        HyruneConfig.ItemizationSpecializedStatsConfig cfg = config();
        if (cfg.rollTypeWeights == null || cfg.rollTypeWeights.isEmpty() || type == null) {
            return 1.0;
        }
        String key = switch (type) {
            case FLAT -> "flat";
            case PERCENT -> "percent";
        };
        Double raw = cfg.rollTypeWeights.get(key);
        return raw == null ? 1.0 : Math.max(0.0, raw);
    }

    public static RollConstraint rollConstraintForStat(ItemizedStat stat) {
        if (stat == null) {
            return RollConstraint.EITHER;
        }
        String raw = configuredRollConstraintValue(stat);
        if (raw == null) {
            return RollConstraint.EITHER;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "flat_only", "flat", "flat-only" -> RollConstraint.FLAT_ONLY;
            case "percent_only", "percent", "percent-only", "pct_only", "pct" -> RollConstraint.PERCENT_ONLY;
            default -> RollConstraint.EITHER;
        };
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

    public static PercentRollDefinition percentRollDefinition(ItemizedStat stat) {
        HyruneConfig.ItemizationSpecializedStatsConfig cfg = config();
        double fallbackMin = clamp(cfg.percentRollMin, 0.0001, 10.0);
        double fallbackMax = clamp(cfg.percentRollMax, 0.0001, 10.0);
        double fallbackWeight = clamp(cfg.percentRollTierInfluence, 0.0, 5.0);

        if (stat == null || cfg.statDefinitions == null || cfg.statDefinitions.isEmpty()) {
            return sanitizePercentDefinition(fallbackMin, fallbackMax, fallbackWeight);
        }

        HyruneConfig.StatDefinitionConfig configured = null;
        for (Map.Entry<String, HyruneConfig.StatDefinitionConfig> entry : cfg.statDefinitions.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            if (entry.getKey().trim().equalsIgnoreCase(stat.getId())) {
                configured = entry.getValue();
                break;
            }
        }
        if (configured == null) {
            return sanitizePercentDefinition(fallbackMin, fallbackMax, fallbackWeight);
        }
        return sanitizePercentDefinition(configured.baseMin, configured.baseMax, configured.scalingWeight);
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
        if (Double.isNaN(best) || Double.isInfinite(best)) {
            return 1.0;
        }
        return Math.max(0.25, best);
    }

    public static int uiDisplayFlatDecimals() {
        return clampInt(config().uiDisplayFlatDecimals, 0, 3);
    }

    public static int uiDisplayPercentDecimals() {
        return clampInt(config().uiDisplayPercentDecimals, 0, 3);
    }

    private static double clamp(double value, double min, double max) {
        if (Double.isNaN(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static PercentRollDefinition sanitizePercentDefinition(double baseMin, double baseMax, double scalingWeight) {
        double min = clamp(baseMin, 0.0001, 10.0);
        double max = clamp(baseMax, 0.0001, 10.0);
        if (max < min) {
            double t = min;
            min = max;
            max = t;
        }
        double weight = clamp(scalingWeight, 0.0, 5.0);
        return new PercentRollDefinition(min, max, weight);
    }

    private static HyruneConfig.ItemizationSpecializedStatsConfig config() {
        HyruneConfig cfg = HyruneConfigManager.getConfig();
        if (cfg == null || cfg.itemizationSpecializedStats == null) {
            return new HyruneConfig.ItemizationSpecializedStatsConfig();
        }
        return cfg.itemizationSpecializedStats;
    }

    private static boolean isStatAllowedForArchetype(ItemArchetype archetype, ItemizedStat stat) {
        if (stat == null) {
            return false;
        }
        ItemArchetype resolvedArchetype = archetype == null ? ItemArchetype.GENERIC : archetype;
        if (stat == ItemizedStat.CRIT_REDUCTION) {
            return isArmorArchetype(resolvedArchetype);
        }
        if (stat == ItemizedStat.BLOCK_EFFICIENCY) {
            return resolvedArchetype == ItemArchetype.WEAPON_SHIELD;
        }
        if (stat == ItemizedStat.BLOCK_BREAK_SPEED
            || stat == ItemizedStat.RARE_DROP_CHANCE
            || stat == ItemizedStat.DOUBLE_DROP_CHANCE) {
            return resolvedArchetype == ItemArchetype.TOOL;
        }
        return true;
    }

    private static boolean isArmorArchetype(ItemArchetype archetype) {
        return archetype == ItemArchetype.ARMOR_HEAVY
            || archetype == ItemArchetype.ARMOR_LIGHT
            || archetype == ItemArchetype.ARMOR_MAGIC;
    }

    private static String configuredRollConstraintValue(ItemizedStat stat) {
        HyruneConfig.ItemizationSpecializedStatsConfig cfg = config();
        Map<String, String> map = cfg.rollTypeConstraintByStat;
        if (map == null || map.isEmpty()) {
            return null;
        }

        String exact = findConstraintValue(map, stat.getId());
        if (exact != null) {
            return exact;
        }

        // Convenience alias for all crit-chance stats.
        if (stat.getId().endsWith("_crit_chance")) {
            String critAlias = findConstraintValue(map, "crit_chance");
            if (critAlias != null) {
                return critAlias;
            }
        }
        return null;
    }

    private static int indexOf(List<ItemizedStat> values, ItemizedStat target) {
        if (values == null || values.isEmpty() || target == null) {
            return -1;
        }
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i) == target) {
                return i;
            }
        }
        return -1;
    }

    private static String findConstraintValue(Map<String, String> map, String key) {
        if (map == null || map.isEmpty() || key == null || key.isBlank()) {
            return null;
        }
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            if (entry.getKey().trim().equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

}
