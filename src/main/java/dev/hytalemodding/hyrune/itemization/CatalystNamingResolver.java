package dev.hytalemodding.hyrune.itemization;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Produces display-friendly names for catalyst-enhanced items.
 */
public final class CatalystNamingResolver {
    private CatalystNamingResolver() {
    }

    public static String resolveDisplayName(String itemId, CatalystAffinity affinity) {
        String base = baseDisplayName(itemId);
        if (affinity == null || affinity == CatalystAffinity.NONE) {
            return base;
        }
        String infix = catalystWord(affinity);
        String[] words = base.split("\\s+");
        if (words.length <= 1) {
            return infix + " " + base;
        }

        StringBuilder out = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                out.append(' ');
            }
            if (i == words.length - 1) {
                out.append(infix).append(' ');
            }
            out.append(words[i]);
        }
        return out.toString();
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
            .map(CatalystNamingResolver::titleCase)
            .collect(Collectors.joining(" "));
    }

    private static String catalystWord(CatalystAffinity affinity) {
        return switch (affinity) {
            case FIRE -> "Flame";
            case WATER -> "Wave";
            case AIR -> "Gale";
            case EARTH -> "Stone";
            case NONE -> "";
        };
    }

    private static String titleCase(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String token = raw.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(token.charAt(0)) + token.substring(1);
    }
}

