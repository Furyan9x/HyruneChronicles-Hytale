package dev.hytalemodding.origins.npc;

import dev.hytalemodding.origins.config.OriginsConfig;
import dev.hytalemodding.origins.config.OriginsConfigManager;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 
 */
public final class NpcLevelHologram {
    private static final Set<String> PREFIX_DESCRIPTOR_TOKENS = new HashSet<>(Arrays.asList(
        "undead",
        "green",
        "red",
        "blue",
        "grizzly",
        "wild",
        "feral",
        "domestic",
        "baby",
        "young",
        "juvenile",
        "alpha",
        "elder",
        "ancient"
    ));

    private NpcLevelHologram() {
    }

    public static String buildLabel(String name, int npcLevel, boolean elite) {
        String safeName = resolveDisplayName(name);
        String star = elite ? "* " : "";
        return star + "[Lvl " + npcLevel + "] " + safeName;
    }

    private static String resolveDisplayName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return "NPC";
        }

        String cleaned = cleanupTokens(rawName);

        if (cleaned.isEmpty()) {
            return "NPC";
        }

        String normalized = normalizeDisplayName(cleaned);
        String defaultDisplay = toTitleCase(cleaned.split(" "));
        if (!normalized.equals(defaultDisplay)) {
            return normalized;
        }

        String override = findConfiguredOverride(rawName, cleaned);
        if (override != null) {
            return override;
        }

        return defaultDisplay;
    }

    private static String normalizeDisplayName(String cleanedName) {
        String[] words = cleanedName.toLowerCase().split(" ");
        if (words.length >= 2) {
            String first = words[0];
            boolean hasFamilyDuplicate = false;
            for (int i = 1; i < words.length; i++) {
                if (words[i].startsWith(first)) {
                    hasFamilyDuplicate = true;
                    break;
                }
            }

            if (hasFamilyDuplicate) {
                words = Arrays.copyOfRange(words, 1, words.length);
            } else if (words.length == 2 && PREFIX_DESCRIPTOR_TOKENS.contains(words[1])) {
                words = new String[]{words[1], words[0]};
            }
        }

        return toTitleCase(words);
    }

    private static String findConfiguredOverride(String rawName, String cleanedName) {
        OriginsConfig cfg = OriginsConfigManager.getConfig();
        if (cfg == null || cfg.npcNameOverrides == null || cfg.npcNameOverrides.isEmpty()) {
            return null;
        }

        String rawKey = canonicalKey(rawName);
        String cleanedKey = canonicalKey(cleanedName);

        for (Map.Entry<String, String> entry : cfg.npcNameOverrides.entrySet()) {
            String configuredKey = entry.getKey();
            String configuredName = formatConfiguredName(entry.getValue());
            if (configuredKey == null || configuredName == null) {
                continue;
            }

            if (configuredKey.equalsIgnoreCase(rawName)
                || configuredKey.equalsIgnoreCase(cleanedName)
                || canonicalKey(configuredKey).equals(rawKey)
                || canonicalKey(configuredKey).equals(cleanedKey)) {
                return configuredName;
            }
        }
        return null;
    }

    private static String formatConfiguredName(String configuredName) {
        if (configuredName == null) {
            return null;
        }
        String cleaned = cleanupTokens(configuredName);
        if (cleaned.isEmpty()) {
            return null;
        }
        return toTitleCase(cleaned.split(" "));
    }

    private static String canonicalKey(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return raw.trim()
            .toLowerCase()
            .replaceAll("[^a-z0-9]+", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private static String cleanupTokens(String raw) {
        return raw.trim()
            .replace('_', ' ')
            .replace('-', ' ')
            .replaceAll("[^A-Za-z0-9 ]", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private static String toTitleCase(String[] words) {
        if (words == null || words.length == 0) {
            return "NPC";
        }

        StringBuilder out = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (word.isEmpty()) {
                continue;
            }
            if (i > 0) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                out.append(word.substring(1).toLowerCase());
            }
        }
        return out.length() == 0 ? "NPC" : out.toString();
    }
}
