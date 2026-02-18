package dev.hytalemodding.hyrune.itemization;

import com.hypixel.hytale.server.core.inventory.ItemStack;
import dev.hytalemodding.hyrune.repair.ItemRarity;

import java.util.Map;

/**
 * Resolves effective specialized stats from base archetype + rolled metadata.
 */
public final class ItemStatResolver {
    private ItemStatResolver() {
    }

    public static EffectiveItemStats resolve(ItemStack stack) {
        return resolveDetailed(stack).getResolvedStats();
    }

    public static ItemStatResolution resolveDetailed(ItemStack stack) {
        String itemId = stack != null ? stack.getItemId() : null;
        ItemInstanceMetadata metadata = stack != null
            ? stack.getFromMetadataOrNull(ItemInstanceMetadata.KEYED_CODEC)
            : null;
        return resolveDetailed(itemId, metadata);
    }

    public static ItemStatResolution resolveDetailed(String itemId, ItemInstanceMetadata metadata) {
        ItemArchetype archetype = ItemArchetypeResolver.resolve(itemId);
        double tierScalar = ItemizationSpecializedStatConfigHelper.tierScalar(itemId);

        ItemizedStatBlock base = baseStatsForArchetype(archetype, tierScalar);
        ItemizedStatBlock resolved = ItemizedStatBlock.empty();
        ItemizedStatBlock socketBonuses = metadata == null
            ? ItemizedStatBlock.empty()
            : GemSocketConfigHelper.socketBonusesForItem(itemId, metadata.getSocketedGems());

        ItemRarity rarity = metadata == null ? ItemRarity.COMMON : metadata.getRarity();
        double rarityScalar = ItemizationSpecializedStatConfigHelper.rarityScalar(rarity);
        double droppedKeep = 1.0 - clamp(metadata == null ? 0.0 : metadata.getDroppedPenalty(), 0.0, 1.0);

        for (ItemizedStat stat : ItemizedStat.values()) {
            // Avoid double-dipping against native item Health bonuses.
            // Archetype MAX_HP bases are hidden from tooltip and stack on top of Hytale armor HP.
            // We only want rolled/socketed MAX_HP to contribute through itemization.
            double baseValue = stat == ItemizedStat.MAX_HP ? 0.0 : base.get(stat);
            double flatRoll = metadata == null ? 0.0 : metadata.getFlatStatRoll(stat);
            double percentRoll = metadata == null ? 0.0 : metadata.getPercentStatRoll(stat);
            double socketFlat = socketBonuses.get(stat);
            double value = ((baseValue + flatRoll + socketFlat) * (1.0 + percentRoll)) * rarityScalar * droppedKeep;
            if (value > 0.0) {
                resolved.set(stat, value);
            }
        }

        EffectiveItemStats baseSummary = summarize(base);
        EffectiveItemStats resolvedSummary = summarize(resolved);

        return new ItemStatResolution(
            itemId,
            archetype,
            base,
            resolved,
            baseSummary,
            resolvedSummary,
            rarityScalar,
            droppedKeep
        );
    }

    private static ItemizedStatBlock baseStatsForArchetype(ItemArchetype archetype, double tierScalar) {
        ItemizedStatBlock out = ItemizedStatBlock.empty();
        Map<ItemizedStat, Double> configured = ItemizationSpecializedStatConfigHelper.baseStatsForArchetype(archetype);
        for (Map.Entry<ItemizedStat, Double> entry : configured.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            out.set(entry.getKey(), Math.max(0.0, entry.getValue() * tierScalar));
        }
        return out;
    }

    private static EffectiveItemStats summarize(ItemizedStatBlock stats) {
        if (stats == null) {
            return new EffectiveItemStats(0, 0, 0, 0);
        }

        double damage = 0.0;
        damage += stats.get(ItemizedStat.PHYSICAL_DAMAGE);
        damage += stats.get(ItemizedStat.MAGICAL_DAMAGE);
        damage += stats.get(ItemizedStat.PHYSICAL_PENETRATION) * 4.0;
        damage += stats.get(ItemizedStat.MAGICAL_PENETRATION) * 4.0;
        damage += stats.get(ItemizedStat.CRIT_BONUS) * 2.0;
        damage += (stats.get(ItemizedStat.PHYSICAL_CRIT_CHANCE) + stats.get(ItemizedStat.MAGICAL_CRIT_CHANCE)) * 2.0;

        double defence = 0.0;
        defence += stats.get(ItemizedStat.PHYSICAL_DEFENCE);
        defence += stats.get(ItemizedStat.MAGICAL_DEFENCE);
        defence += stats.get(ItemizedStat.BLOCK_EFFICIENCY) * 25.0;
        defence += stats.get(ItemizedStat.CRIT_REDUCTION) * 20.0;
        defence += stats.get(ItemizedStat.MAX_HP) * 0.25;
        defence += stats.get(ItemizedStat.HP_REGEN) * 15.0;
        defence += stats.get(ItemizedStat.REFLECT_DAMAGE) * 15.0;

        double healing = 0.0;
        healing += stats.get(ItemizedStat.HEALING_POWER);
        healing += stats.get(ItemizedStat.HEALING_CRIT_CHANCE) * 10.0;
        healing += stats.get(ItemizedStat.HEALING_CRIT_BONUS) * 8.0;
        healing += stats.get(ItemizedStat.MANA_COST_REDUCTION) * 6.0;

        double utility = 0.0;
        utility += stats.get(ItemizedStat.MANA_REGEN) * 15.0;
        utility += stats.get(ItemizedStat.MOVEMENT_SPEED) * 40.0;
        utility += stats.get(ItemizedStat.ATTACK_SPEED) * 30.0;
        utility += stats.get(ItemizedStat.CAST_SPEED) * 30.0;
        utility += stats.get(ItemizedStat.BLOCK_BREAK_SPEED) * 35.0;
        utility += stats.get(ItemizedStat.RARE_DROP_CHANCE) * 30.0;
        utility += stats.get(ItemizedStat.DOUBLE_DROP_CHANCE) * 35.0;

        return new EffectiveItemStats(damage, defence, healing, utility);
    }

    private static double clamp(double value, double min, double max) {
        if (Double.isNaN(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }
}
