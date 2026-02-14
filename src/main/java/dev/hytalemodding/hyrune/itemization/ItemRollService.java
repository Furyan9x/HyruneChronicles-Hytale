package dev.hytalemodding.hyrune.itemization;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import dev.hytalemodding.hyrune.config.HyruneConfigManager;
import dev.hytalemodding.hyrune.repair.ItemRarity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

/**
 * Applies rarity and specialized stat rolls to item instances.
 */
public final class ItemRollService {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final double DROPPED_GEAR_STAT_PENALTY = 0.10;

    private ItemRollService() {
    }

    public static boolean isEligibleForRoll(ItemStack stack) {
        return ItemizationEligibilityService.isEligible(stack);
    }

    public static ItemStack rollCrafted(ItemStack stack) {
        return ItemGenerationService.rollIfEligible(
            stack,
            ItemRollSource.CRAFTED,
            ItemGenerationContext.of("legacy_roll_crafted")
        );
    }

    public static ItemStack rollDropped(ItemStack stack) {
        return ItemGenerationService.rollIfEligible(
            stack,
            ItemRollSource.DROPPED,
            ItemGenerationContext.of("legacy_roll_dropped")
        );
    }

    static ItemStack rollNewInstance(ItemStack stack,
                                     ItemRollSource source,
                                     CatalystAffinity catalyst,
                                     ItemGenerationContext context) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        ItemRarityRollModel.Result rarityResult = ItemRarityRollModel.roll(source, stack.getItemId(), context, random);
        ItemRarity rarity = rarityResult == null ? ItemRarity.COMMON : rarityResult.rarity();
        Map<String, Double> rarityDebug = rarityResult == null ? Map.of() : rarityResult.debug();

        String itemId = stack.getItemId();
        ItemArchetype archetype = ItemArchetypeResolver.resolve(itemId);
        double tierScalar = ItemizationSpecializedStatConfigHelper.tierScalar(itemId);
        int statCount = ItemizationSpecializedStatConfigHelper.statsForRarity(rarity);
        RollResults rolled = rollStatPool(itemId, archetype, catalyst, statCount, random);

        ItemInstanceMetadata data = new ItemInstanceMetadata();
        data.setVersion(ItemInstanceMetadata.CURRENT_SCHEMA_VERSION);
        data.setSource(source);
        data.setCatalyst(catalyst);
        data.setRarity(rarity);
        data.setSeed(random.nextLong());
        data.setStatFlatRollsRaw(rolled.flatRolls());
        data.setStatPercentRollsRaw(rolled.percentRolls());
        data.setDroppedPenalty(source == ItemRollSource.DROPPED ? DROPPED_GEAR_STAT_PENALTY : 0d);

        if (HyruneConfigManager.getConfig().itemizationDebugLogging) {
            LOGGER.at(Level.INFO).log("[Itemization] Rolled item="
                + stack.getItemId()
                + ", source=" + source
                + ", rarity=" + data.getRarity().name()
                + ", catalyst=" + data.getCatalyst().name()
                + ", archetype=" + archetype.getId()
                + ", tierScalar=" + String.format(Locale.US, "%.3f", tierScalar)
                + ", flatStats=" + rolled.flatRolls()
                + ", percentStats=" + rolled.percentRolls()
                + ", rarityDebug=" + rarityDebug);
        }

        return stack.withMetadata(ItemInstanceMetadata.KEYED_CODEC, data);
    }

    private static RollResults rollStatPool(String itemId,
                                            ItemArchetype archetype,
                                            CatalystAffinity catalyst,
                                            int statCount,
                                            ThreadLocalRandom random) {
        List<ItemizedStat> pool = new ArrayList<>(ItemizationSpecializedStatConfigHelper.poolForArchetype(archetype));
        if (pool.isEmpty() || statCount <= 0) {
            return new RollResults(Map.of(), Map.of());
        }

        int picks = Math.min(statCount, pool.size());
        Map<String, Double> flatOut = new LinkedHashMap<>();
        Map<String, Double> percentOut = new LinkedHashMap<>();
        for (int i = 0; i < picks; i++) {
            ItemizedStat selected = weightedPick(pool, catalyst, random);
            if (selected == null) {
                break;
            }
            pool.remove(selected);
            ItemizationSpecializedStatConfigHelper.RollType type = weightedRollType(random);
            switch (type) {
                case FLAT -> flatOut.put(selected.getId(), rollFlatBonus(itemId, archetype, selected, false, random));
                case PERCENT -> percentOut.put(selected.getId(), rollPercentBonus(itemId, false, random));
                case HYBRID -> {
                    flatOut.put(selected.getId(), rollFlatBonus(itemId, archetype, selected, true, random));
                    percentOut.put(selected.getId(), rollPercentBonus(itemId, true, random));
                }
            }
        }
        return new RollResults(flatOut, percentOut);
    }

    private static ItemizedStat weightedPick(List<ItemizedStat> pool,
                                             CatalystAffinity catalyst,
                                             ThreadLocalRandom random) {
        if (pool == null || pool.isEmpty()) {
            return null;
        }
        double total = 0.0;
        double[] weights = new double[pool.size()];
        for (int i = 0; i < pool.size(); i++) {
            ItemizedStat stat = pool.get(i);
            double baseWeight = ItemizationSpecializedStatConfigHelper.statWeight(stat);
            double familyBias = ItemizationSpecializedStatConfigHelper.catalystFamilyBias(catalyst, stat.getFamily());
            double weight = Math.max(0.0, baseWeight * familyBias);
            weights[i] = weight;
            total += weight;
        }
        if (total <= 0.0) {
            return pool.get(random.nextInt(pool.size()));
        }

        double roll = random.nextDouble(total);
        double cursor = 0.0;
        for (int i = 0; i < pool.size(); i++) {
            cursor += weights[i];
            if (roll <= cursor) {
                return pool.get(i);
            }
        }
        return pool.get(pool.size() - 1);
    }

    private static ItemizationSpecializedStatConfigHelper.RollType weightedRollType(ThreadLocalRandom random) {
        double flatWeight = Math.max(0.0, ItemizationSpecializedStatConfigHelper.rollTypeWeight(ItemizationSpecializedStatConfigHelper.RollType.FLAT));
        double percentWeight = Math.max(0.0, ItemizationSpecializedStatConfigHelper.rollTypeWeight(ItemizationSpecializedStatConfigHelper.RollType.PERCENT));
        double hybridWeight = Math.max(0.0, ItemizationSpecializedStatConfigHelper.rollTypeWeight(ItemizationSpecializedStatConfigHelper.RollType.HYBRID));
        double total = flatWeight + percentWeight + hybridWeight;
        if (total <= 1e-9) {
            return ItemizationSpecializedStatConfigHelper.RollType.FLAT;
        }

        double pick = random.nextDouble(total);
        if (pick < flatWeight) {
            return ItemizationSpecializedStatConfigHelper.RollType.FLAT;
        }
        pick -= flatWeight;
        if (pick < percentWeight) {
            return ItemizationSpecializedStatConfigHelper.RollType.PERCENT;
        }
        return ItemizationSpecializedStatConfigHelper.RollType.HYBRID;
    }

    private static double rollFlatBonus(String itemId,
                                        ItemArchetype archetype,
                                        ItemizedStat stat,
                                        boolean hybrid,
                                        ThreadLocalRandom random) {
        double minScalar = ItemizationSpecializedStatConfigHelper.flatRollMinScalar();
        double maxScalar = ItemizationSpecializedStatConfigHelper.flatRollMaxScalar();
        double scalar = randomBetween(minScalar, maxScalar, random);
        if (hybrid) {
            scalar *= Math.max(0.0, ItemizationSpecializedStatConfigHelper.hybridFlatScalar());
        }
        double base = ItemizationSpecializedStatConfigHelper.baseValueForArchetypeStat(archetype, stat);
        double tierScalar = ItemizationSpecializedStatConfigHelper.tierScalar(itemId);
        return round4(base * scalar * tierScalar);
    }

    private static double rollPercentBonus(String itemId, boolean hybrid, ThreadLocalRandom random) {
        double min = ItemizationSpecializedStatConfigHelper.percentRollMin();
        double max = ItemizationSpecializedStatConfigHelper.percentRollMax();
        double rolled = randomBetween(min, max, random);
        double tierScalar = ItemizationSpecializedStatConfigHelper.tierScalar(itemId);
        double tierFactor = 1.0 + ((tierScalar - 1.0) * Math.max(0.0, ItemizationSpecializedStatConfigHelper.percentRollTierInfluence()));
        rolled *= Math.max(0.10, tierFactor);
        if (hybrid) {
            rolled *= Math.max(0.0, ItemizationSpecializedStatConfigHelper.hybridPercentScalar());
        }
        return round4(rolled);
    }

    private static double randomBetween(double min, double max, ThreadLocalRandom random) {
        double low = Math.min(min, max);
        double high = Math.max(min, max);
        if (Math.abs(high - low) < 1e-9) {
            return low;
        }
        return random.nextDouble(low, high);
    }

    private static double round4(double value) {
        return Double.parseDouble(String.format(Locale.US, "%.4f", value));
    }

    private record RollResults(Map<String, Double> flatRolls, Map<String, Double> percentRolls) {
    }
}
