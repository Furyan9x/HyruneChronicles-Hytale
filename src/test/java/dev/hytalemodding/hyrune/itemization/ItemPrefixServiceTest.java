package dev.hytalemodding.hyrune.itemization;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemPrefixServiceTest {
    @Test
    void tierPrefixTypeOrderingForSwordStaysCorrect() {
        assertEquals("Copper Flame Sword", ItemPrefixService.resolveDisplayName("Weapon_Sword_Copper", "Flame"));
    }

    @Test
    void bowNamingUsesTypeAsSuffix() {
        assertEquals("Bomb Gale Shortbow", ItemPrefixService.resolveDisplayName("Weapon_Shortbow_Bomb", "Gale"));
    }
}
