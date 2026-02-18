package dev.hytalemodding.hyrune.npc;

import com.hypixel.hytale.server.npc.entities.NPCEntity;

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
                                 String archetypeId) {
    }

    private final NpcLevelConfig config;
    private final Map<String, NpcLevelConfig.NpcArchetypeProfile> archetypeProfilesById;
    private final Random random = new Random();

    public NpcLevelService(NpcLevelConfig config) {
        this.config = config == null ? new NpcLevelConfig() : config;
        this.archetypeProfilesById = indexProfiles(this.config);
    }

    public NpcLevelConfig getConfig() {
        return config;
    }

    public NpcLevelComponent buildComponent(String npcTypeId, String baseName) {
        if (isExcluded(npcTypeId, baseName)) {
            return null;
        }
        NpcLevelConfig.NpcLevelOverride override = findOverride(npcTypeId);
        if (override != null) {
            return buildFromOverride(override, baseName);
        }

        NpcLevelConfig.NpcLevelGroup group = findGroup(npcTypeId);
        if (group != null) {
            return buildFromGroup(group, baseName);
        }

        int level = rollLevel(config.getDefaultLevel(), config.getDefaultVariance());
        CombatStyle weakness = CombatStyle.fromString(config.getDefaultWeakness(), CombatStyle.MELEE);
        String archetypeId = normalizeArchetypeOrDefault(config.getDefaultArchetype());
        return new NpcLevelComponent(level, "default", weakness, archetypeId, false, baseName);
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
            ? normalizeArchetypeOrDefault(config.getDefaultArchetype())
            : normalizeArchetypeOrDefault(component.getArchetypeId());
        NpcLevelConfig.NpcArchetypeProfile profile = resolveArchetypeProfile(archetypeId);
        int level = component == null ? 1 : Math.max(1, Math.min(99, component.getLevel()));

        double styleBias = switch (resolvedStyle) {
            case RANGED -> clamp(profile.getRangedBias(), 0.10, 5.0);
            case MAGIC -> clamp(profile.getMagicBias(), 0.10, 5.0);
            case MELEE -> clamp(profile.getMeleeBias(), 0.10, 5.0);
        };
        double damageMultiplier = clamp(
            (profile.getBaseDamage() + (level * profile.getDamagePerLevel())) * styleBias,
            0.10,
            25.0
        );
        double defenceReduction = clamp(
            profile.getBaseDefenceReduction() + (level * profile.getDefencePerLevel()),
            0.0,
            Math.max(0.0, profile.getDefenceCap())
        );
        double critChance = clamp(
            profile.getBaseCritChance() + (level * profile.getCritChancePerLevel()),
            0.0,
            Math.max(0.0, profile.getCritChanceCap())
        );
        double critMultiplier = clamp(
            profile.getBaseCritMultiplier() + (level * profile.getCritMultiplierPerLevel()),
            1.0,
            Math.max(1.0, profile.getCritMultiplierCap())
        );

        return new NpcCombatStats(
            damageMultiplier,
            defenceReduction,
            critChance,
            critMultiplier,
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
        for (NpcLevelConfig.NpcLevelGroup group : config.getGroups()) {
            if (group == null || group.getId() == null || group.getMatch() == null) {
                continue;
            }
            if (group.getId().equalsIgnoreCase(groupId) && matches(group.getMatch(), normalized)) {
                return true;
            }
        }
        return false;
    }

    private NpcLevelComponent buildFromGroup(NpcLevelConfig.NpcLevelGroup group, String baseName) {
        int level = rollLevel(group.getBaseLevel(), group.getVariance());
        CombatStyle weakness = CombatStyle.fromString(group.getWeakness(), CombatStyle.MELEE);
        String groupId = group.getId() != null ? group.getId() : "group";
        String archetypeId = normalizeArchetypeOrDefault(group.getArchetype());
        return new NpcLevelComponent(level, groupId, weakness, archetypeId, group.isElite(), baseName);
    }

    private NpcLevelComponent buildFromOverride(NpcLevelConfig.NpcLevelOverride override, String baseName) {
        int level = rollLevel(override.getLevel(), override.getVariance());
        CombatStyle weakness = CombatStyle.fromString(override.getWeakness(), CombatStyle.MELEE);
        String groupId = override.getTypeId() != null ? override.getTypeId() : "override";
        String archetypeId = normalizeArchetypeOrDefault(override.getArchetype());
        return new NpcLevelComponent(level, groupId, weakness, archetypeId, override.isElite(), baseName);
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

    private NpcLevelConfig.NpcLevelOverride findOverride(String npcTypeId) {
        if (npcTypeId == null) {
            return null;
        }
        String normalized = npcTypeId.toLowerCase(Locale.ROOT);
        for (NpcLevelConfig.NpcLevelOverride override : config.getOverrides()) {
            if (override == null || override.getTypeId() == null) {
                continue;
            }
            if (override.getTypeId().toLowerCase(Locale.ROOT).equals(normalized)) {
                return override;
            }
        }
        return null;
    }

    private NpcLevelConfig.NpcLevelGroup findGroup(String npcTypeId) {
        if (npcTypeId == null) {
            return null;
        }
        String normalized = npcTypeId.toLowerCase(Locale.ROOT);
        List<NpcLevelConfig.NpcLevelGroup> groups = config.getGroups();
        for (NpcLevelConfig.NpcLevelGroup group : groups) {
            if (group == null || group.getMatch() == null) {
                continue;
            }
            if (matches(group.getMatch(), normalized)) {
                return group;
            }
        }
        return null;
    }

    public boolean isExcluded(String npcTypeId, String baseName) {
        if (npcTypeId == null && baseName == null) {
            return true;
        }
        for (String excluded : config.getExcludedNpcIds()) {
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
        for (NpcLevelConfig.NpcLevelGroup group : config.getGroups()) {
            if (group == null || group.getId() == null || group.getMatch() == null) {
                continue;
            }
            if (!group.getId().equalsIgnoreCase(groupId)) {
                continue;
            }
            List<String> contains = group.getMatch().getContains();
            if (contains == null || contains.isEmpty()) {
                return null;
            }
            return contains.get(ThreadLocalRandom.current().nextInt(contains.size()));
        }
        return null;
    }

    private boolean matchesExclude(String value, String excluded) {
        if (value == null || excluded == null) {
            return false;
        }
        String needle = excluded.toLowerCase(Locale.ROOT);
        String haystack = value.toLowerCase(Locale.ROOT);
        return haystack.equals(needle) || haystack.contains(needle);
    }

    private boolean matches(NpcLevelConfig.NpcLevelMatch match, String npcTypeId) {
        for (String id : match.getTypeIds()) {
            if (id != null && npcTypeId.equals(id.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        for (String prefix : match.getStartsWith()) {
            if (prefix != null && npcTypeId.startsWith(prefix.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        for (String fragment : match.getContains()) {
            if (fragment != null && npcTypeId.contains(fragment.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private NpcLevelConfig.NpcArchetypeProfile resolveArchetypeProfile(String archetypeId) {
        String normalized = normalizeArchetype(archetypeId);
        NpcLevelConfig.NpcArchetypeProfile profile = archetypeProfilesById.get(normalized);
        if (profile != null) {
            return profile;
        }

        profile = archetypeProfilesById.get(normalizeArchetype(config.getDefaultArchetype()));
        if (profile != null) {
            return profile;
        }

        profile = archetypeProfilesById.get(DEFAULT_ARCHETYPE_ID);
        if (profile != null) {
            return profile;
        }

        return new NpcLevelConfig.NpcArchetypeProfile();
    }

    private String normalizeArchetypeOrDefault(String archetypeId) {
        String normalized = normalizeArchetype(archetypeId);
        if (archetypeProfilesById.containsKey(normalized)) {
            return normalized;
        }
        String configuredDefault = normalizeArchetype(config.getDefaultArchetype());
        if (archetypeProfilesById.containsKey(configuredDefault)) {
            return configuredDefault;
        }
        return DEFAULT_ARCHETYPE_ID;
    }

    private static String normalizeArchetype(String archetypeId) {
        if (archetypeId == null || archetypeId.isBlank()) {
            return DEFAULT_ARCHETYPE_ID;
        }
        return archetypeId.trim().toUpperCase(Locale.ROOT);
    }

    private static Map<String, NpcLevelConfig.NpcArchetypeProfile> indexProfiles(NpcLevelConfig config) {
        Map<String, NpcLevelConfig.NpcArchetypeProfile> out = new LinkedHashMap<>();
        if (config != null && config.getArchetypeProfiles() != null) {
            for (NpcLevelConfig.NpcArchetypeProfile profile : config.getArchetypeProfiles()) {
                if (profile == null) {
                    continue;
                }
                String key = normalizeArchetype(profile.getId());
                out.put(key, profile);
            }
        }
        if (!out.containsKey(DEFAULT_ARCHETYPE_ID)) {
            NpcLevelConfig.NpcArchetypeProfile fallback = new NpcLevelConfig.NpcArchetypeProfile();
            fallback.setId(DEFAULT_ARCHETYPE_ID);
            out.put(DEFAULT_ARCHETYPE_ID, fallback);
        }
        return out;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
