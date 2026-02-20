package dev.hytalemodding.hyrune.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.hyrune.bonus.SkillStatBonusApplier;
import dev.hytalemodding.hyrune.itemization.EffectiveItemStats;
import dev.hytalemodding.hyrune.itemization.ItemizedStat;
import dev.hytalemodding.hyrune.itemization.ItemizedStatRuntimeContracts;
import dev.hytalemodding.hyrune.itemization.PlayerItemizationStats;
import dev.hytalemodding.hyrune.itemization.PlayerItemizationStatsService;

import javax.annotation.Nonnull;
import java.util.Locale;

/**
 * Shows aggregated equipped itemization stats and applied gameplay modifiers.
 */
public class ItemStatsCommand extends AbstractPlayerCommand {
    public ItemStatsCommand() {
        super("itemstats", "Show equipped specialized itemization stats and gameplay modifiers.");
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
                           @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref,
                           @Nonnull PlayerRef playerRef,
                           @Nonnull World world) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            ctx.sendMessage(Message.raw("Player is not available."));
            return;
        }

        PlayerItemizationStats stats = PlayerItemizationStatsService.getOrRecompute(player);
        ctx.sendMessage(Message.raw("[ItemStats] fp=" + stats.getEquipmentFingerprint()
            + ", computedAtMs=" + stats.getComputedAtEpochMs()));
        ctx.sendMessage(Message.raw("[ItemStats] held.base=" + fmt(stats.getHeldBaseStats())
            + ", held.resolved=" + fmt(stats.getHeldResolvedStats())));
        ctx.sendMessage(Message.raw("[ItemStats] armor.base=" + fmt(stats.getArmorBaseStats())
            + ", armor.resolved=" + fmt(stats.getArmorResolvedStats())));
        ctx.sendMessage(Message.raw("[ItemStats] totals.base=" + fmt(stats.getTotalBaseStats())
            + ", totals.resolved=" + fmt(stats.getTotalResolvedStats())));

        ctx.sendMessage(Message.raw("[ItemStats] Applied Modifiers"));
        ctx.sendMessage(Message.raw("  Physical Damage Multiplier: " + val(stats.getPhysicalDamageMultiplier()) + "x"));
        ctx.sendMessage(Message.raw("  Magical Damage Multiplier: " + val(stats.getMagicalDamageMultiplier()) + "x"));
        ctx.sendMessage(Message.raw("  Physical Damage Reduction: " + pct(stats.getPhysicalDefenceReductionBonus())));
        ctx.sendMessage(Message.raw("  Magical Damage Reduction: " + pct(stats.getMagicalDefenceReductionBonus())));
        ctx.sendMessage(Message.raw("  Physical Crit Chance Bonus: " + pct(stats.getPhysicalCritChanceBonus())));
        ctx.sendMessage(Message.raw("  Magical Crit Chance Bonus: " + pct(stats.getMagicalCritChanceBonus())));
        ctx.sendMessage(Message.raw("  Critical Bonus Multiplier: " + val(stats.getCritBonusMultiplier()) + "x"));
        double moveRaw = stats.getItemUtilityMoveSpeedBonus();
        double moveCapped = SkillStatBonusApplier.applyItemMovementSpeedSoftCap(moveRaw);
        ctx.sendMessage(Message.raw("  Movement Speed Bonus: " + pct(moveCapped) + " (raw " + pct(moveRaw) + ")"));
        ctx.sendMessage(Message.raw("  Attack Speed Bonus: " + pct(stats.getItemAttackSpeedBonus())));
        ctx.sendMessage(Message.raw("  Cast Speed Bonus: " + pct(stats.getItemCastSpeedBonus())));
        ctx.sendMessage(Message.raw("  Block Break Speed Bonus: " + pct(stats.getItemBlockBreakSpeedBonus())));
        ctx.sendMessage(Message.raw("  Rare Drop Chance Bonus: " + pct(stats.getItemRareDropChanceBonus())));
        ctx.sendMessage(Message.raw("  Double Drop Chance Bonus: " + pct(stats.getItemDoubleDropChanceBonus())));
        ctx.sendMessage(Message.raw("  Mana Regen Bonus: " + flat(stats.getItemManaRegenBonusPerSecond()) + " /s"));
        ctx.sendMessage(Message.raw("  Stamina Regen Bonus: " + flat(stats.getItemStaminaRegenBonusPerSecond()) + " /s"));
        ctx.sendMessage(Message.raw("  Health Regen Bonus: " + flat(stats.getItemHpRegenBonusPerSecond()) + " /s"));
        ctx.sendMessage(Message.raw("  Max Health Bonus: " + flat(stats.getItemMaxHpBonus())));
        ctx.sendMessage(Message.raw("  Mana Cost Reduction: " + pct(stats.getItemManaCostReduction())));
        ctx.sendMessage(Message.raw("  Reflect Damage: " + pct(stats.getItemReflectDamage())));

        ctx.sendMessage(Message.raw("[ItemStats] Totals (Specialized, Vertical)"));
        for (ItemizedStat stat : ItemizedStat.values()) {
            double value = stats.getTotalResolvedSpecialized().get(stat);
            if (Math.abs(value) <= 1e-9) {
                continue;
            }
            if (preferPercentPrimary(stat)) {
                ctx.sendMessage(Message.raw("  " + stat.getDisplayName() + ": " + pct(value) + " (" + flat(value) + ")"));
            } else {
                ctx.sendMessage(Message.raw("  " + stat.getDisplayName() + ": " + flat(value) + " (" + pct(value) + ")"));
            }
        }

        ctx.sendMessage(Message.raw("[ItemStats] Runtime Consumers (Active Stats)"));
        boolean anyActive = false;
        for (ItemizedStat stat : ItemizedStat.values()) {
            double value = stats.getTotalResolvedSpecialized().get(stat);
            if (Math.abs(value) <= 1e-9) {
                continue;
            }
            ItemizedStatRuntimeContracts.Contract contract = ItemizedStatRuntimeContracts.get(stat);
            if (contract == null) {
                continue;
            }
            anyActive = true;
            String formattedValue = preferPercentPrimary(stat) ? pct(value) : flat(value);
            ctx.sendMessage(Message.raw("  " + stat.getDisplayName() + ": " + formattedValue
                + " -> " + contract.hookLocation()));
        }
        if (!anyActive) {
            ctx.sendMessage(Message.raw("  No active specialized stats."));
        }
    }

    private static String fmt(EffectiveItemStats stats) {
        return "{dmg=" + val(stats.getDamage())
            + ", def=" + val(stats.getDefence())
            + ", heal=" + val(stats.getHealingPower())
            + ", util=" + val(stats.getUtilityPower()) + "}";
    }

    private static String val(double value) {
        return String.format(Locale.US, "%.4f", value);
    }

    private static String flat(double value) {
        double abs = Math.abs(value);
        if (abs >= 100.0) {
            return String.format(Locale.US, "%+.0f", value);
        }
        if (abs >= 10.0) {
            return String.format(Locale.US, "%+.1f", value);
        }
        if (abs >= 1.0) {
            return String.format(Locale.US, "%+.2f", value);
        }
        return String.format(Locale.US, "%+.3f", value);
    }

    private static String pct(double value) {
        return String.format(Locale.US, "%.2f%%", value * 100.0);
    }

    private static boolean preferPercentPrimary(ItemizedStat stat) {
        return stat != null && stat.isPercentPrimary();
    }
}
