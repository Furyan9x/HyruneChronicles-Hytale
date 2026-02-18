package dev.hytalemodding.hyrune.itemization;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import dev.hytalemodding.hyrune.config.HyruneConfigManager;
import dev.hytalemodding.hyrune.repair.DurabilityPolicy;
import dev.hytalemodding.hyrune.repair.ItemRarity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
            ItemRarityRollModel.GenerationContext.of("legacy_roll_crafted")
        );
    }

    public static ItemStack rollDropped(ItemStack stack) {
        return ItemGenerationService.rollIfEligible(
            stack,
            ItemRollSource.DROPPED,
            ItemRarityRollModel.GenerationContext.of("legacy_roll_dropped")
        );
    }

    static ItemStack rollNewInstance(ItemStack stack,
                                     ItemRollSource source,
                                     String prefixWord,
                                     ItemRarityRollModel.GenerationContext context) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        ItemRarityRollModel.Result rarityResult = ItemRarityRollModel.roll(source, context, random);
        ItemRarity rarity = rarityResult == null ? ItemRarity.COMMON : rarityResult.rarity();
        Map<String, Double> rarityDebug = rarityResult == null ? Map.of() : rarityResult.debug();

        String itemId = stack.getItemId();
        ItemStack durabilityAdjusted = applyDurabilityScaling(stack, itemId, rarity);
        ItemArchetype archetype = ItemArchetypeResolver.resolve(itemId);
        double tierScalar = ItemizationSpecializedStatConfigHelper.tierScalar(itemId);
        int statCount = ItemizationSpecializedStatConfigHelper.statsForRarity(rarity);
        RollResults rolled = rollStatPool(itemId, archetype, prefixWord, statCount, random);

        ItemInstanceMetadata data = new ItemInstanceMetadata();
        data.setVersion(ItemInstanceMetadata.CURRENT_SCHEMA_VERSION);
        data.setSource(source);
        data.setPrefixRaw(prefixWord);
        data.setRarity(rarity);
        data.setSeed(random.nextLong());
        data.setSocketCapacity(GemSocketConfigHelper.socketsForRarity(rarity));
        data.setSocketedGems(List.of());
        data.setStatFlatRollsRaw(rolled.flatRolls());
        data.setStatPercentRollsRaw(rolled.percentRolls());
        data.setDroppedPenalty(source == ItemRollSource.DROPPED ? DROPPED_GEAR_STAT_PENALTY : 0d);

        if (HyruneConfigManager.getConfig().itemizationDebugLogging) {
            LOGGER.at(Level.INFO).log("[Itemization][Roll] item=" + stack.getItemId()
                + ", src=" + source
                + ", rarity=" + data.getRarity().name()
                + ", score=" + scoreSummary(rarityDebug)
                + ", prefix=" + (data.getPrefixRaw().isBlank() ? "none" : data.getPrefixRaw())
                + ", arch=" + archetype.getId()
                + ", rows=" + formatRows(rolled.rowSelections())
                + ", flat=" + rolled.flatRolls().size()
                + ", pct=" + rolled.percentRolls().size()
                + ", tier=" + String.format(Locale.US, "%.3f", tierScalar));
        }

        return durabilityAdjusted.withMetadata(ItemInstanceMetadata.KEYED_CODEC, data);
    }

    private static RollResults rollStatPool(String itemId,
                                            ItemArchetype archetype,
                                            String prefixWord,
                                            int statCount,
                                            ThreadLocalRandom random) {
        List<ItemizedStat> archetypePool = new ArrayList<>(ItemizationSpecializedStatConfigHelper.poolForArchetype(archetype));
        List<ItemizedStat> prefixPool = new ArrayList<>(ItemizationSpecializedStatConfigHelper.poolForPrefix(prefixWord));
        List<ItemizedStat> intersectPool = new ArrayList<>(ItemizationSpecializedStatConfigHelper.intersectPools(archetype, prefixWord));
        if ((archetypePool.isEmpty() && prefixPool.isEmpty()) || statCount <= 0) {
            return new RollResults(Map.of(), Map.of(), List.of());
        }

        int picks = Math.min(statCount, 6);
        Set<ItemizedStat> selected = new HashSet<>();
        Map<String, Double> flatOut = new LinkedHashMap<>();
        Map<String, Double> percentOut = new LinkedHashMap<>();
        List<RowSelection> rows = new ArrayList<>(picks);
        for (int row = 1; row <= picks; row++) {
            List<ItemizedStat> candidates;
            boolean usePrefixPriority;
            String targetLane = targetLaneForRow(row, picks);
            switch (targetLane) {
                case "A" -> {
                    candidates = remaining(archetypePool, selected);
                    usePrefixPriority = false;
                    if (candidates.isEmpty()) {
                        candidates = remaining(prefixPool, selected);
                        usePrefixPriority = true;
                        targetLane = "P-FB";
                    }
                }
                case "P" -> {
                    candidates = remaining(prefixPool, selected);
                    usePrefixPriority = true;
                    if (candidates.isEmpty()) {
                        candidates = remaining(archetypePool, selected);
                        usePrefixPriority = false;
                        targetLane = "A-FB";
                    }
                }
                default -> {
                    // "I" lane: intersect first, then prefix, then archetype fallback.
                    candidates = remaining(intersectPool, selected);
                    usePrefixPriority = true;
                    if (candidates.isEmpty()) {
                        candidates = remaining(prefixPool, selected);
                        usePrefixPriority = true;
                        targetLane = "P";
                    }
                    if (candidates.isEmpty()) {
                        candidates = remaining(archetypePool, selected);
                        usePrefixPriority = false;
                        targetLane = "A-FB";
                    }
                }
            }

            ItemizedStat chosen = weightedPick(candidates, prefixWord, usePrefixPriority, random);
            if (chosen == null) {
                break;
            }
            selected.add(chosen);
            ItemizationSpecializedStatConfigHelper.RollType type = resolveRollTypeForStat(chosen, random);
            rows.add(new RowSelection(row, targetLane, chosen.getId(), type));
            switch (type) {
                case FLAT -> flatOut.put(chosen.getId(), rollFlatBonus(itemId, archetype, chosen, random));
                case PERCENT -> percentOut.put(chosen.getId(), rollPercentBonus(itemId, random));
            }
        }
        return new RollResults(flatOut, percentOut, rows);
    }

    private static String targetLaneForRow(int row, int picks) {
        // Low-row remap so uncommon/rare items still express prefix identity.
        if (picks <= 1) {
            return "A";
        }
        if (picks == 2) {
            return row == 1 ? "A" : "P";
        }
        if (picks == 3) {
            if (row == 1) {
                return "A";
            }
            return "P";
        }

        // Standard lane model for 4-6 rows.
        if (row <= 2) {
            return "A";
        }
        if (row <= 4) {
            return "P";
        }
        return "I";
    }

    private static ItemizedStat weightedPick(List<ItemizedStat> pool,
                                             String prefixWord,
                                             boolean usePrefixPriority,
                                             ThreadLocalRandom random) {
        if (pool == null || pool.isEmpty()) {
            return null;
        }
        double total = 0.0;
        double[] weights = new double[pool.size()];
        for (int i = 0; i < pool.size(); i++) {
            ItemizedStat stat = pool.get(i);
            double baseWeight = ItemizationSpecializedStatConfigHelper.statWeight(stat);
            double prefixPriority = usePrefixPriority
                ? ItemizationSpecializedStatConfigHelper.prefixPriorityWeight(prefixWord, stat)
                : 1.0;
            double weight = Math.max(0.0, baseWeight * prefixPriority);
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

    private static List<ItemizedStat> remaining(List<ItemizedStat> source, Set<ItemizedStat> selected) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        List<ItemizedStat> out = new ArrayList<>();
        for (ItemizedStat stat : source) {
            if (stat != null && !selected.contains(stat)) {
                out.add(stat);
            }
        }
        return out;
    }

    private static ItemizationSpecializedStatConfigHelper.RollType weightedRollType(ThreadLocalRandom random) {
        double flatWeight = Math.max(0.0, ItemizationSpecializedStatConfigHelper.rollTypeWeight(ItemizationSpecializedStatConfigHelper.RollType.FLAT));
        double percentWeight = Math.max(0.0, ItemizationSpecializedStatConfigHelper.rollTypeWeight(ItemizationSpecializedStatConfigHelper.RollType.PERCENT));
        double total = flatWeight + percentWeight;
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
        return ItemizationSpecializedStatConfigHelper.RollType.FLAT;
    }

    private static ItemizationSpecializedStatConfigHelper.RollType resolveRollTypeForStat(ItemizedStat stat,
                                                                                            ThreadLocalRandom random) {
        ItemizationSpecializedStatConfigHelper.RollConstraint constraint =
            ItemizationSpecializedStatConfigHelper.rollConstraintForStat(stat);
        return switch (constraint) {
            case FLAT_ONLY -> ItemizationSpecializedStatConfigHelper.RollType.FLAT;
            case PERCENT_ONLY -> ItemizationSpecializedStatConfigHelper.RollType.PERCENT;
            case EITHER -> weightedRollType(random);
        };
    }

    private static double rollFlatBonus(String itemId,
                                        ItemArchetype archetype,
                                        ItemizedStat stat,
                                        ThreadLocalRandom random) {
        double minScalar = ItemizationSpecializedStatConfigHelper.flatRollMinScalar();
        double maxScalar = ItemizationSpecializedStatConfigHelper.flatRollMaxScalar();
        double scalar = randomBetween(minScalar, maxScalar, random);
        double base = ItemizationSpecializedStatConfigHelper.baseValueForArchetypeStat(archetype, stat);
        double tierScalar = ItemizationSpecializedStatConfigHelper.tierScalar(itemId);
        return round4(base * scalar * tierScalar);
    }

    private static double rollPercentBonus(String itemId, ThreadLocalRandom random) {
        double min = ItemizationSpecializedStatConfigHelper.percentRollMin();
        double max = ItemizationSpecializedStatConfigHelper.percentRollMax();
        double rolled = randomBetween(min, max, random);
        double tierScalar = ItemizationSpecializedStatConfigHelper.tierScalar(itemId);
        double tierFactor = 1.0 + ((tierScalar - 1.0) * Math.max(0.0, ItemizationSpecializedStatConfigHelper.percentRollTierInfluence()));
        rolled *= Math.max(0.10, tierFactor);
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

    private static ItemStack applyDurabilityScaling(ItemStack stack, String itemId, ItemRarity rarity) {
        if (stack == null) {
            return null;
        }

        double baseMaxDurability = stack.getMaxDurability();
        if (baseMaxDurability <= 0d) {
            return stack;
        }

        double currentDurability = stack.getDurability();
        double normalizedDurability = currentDurability <= 0d
            ? 1.0
            : clamp(currentDurability / baseMaxDurability, 0.0, 1.0);

        double tierScalar = ItemizationSpecializedStatConfigHelper.tierScalar(itemId);
        double tierInfluence = ItemizationSpecializedStatConfigHelper.durabilityTierInfluence();
        double tierFactor = 1.0 + ((tierScalar - 1.0) * tierInfluence);
        double tierAdjustedBase = baseMaxDurability * Math.max(0.1, tierFactor);

        double adjustedMaxDurability = DurabilityPolicy.getRarityAdjustedBaseMax(tierAdjustedBase, rarity);
        double adjustedCurrentDurability = adjustedMaxDurability * normalizedDurability;
        return stack.withMaxDurability(adjustedMaxDurability).withDurability(adjustedCurrentDurability);
    }

    private static double clamp(double value, double min, double max) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    private static String scoreSummary(Map<String, Double> rarityDebug) {
        if (rarityDebug == null || rarityDebug.isEmpty()) {
            return "n/a";
        }
        Double score = rarityDebug.get("rarityScoreClamped");
        if (score == null) {
            score = rarityDebug.get("rarityScoreRaw");
        }
        return score == null ? "n/a" : String.format(Locale.US, "%.3f", score);
    }

    private static String formatRows(List<RowSelection> rows) {
        if (rows == null || rows.isEmpty()) {
            return "none";
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < rows.size(); i++) {
            RowSelection row = rows.get(i);
            if (i > 0) {
                out.append("|");
            }
            out.append(row.row())
                .append(":")
                .append(row.lane())
                .append(":")
                .append(row.statId())
                .append(row.rollType() == ItemizationSpecializedStatConfigHelper.RollType.PERCENT ? "%" : "F");
        }
        return out.toString();
    }

    private record RollResults(Map<String, Double> flatRolls,
                               Map<String, Double> percentRolls,
                               List<RowSelection> rowSelections) {
    }

    private record RowSelection(int row,
                                String lane,
                                String statId,
                                ItemizationSpecializedStatConfigHelper.RollType rollType) {
    }
}

