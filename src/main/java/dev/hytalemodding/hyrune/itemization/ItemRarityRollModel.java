package dev.hytalemodding.hyrune.itemization;

import dev.hytalemodding.hyrune.config.HyruneConfig;
import dev.hytalemodding.hyrune.config.HyruneConfigManager;
import dev.hytalemodding.hyrune.repair.ItemRarity;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Config-driven rarity rolling with profession and bench-tier modifiers.
 */
public final class ItemRarityRollModel {
    private static final ItemRarity[] ROLLABLE_RARITIES = new ItemRarity[]{
        ItemRarity.COMMON,
        ItemRarity.UNCOMMON,
        ItemRarity.RARE,
        ItemRarity.EPIC,
        ItemRarity.LEGENDARY,
        ItemRarity.MYTHIC
    };

    private ItemRarityRollModel() {
    }

    public static Result roll(ItemRollSource source, String itemId, ItemGenerationContext context, ThreadLocalRandom random) {
        HyruneConfig cfg = HyruneConfigManager.getConfig();
        HyruneConfig.ItemizationRarityModelConfig model = cfg.itemizationRarityModel == null
            ? new HyruneConfig.ItemizationRarityModelConfig()
            : cfg.itemizationRarityModel;

        double professionBonus = computeProfessionBonus(model, context);
        double benchBonus = computeBenchBonus(model, context);
        double rawScore = professionBonus + benchBonus;
        double clampedScore = clamp(rawScore, model.minRarityScore, model.maxRarityScore);

        double[] weights = resolveBaseWeights(model, source);
        double[] adjusted = adjustWeights(weights, clampedScore, model.rarityShiftStrength);
        ItemRarity rolled = sample(adjusted, random.nextDouble());

        Map<String, Double> debug = new LinkedHashMap<>();
        debug.put("professionBonus", professionBonus);
        debug.put("benchBonus", benchBonus);
        debug.put("rarityScoreRaw", rawScore);
        debug.put("rarityScoreClamped", clampedScore);
        debug.put("commonW", adjusted[0]);
        debug.put("uncommonW", adjusted[1]);
        debug.put("rareW", adjusted[2]);
        debug.put("epicW", adjusted[3]);
        debug.put("legendaryW", adjusted[4]);
        debug.put("mythicW", adjusted[5]);
        return new Result(rolled, debug);
    }

    private static double computeProfessionBonus(HyruneConfig.ItemizationRarityModelConfig model, ItemGenerationContext context) {
        if (context == null || context.professionSkill() == null || context.professionLevel() == null) {
            return 0.0;
        }
        String skillKey = context.professionSkill().toUpperCase(Locale.ROOT);
        Double perLevel = model.professionBonusPerLevel == null ? null : model.professionBonusPerLevel.get(skillKey);
        if (perLevel == null) {
            return 0.0;
        }
        int levelCap = Math.max(1, model.maxProfessionLevel);
        int effectiveLevel = Math.max(0, Math.min(levelCap, context.professionLevel()));
        return perLevel * effectiveLevel;
    }

    private static double computeBenchBonus(HyruneConfig.ItemizationRarityModelConfig model, ItemGenerationContext context) {
        if (context == null || context.benchTier() == null || model.benchTierBonus == null) {
            return 0.0;
        }
        return model.benchTierBonus.getOrDefault(context.benchTier(), 0.0);
    }

    private static double[] resolveBaseWeights(HyruneConfig.ItemizationRarityModelConfig model, ItemRollSource source) {
        String sourceKey = (source == null ? ItemRollSource.CRAFTED : source).name().toLowerCase(Locale.ROOT);
        HyruneConfig.RarityWeights weights = null;
        if (model.baseWeightsBySource != null) {
            weights = model.baseWeightsBySource.get(sourceKey);
        }
        if (weights == null) {
            weights = new HyruneConfig.RarityWeights();
        }
        return new double[]{
            Math.max(0.0, weights.common),
            Math.max(0.0, weights.uncommon),
            Math.max(0.0, weights.rare),
            Math.max(0.0, weights.epic),
            Math.max(0.0, weights.legendary),
            Math.max(0.0, weights.mythic)
        };
    }

    private static double[] adjustWeights(double[] baseWeights, double rarityScore, double shiftStrength) {
        double[] adjusted = new double[baseWeights.length];
        double midpoint = (baseWeights.length - 1) / 2.0;
        for (int i = 0; i < baseWeights.length; i++) {
            double rarityBias = i - midpoint;
            double factor = 1.0 + (rarityScore * shiftStrength * rarityBias);
            if (factor < 0.05) {
                factor = 0.05;
            }
            adjusted[i] = baseWeights[i] * factor;
        }
        return normalize(adjusted);
    }

    private static double[] normalize(double[] weights) {
        double sum = 0.0;
        for (double weight : weights) {
            sum += weight;
        }
        if (sum <= 0.0) {
            return new double[]{1.0, 0.0, 0.0, 0.0, 0.0, 0.0};
        }
        double[] normalized = new double[weights.length];
        for (int i = 0; i < weights.length; i++) {
            normalized[i] = weights[i] / sum;
        }
        return normalized;
    }

    private static ItemRarity sample(double[] normalizedWeights, double roll) {
        double cursor = 0.0;
        for (int i = 0; i < normalizedWeights.length; i++) {
            cursor += normalizedWeights[i];
            if (roll <= cursor) {
                return ROLLABLE_RARITIES[i];
            }
        }
        return ItemRarity.COMMON;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public record Result(ItemRarity rarity, Map<String, Double> debug) {
    }
}
