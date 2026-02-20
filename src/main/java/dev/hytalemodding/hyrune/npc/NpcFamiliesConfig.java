package dev.hytalemodding.hyrune.npc;

import java.util.ArrayList;
import java.util.List;

/**
 * Data-driven family/profile/rank model for NPC assignment.
 */
public class NpcFamiliesConfig {
    public int schemaVersion = 1;
    public int defaultLevel = 1;
    public int defaultVariance = 3;
    public String defaultWeakness = CombatStyle.MELEE.name();
    public String defaultArchetype = "DPS";
    public double weaknessMultiplier = 1.20;
    public double resistanceMultiplier = 0.80;
    public List<String> excludedNpcIds = new ArrayList<>();
    public List<NpcArchetypeProfile> archetypeProfiles = new ArrayList<>();
    public List<NpcRankProfile> rankProfiles = new ArrayList<>();
    public List<NpcFamilyDefinition> families = new ArrayList<>();

    public static class NpcArchetypeProfile {
        public String id = "DPS";
        public double baseDamage = 1.05;
        public double damageGrowthRate = 1.025;
        public double baseDefenceReduction = 0.02;
        public double defenceGrowthRate = 1.028;
        public double defenceCap = 0.58;
        public double baseCritChance = 0.05;
        public double critGrowthRate = 1.015;
        public double critChanceCap = 0.35;
        public double baseCritMultiplier = 1.45;
        public double critMultiplierPerLevel = 0.0040;
        public double critMultiplierCap = 2.25;
        public double baseHealthMultiplier = 1.00;
        public double meleeBias = 1.00;
        public double rangedBias = 1.06;
        public double magicBias = 0.98;
    }

    public static class NpcRankProfile {
        public String id = NpcRank.NORMAL.name();
        public int levelOffset = 0;
        public double statMultiplier = 1.0;
    }

    public static class NpcFamilyDefinition {
        public String id;
        public List<String> tags = new ArrayList<>();
        public List<String> typeIds = new ArrayList<>();
        public List<String> rolePaths = new ArrayList<>();
        public int baseLevel = 1;
        public int variance = 2;
        public String weakness = CombatStyle.MELEE.name();
        public String archetype = "DPS";
        public String rank = NpcRank.NORMAL.name();
        public boolean elite;
    }
}
