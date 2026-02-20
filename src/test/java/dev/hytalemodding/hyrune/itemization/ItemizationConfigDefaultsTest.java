package dev.hytalemodding.hyrune.itemization;

import dev.hytalemodding.hyrune.config.HyruneConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemizationConfigDefaultsTest {
    @Test
    void hpRegenIsPercentOnlyByDefault() {
        HyruneConfig cfg = new HyruneConfig();
        String value = cfg.itemizationSpecializedStats.rollTypeConstraintByStat.get("hp_regen");
        assertEquals("percent_only", value);
    }

    @Test
    void arrowsAreExcludedByDefault() {
        HyruneConfig cfg = new HyruneConfig();
        assertTrue(cfg.itemizationExcludedPrefixes.contains("weapon_arrow_"));
    }

    @Test
    void rarityModelIncludesExpandedGenerationSources() {
        HyruneConfig cfg = new HyruneConfig();
        var sources = cfg.itemizationRarityModel.baseWeightsBySource;
        assertTrue(sources.containsKey("monster_drop"));
        assertTrue(sources.containsKey("container_loot"));
        assertTrue(sources.containsKey("quest_reward"));
        assertTrue(sources.containsKey("slayer_shop"));
        assertTrue(sources.containsKey("fishing"));
        assertTrue(sources.containsKey("world_pickup"));
        assertTrue(sources.containsKey("starter_kit"));
        assertEquals(1.0, sources.get("starter_kit").common);
    }

    @Test
    void gatheringRareDropConfigIncludesSkillPlaceholders() {
        HyruneConfig cfg = new HyruneConfig();
        var bySkill = cfg.gatheringUtilityDrops.rareDropsBySkill;
        assertTrue(bySkill.containsKey("MINING"));
        assertTrue(bySkill.containsKey("WOODCUTTING"));
        assertTrue(bySkill.containsKey("FISHING"));
        assertTrue(bySkill.containsKey("FARMING"));
    }
}
