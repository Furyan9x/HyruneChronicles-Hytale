package dev.hytalemodding.hyrune.npc;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Shared matcher for NPC type IDs against family definitions.
 */
public final class NpcFamilyMatcher {
    private static final Set<String> RUNTIME_PREFIX_TOKENS = Set.of(
        "dungeon", "temple", "wild", "quest", "test", "npc", "spawn", "event", "zone", "instance"
    );

    private NpcFamilyMatcher() {
    }

    public static boolean matchesFamily(NpcFamiliesConfig.NpcFamilyDefinition family, String npcTypeIdLower) {
        if (family == null || npcTypeIdLower == null || family.typeIds == null || family.typeIds.isEmpty()) {
            return false;
        }
        Set<String> candidates = candidateIds(npcTypeIdLower);
        for (String id : family.typeIds) {
            if (id == null || id.isBlank()) {
                continue;
            }
            String expected = id.toLowerCase(Locale.ROOT);
            for (String candidate : candidates) {
                if (candidate.equals(expected) || candidate.endsWith("_" + expected)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Set<String> candidateIds(String npcTypeIdLower) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        if (npcTypeIdLower == null || npcTypeIdLower.isBlank()) {
            return candidates;
        }
        String normalized = canonicalize(npcTypeIdLower);
        if (normalized.isBlank()) {
            return candidates;
        }
        candidates.add(normalized);

        String stripped = stripLeadingRuntimePrefixes(normalized);
        if (!stripped.equals(normalized)) {
            candidates.add(stripped);
        }
        return candidates;
    }

    private static String stripLeadingRuntimePrefixes(String normalizedId) {
        String current = normalizedId;
        while (true) {
            int split = current.indexOf('_');
            if (split <= 0) {
                return current;
            }
            String first = current.substring(0, split);
            if (!RUNTIME_PREFIX_TOKENS.contains(first)) {
                return current;
            }
            current = current.substring(split + 1);
        }
    }

    private static String canonicalize(String raw) {
        return raw.toLowerCase(Locale.ROOT)
            .replace('-', '_')
            .replaceAll("[^a-z0-9_]+", "_")
            .replaceAll("_+", "_")
            .replaceAll("^_+|_+$", "");
    }
}
