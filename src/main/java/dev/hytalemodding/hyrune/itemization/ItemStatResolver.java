package dev.hytalemodding.hyrune.itemization;

import com.hypixel.hytale.server.core.inventory.ItemStack;
import dev.hytalemodding.hyrune.repair.ItemRarity;

import java.util.Locale;

/**
 * Resolves effective stats from base item family + rolled metadata.
 */
public final class ItemStatResolver {
    private ItemStatResolver() {
    }

    public static EffectiveItemStats resolve(ItemStack stack) {
        BaseStats base = resolveBaseStats(stack != null ? stack.getItemId() : null);
        ItemInstanceMetadata metadata = stack != null
            ? stack.getFromMetadataOrNull(ItemInstanceMetadata.KEYED_CODEC)
            : null;
        if (metadata == null) {
            return new EffectiveItemStats(base.damage, base.defence, base.healing, base.utility);
        }

        ItemRarity rarity = metadata.getRarity();
        double rarityMult = getRarityStatMultiplier(rarity);
        double dmg = base.damage * rarityMult * (1.0 + metadata.getDamageRoll());
        double def = base.defence * rarityMult * (1.0 + metadata.getDefenceRoll());
        double heal = base.healing * rarityMult * (1.0 + metadata.getHealingRoll());
        double util = base.utility * rarityMult * (1.0 + metadata.getUtilityRoll());

        switch (metadata.getCatalyst()) {
            case WATER:
                heal *= 1.20;
                util *= 1.10;
                break;
            case FIRE:
                dmg *= 1.20;
                break;
            case EARTH:
                def *= 1.20;
                break;
            case NONE:
            default:
                break;
        }

        double droppedPenalty = clamp(metadata.getDroppedPenalty(), 0d, 1d);
        if (droppedPenalty > 0d) {
            double keep = 1.0 - droppedPenalty;
            dmg *= keep;
            def *= keep;
            heal *= keep;
            util *= keep;
        }

        return new EffectiveItemStats(dmg, def, heal, util);
    }

    private static double getRarityStatMultiplier(ItemRarity rarity) {
        if (rarity == null) {
            return 1.0;
        }
        switch (rarity) {
            case UNCOMMON:
                return 1.10;
            case RARE:
                return 1.22;
            case EPIC:
                return 1.36;
            case VOCATIONAL:
                return 1.36;
            case LEGENDARY:
                return 1.52;
            case MYTHIC:
                return 1.70;
            case COMMON:
            default:
                return 1.0;
        }
    }

    private static BaseStats resolveBaseStats(String itemId) {
        if (itemId == null) {
            return new BaseStats(0, 0, 0, 0);
        }
        String id = itemId.toLowerCase(Locale.ROOT);

        if (id.startsWith("weapon_")) {
            return new BaseStats(12, 2, 0, 2);
        }
        if (id.startsWith("armor_")) {
            return new BaseStats(1, 10, 0, 1);
        }
        if (id.startsWith("tool_")) {
            return new BaseStats(3, 3, 0, 6);
        }
        if (id.contains("staff") || id.contains("wand")) {
            return new BaseStats(8, 3, 6, 3);
        }
        return new BaseStats(1, 1, 0, 0);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class BaseStats {
        private final double damage;
        private final double defence;
        private final double healing;
        private final double utility;

        private BaseStats(double damage, double defence, double healing, double utility) {
            this.damage = damage;
            this.defence = defence;
            this.healing = healing;
            this.utility = utility;
        }
    }
}
