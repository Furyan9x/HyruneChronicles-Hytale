package dev.hytalemodding.hyrune.npc;

import com.hypixel.hytale.server.npc.entities.NPCEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Service for npc level.
 */
public class NpcLevelService {
    private static final String DEFAULT_ARCHETYPE_ID = "DPS";

    public record NpcCombatStats(double damageMultiplier,
                                 double defenceReduction,
                                 double critChance,
                                 double critMultiplier,
                                 double healthMultiplier,
                                 String rankId,
                                 String archetypeId) {
    }

    private final NpcFamiliesConfig familiesConfig;
    private final Map<String, NpcFamiliesConfig.NpcArchetypeProfile> archetypeProfilesById;
    private final Map<String, NpcFamiliesConfig.NpcFamilyDefinition> familyById;
    private final Map<String, NpcFamiliesConfig.NpcRankProfile> rankProfileById;
    private final List<NpcFamiliesConfig.NpcFamilyDefinition> orderedFamilies;
    private final Random random = new Random();

    public NpcLevelService(NpcFamiliesConfig familiesConfig) {
        this.familiesConfig = familiesConfig == null ? new NpcFamiliesConfig() : familiesConfig;
        this.archetypeProfilesById = indexProfiles(this.familiesConfig);
        this.familyById = indexFamilies(this.familiesConfig);
        this.rankProfileById = indexRankProfiles(this.familiesConfig);
        this.orderedFamilies = orderFamilies(this.familiesConfig);
    }

    public NpcFamiliesConfig getFamiliesConfig() {
        return familiesConfig;
    }

    public String getDefaultWeakness() {
        return familiesConfig.defaultWeakness;
    }

    public double getWeaknessMultiplier() {
        return familiesConfig.weaknessMultiplier;
    }

    public double getResistanceMultiplier() {
        return familiesConfig.resistanceMultiplier;
    }

    public NpcLevelComponent buildComponent(String npcTypeId, String baseName) {
        if (isExcluded(npcTypeId, baseName)) {
            return null;
        }

        ResolvedFamilyAssignment familyAssignment = findFamilyAssignment(npcTypeId);
        if (familyAssignment != null) {
            return buildFromFamilyAssignment(familyAssignment, baseName);
        }

        return buildFromConfigDefault(baseName);
    }

    public int getCombatLevel(int level) {
        int def = level;
        int hp = level;
        int res = level;
        int att = level;
        int str = level;
        int range = level;
        int magic = level;

        double base = 0.25 * (def + hp + res);
        double melee = 0.325 * (att + str);
        double ranged = 0.325 * (1.5 * range);
        double mage = 0.325 * (1.5 * magic);
        double maxOffense = Math.max(melee, Math.max(ranged, mage));
        return (int) (base + maxOffense);
    }

    public CombatStyle resolveWeakness(String weakness) {
        return CombatStyle.fromString(weakness, CombatStyle.MELEE);
    }

    public NpcCombatStats resolveCombatStats(NpcLevelComponent component, CombatStyle style) {
        CombatStyle resolvedStyle = style == null ? CombatStyle.MELEE : style;
        String archetypeId = component == null
            ? normalizeArchetypeOrDefault(familiesConfig.defaultArchetype)
            : normalizeArchetypeOrDefault(component.getArchetypeId());
        String rankId = component == null ? NpcRank.NORMAL.name() : normalizeRank(component.getRankId());
        NpcFamiliesConfig.NpcRankProfile rankProfile = resolveRankProfile(rankId);
        NpcFamiliesConfig.NpcArchetypeProfile profile = resolveArchetypeProfile(archetypeId);
        int level = component == null ? 1 : Math.max(1, Math.min(99, component.getLevel()));
        int effectiveLevel = level;

        double styleBias = switch (resolvedStyle) {
            case RANGED -> clamp(profile.rangedBias, 0.10, 5.0);
            case MAGIC -> clamp(profile.magicBias, 0.10, 5.0);
            case MELEE -> clamp(profile.meleeBias, 0.10, 5.0);
        };
        double damageGrowthRate = clamp(profile.damageGrowthRate, 1.0, 5.0);
        double defenceGrowthRate = clamp(profile.defenceGrowthRate, 1.0, 5.0);
        double critGrowthRate = clamp(profile.critGrowthRate, 1.0, 5.0);
        double rankMultiplier = clamp(rankProfile.statMultiplier, 1.0, 100.0);
        double damageMultiplier = clamp(
            exponentialStat(profile.baseDamage, damageGrowthRate, effectiveLevel) * styleBias * rankMultiplier,
            0.10,
            25.0
        );
        double defenceReduction = clamp(
            exponentialStat(profile.baseDefenceReduction, defenceGrowthRate, effectiveLevel) * rankMultiplier,
            0.0,
            Math.max(0.0, profile.defenceCap)
        );
        double critChance = clamp(
            exponentialStat(profile.baseCritChance, critGrowthRate, effectiveLevel) * rankMultiplier,
            0.0,
            Math.max(0.0, profile.critChanceCap)
        );
        double critMultiplier = clamp(
            (profile.baseCritMultiplier + (effectiveLevel * profile.critMultiplierPerLevel)) * rankMultiplier,
            1.0,
            Math.max(1.0, profile.critMultiplierCap)
        );
        double healthMultiplier = clamp(
            Math.max(0.10, profile.baseHealthMultiplier) * Math.pow(defenceGrowthRate, effectiveLevel) * rankMultiplier,
            0.10,
            500.0
        );

        return new NpcCombatStats(
            damageMultiplier,
            defenceReduction,
            critChance,
            critMultiplier,
            healthMultiplier,
            rankId,
            archetypeId
        );
    }

    public boolean isNpc(NPCEntity npc) {
        return npc != null && npc.getNPCTypeId() != null;
    }

    public boolean isNpcInGroup(String npcTypeId, String groupId) {
        if (npcTypeId == null || groupId == null) {
            return false;
        }
        String normalized = npcTypeId.toLowerCase(Locale.ROOT);
        NpcFamiliesConfig.NpcFamilyDefinition family = familyById.get(groupId.toLowerCase(Locale.ROOT));
        return family != null && NpcFamilyMatcher.matchesFamily(family, normalized);
    }

    private NpcLevelComponent buildFromFamilyAssignment(ResolvedFamilyAssignment resolved, String baseName) {
        if (resolved == null || resolved.family == null) {
            return null;
        }
        NpcFamiliesConfig.NpcFamilyDefinition family = resolved.family;
        int level = rollLevel(family.baseLevel, family.variance);
        NpcRank rank = NpcRank.fromString(family.rank, NpcRank.NORMAL);
        NpcFamiliesConfig.NpcRankProfile rankProfile = resolveRankProfile(rank.name());
        int adjustedLevel = Math.max(1, Math.min(99, level + rankProfile.levelOffset));
        CombatStyle weakness = CombatStyle.fromString(family.weakness, CombatStyle.MELEE);
        String archetypeId = normalizeArchetypeOrDefault(family.archetype);
        String groupId = family.id == null || family.id.isBlank()
            ? "family"
            : family.id;
        return new NpcLevelComponent(
            adjustedLevel,
            groupId,
            weakness,
            archetypeId,
            rank.name(),
            family.elite,
            baseName
        );
    }

    private NpcLevelComponent buildFromConfigDefault(String baseName) {
        int level = rollLevel(familiesConfig.defaultLevel, familiesConfig.defaultVariance);
        CombatStyle weakness = CombatStyle.fromString(familiesConfig.defaultWeakness, CombatStyle.MELEE);
        String archetypeId = normalizeArchetypeOrDefault(familiesConfig.defaultArchetype);
        return new NpcLevelComponent(level, "default", weakness, archetypeId, NpcRank.NORMAL.name(), false, baseName);
    }

    private int rollLevel(int baseLevel, int variance) {
        int clampedBaseLevel = Math.max(1, Math.min(99, baseLevel));
        int clampedVariance = Math.max(0, variance);
        int min = Math.max(1, clampedBaseLevel - clampedVariance);
        int max = Math.min(99, clampedBaseLevel + clampedVariance);
        if (max < min) {
            max = min;
        }
        return min + random.nextInt(max - min + 1);
    }

    public boolean isExcluded(String npcTypeId, String baseName) {
        if (npcTypeId == null && baseName == null) {
            return true;
        }
        for (String excluded : familiesConfig.excludedNpcIds) {
            if (excluded == null || excluded.isBlank()) {
                continue;
            }
            if (npcTypeId != null && matchesExclude(npcTypeId, excluded)) {
                return true;
            }
            if (baseName != null && matchesExclude(baseName, excluded)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a random NPC ID from the specified group ID.
     * Returns null if group doesn't exist or is empty.
     */
    public String getRandomIdFromGroup(String groupId) {
        if (groupId == null || groupId.isBlank()) {
            return null;
        }
        NpcFamiliesConfig.NpcFamilyDefinition family = familyById.get(groupId.toLowerCase(Locale.ROOT));
        if (family == null) {
            return null;
        }
        if (family.typeIds != null && !family.typeIds.isEmpty()) {
            return family.typeIds.get(ThreadLocalRandom.current().nextInt(family.typeIds.size()));
        }
        return null;
    }

    public NpcFamiliesConfig.NpcFamilyDefinition getFamilyDefinition(String groupId) {
        if (groupId == null || groupId.isBlank()) {
            return null;
        }
        return familyById.get(groupId.toLowerCase(Locale.ROOT));
    }

    public NpcFamiliesConfig.NpcArchetypeProfile getArchetypeProfile(String archetypeId) {
        return resolveArchetypeProfile(archetypeId);
    }

    public NpcFamiliesConfig.NpcRankProfile getRankProfile(String rankId) {
        return resolveRankProfile(rankId);
    }

    private boolean matchesExclude(String value, String excluded) {
        if (value == null || excluded == null) {
            return false;
        }
        String needle = excluded.toLowerCase(Locale.ROOT);
        String haystack = value.toLowerCase(Locale.ROOT);
        return haystack.equals(needle) || haystack.contains(needle);
    }

    private NpcFamiliesConfig.NpcArchetypeProfile resolveArchetypeProfile(String archetypeId) {
        String normalized = normalizeArchetype(archetypeId);
        NpcFamiliesConfig.NpcArchetypeProfile profile = archetypeProfilesById.get(normalized);
        if (profile != null) {
            return profile;
        }

        profile = archetypeProfilesById.get(normalizeArchetype(familiesConfig.defaultArchetype));
        if (profile != null) {
            return profile;
        }

        profile = archetypeProfilesById.get(DEFAULT_ARCHETYPE_ID);
        if (profile != null) {
            return profile;
        }

        return new NpcFamiliesConfig.NpcArchetypeProfile();
    }

    private String normalizeArchetypeOrDefault(String archetypeId) {
        String normalized = normalizeArchetype(archetypeId);
        if (archetypeProfilesById.containsKey(normalized)) {
            return normalized;
        }
        String configuredDefault = normalizeArchetype(familiesConfig.defaultArchetype);
        if (archetypeProfilesById.containsKey(configuredDefault)) {
            return configuredDefault;
        }
        return DEFAULT_ARCHETYPE_ID;
    }

    private ResolvedFamilyAssignment findFamilyAssignment(String npcTypeId) {
        if (npcTypeId == null || npcTypeId.isBlank() || orderedFamilies.isEmpty()) {
            return null;
        }
        String normalized = npcTypeId.toLowerCase(Locale.ROOT);
        for (NpcFamiliesConfig.NpcFamilyDefinition family : orderedFamilies) {
            if (family == null || family.id == null) {
                continue;
            }
            if (!NpcFamilyMatcher.matchesFamily(family, normalized)) {
                continue;
            }
            return new ResolvedFamilyAssignment(family);
        }
        return null;
    }

    private static String normalizeArchetype(String archetypeId) {
        if (archetypeId == null || archetypeId.isBlank()) {
            return DEFAULT_ARCHETYPE_ID;
        }
        return archetypeId.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeRank(String rankId) {
        return NpcRank.fromString(rankId, NpcRank.NORMAL).name();
    }

    private NpcFamiliesConfig.NpcRankProfile resolveRankProfile(String rankId) {
        String normalized = normalizeRank(rankId);
        NpcFamiliesConfig.NpcRankProfile profile = rankProfileById.get(normalized);
        if (profile != null) {
            return profile;
        }
        profile = rankProfileById.get(NpcRank.NORMAL.name());
        if (profile != null) {
            return profile;
        }
        NpcFamiliesConfig.NpcRankProfile fallback = new NpcFamiliesConfig.NpcRankProfile();
        fallback.id = NpcRank.NORMAL.name();
        fallback.levelOffset = 0;
        fallback.statMultiplier = 1.0;
        return fallback;
    }

    private static Map<String, NpcFamiliesConfig.NpcArchetypeProfile> indexProfiles(NpcFamiliesConfig config) {
        Map<String, NpcFamiliesConfig.NpcArchetypeProfile> out = new LinkedHashMap<>();
        if (config != null && config.archetypeProfiles != null) {
            for (NpcFamiliesConfig.NpcArchetypeProfile profile : config.archetypeProfiles) {
                if (profile == null) {
                    continue;
                }
                String key = normalizeArchetype(profile.id);
                out.put(key, profile);
            }
        }
        if (!out.containsKey(DEFAULT_ARCHETYPE_ID)) {
            NpcFamiliesConfig.NpcArchetypeProfile fallback = new NpcFamiliesConfig.NpcArchetypeProfile();
            fallback.id = DEFAULT_ARCHETYPE_ID;
            out.put(DEFAULT_ARCHETYPE_ID, fallback);
        }
        return out;
    }

    private static Map<String, NpcFamiliesConfig.NpcFamilyDefinition> indexFamilies(NpcFamiliesConfig config) {
        Map<String, NpcFamiliesConfig.NpcFamilyDefinition> out = new HashMap<>();
        if (config == null || config.families == null) {
            return out;
        }
        for (NpcFamiliesConfig.NpcFamilyDefinition family : config.families) {
            if (family == null || family.id == null || family.id.isBlank()) {
                continue;
            }
            out.put(family.id.toLowerCase(Locale.ROOT), family);
        }
        return out;
    }

    private static Map<String, NpcFamiliesConfig.NpcRankProfile> indexRankProfiles(NpcFamiliesConfig config) {
        Map<String, NpcFamiliesConfig.NpcRankProfile> out = new HashMap<>();
        if (config == null || config.rankProfiles == null) {
            return out;
        }
        for (NpcFamiliesConfig.NpcRankProfile rank : config.rankProfiles) {
            if (rank == null || rank.id == null || rank.id.isBlank()) {
                continue;
            }
            out.put(rank.id.toUpperCase(Locale.ROOT), rank);
        }
        return out;
    }

    private static List<NpcFamiliesConfig.NpcFamilyDefinition> orderFamilies(NpcFamiliesConfig config) {
        if (config == null || config.families == null || config.families.isEmpty()) {
            return List.of();
        }
        List<NpcFamiliesConfig.NpcFamilyDefinition> ordered = new ArrayList<>(config.families);
        ordered.sort(Comparator.comparingInt((NpcFamiliesConfig.NpcFamilyDefinition a) -> {
            if (a == null || a.id == null) {
                return 0;
            }
            return a.id.length();
        }).reversed());
        return ordered;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double exponentialStat(double base, double growthRate, int level) {
        return Math.max(0.0, base) * Math.pow(Math.max(0.0, growthRate), Math.max(0, level));
    }

    private record ResolvedFamilyAssignment(NpcFamiliesConfig.NpcFamilyDefinition family) {
    }
}
