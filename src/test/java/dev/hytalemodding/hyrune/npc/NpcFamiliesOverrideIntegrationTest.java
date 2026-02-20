package dev.hytalemodding.hyrune.npc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcFamiliesOverrideIntegrationTest {

    @Test
    void trorkChieftainUsesConfiguredRankAndProducesStrongerStatsThanNormalRank() {
        NpcFamiliesConfig familiesConfig = new NpcFamiliesConfigRepository("./hyrune_data", "./lib/Server/NPC/Roles")
            .loadOrCreate();
        NpcLevelService service = new NpcLevelService(familiesConfig);

        NpcLevelComponent component = service.buildComponent("Trork_Chieftain", "Trork Chieftain");
        assertNotNull(component);
        assertEquals("HERO", component.getRankId());

        NpcFamiliesConfig.NpcFamilyDefinition assignment = familiesConfig.families.stream()
            .filter(a -> "trork_chieftain".equalsIgnoreCase(a.id))
            .findFirst()
            .orElseThrow();
        NpcFamiliesConfig.NpcRankProfile rankProfile = familiesConfig.rankProfiles.stream()
            .filter(r -> "HERO".equalsIgnoreCase(r.id))
            .findFirst()
            .orElseThrow();
        int minExpected = Math.max(1, assignment.baseLevel - assignment.variance) + rankProfile.levelOffset;
        int maxExpected = Math.min(99, assignment.baseLevel + assignment.variance) + rankProfile.levelOffset;
        assertTrue(component.getLevel() >= minExpected && component.getLevel() <= maxExpected);

        NpcLevelService.NpcCombatStats eliteStats = service.resolveCombatStats(component, CombatStyle.MELEE);
        NpcLevelComponent normal = component.clone();
        normal.setRankId("NORMAL");
        NpcLevelService.NpcCombatStats normalStats = service.resolveCombatStats(normal, CombatStyle.MELEE);

        assertTrue(eliteStats.damageMultiplier() > normalStats.damageMultiplier());
    }
}
