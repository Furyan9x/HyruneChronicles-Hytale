package dev.hytalemodding.hyrune.itemization;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import dev.hytalemodding.hyrune.config.HyruneConfigManager;
import dev.hytalemodding.hyrune.repair.ItemRarity;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

/**
 * Applies rarity/stat rolls to item instances.
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
        ItemInstanceMetadata data = new ItemInstanceMetadata();
        data.setVersion(ItemInstanceMetadata.CURRENT_SCHEMA_VERSION);
        data.setSource(source);
        data.setCatalyst(catalyst);
        data.setRarity(rarity);
        data.setSeed(random.nextLong());
        data.setDamageRoll(rollStatBonus(random));
        data.setDefenceRoll(rollStatBonus(random));
        data.setHealingRoll(rollStatBonus(random));
        data.setUtilityRoll(rollStatBonus(random));
        data.setDroppedPenalty(source == ItemRollSource.DROPPED ? DROPPED_GEAR_STAT_PENALTY : 0d);

        if (HyruneConfigManager.getConfig().itemizationDebugLogging) {
            LOGGER.at(Level.INFO).log("[Itemization] Rolled item="
                + stack.getItemId()
                + ", source=" + source
                + ", rarity=" + data.getRarity().name()
                + ", catalyst=" + data.getCatalyst().name()
                + ", rolls={dmg=" + String.format(Locale.US, "%.3f", data.getDamageRoll())
                + ", def=" + String.format(Locale.US, "%.3f", data.getDefenceRoll())
                + ", heal=" + String.format(Locale.US, "%.3f", data.getHealingRoll())
                + ", util=" + String.format(Locale.US, "%.3f", data.getUtilityRoll())
                + "}"
                + ", rarityDebug=" + rarityDebug);
        }

        return stack.withMetadata(ItemInstanceMetadata.KEYED_CODEC, data);
    }

    private static double rollStatBonus(ThreadLocalRandom random) {
        return random.nextDouble(0.00, 0.12);
    }
}
