package dev.hytalemodding.hyrune.itemization;

import dev.hytalemodding.hyrune.config.HyruneConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemizationEligibilityServiceTest {
    @Test
    void eligiblePrefixIsAccepted() {
        HyruneConfig config = new HyruneConfig();
        config.itemizationEligiblePrefixes = List.of("weapon_", "armor_", "tool_");
        config.itemizationExcludedPrefixes = List.of("weapon_bomb_");
        config.itemizationExcludedIds = List.of();

        assertTrue(ItemizationEligibilityService.isEligibleItemId("Weapon_Sword_Bronze", config));
    }

    @Test
    void excludedPrefixWinsOverEligiblePrefix() {
        HyruneConfig config = new HyruneConfig();
        config.itemizationEligiblePrefixes = List.of("weapon_");
        config.itemizationExcludedPrefixes = List.of("weapon_bomb_", "weapon_arrow_");
        config.itemizationExcludedIds = List.of();

        assertFalse(ItemizationEligibilityService.isEligibleItemId("Weapon_Bomb_Fire", config));
        assertFalse(ItemizationEligibilityService.isEligibleItemId("Weapon_Arrow_Iron", config));
    }

    @Test
    void excludedExactIdWins() {
        HyruneConfig config = new HyruneConfig();
        config.itemizationEligiblePrefixes = List.of("weapon_");
        config.itemizationExcludedPrefixes = List.of();
        config.itemizationExcludedIds = List.of("weapon_sword_bronze");

        assertFalse(ItemizationEligibilityService.isEligibleItemId("Weapon_Sword_Bronze", config));
    }

    @Test
    void nonEligiblePrefixIsRejected() {
        HyruneConfig config = new HyruneConfig();
        config.itemizationEligiblePrefixes = List.of("weapon_");
        config.itemizationExcludedPrefixes = List.of();
        config.itemizationExcludedIds = List.of();

        assertFalse(ItemizationEligibilityService.isEligibleItemId("Furniture_Bookshelf_Single", config));
    }
}
