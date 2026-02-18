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
 * Config helper for socket capacity and gem stat stubs.
 */
public final class GemSocketConfigHelper {
    private GemSocketConfigHelper() {
    }

    public static int socketsForRarity(ItemRarity rarity) {
        HyruneConfig.GemSocketConfig cfg = config();
        if (!cfg.enabled) {
            return 0;
        }
        Map<String, Integer> map = cfg.socketsPerRarity;
        if (map == null || map.isEmpty()) {
            return 0;
        }
        String key = rarity == null ? ItemRarity.COMMON.name() : rarity.name();
        Integer value = map.get(key);
        if (value != null) {
            return Math.max(0, value);
        }
        Integer fallback = map.get(ItemRarity.COMMON.name());
        return Math.max(0, fallback == null ? 0 : fallback);
    }

    public static double maxHpPerSocketedGem() {
        HyruneConfig.GemSocketConfig cfg = config();
        if (!cfg.enabled) {
            return 0.0;
        }
        return clamp(cfg.maxHpPerSocketedGem, 0.0, 1000.0);
    }

    public static boolean isGemItemId(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return false;
        }
        String normalized = itemId.trim().toLowerCase(Locale.ROOT);
        List<String> configured = config().gemItemIds;
        if (configured != null) {
            for (String candidate : configured) {
                if (matchesGemPattern(normalized, candidate)) {
                    return true;
                }
            }
        }
        return normalized.startsWith("rock_gem_");
    }

    public static ItemizedStatBlock socketBonusesForItem(String itemId, List<String> socketedGemItemIds) {
        ItemizedStatBlock out = ItemizedStatBlock.empty();
        if (socketedGemItemIds == null || socketedGemItemIds.isEmpty()) {
            return out;
        }
        ItemArchetype archetype = ItemArchetypeResolver.resolve(itemId);
        for (String gemItemId : socketedGemItemIds) {
            if (gemItemId == null || gemItemId.isBlank()) {
                continue;
            }
            Map<String, Double> perStat = resolveGemArchetypeBonuses(gemItemId, archetype);
            if (perStat.isEmpty()) {
                out.add(ItemizedStat.MAX_HP, maxHpPerSocketedGem());
                continue;
            }
            for (Map.Entry<String, Double> entry : perStat.entrySet()) {
                ItemizedStat stat = ItemizedStat.fromId(entry.getKey());
                Double value = entry.getValue();
                if (stat == null || value == null || Double.isNaN(value) || Double.isInfinite(value)) {
                    continue;
                }
                out.add(stat, value);
            }
        }
        return out;
    }

    public static String describeGemBonusForItem(String gemItemId, String itemId) {
        ItemizedStatBlock block = socketBonusesForItem(itemId, List.of(gemItemId));
        for (ItemizedStat stat : ItemizedStat.values()) {
            double value = block.get(stat);
            if (Math.abs(value) <= 1e-9) {
                continue;
            }
            return ItemStatDisplayFormatter.formatFlat(stat, value) + " " + stat.getDisplayName();
        }
        return ItemStatDisplayFormatter.formatFlat(ItemizedStat.MAX_HP, maxHpPerSocketedGem()) + " Max HP";
    }

    public static List<String> describeSocketedGemLinesForItem(String itemId, List<String> socketedGemItemIds) {
        List<String> out = new ArrayList<>();
        if (socketedGemItemIds == null || socketedGemItemIds.isEmpty()) {
            return out;
        }
        for (String gemItemId : socketedGemItemIds) {
            if (gemItemId == null || gemItemId.isBlank()) {
                continue;
            }
            out.add(displayGemName(gemItemId) + ": " + describeGemBonusForItem(gemItemId, itemId));
        }
        return out;
    }

    public static String displayGemName(String gemItemId) {
        if (gemItemId == null || gemItemId.isBlank()) {
            return "Gem";
        }
        String token = gemItemId.trim();
        String normalized = token.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("rock_gem_") && token.length() > "Rock_Gem_".length()) {
            token = token.substring("Rock_Gem_".length());
        } else {
            int lastUnderscore = token.lastIndexOf('_');
            if (lastUnderscore >= 0 && lastUnderscore + 1 < token.length()) {
                token = token.substring(lastUnderscore + 1);
            }
        }
        return titleCase(token.replace('_', ' '));
    }

    private static Map<String, Double> resolveGemArchetypeBonuses(String gemItemId, ItemArchetype archetype) {
        Map<String, Map<String, Map<String, Double>>> configured = config().bonusesByGemPatternAndArchetype;
        if (configured == null || configured.isEmpty()) {
            return Map.of();
        }
        String normalizedGemItemId = gemItemId.trim().toLowerCase(Locale.ROOT);

        String bestPattern = null;
        int bestScore = -1;
        for (String pattern : configured.keySet()) {
            int score = patternMatchScore(normalizedGemItemId, pattern);
            if (score > bestScore) {
                bestScore = score;
                bestPattern = pattern;
            }
        }
        if (bestPattern == null || bestScore < 0) {
            return Map.of();
        }

        Map<String, Map<String, Double>> byArchetype = configured.get(bestPattern);
        if (byArchetype == null || byArchetype.isEmpty()) {
            return Map.of();
        }
        String archetypeKey = archetype == null ? ItemArchetype.GENERIC.getId() : archetype.getId();
        Map<String, Double> exact = byArchetype.get(archetypeKey);
        if (exact != null && !exact.isEmpty()) {
            return sanitizeStatMap(exact);
        }
        Map<String, Double> fallback = byArchetype.get(ItemArchetype.GENERIC.getId());
        if (fallback != null && !fallback.isEmpty()) {
            return sanitizeStatMap(fallback);
        }
        return Map.of();
    }

    private static Map<String, Double> sanitizeStatMap(Map<String, Double> raw) {
        if (raw == null || raw.isEmpty()) {
            return Map.of();
        }
        Map<String, Double> out = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : raw.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null) {
                continue;
            }
            double value = entry.getValue();
            if (Double.isNaN(value) || Double.isInfinite(value) || Math.abs(value) <= 1e-9) {
                continue;
            }
            out.put(entry.getKey().trim().toLowerCase(Locale.ROOT), value);
        }
        return out;
    }

    private static int patternMatchScore(String normalizedItemId, String configuredPattern) {
        if (configuredPattern == null || configuredPattern.isBlank()) {
            return -1;
        }
        String normalizedPattern = configuredPattern.trim().toLowerCase(Locale.ROOT);
        int wildcard = normalizedPattern.indexOf('*');
        if (wildcard < 0) {
            return normalizedItemId.equals(normalizedPattern) ? 10_000 + normalizedPattern.length() : -1;
        }
        String prefix = normalizedPattern.substring(0, wildcard);
        String suffix = normalizedPattern.substring(wildcard + 1);
        if (!normalizedItemId.startsWith(prefix) || !normalizedItemId.endsWith(suffix)) {
            return -1;
        }
        return prefix.length() + suffix.length();
    }

    private static boolean matchesGemPattern(String normalizedItemId, String configuredPattern) {
        if (configuredPattern == null || configuredPattern.isBlank()) {
            return false;
        }
        return patternMatchScore(normalizedItemId, configuredPattern) >= 0;
    }

    private static double clamp(double value, double min, double max) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    private static String titleCase(String text) {
        if (text == null || text.isBlank()) {
            return "Gem";
        }
        String[] parts = text.trim().split("\\s+");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            String lower = part.toLowerCase(Locale.ROOT);
            out.append(Character.toUpperCase(lower.charAt(0)));
            if (lower.length() > 1) {
                out.append(lower.substring(1));
            }
        }
        return out.length() == 0 ? "Gem" : out.toString();
    }

    private static HyruneConfig.GemSocketConfig config() {
        try {
            HyruneConfig cfg = HyruneConfigManager.getConfig();
            if (cfg == null || cfg.gemSockets == null) {
                return new HyruneConfig.GemSocketConfig();
            }
            return cfg.gemSockets;
        } catch (Throwable ignored) {
            return new HyruneConfig.GemSocketConfig();
        }
    }
}
