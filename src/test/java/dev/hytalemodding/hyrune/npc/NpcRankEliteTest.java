package dev.hytalemodding.hyrune.npc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcRankEliteTest {

    @Test
    void eliteRankAppliesPlusFiveLevelsAndOnePointFiveStatMultiplier() {
        NpcFamiliesConfig families = new NpcFamiliesConfig();
        families.rankProfiles.clear();
        families.rankProfiles.add(rank("NORMAL", 0, 1.0));
        families.rankProfiles.add(rank("ELITE", 5, 1.5));

        NpcFamiliesConfig.NpcFamilyDefinition family = new NpcFamiliesConfig.NpcFamilyDefinition();
        family.id = "trork_chieftain";
        family.typeIds.add("Trork_Chieftain");
        family.baseLevel = 40;
        family.variance = 0;
        family.weakness = CombatStyle.MAGIC.name();
        family.archetype = "DPS";
        family.rank = "ELITE";
        family.elite = true;
        families.families.add(family);

        NpcLevelService service = new NpcLevelService(families);
        NpcLevelComponent elite = service.buildComponent("Trork_Chieftain", "Trork Chieftain");
        assertNotNull(elite);
        assertEquals(45, elite.getLevel(), "ELITE rank should apply +5 level to base 40");
        assertEquals("ELITE", elite.getRankId());

        NpcLevelService.NpcCombatStats eliteStats = service.resolveCombatStats(elite, CombatStyle.MELEE);
        NpcLevelComponent normal = elite.clone();
        normal.setRankId("NORMAL");
        NpcLevelService.NpcCombatStats normalStats = service.resolveCombatStats(normal, CombatStyle.MELEE);

        double damageRatio = eliteStats.damageMultiplier() / normalStats.damageMultiplier();
        double healthRatio = eliteStats.healthMultiplier() / normalStats.healthMultiplier();
        assertTrue(Math.abs(damageRatio - 1.5) < 0.0001, "Expected damage multiplier ratio to be 1.5");
        assertTrue(Math.abs(healthRatio - 1.5) < 0.0001, "Expected health multiplier ratio to be 1.5");
    }

    private static NpcFamiliesConfig.NpcRankProfile rank(String id, int levelOffset, double statMultiplier) {
        NpcFamiliesConfig.NpcRankProfile profile = new NpcFamiliesConfig.NpcRankProfile();
        profile.id = id;
        profile.levelOffset = levelOffset;
        profile.statMultiplier = statMultiplier;
        return profile;
    }
}
