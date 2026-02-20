package dev.hytalemodding.hyrune.itemization;

import dev.hytalemodding.hyrune.bonus.SkillStatBonusApplier;
import dev.hytalemodding.hyrune.system.MiningSpeedSystem;
import dev.hytalemodding.hyrune.system.SkillCombatBonusSystem;
import dev.hytalemodding.hyrune.system.WoodcuttingSpeedSystem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemizationRuntimeHooksIntegrationTest {
    @Test
    void combatHooksRespectPenetrationAndCritSuppression() {
        assertEquals(900.0, SkillCombatBonusSystem.computeEffectiveArmorAfterPenetration(1000.0, 100.0), 1e-9);
        assertEquals(0.25, SkillCombatBonusSystem.suppressCritChance(0.40, 0.15), 1e-9);
    }

    @Test
    void combatHooksRespectManaCostAndCadence() {
        assertEquals(6.0, SkillCombatBonusSystem.computeManaCost(8.0, 0.25), 1e-9);
        assertEquals(520L, SkillCombatBonusSystem.computeCadenceIntervalMs(SkillCombatBonusSystem.BASE_ATTACK_CADENCE_MS, 0.25));
    }

    @Test
    void movementAndGatheringHooksUseRuntimeMultipliers() {
        assertEquals(0.2375f, SkillStatBonusApplier.applyItemMovementSpeedSoftCap(0.35), 1e-6f);
        assertEquals(2.50f, MiningSpeedSystem.computeMiningDamageMultiplier(50, 0.25), 1e-6f);
        assertEquals(2.50f, WoodcuttingSpeedSystem.computeWoodcuttingDamageMultiplier(50, 0.25), 1e-6f);
    }

    @Test
    void gatheringLootHooksApplyDoubleDrop() {
        assertTrue(GatheringUtilityDropService.shouldDoubleDrop(0.60, 0.59));
        assertFalse(GatheringUtilityDropService.shouldDoubleDrop(0.60, 0.60));
        assertEquals(4, GatheringUtilityDropService.doubledQuantity(2));
    }

    @Test
    void contractMatrixCoversEveryItemizedStat() {
        assertEquals(ItemizedStat.values().length, ItemizedStatRuntimeContracts.all().size());
        for (ItemizedStat stat : ItemizedStat.values()) {
            assertNotNull(ItemizedStatRuntimeContracts.get(stat));
        }
    }
}
