package dev.hytalemodding.origins.npc;

import com.hypixel.hytale.server.npc.entities.NPCEntity;

import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class NpcLevelService {

    private final NpcLevelConfig config;
    private final Random random = new Random();

    public NpcLevelService(NpcLevelConfig config) {
        this.config = config;
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
        return new NpcLevelComponent(level, "default", weakness, false, baseName);
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

    public boolean isNpc(NPCEntity npc) {
        return npc != null && npc.getNPCTypeId() != null;
    }

    private NpcLevelComponent buildFromGroup(NpcLevelConfig.NpcLevelGroup group, String baseName) {
        int level = rollLevel(group.getBaseLevel(), group.getVariance());
        CombatStyle weakness = CombatStyle.fromString(group.getWeakness(), CombatStyle.MELEE);
        String groupId = group.getId() != null ? group.getId() : "group";
        return new NpcLevelComponent(level, groupId, weakness, group.isElite(), baseName);
    }

    private NpcLevelComponent buildFromOverride(NpcLevelConfig.NpcLevelOverride override, String baseName) {
        int level = rollLevel(override.getLevel(), override.getVariance());
        CombatStyle weakness = CombatStyle.fromString(override.getWeakness(), CombatStyle.MELEE);
        String groupId = override.getTypeId() != null ? override.getTypeId() : "override";
        return new NpcLevelComponent(level, groupId, weakness, override.isElite(), baseName);
    }

    private int rollLevel(int baseLevel, int variance) {
        int clampedVariance = Math.min(Math.max(variance, 0), 2);
        int min = baseLevel - clampedVariance;
        int max = baseLevel + clampedVariance;
        int level = min + random.nextInt(max - min + 1);
        if (level < 1) {
            level = 1;
        }
        if (level > 99) {
            level = 99;
        }
        return level;
    }

    private NpcLevelConfig.NpcLevelOverride findOverride(String npcTypeId) {
        if (npcTypeId == null) {
            return null;
        }
        String normalized = npcTypeId.toLowerCase(Locale.ROOT);
        for (NpcLevelConfig.NpcLevelOverride override : config.getOverrides()) {
            if (override.getTypeId() == null) {
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
        for (NpcLevelConfig.NpcLevelGroup group : config.getGroups()) {
            if (group.getId().equalsIgnoreCase(groupId)) {
                List<String> contains = group.getMatch().getContains();
                if (contains == null || contains.isEmpty()) return null;

                return contains.get(ThreadLocalRandom.current().nextInt(contains.size()));
            }
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
}
