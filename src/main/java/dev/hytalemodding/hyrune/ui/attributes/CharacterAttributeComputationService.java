package dev.hytalemodding.hyrune.ui.attributes;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import dev.hytalemodding.hyrune.bonus.SkillStatBonusApplier;
import dev.hytalemodding.hyrune.config.HyruneConfig;
import dev.hytalemodding.hyrune.config.HyruneConfigManager;
import dev.hytalemodding.hyrune.itemization.ItemizedStat;
import dev.hytalemodding.hyrune.itemization.ItemizedStatBlock;
import dev.hytalemodding.hyrune.itemization.PlayerItemizationStats;
import dev.hytalemodding.hyrune.itemization.PlayerItemizationStatsService;
import dev.hytalemodding.hyrune.level.LevelingService;
import dev.hytalemodding.hyrune.registry.FishingRegistry;
import dev.hytalemodding.hyrune.skills.SkillType;
import dev.hytalemodding.hyrune.system.MiningDurabilitySystem;
import dev.hytalemodding.hyrune.system.MiningSpeedSystem;
import dev.hytalemodding.hyrune.system.SkillCombatBonusSystem;
import dev.hytalemodding.hyrune.system.TimedCraftingXpSystem;
import dev.hytalemodding.hyrune.system.WoodcuttingDurabilitySystem;
import dev.hytalemodding.hyrune.system.WoodcuttingSpeedSystem;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Canonical source of truth for Character Menu attribute rows.
 * Centralizes skill + gear formulas to keep overview and details aligned.
 */
public final class CharacterAttributeComputationService {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private CharacterAttributeComputationService() {
    }

    public static AttributeModel compute(UUID uuid,
                                         @Nullable Player player,
                                         @Nullable LevelingService service,
                                         DetailCategory selectedCategory,
                                         @Nullable Double movementSpeedMetersPerSecond) {
        Context ctx = buildContext(uuid, player, service, movementSpeedMetersPerSecond);
        DetailCategory safeCategory = selectedCategory == null ? DetailCategory.OFFENSE : selectedCategory;
        List<AttributeRow> overviewRows = buildOverviewRows(ctx);
        List<AttributeRow> detailRows = buildDetailRows(ctx, safeCategory);
        String hint = detailHint(safeCategory);

        HyruneConfig cfg = HyruneConfigManager.getConfig();
        if (cfg != null && cfg.itemizationDebugLogging) {
            LOGGER.at(Level.INFO).log("[Attributes] category=" + safeCategory.getId()
                + ", overviewRows=" + overviewRows.size()
                + ", detailRows=" + detailRows.size());
        }

        return new AttributeModel(
            safeCategory.getDisplayName(),
            hint,
            overviewRows,
            detailRows
        );
    }

    private static Context buildContext(UUID uuid,
                                        @Nullable Player player,
                                        @Nullable LevelingService service,
                                        @Nullable Double movementSpeedMetersPerSecond) {
        LevelingService safeService = service == null ? LevelingService.get() : service;

        int attack = skill(safeService, uuid, SkillType.ATTACK);
        int strength = skill(safeService, uuid, SkillType.STRENGTH);
        int defence = skill(safeService, uuid, SkillType.DEFENCE);
        int ranged = skill(safeService, uuid, SkillType.RANGED);
        int magic = skill(safeService, uuid, SkillType.MAGIC);
        int constitution = skill(safeService, uuid, SkillType.CONSTITUTION);
        int agility = skill(safeService, uuid, SkillType.AGILITY);
        int mining = skill(safeService, uuid, SkillType.MINING);
        int woodcutting = skill(safeService, uuid, SkillType.WOODCUTTING);
        int fishing = skill(safeService, uuid, SkillType.FISHING);
        int cooking = skill(safeService, uuid, SkillType.COOKING);
        int alchemy = skill(safeService, uuid, SkillType.ALCHEMY);
        int restoration = skill(safeService, uuid, SkillType.RESTORATION);

        PlayerItemizationStats itemStats = player == null
            ? PlayerItemizationStatsService.getCached(uuid)
            : PlayerItemizationStatsService.getOrRecompute(player);
        ItemizedStatBlock heldStats = itemStats.getHeldResolvedSpecialized();
        ItemizedStatBlock totalStats = itemStats.getTotalResolvedSpecialized();

        double skillMeleeDamageBonus = strength * SkillCombatBonusSystem.STRENGTH_DAMAGE_PER_LEVEL;
        double skillRangedDamageBonus = ranged * SkillCombatBonusSystem.RANGED_DAMAGE_PER_LEVEL;
        double skillMagicDamageBonus = magic * SkillCombatBonusSystem.MAGIC_DAMAGE_PER_LEVEL;

        double skillDefenceReduction = Math.min(
            SkillCombatBonusSystem.DEFENCE_DAMAGE_REDUCTION_CAP,
            defence * SkillCombatBonusSystem.DEFENCE_DAMAGE_REDUCTION_PER_LEVEL
        );

        double skillMaxHpBonus = constitution * SkillStatBonusApplier.HEALTH_PER_CONSTITUTION;
        double skillMaxManaBonus = magic * SkillStatBonusApplier.MANA_MAX_PER_MAGIC;
        double skillMaxStaminaBonus = agility * SkillStatBonusApplier.STAMINA_MAX_PER_AGILITY;
        double skillManaRegen = magic * SkillStatBonusApplier.MANA_REGEN_PER_MAGIC;
        double skillStaminaRegen = agility * SkillStatBonusApplier.STAMINA_REGEN_PER_AGILITY;
        double skillMoveSpeedBonus = SkillStatBonusApplier.computeAgilityMovementSpeedBonus(agility);

        double skillMiningSpeed = mining * MiningSpeedSystem.MINING_DAMAGE_PER_LEVEL;
        double skillWoodcuttingSpeed = woodcutting * WoodcuttingSpeedSystem.WOODCUTTING_DAMAGE_PER_LEVEL;
        double skillMiningDurabilityReduction = Math.min(
            MiningDurabilitySystem.MINING_DURABILITY_REDUCTION_CAP,
            mining * MiningDurabilitySystem.MINING_DURABILITY_REDUCTION_PER_LEVEL
        );
        double skillWoodcuttingDurabilityReduction = Math.min(
            WoodcuttingDurabilitySystem.WOODCUTTING_DURABILITY_REDUCTION_CAP,
            woodcutting * WoodcuttingDurabilitySystem.WOODCUTTING_DURABILITY_REDUCTION_PER_LEVEL
        );
        double fishingLevelRatio = Math.min(fishing, 99) / 99.0;
        double skillFishingBiteSpeed = fishingLevelRatio * FishingRegistry.BITE_SPEED_MAX_BONUS;
        double skillFishingRareChance = FishingRegistry.BASE_RARE_CHANCE + (fishingLevelRatio * FishingRegistry.RARE_CHANCE_BONUS);
        double skillCookingDoubleProc = Math.min(
            TimedCraftingXpSystem.DOUBLE_PROC_CHANCE_CAP,
            cooking * TimedCraftingXpSystem.DOUBLE_PROC_CHANCE_PER_LEVEL
        );
        double skillAlchemyDoubleProc = Math.min(
            TimedCraftingXpSystem.DOUBLE_PROC_CHANCE_CAP,
            alchemy * TimedCraftingXpSystem.DOUBLE_PROC_CHANCE_PER_LEVEL
        );
        double skillFarmingSickleXp = dev.hytalemodding.hyrune.system.FarmingHarvestPickupSystem.SICKLE_XP_BONUS - 1.0;

        double gearPhysicalDamageBonus = Math.max(0.0, itemStats.getPhysicalDamageMultiplier() - 1.0);
        double gearMagicDamageBonus = Math.max(0.0, itemStats.getMagicalDamageMultiplier() - 1.0);
        double gearPhysicalCritChance = itemStats.getPhysicalCritChanceBonus();
        double gearMagicalCritChance = itemStats.getMagicalCritChanceBonus();
        double gearCritMultiplierBonus = Math.max(0.0, itemStats.getCritBonusMultiplier() - 1.0);
        double gearCritMultiplier = Math.max(1.0, itemStats.getCritBonusMultiplier());

        double gearPhysicalReduction = itemStats.getPhysicalDefenceReductionBonus();
        double gearMagicalReduction = itemStats.getMagicalDefenceReductionBonus();
        double gearMoveSpeedSoftCapped = SkillStatBonusApplier.applyItemMovementSpeedSoftCap(itemStats.getItemUtilityMoveSpeedBonus());
        double gearBlockBreak = itemStats.getItemBlockBreakSpeedBonus();
        double gearRareDrop = itemStats.getItemRareDropChanceBonus();
        double gearDoubleDrop = itemStats.getItemDoubleDropChanceBonus();
        double gearManaRegen = itemStats.getItemManaRegenBonusPerSecond();
        double gearHpRegen = itemStats.getItemHpRegenBonusPerSecond();
        double gearMaxHp = itemStats.getItemMaxHpBonus();
        double gearManaCostReduction = itemStats.getItemManaCostReduction();
        double gearReflectDamage = itemStats.getItemReflectDamage();
        double currentMoveSpeed = resolveMovementSpeedMetersPerSecond(
            movementSpeedMetersPerSecond,
            skillMoveSpeedBonus,
            gearMoveSpeedSoftCapped
        );

        return new Context(
            attack,
            strength,
            defence,
            ranged,
            magic,
            constitution,
            agility,
            restoration,
            itemStats,
            heldStats,
            totalStats,
            skillMeleeDamageBonus,
            skillRangedDamageBonus,
            skillMagicDamageBonus,
            skillDefenceReduction,
            skillMaxHpBonus,
            skillMaxManaBonus,
            skillMaxStaminaBonus,
            skillManaRegen,
            skillStaminaRegen,
            skillMoveSpeedBonus,
            currentMoveSpeed,
            skillMiningSpeed,
            skillWoodcuttingSpeed,
            skillMiningDurabilityReduction,
            skillWoodcuttingDurabilityReduction,
            skillFishingBiteSpeed,
            skillFishingRareChance,
            skillCookingDoubleProc,
            skillAlchemyDoubleProc,
            skillFarmingSickleXp,
            gearPhysicalDamageBonus,
            gearMagicDamageBonus,
            gearPhysicalCritChance,
            gearMagicalCritChance,
            gearCritMultiplierBonus,
            gearCritMultiplier,
            gearPhysicalReduction,
            gearMagicalReduction,
            gearMoveSpeedSoftCapped,
            gearBlockBreak,
            gearRareDrop,
            gearDoubleDrop,
            gearManaRegen,
            gearHpRegen,
            gearMaxHp,
            gearManaCostReduction,
            gearReflectDamage
        );
    }

    private static List<AttributeRow> buildOverviewRows(Context ctx) {
        List<AttributeRow> rows = new ArrayList<>();
        rows.add(new AttributeRow("Melee Attack", formatFlatStat(meleeAttackRating(ctx))));
        rows.add(new AttributeRow("Ranged Attack", formatFlatStat(rangedAttackRating(ctx))));
        rows.add(new AttributeRow("Magic Attack", formatFlatStat(magicAttackRating(ctx))));
        rows.add(new AttributeRow("Healing Power", formatFlatStat(healingPowerRating(ctx))));
        rows.add(new AttributeRow("Physical Defence", formatFlatStat(physicalDefenceRating(ctx))));
        rows.add(new AttributeRow("Magic Defence", formatFlatStat(magicalDefenceRating(ctx))));
        rows.add(new AttributeRow("Max Health", formatFlatStat(ctx.skillMaxHpBonus() + ctx.gearMaxHp())));
        rows.add(new AttributeRow("Mana Regen / sec", formatFlatStat(ctx.skillManaRegen() + ctx.gearManaRegen())));
        rows.add(new AttributeRow("Movement Speed", formatMovementSpeed(ctx.currentMoveSpeed())));
        rows.add(new AttributeRow(" ", ""));
        rows.add(new AttributeRow("GATHERING", ""));
        rows.add(new AttributeRow("Block Break Speed", formatBonusPercent(ctx.gearBlockBreak())));
        rows.add(new AttributeRow("Rare Drop Chance", formatPercent(ctx.gearRareDrop())));
        rows.add(new AttributeRow("Double Drop Chance", formatPercent(ctx.gearDoubleDrop())));
        return rows;
    }

    private static List<AttributeRow> buildDetailRows(Context ctx, DetailCategory category) {
        List<AttributeRow> rows = new ArrayList<>();
        switch (category) {
            case OFFENSE -> {
                rows.add(new AttributeRow("Melee Damage Bonus", formatBonusPercent(totalMeleeDamageBonus(ctx))));
                rows.add(new AttributeRow("Ranged Damage Bonus", formatBonusPercent(totalRangedDamageBonus(ctx))));
                rows.add(new AttributeRow("Magic Damage Bonus", formatBonusPercent(totalMagicDamageBonus(ctx))));
                rows.add(new AttributeRow("Physical Crit. Chance", formatPercent(clamp(ctx.gearPhysicalCritChance(), 0.0, 0.95))));
                rows.add(new AttributeRow("Magical Crit. Chance", formatPercent(clamp(ctx.gearMagicalCritChance(), 0.0, 0.95))));
                rows.add(new AttributeRow("Crit. Bonus Damage", formatBonusPercent(ctx.gearCritMultiplierBonus())));
                rows.add(new AttributeRow("Physical Penetration", formatBonusPercent(ctx.heldStats().get(ItemizedStat.PHYSICAL_PENETRATION))));
                rows.add(new AttributeRow("Magical Penetration", formatBonusPercent(ctx.heldStats().get(ItemizedStat.MAGICAL_PENETRATION))));
                rows.add(new AttributeRow("Attack Speed", formatBonusPercent(ctx.itemStats().getItemAttackSpeedBonus())));
                rows.add(new AttributeRow("Cast Speed", formatBonusPercent(ctx.itemStats().getItemCastSpeedBonus())));
                rows.add(new AttributeRow("Melee Accuracy (stub)", formatPercent(accuracyStub(ctx.attack()))));
                rows.add(new AttributeRow("Ranged Accuracy (stub)", formatPercent(accuracyStub(ctx.ranged()))));
                rows.add(new AttributeRow("Magic Accuracy (stub)", formatPercent(accuracyStub(ctx.magic()))));
            }
            case DEFENSE -> {
                rows.add(new AttributeRow("Physical Damage Reduction", formatPercent(totalPhysicalReduction(ctx))));
                rows.add(new AttributeRow("Magical Damage Reduction", formatPercent(totalMagicalReduction(ctx))));
                rows.add(new AttributeRow("Defence Reduction", formatPercent(ctx.skillDefenceReduction())));
                rows.add(new AttributeRow("Reduction from Gear (physical)", formatPercent(ctx.gearPhysicalReduction())));
                rows.add(new AttributeRow("Reduction from Gear (magical)", formatPercent(ctx.gearMagicalReduction())));
                rows.add(new AttributeRow("Crit Reduction", formatPercent(ctx.totalStats().get(ItemizedStat.CRIT_REDUCTION))));
                rows.add(new AttributeRow("Block Efficiency", formatPercent(ctx.totalStats().get(ItemizedStat.BLOCK_EFFICIENCY))));
                rows.add(new AttributeRow("Reflect Damage", formatPercent(ctx.gearReflectDamage())));
            }
            case RESOURCES -> {
                rows.add(new AttributeRow("Max Health Bonus", formatFlat(ctx.skillMaxHpBonus() + ctx.gearMaxHp())));
                rows.add(new AttributeRow("Max Mana Bonus", formatFlat(ctx.skillMaxManaBonus())));
                rows.add(new AttributeRow("Max Stamina Bonus", formatFlat(ctx.skillMaxStaminaBonus())));
                rows.add(new AttributeRow("HP Regen / sec", formatFlat(ctx.gearHpRegen())));
                rows.add(new AttributeRow("Mana Regen / sec", formatFlat(ctx.skillManaRegen() + ctx.gearManaRegen())));
                rows.add(new AttributeRow("Stamina Regen / sec", formatFlat(ctx.skillStaminaRegen())));
            }
            case MOBILITY -> {
                rows.add(new AttributeRow("Move Speed Bonus", formatBonusPercent(ctx.skillMoveSpeedBonus() + ctx.gearMoveSpeedSoftCapped())));
                rows.add(new AttributeRow("Move Speed from Agility", formatBonusPercent(ctx.skillMoveSpeedBonus())));
                rows.add(new AttributeRow("Move Speed from Gear", formatBonusPercent(ctx.gearMoveSpeedSoftCapped())));
                rows.add(new AttributeRow("Attack Speed", formatBonusPercent(ctx.itemStats().getItemAttackSpeedBonus())));
                rows.add(new AttributeRow("Cast Speed", formatBonusPercent(ctx.itemStats().getItemCastSpeedBonus())));
            }
            case GATHERING -> {
                rows.add(new AttributeRow("Mining Speed Bonus", formatBonusPercent(totalMiningSpeedBonus(ctx))));
                rows.add(new AttributeRow("Woodcutting Speed Bonus", formatBonusPercent(totalWoodcuttingSpeedBonus(ctx))));
                rows.add(new AttributeRow("Mining Durability Reduction", formatPercent(ctx.skillMiningDurabilityReduction())));
                rows.add(new AttributeRow("Woodcutting Durability Reduction", formatPercent(ctx.skillWoodcuttingDurabilityReduction())));
                rows.add(new AttributeRow("Block Break Speed", formatBonusPercent(ctx.gearBlockBreak())));
                rows.add(new AttributeRow("Fishing Bite Speed Bonus", formatBonusPercent(ctx.skillFishingBiteSpeed())));
                rows.add(new AttributeRow("Fishing Rare Fish Chance", formatPercent(ctx.skillFishingRareChance())));
                rows.add(new AttributeRow("Rare Drop Chance", formatPercent(ctx.gearRareDrop())));
                rows.add(new AttributeRow("Double Drop Chance", formatPercent(ctx.gearDoubleDrop())));
                rows.add(new AttributeRow("Cooking Double Proc", formatPercent(ctx.skillCookingDoubleProc())));
                rows.add(new AttributeRow("Alchemy Double Proc", formatPercent(ctx.skillAlchemyDoubleProc())));
                rows.add(new AttributeRow("Farming Sickle XP Bonus", formatBonusPercent(ctx.skillFarmingSickleXp())));
            }
            case HEALING -> {
                rows.add(new AttributeRow("Healing Power", formatBonusPercent(ctx.totalStats().get(ItemizedStat.HEALING_POWER))));
                rows.add(new AttributeRow("Healing Crit Chance ", formatPercent(ctx.totalStats().get(ItemizedStat.HEALING_CRIT_CHANCE))));
                rows.add(new AttributeRow("Healing Crit Bonus", formatBonusPercent(ctx.totalStats().get(ItemizedStat.HEALING_CRIT_BONUS))));
                rows.add(new AttributeRow("Restoration Scaling (stub)", formatBonusPercent(restorationPotencyStub(ctx.restoration()))));
            }
        }
        return rows;
    }

    private static String detailHint(DetailCategory category) {
        return switch (category) {
            case OFFENSE -> "Crit chance/bonus is gear-driven; accuracy and glancing remain stubbed for the next combat pass.";
            case DEFENSE -> "Reduction totals match active runtime combat mitigation.";
            case RESOURCES -> "Resource rows combine skill baselines with gear bonuses where applicable.";
            case MOBILITY -> "Movement speed uses agility plus gear soft-capped movement speed only.";
            case GATHERING -> "Gathering speed totals reflect skill speed plus gear block-break bonuses.";
            case HEALING -> "Restoration rows are placeholders until Restoration is fully implemented.";
        };
    }

    private static double totalMeleeDamageBonus(Context ctx) {
        return combineMultiplicativeBonus(ctx.skillMeleeDamageBonus(), ctx.gearPhysicalDamageBonus());
    }

    private static double totalRangedDamageBonus(Context ctx) {
        return combineMultiplicativeBonus(ctx.skillRangedDamageBonus(), ctx.gearPhysicalDamageBonus());
    }

    private static double totalMagicDamageBonus(Context ctx) {
        return combineMultiplicativeBonus(ctx.skillMagicDamageBonus(), ctx.gearMagicDamageBonus());
    }

    private static double meleeAttackRating(Context ctx) {
        double base = heldPhysicalBaseDamage(ctx);
        return base * (1.0 + totalMeleeDamageBonus(ctx));
    }

    private static double rangedAttackRating(Context ctx) {
        double base = heldPhysicalBaseDamage(ctx);
        return base * (1.0 + totalRangedDamageBonus(ctx));
    }

    private static double magicAttackRating(Context ctx) {
        double base = heldMagicalBaseDamage(ctx);
        return base * (1.0 + totalMagicDamageBonus(ctx));
    }

    private static double healingPowerRating(Context ctx) {
        return Math.max(0.0, ctx.itemStats().getTotalResolvedStats().getHealingPower());
    }

    private static double physicalDefenceRating(Context ctx) {
        return Math.max(0.0, ctx.totalStats().get(ItemizedStat.PHYSICAL_DEFENCE));
    }

    private static double magicalDefenceRating(Context ctx) {
        return Math.max(0.0, ctx.totalStats().get(ItemizedStat.MAGICAL_DEFENCE));
    }

    private static double heldPhysicalBaseDamage(Context ctx) {
        double specialized = Math.max(0.0, ctx.heldStats().get(ItemizedStat.PHYSICAL_DAMAGE));
        if (specialized > 0.0) {
            return specialized;
        }
        return Math.max(0.0, ctx.itemStats().getHeldResolvedStats().getDamage());
    }

    private static double heldMagicalBaseDamage(Context ctx) {
        return Math.max(0.0, ctx.heldStats().get(ItemizedStat.MAGICAL_DAMAGE));
    }

    private static double totalPhysicalReduction(Context ctx) {
        return clamp(ctx.skillDefenceReduction() + ctx.gearPhysicalReduction(), -0.50, 0.85);
    }

    private static double totalMagicalReduction(Context ctx) {
        return clamp(ctx.skillDefenceReduction() + ctx.gearMagicalReduction(), -0.50, 0.85);
    }

    private static double totalMiningSpeedBonus(Context ctx) {
        return combineMultiplicativeBonus(ctx.skillMiningSpeed(), ctx.gearBlockBreak());
    }

    private static double totalWoodcuttingSpeedBonus(Context ctx) {
        return combineMultiplicativeBonus(ctx.skillWoodcuttingSpeed(), ctx.gearBlockBreak());
    }

    private static double accuracyStub(int level) {
        // Stubbed until glancing/miss combat resolution is implemented.
        double normalized = clamp(level / 99.0, 0.0, 1.0);
        return 0.75 + (normalized * 0.25);
    }

    private static double restorationPotencyStub(int level) {
        // Stubbed until Restoration gameplay implementation.
        return clamp(level / 99.0, 0.0, 1.0) * 0.20;
    }

    private static double resolveMovementSpeedMetersPerSecond(@Nullable Double movementSpeedMetersPerSecond,
                                                              double skillMoveSpeedBonus,
                                                              double gearMoveSpeedSoftCapped) {
        if (movementSpeedMetersPerSecond != null && movementSpeedMetersPerSecond > 0.1) {
            return movementSpeedMetersPerSecond;
        }
        double fallbackBase = 5.5;
        return fallbackBase * Math.max(0.1, 1.0 + skillMoveSpeedBonus + gearMoveSpeedSoftCapped);
    }

    private static int skill(LevelingService service, UUID uuid, SkillType skill) {
        if (service == null || uuid == null || skill == null) {
            return 0;
        }
        return Math.max(0, service.getSkillLevel(uuid, skill));
    }

    private static String formatPercent(double value) {
        return String.format(Locale.US, "%.1f%%", value * 100.0);
    }

    private static String formatBonusPercent(double value) {
        return String.format(Locale.US, "%+.1f%%", value * 100.0);
    }

    private static String formatMultiplier(double value) {
        return String.format(Locale.US, "x%.2f", value);
    }

    private static String formatFlat(double value) {
        return String.format(Locale.US, "%+.2f", value);
    }

    private static String formatFlatStat(double value) {
        double abs = Math.abs(value);
        if (abs >= 1000.0) {
            return String.format(Locale.US, "%.0f", value);
        }
        if (abs >= 100.0) {
            return String.format(Locale.US, "%.1f", value);
        }
        return String.format(Locale.US, "%.2f", value);
    }

    private static String formatMovementSpeed(double value) {
        return String.format(Locale.US, "%.2f m/s", value);
    }

    private static double combineMultiplicativeBonus(double skillBonus, double gearBonus) {
        return ((1.0 + skillBonus) * (1.0 + gearBonus)) - 1.0;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public enum DetailCategory {
        OFFENSE("offense", "Offense"),
        DEFENSE("defense", "Defense"),
        RESOURCES("resources", "Resources"),
        MOBILITY("mobility", "Mobility"),
        GATHERING("gathering", "Gathering"),
        HEALING("healing", "Healing");

        private final String id;
        private final String displayName;

        DetailCategory(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        public String getId() {
            return id;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static DetailCategory fromIdOrDefault(String id, DetailCategory fallback) {
            if (id == null || id.isBlank()) {
                return fallback;
            }
            for (DetailCategory value : values()) {
                if (value.id.equalsIgnoreCase(id)) {
                    return value;
                }
            }
            return fallback;
        }
    }

    public record AttributeRow(String label, String value) {
    }

    public record AttributeModel(String detailHeader,
                                 String detailHint,
                                 List<AttributeRow> overviewRows,
                                 List<AttributeRow> detailRows) {
    }

    private record Context(int attack,
                           int strength,
                           int defence,
                           int ranged,
                           int magic,
                           int constitution,
                           int agility,
                           int restoration,
                           PlayerItemizationStats itemStats,
                           ItemizedStatBlock heldStats,
                           ItemizedStatBlock totalStats,
                           double skillMeleeDamageBonus,
                           double skillRangedDamageBonus,
                           double skillMagicDamageBonus,
                           double skillDefenceReduction,
                           double skillMaxHpBonus,
                           double skillMaxManaBonus,
                           double skillMaxStaminaBonus,
                           double skillManaRegen,
                           double skillStaminaRegen,
                           double skillMoveSpeedBonus,
                           double currentMoveSpeed,
                           double skillMiningSpeed,
                           double skillWoodcuttingSpeed,
                           double skillMiningDurabilityReduction,
                           double skillWoodcuttingDurabilityReduction,
                           double skillFishingBiteSpeed,
                           double skillFishingRareChance,
                           double skillCookingDoubleProc,
                           double skillAlchemyDoubleProc,
                           double skillFarmingSickleXp,
                           double gearPhysicalDamageBonus,
                           double gearMagicDamageBonus,
                           double gearPhysicalCritChance,
                           double gearMagicalCritChance,
                           double gearCritMultiplierBonus,
                           double gearCritMultiplier,
                           double gearPhysicalReduction,
                           double gearMagicalReduction,
                           double gearMoveSpeedSoftCapped,
                           double gearBlockBreak,
                           double gearRareDrop,
                           double gearDoubleDrop,
                           double gearManaRegen,
                           double gearHpRegen,
                           double gearMaxHp,
                           double gearManaCostReduction,
                           double gearReflectDamage) {
    }
}
