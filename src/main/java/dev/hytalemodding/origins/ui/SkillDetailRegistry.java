package dev.hytalemodding.origins.ui;

import dev.hytalemodding.origins.skills.SkillType;

import java.util.EnumMap;
import java.util.Map;

final class SkillDetailRegistry {
    private static final Map<SkillType, SkillDetail> DETAILS = buildSkillDetails();

    private SkillDetailRegistry() {
    }

    static SkillDetail getDetail(SkillType skill) {
        return skill != null ? DETAILS.get(skill) : null;
    }

    private static Map<SkillType, SkillDetail> buildSkillDetails() {
        EnumMap<SkillType, SkillDetail> details = new EnumMap<>(SkillType.class);

        details.put(SkillType.ATTACK, new SkillDetail(
            "Attack",
            "Increases melee damage and allows stronger weapons.",
            new SkillUnlock[]{
                new SkillUnlock(1, "Crude, Wooden, Scrap weapons"),
                new SkillUnlock(10, "Bone, Stone, Iron weapons"),
                new SkillUnlock(30, "Cobalt weapons"),
                new SkillUnlock(40, "Thorium weapons"),
                new SkillUnlock(50, "Adamantite, Doomed weapons"),
                new SkillUnlock(60, "Mithril, Onyxium weapons")
            }
        ));

        details.put(SkillType.DEFENCE, new SkillDetail(
            "Defence",
            "Reduces incoming damage and allows stronger armor.",
            new SkillUnlock[]{
                new SkillUnlock(1, "Crude, Wooden, Scrap armor"),
                new SkillUnlock(10, "Bone, Stone, Iron armor"),
                new SkillUnlock(30, "Cobalt armor"),
                new SkillUnlock(40, "Thorium armor"),
                new SkillUnlock(50, "Adamantite, Doomed armor"),
                new SkillUnlock(60, "Mithril, Onyxium armor")
            }
        ));

        details.put(SkillType.STRENGTH, new SkillDetail(
            "Strength",
            "Increases melee crit chance and crit damage.",
            new SkillUnlock[]{
                new SkillUnlock(1, "Crit chance and damage scaling")
            }
        ));

        details.put(SkillType.RANGED, new SkillDetail(
            "Ranged",
            "Increases ranged damage and crit chance.",
            new SkillUnlock[]{
                new SkillUnlock(1, "Crude, Wooden, Scrap ranged weapons"),
                new SkillUnlock(10, "Bone, Stone, Iron ranged weapons"),
                new SkillUnlock(30, "Cobalt ranged weapons"),
                new SkillUnlock(40, "Thorium ranged weapons"),
                new SkillUnlock(50, "Adamantite, Doomed ranged weapons"),
                new SkillUnlock(60, "Mithril, Onyxium ranged weapons")
            }
        ));

        details.put(SkillType.MAGIC, new SkillDetail(
            "Magic",
            "Increases magic damage, crits, and max mana.",
            new SkillUnlock[]{
                new SkillUnlock(1, "Crude, Wooden, Scrap magic weapons"),
                new SkillUnlock(10, "Bone, Stone, Iron magic weapons"),
                new SkillUnlock(30, "Cobalt magic weapons"),
                new SkillUnlock(40, "Thorium magic weapons"),
                new SkillUnlock(50, "Adamantite, Doomed magic weapons"),
                new SkillUnlock(60, "Mithril, Onyxium magic weapons")
            }
        ));

        details.put(SkillType.CONSTITUTION, new SkillDetail(
            "Constitution",
            "Increases maximum health.",
            new SkillUnlock[]{
                new SkillUnlock(1, "Max health +1 per level")
            }
        ));

        details.put(SkillType.RESTORATION, new SkillDetail(
            "Restoration",
            "Reserved for future healing and support bonuses.",
            new SkillUnlock[]{
                new SkillUnlock(1, "No unlocks yet")
            }
        ));

        details.put(SkillType.AGILITY, new SkillDetail(
            "Agility",
            "Increases stamina, stamina regen, and movement speed.",
            new SkillUnlock[]{
                new SkillUnlock(1, "Max stamina and regen scaling"),
                new SkillUnlock(1, "Movement speed scaling")
            }
        ));

        details.put(SkillType.MINING, new SkillDetail(
            "Mining",
            "Increases mining speed and reduces tool durability loss.",
            new SkillUnlock[]{
                new SkillUnlock(1, "Tool requirements by material"),
                new SkillUnlock(1, "Speed and durability scaling")
            }
        ));

        details.put(SkillType.WOODCUTTING, new SkillDetail(
            "Woodcutting",
            "Increases woodcutting speed and reduces tool durability loss.",
            new SkillUnlock[]{
                new SkillUnlock(1, "Tool requirements by material"),
                new SkillUnlock(1, "Speed and durability scaling")
            }
        ));

        details.put(SkillType.FARMING, new SkillDetail(
            "Farming",
            "Increases crop yields and sickle harvest XP.",
            new SkillUnlock[]{
                new SkillUnlock(1, "Yield bonus scaling (up to 50%)"),
                new SkillUnlock(1, "Sickle XP bonus (+25%)")
            }
        ));

        details.put(SkillType.COOKING, new SkillDetail(
            "Cooking",
            "Chance to cook double portions.",
            new SkillUnlock[]{
                new SkillUnlock(1, "Double proc chance up to 20%")
            }
        ));

        details.put(SkillType.ALCHEMY, new SkillDetail(
            "Alchemy",
            "Chance to brew double potions.",
            new SkillUnlock[]{
                new SkillUnlock(1, "Double proc chance up to 20%")
            }
        ));

        details.put(SkillType.FISHING, new SkillDetail(
            "Fishing",
            "Improves bite speed and rare fish chance.",
            new SkillUnlock[]{
                new SkillUnlock(1, "Faster bites and rare fish chance")
            }
        ));

        details.put(SkillType.SLAYER, new SkillDetail(
            "Slayer",
            "Complete tasks for points and rewards.",
            new SkillUnlock[]{
                new SkillUnlock(1, "Slayer tasks and points")
            }
        ));

        details.put(SkillType.ARCHITECT, new SkillDetail(
            "Architect",
            "Tracks crafting XP for building items.",
            new SkillUnlock[]{
                new SkillUnlock(1, "No unlocks yet")
            }
        ));

        details.put(SkillType.SMELTING, new SkillDetail(
            "Smelting",
            "Tracks crafting XP for smelting items.",
            new SkillUnlock[]{
                new SkillUnlock(1, "No unlocks yet")
            }
        ));

        details.put(SkillType.LEATHERWORKING, new SkillDetail(
            "Leatherworking",
            "Tracks crafting XP for leather items.",
            new SkillUnlock[]{
                new SkillUnlock(1, "No unlocks yet")
            }
        ));

        details.put(SkillType.ARMORSMITHING, new SkillDetail(
            "Armorsmithing",
            "Tracks crafting XP for armor.",
            new SkillUnlock[]{
                new SkillUnlock(1, "No unlocks yet")
            }
        ));

        details.put(SkillType.WEAPONSMITHING, new SkillDetail(
            "Weaponsmithing",
            "Tracks crafting XP for weapons.",
            new SkillUnlock[]{
                new SkillUnlock(1, "No unlocks yet")
            }
        ));

        details.put(SkillType.ARCANE_ENGINEERING, new SkillDetail(
            "Arcane Engineering",
            "Tracks crafting XP for arcane items.",
            new SkillUnlock[]{
                new SkillUnlock(1, "No unlocks yet")
            }
        ));

        return details;
    }

    static final class SkillDetail {
        final String title;
        final String description;
        final SkillUnlock[] unlocks;

        SkillDetail(String title, String description, SkillUnlock[] unlocks) {
            this.title = title;
            this.description = description;
            this.unlocks = unlocks;
        }
    }

    static final class SkillUnlock {
        final int level;
        final String text;

        SkillUnlock(int level, String text) {
            this.level = level;
            this.text = text;
        }
    }
}
