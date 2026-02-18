package dev.hytalemodding.hyrune.itemization;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import dev.hytalemodding.hyrune.config.HyruneConfig;
import dev.hytalemodding.hyrune.config.HyruneConfigManager;
import dev.hytalemodding.hyrune.util.PlayerEntityAccess;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Event-driven cache for resolved player itemization stat snapshots.
 */
public final class PlayerItemizationStatsService {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final Map<UUID, PlayerItemizationStats> CACHE = new ConcurrentHashMap<>();
    private static final PlayerItemizationStats EMPTY = new PlayerItemizationStats(
        0L,
        new EffectiveItemStats(0, 0, 0, 0),
        new EffectiveItemStats(0, 0, 0, 0),
        new EffectiveItemStats(0, 0, 0, 0),
        new EffectiveItemStats(0, 0, 0, 0),
        new EffectiveItemStats(0, 0, 0, 0),
        new EffectiveItemStats(0, 0, 0, 0),
        ItemizedStatBlock.empty(),
        ItemizedStatBlock.empty(),
        ItemizedStatBlock.empty(),
        1.0,
        1.0,
        0.0,
        0.0,
        0.0,
        0.0,
        1.0,
        0.0,
        0.0,
        0.0,
        0.0,
        0.0,
        0.0,
        0.0,
        0.0,
        0.0,
        0.0,
        0.0,
        0.0,
        0L
    );

    private PlayerItemizationStatsService() {
    }

    public static PlayerItemizationStats getOrRecompute(Player player) {
        if (player == null) {
            return EMPTY;
        }
        Inventory inventory = player.getInventory();
        if (inventory == null) {
            return EMPTY;
        }

        UUID uuid = PlayerEntityAccess.getPlayerUuid(player);
        if (uuid == null) {
            return EMPTY;
        }
        long fingerprint = computeFingerprint(inventory);
        PlayerItemizationStats cached = CACHE.get(uuid);
        if (cached != null && cached.getEquipmentFingerprint() == fingerprint) {
            return cached;
        }
        return recompute(player, fingerprint);
    }

    public static PlayerItemizationStats recompute(Player player) {
        if (player == null) {
            return EMPTY;
        }
        Inventory inventory = player.getInventory();
        if (inventory == null) {
            return EMPTY;
        }
        UUID playerUuid = PlayerEntityAccess.getPlayerUuid(player);
        if (playerUuid == null) {
            return EMPTY;
        }
        return recompute(player, computeFingerprint(inventory));
    }

    public static PlayerItemizationStats getCached(UUID uuid) {
        if (uuid == null) {
            return EMPTY;
        }
        return CACHE.getOrDefault(uuid, EMPTY);
    }

    public static double getUtilityMoveSpeedBonus(UUID uuid) {
        return getCached(uuid).getItemUtilityMoveSpeedBonus();
    }

    public static void clear(UUID uuid) {
        if (uuid == null) {
            return;
        }
        CACHE.remove(uuid);
    }

    private static PlayerItemizationStats recompute(Player player, long fingerprint) {
        Inventory inventory = player.getInventory();
        if (inventory == null) {
            return EMPTY;
        }
        UUID playerUuid = PlayerEntityAccess.getPlayerUuid(player);
        if (playerUuid == null) {
            return EMPTY;
        }

        ItemStatResolution held = ItemStatResolver.resolveDetailed(inventory.getItemInHand());
        EffectiveItemStats armorBase = new EffectiveItemStats(0, 0, 0, 0);
        EffectiveItemStats armorResolved = new EffectiveItemStats(0, 0, 0, 0);
        ItemizedStatBlock armorSpecialized = ItemizedStatBlock.empty();

        ItemContainer armorContainer = inventory.getArmor();
        if (armorContainer != null) {
            short capacity = armorContainer.getCapacity();
            for (short slot = 0; slot < capacity; slot++) {
                ItemStack stack = armorContainer.getItemStack(slot);
                if (stack == null || stack.isEmpty()) {
                    continue;
                }
                ItemStatResolution res = ItemStatResolver.resolveDetailed(stack);
                armorBase = add(armorBase, res.getBaseStats());
                armorResolved = add(armorResolved, res.getResolvedStats());
                armorSpecialized.addAll(res.getResolvedSpecializedStats());
            }
        }

        EffectiveItemStats heldBase = held.getBaseStats();
        EffectiveItemStats heldResolved = held.getResolvedStats();
        ItemizedStatBlock heldSpecialized = held.getResolvedSpecializedStats();

        EffectiveItemStats totalBase = add(heldBase, armorBase);
        EffectiveItemStats totalResolved = add(heldResolved, armorResolved);

        ItemizedStatBlock totalSpecialized = ItemizedStatBlock.empty();
        totalSpecialized.addAll(heldSpecialized);
        totalSpecialized.addAll(armorSpecialized);
        ItemizedStatBlock defensiveSpecialized = armorSpecialized.copy();
        if (isShieldItem(inventory.getItemInHand())) {
            addShieldDefensiveStats(defensiveSpecialized, heldSpecialized);
        }

        double physicalDamageMultiplier = clamp(
            1.0
                + (heldSpecialized.get(ItemizedStat.PHYSICAL_DAMAGE) * 0.02)
                + (heldSpecialized.get(ItemizedStat.PHYSICAL_PENETRATION) * 0.01)
                + (heldSpecialized.get(ItemizedStat.ATTACK_SPEED) * 0.20),
            0.20,
            4.0
        );
        double magicalDamageMultiplier = clamp(
            1.0
                + (heldSpecialized.get(ItemizedStat.MAGICAL_DAMAGE) * 0.02)
                + (heldSpecialized.get(ItemizedStat.MAGICAL_PENETRATION) * 0.01)
                + (heldSpecialized.get(ItemizedStat.CAST_SPEED) * 0.20),
            0.20,
            4.0
        );

        double physicalDefenceReductionBonus = clamp(
            (defensiveSpecialized.get(ItemizedStat.PHYSICAL_DEFENCE) * 0.004)
                + (defensiveSpecialized.get(ItemizedStat.BLOCK_EFFICIENCY) * 0.010)
                + (defensiveSpecialized.get(ItemizedStat.CRIT_REDUCTION) * 0.005)
                + (defensiveSpecialized.get(ItemizedStat.MAX_HP) * 0.0008)
                + (defensiveSpecialized.get(ItemizedStat.HP_REGEN) * 0.020),
            0.0,
            0.70
        );
        double magicalDefenceReductionBonus = clamp(
            (defensiveSpecialized.get(ItemizedStat.MAGICAL_DEFENCE) * 0.004)
                + (defensiveSpecialized.get(ItemizedStat.BLOCK_EFFICIENCY) * 0.006)
                + (defensiveSpecialized.get(ItemizedStat.CRIT_REDUCTION) * 0.005)
                + (defensiveSpecialized.get(ItemizedStat.MAX_HP) * 0.0008)
                + (defensiveSpecialized.get(ItemizedStat.HP_REGEN) * 0.020),
            0.0,
            0.70
        );

        double physicalCritChanceBonus = clamp(heldSpecialized.get(ItemizedStat.PHYSICAL_CRIT_CHANCE), 0.0, 0.60);
        double magicalCritChanceBonus = clamp(heldSpecialized.get(ItemizedStat.MAGICAL_CRIT_CHANCE), 0.0, 0.60);
        double critBonusMultiplier = clamp(1.0 + heldSpecialized.get(ItemizedStat.CRIT_BONUS), 1.0, 4.0);

        double moveSpeedBonus = clamp(totalSpecialized.get(ItemizedStat.MOVEMENT_SPEED), -0.20, 0.60);
        double attackSpeedBonus = clamp(totalSpecialized.get(ItemizedStat.ATTACK_SPEED), -0.20, 0.80);
        double castSpeedBonus = clamp(totalSpecialized.get(ItemizedStat.CAST_SPEED), -0.20, 0.80);
        double blockBreakSpeedBonus = clamp(totalSpecialized.get(ItemizedStat.BLOCK_BREAK_SPEED), 0.0, 1.50);
        double rareDropChanceBonus = clamp(totalSpecialized.get(ItemizedStat.RARE_DROP_CHANCE), 0.0, 0.75);
        double doubleDropChanceBonus = clamp(totalSpecialized.get(ItemizedStat.DOUBLE_DROP_CHANCE), 0.0, 0.60);
        double manaRegenBonus = Math.max(0.0, totalSpecialized.get(ItemizedStat.MANA_REGEN));
        double staminaRegenBonus = 0.0;
        double hpRegenBonus = Math.max(0.0, totalSpecialized.get(ItemizedStat.HP_REGEN));
        double maxHpBonus = Math.max(0.0, totalSpecialized.get(ItemizedStat.MAX_HP));
        double manaCostReduction = clamp(totalSpecialized.get(ItemizedStat.MANA_COST_REDUCTION), 0.0, 0.75);
        double reflectDamage = Math.max(0.0, defensiveSpecialized.get(ItemizedStat.REFLECT_DAMAGE));

        PlayerItemizationStats out = new PlayerItemizationStats(
            fingerprint,
            heldBase,
            heldResolved,
            armorBase,
            armorResolved,
            totalBase,
            totalResolved,
            heldSpecialized,
            armorSpecialized,
            totalSpecialized,
            physicalDamageMultiplier,
            magicalDamageMultiplier,
            physicalDefenceReductionBonus,
            magicalDefenceReductionBonus,
            physicalCritChanceBonus,
            magicalCritChanceBonus,
            critBonusMultiplier,
            moveSpeedBonus,
            attackSpeedBonus,
            castSpeedBonus,
            blockBreakSpeedBonus,
            rareDropChanceBonus,
            doubleDropChanceBonus,
            manaRegenBonus,
            staminaRegenBonus,
            hpRegenBonus,
            maxHpBonus,
            manaCostReduction,
            reflectDamage,
            System.currentTimeMillis()
        );
        CACHE.put(playerUuid, out);

        HyruneConfig cfg = HyruneConfigManager.getConfig();
        if (cfg != null && cfg.itemizationDebugLogging) {
            LOGGER.at(Level.INFO).log("[Itemization][Stats] p=" + shortUuid(playerUuid)
                + ", dmg=" + fmt(out.getPhysicalDamageMultiplier()) + "/" + fmt(out.getMagicalDamageMultiplier())
                + ", def=" + fmt(out.getPhysicalDefenceReductionBonus()) + "/" + fmt(out.getMagicalDefenceReductionBonus())
                + ", util(move/break/hp)=" + fmt(out.getItemUtilityMoveSpeedBonus())
                + "/" + fmt(out.getItemBlockBreakSpeedBonus())
                + "/" + fmt(out.getItemMaxHpBonus()));
        }

        return out;
    }

    private static long computeFingerprint(Inventory inventory) {
        if (inventory == null) {
            return 0L;
        }
        long hash = 17L;
        hash = hash * 31 + stackHash(inventory.getItemInHand());

        ItemContainer armor = inventory.getArmor();
        if (armor != null) {
            short capacity = armor.getCapacity();
            for (short slot = 0; slot < capacity; slot++) {
                hash = hash * 31 + stackHash(armor.getItemStack(slot));
            }
        }
        return hash;
    }

    private static long stackHash(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItemId() == null) {
            return 0L;
        }

        long hash = stack.getItemId().hashCode();
        hash = hash * 31 + stack.getQuantity();
        ItemInstanceMetadata metadata = stack.getFromMetadataOrNull(ItemInstanceMetadata.KEYED_CODEC);
        if (metadata == null) {
            return hash;
        }
        hash = hash * 31 + metadata.getVersion();
        hash = hash * 31 + safeHash(metadata.getRarityRaw());
        hash = hash * 31 + metadata.getSeed();
        hash = hash * 31 + metadata.getSocketCapacityRaw();
        hash = hash * 31 + safeHash(metadata.getSocketedGemsJson());
        hash = hash * 31 + safeHash(metadata.getStatFlatRollsJson());
        hash = hash * 31 + safeHash(metadata.getStatPercentRollsJson());
        hash = hash * 31 + Double.doubleToLongBits(metadata.getDroppedPenalty());
        return hash;
    }

    private static EffectiveItemStats add(EffectiveItemStats left, EffectiveItemStats right) {
        return new EffectiveItemStats(
            left.getDamage() + right.getDamage(),
            left.getDefence() + right.getDefence(),
            left.getHealingPower() + right.getHealingPower(),
            left.getUtilityPower() + right.getUtilityPower()
        );
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String fmt(double value) {
        return String.format(java.util.Locale.US, "%.4f", value);
    }

    private static int safeHash(String value) {
        return value == null ? 0 : value.hashCode();
    }

    private static String shortUuid(UUID uuid) {
        if (uuid == null) {
            return "null";
        }
        String raw = uuid.toString();
        return raw.length() <= 8 ? raw : raw.substring(0, 8);
    }

    private static boolean isShieldItem(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItemId() == null) {
            return false;
        }
        String id = stack.getItemId().toLowerCase(java.util.Locale.ROOT);
        return id.contains("shield") || id.contains("buckler");
    }

    private static void addShieldDefensiveStats(ItemizedStatBlock destination, ItemizedStatBlock source) {
        if (destination == null || source == null) {
            return;
        }
        destination.add(ItemizedStat.PHYSICAL_DEFENCE, source.get(ItemizedStat.PHYSICAL_DEFENCE));
        destination.add(ItemizedStat.MAGICAL_DEFENCE, source.get(ItemizedStat.MAGICAL_DEFENCE));
        destination.add(ItemizedStat.BLOCK_EFFICIENCY, source.get(ItemizedStat.BLOCK_EFFICIENCY));
        destination.add(ItemizedStat.MAX_HP, source.get(ItemizedStat.MAX_HP));
        destination.add(ItemizedStat.HP_REGEN, source.get(ItemizedStat.HP_REGEN));
        destination.add(ItemizedStat.REFLECT_DAMAGE, source.get(ItemizedStat.REFLECT_DAMAGE));
    }
}

