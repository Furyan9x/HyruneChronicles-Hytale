package dev.hytalemodding.origins.npc;

import java.util.ArrayList;
import java.util.List;

public class NpcLevelConfig {
    private int defaultLevel = 1;
    private int defaultVariance = 3;
    private String defaultWeakness = CombatStyle.MELEE.name();
    private double weaknessMultiplier = 1.20;
    private double resistanceMultiplier = 0.80;
    private List<String> excludedNpcIds = new ArrayList<>();
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

    public double getWeaknessMultiplier() {
        return weaknessMultiplier;
    }

    public double getResistanceMultiplier() {
        return resistanceMultiplier;
    }

    public List<String> getExcludedNpcIds() {
        return excludedNpcIds;
    }

    public void setExcludedNpcIds(List<String> excludedNpcIds) {
        this.excludedNpcIds = excludedNpcIds;
    }

    public List<NpcLevelGroup> getGroups() {
        return groups;
    }

    public List<NpcLevelOverride> getOverrides() {
        return overrides;
    }

    public static class NpcLevelGroup {
        private String id;
        private NpcLevelMatch match;
        private int baseLevel = 1;
        private int variance = 2;
        private String weakness = CombatStyle.MELEE.name();
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

        public void setElite(boolean elite) {
            this.elite = elite;
        }
    }

    public static class NpcLevelOverride {
        private String typeId;
        private int level = 1;
        private int variance = 2;
        private String weakness = CombatStyle.MELEE.name();
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
}
