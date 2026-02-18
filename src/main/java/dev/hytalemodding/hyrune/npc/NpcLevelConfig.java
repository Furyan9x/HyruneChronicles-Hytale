package dev.hytalemodding.hyrune.npc;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for npc level.
 */
public class NpcLevelConfig {
    private int defaultLevel = 1;
    private int defaultVariance = 3;
    private String defaultWeakness = CombatStyle.MELEE.name();
    private String defaultArchetype = "DPS";
    private double weaknessMultiplier = 1.20;
    private double resistanceMultiplier = 0.80;
    private List<String> excludedNpcIds = new ArrayList<>();
    private List<NpcArchetypeProfile> archetypeProfiles = new ArrayList<>();
    private List<NpcLevelGroup> groups = new ArrayList<>();
    private List<NpcLevelOverride> overrides = new ArrayList<>();

    public int getDefaultLevel() {
        return defaultLevel;
    }

    public int getDefaultVariance() {
        return defaultVariance;
    }

    public String getDefaultWeakness() {
        return defaultWeakness;
    }

    public String getDefaultArchetype() {
        return defaultArchetype;
    }

    public void setDefaultArchetype(String defaultArchetype) {
        this.defaultArchetype = defaultArchetype;
    }

    public double getWeaknessMultiplier() {
        return weaknessMultiplier;
    }

    public double getResistanceMultiplier() {
        return resistanceMultiplier;
    }

    public List<String> getExcludedNpcIds() {
        return excludedNpcIds;
    }

    public List<NpcArchetypeProfile> getArchetypeProfiles() {
        return archetypeProfiles;
    }

    public void setArchetypeProfiles(List<NpcArchetypeProfile> archetypeProfiles) {
        this.archetypeProfiles = archetypeProfiles;
    }

    public void setExcludedNpcIds(List<String> excludedNpcIds) {
        this.excludedNpcIds = excludedNpcIds;
    }

    public List<NpcLevelGroup> getGroups() {
        return groups;
    }

    public void setGroups(List<NpcLevelGroup> groups) {
        this.groups = groups;
    }

    public List<NpcLevelOverride> getOverrides() {
        return overrides;
    }

    public void setOverrides(List<NpcLevelOverride> overrides) {
        this.overrides = overrides;
    }

    public static class NpcLevelGroup {
        private String id;
        private NpcLevelMatch match;
        private int baseLevel = 1;
        private int variance = 2;
        private String weakness = CombatStyle.MELEE.name();
        private String archetype = "DPS";
        private boolean elite;

        public String getId() {
            return id;
        }

        public NpcLevelMatch getMatch() {
            return match;
        }

        public int getBaseLevel() {
            return baseLevel;
        }

        public int getVariance() {
            return variance;
        }

        public String getWeakness() {
            return weakness;
        }

        public String getArchetype() {
            return archetype;
        }

        public boolean isElite() {
            return elite;
        }

        public void setId(String id) {
            this.id = id;
        }

        public void setMatch(NpcLevelMatch match) {
            this.match = match;
        }

        public void setBaseLevel(int baseLevel) {
            this.baseLevel = baseLevel;
        }

        public void setVariance(int variance) {
            this.variance = variance;
        }

        public void setWeakness(String weakness) {
            this.weakness = weakness;
        }

        public void setArchetype(String archetype) {
            this.archetype = archetype;
        }

        public void setElite(boolean elite) {
            this.elite = elite;
        }
    }

    public static class NpcLevelOverride {
        private String typeId;
        private int level = 1;
        private int variance = 2;
        private String weakness = CombatStyle.MELEE.name();
        private String archetype = "DPS";
        private boolean elite;

        public String getTypeId() {
            return typeId;
        }

        public int getLevel() {
            return level;
        }

        public int getVariance() {
            return variance;
        }

        public String getWeakness() {
            return weakness;
        }

        public String getArchetype() {
            return archetype;
        }

        public boolean isElite() {
            return elite;
        }

        public void setTypeId(String typeId) {
            this.typeId = typeId;
        }

        public void setLevel(int level) {
            this.level = level;
        }

        public void setVariance(int variance) {
            this.variance = variance;
        }

        public void setWeakness(String weakness) {
            this.weakness = weakness;
        }

        public void setArchetype(String archetype) {
            this.archetype = archetype;
        }

        public void setElite(boolean elite) {
            this.elite = elite;
        }
    }

    public static class NpcLevelMatch {
        private List<String> typeIds = new ArrayList<>();
        private List<String> contains = new ArrayList<>();
        private List<String> startsWith = new ArrayList<>();

        public List<String> getTypeIds() {
            return typeIds;
        }

        public List<String> getContains() {
            return contains;
        }

        public List<String> getStartsWith() {
            return startsWith;
        }

        public void setTypeIds(List<String> typeIds) {
            this.typeIds = typeIds;
        }

        public void setContains(List<String> contains) {
            this.contains = contains;
        }

        public void setStartsWith(List<String> startsWith) {
            this.startsWith = startsWith;
        }
    }

    public static class NpcArchetypeProfile {
        private String id = "DPS";
        private double baseDamage = 1.05;
        private double damagePerLevel = 0.018;
        private double baseDefenceReduction = 0.02;
        private double defencePerLevel = 0.0022;
        private double defenceCap = 0.58;
        private double baseCritChance = 0.05;
        private double critChancePerLevel = 0.0025;
        private double critChanceCap = 0.35;
        private double baseCritMultiplier = 1.45;
        private double critMultiplierPerLevel = 0.0040;
        private double critMultiplierCap = 2.25;
        private double meleeBias = 1.00;
        private double rangedBias = 1.06;
        private double magicBias = 0.98;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public double getBaseDamage() {
            return baseDamage;
        }

        public void setBaseDamage(double baseDamage) {
            this.baseDamage = baseDamage;
        }

        public double getDamagePerLevel() {
            return damagePerLevel;
        }

        public void setDamagePerLevel(double damagePerLevel) {
            this.damagePerLevel = damagePerLevel;
        }

        public double getBaseDefenceReduction() {
            return baseDefenceReduction;
        }

        public void setBaseDefenceReduction(double baseDefenceReduction) {
            this.baseDefenceReduction = baseDefenceReduction;
        }

        public double getDefencePerLevel() {
            return defencePerLevel;
        }

        public void setDefencePerLevel(double defencePerLevel) {
            this.defencePerLevel = defencePerLevel;
        }

        public double getDefenceCap() {
            return defenceCap;
        }

        public void setDefenceCap(double defenceCap) {
            this.defenceCap = defenceCap;
        }

        public double getBaseCritChance() {
            return baseCritChance;
        }

        public void setBaseCritChance(double baseCritChance) {
            this.baseCritChance = baseCritChance;
        }

        public double getCritChancePerLevel() {
            return critChancePerLevel;
        }

        public void setCritChancePerLevel(double critChancePerLevel) {
            this.critChancePerLevel = critChancePerLevel;
        }

        public double getCritChanceCap() {
            return critChanceCap;
        }

        public void setCritChanceCap(double critChanceCap) {
            this.critChanceCap = critChanceCap;
        }

        public double getBaseCritMultiplier() {
            return baseCritMultiplier;
        }

        public void setBaseCritMultiplier(double baseCritMultiplier) {
            this.baseCritMultiplier = baseCritMultiplier;
        }

        public double getCritMultiplierPerLevel() {
            return critMultiplierPerLevel;
        }

        public void setCritMultiplierPerLevel(double critMultiplierPerLevel) {
            this.critMultiplierPerLevel = critMultiplierPerLevel;
        }

        public double getCritMultiplierCap() {
            return critMultiplierCap;
        }

        public void setCritMultiplierCap(double critMultiplierCap) {
            this.critMultiplierCap = critMultiplierCap;
        }

        public double getMeleeBias() {
            return meleeBias;
        }

        public void setMeleeBias(double meleeBias) {
            this.meleeBias = meleeBias;
        }

        public double getRangedBias() {
            return rangedBias;
        }

        public void setRangedBias(double rangedBias) {
            this.rangedBias = rangedBias;
        }

        public double getMagicBias() {
            return magicBias;
        }

        public void setMagicBias(double magicBias) {
            this.magicBias = magicBias;
        }
    }
}
