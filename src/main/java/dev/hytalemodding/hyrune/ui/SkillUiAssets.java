package dev.hytalemodding.hyrune.ui;

import dev.hytalemodding.hyrune.skills.SkillType;

/**
 * Shared UI asset helpers for skill-related pages.
 */
public final class SkillUiAssets {
    private SkillUiAssets() {
    }

    public static String getSkillIconPath(SkillType skill) {
        if (skill == null) {
            return null;
        }
        switch (skill) {
            case ATTACK:
                return "Pages/attack.png";
            case DEFENCE:
                return "Pages/defence.png";
            case STRENGTH:
                return "Pages/strength.png";
            case FISHING:
                return "Pages/fishing.png";
            case MINING:
                return "Pages/mining.png";
            case SMELTING:
                return "Pages/smelting.png";
            case ARCANE_ENGINEERING:
                return "Pages/arcane_engineering.png";
            case ARMORSMITHING:
                return "Pages/armorsmithing.png";
            case WEAPONSMITHING:
                return "Pages/weaponsmithing.png";
            case LEATHERWORKING:
                return "Pages/leatherworking.png";
            case ARCHITECT:
                return "Pages/architect.png";
            case WOODCUTTING:
                return "Pages/woodcutting.png";
            case RANGED:
                return "Pages/ranged.png";
            case CONSTITUTION:
                return "Pages/constitution.png";
            case MAGIC:
                return "Pages/magic.png";
            case RESTORATION:
                return "Pages/restoration.png";
            case SLAYER:
                return "Pages/slayer.png";
            case AGILITY:
                return "Pages/agility.png";
            case FARMING:
                return "Pages/farming.png";
            case COOKING:
                return "Pages/cooking.png";
            case ALCHEMY:
                return "Pages/alchemy.png";
            default:
                return null;
        }
    }
}
