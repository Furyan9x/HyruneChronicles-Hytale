package dev.hytalemodding.hyrune.itemization;

import dev.hytalemodding.hyrune.config.HyruneConfig;
import dev.hytalemodding.hyrune.config.HyruneConfigManager;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Prefix utility for crafted-item prefix rolls and display naming.
 */
public final class ItemPrefixService {
    private static final Set<String> TIER_TOKENS = new HashSet<>(Set.of(
        "crude", "wood", "copper", "bronze", "iron", "steel", "silversteel",
        "silver", "gold", "cobalt", "thorium", "adamantite", "mithril",
        "onyxium", "prisma", "wool", "linen", "silk", "cotton", "leather",
        "raven", "cindercloth", "shadoweave", "doomed", "ancient", "runic",
        "frost", "bone", "tribal", "void"
    ));

    private ItemPrefixService() {
    }

    public static String rollRandomPrefix() {
        List<String> words = prefixWords();
        if (words.isEmpty()) {
            return "";
        }
        int pick = ThreadLocalRandom.current().nextInt(words.size());
        String rolled = words.get(pick);
        return rolled == null ? "" : rolled.trim();
    }

    public static String resolveDisplayName(String itemId, String prefixWord) {
        String base = baseDisplayName(itemId);
        if (prefixWord == null || prefixWord.isBlank()) {
            return base;
        }

        String[] words = base.split("\\s+");
        int tierIndex = findTierIndex(words);
        if (tierIndex >= 0 && words.length >= 2) {
            String tier = words[tierIndex];
            StringBuilder type = new StringBuilder();
            for (int i = 0; i < words.length; i++) {
                if (i == tierIndex) {
                    continue;
                }
                if (type.length() > 0) {
                    type.append(' ');
                }
                type.append(words[i]);
            }
            return tier + " " + prefixWord.trim() + " " + type;
        }

        if (words.length <= 1) {
            return prefixWord.trim() + " " + base;
        }

        return words[0] + " " + prefixWord.trim() + " " + String.join(" ", Arrays.copyOfRange(words, 1, words.length));
    }

    private static String baseDisplayName(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return "Unknown Item";
        }

        String normalized = itemId.trim();
        if (normalized.toLowerCase(Locale.ROOT).startsWith("weapon_")) {
            normalized = normalized.substring("weapon_".length());
        } else if (normalized.toLowerCase(Locale.ROOT).startsWith("armor_")) {
            normalized = normalized.substring("armor_".length());
        } else if (normalized.toLowerCase(Locale.ROOT).startsWith("tool_")) {
            normalized = normalized.substring("tool_".length());
        }

        return Arrays.stream(normalized.split("_"))
            .filter(token -> !token.isBlank())
            .map(ItemPrefixService::titleCase)
            .collect(Collectors.joining(" "));
    }

    private static String titleCase(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String token = raw.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(token.charAt(0)) + token.substring(1);
    }

    private static int findTierIndex(String[] words) {
        if (words == null) {
            return -1;
        }
        for (int i = 0; i < words.length; i++) {
            String token = words[i];
            if (token != null && TIER_TOKENS.contains(token.toLowerCase(Locale.ROOT))) {
                return i;
            }
        }
        return -1;
    }

    private static List<String> prefixWords() {
        try {
            HyruneConfig cfg = HyruneConfigManager.getConfig();
            if (cfg == null || cfg.prefixes == null || cfg.prefixes.rollableWords == null || cfg.prefixes.rollableWords.isEmpty()) {
                return new HyruneConfig.PrefixConfig().rollableWords;
            }
            return cfg.prefixes.rollableWords;
        } catch (Throwable ignored) {
            return new HyruneConfig.PrefixConfig().rollableWords;
        }
    }
}

