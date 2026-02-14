package dev.hytalemodding.hyrune.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.hyrune.itemization.EffectiveItemStats;
import dev.hytalemodding.hyrune.itemization.ItemInstanceMetadata;
import dev.hytalemodding.hyrune.itemization.ItemizedStat;
import dev.hytalemodding.hyrune.itemization.ItemStatResolution;
import dev.hytalemodding.hyrune.itemization.ItemStatResolver;

import javax.annotation.Nonnull;
import java.util.Locale;

/**
 * Shows roll and effective-stat detail for the held item.
 */
public class ItemRollsCommand extends AbstractPlayerCommand {
    public ItemRollsCommand() {
        super("itemrolls", "Show roll and effective specialized stat details for the held item.");
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

        Inventory inventory = player.getInventory();
        if (inventory == null) {
            ctx.sendMessage(Message.raw("Inventory is not available."));
            return;
        }

        ItemStack held = inventory.getItemInHand();
        if (held == null || held.isEmpty() || held.getItemId() == null) {
            ctx.sendMessage(Message.raw("No item in hand."));
            return;
        }

        ItemInstanceMetadata metadata = held.getFromMetadataOrNull(ItemInstanceMetadata.KEYED_CODEC);
        if (metadata == null) {
            ctx.sendMessage(Message.raw("[ItemRolls] Held item has no Hyrune itemization metadata."));
            return;
        }

        ItemStatResolution resolution = ItemStatResolver.resolveDetailed(held);
        EffectiveItemStats effective = resolution.getResolvedStats();
        ctx.sendMessage(Message.raw("[ItemRolls] item=" + held.getItemId()
            + ", archetype=" + resolution.getArchetype().getId()
            + ", version=" + metadata.getVersion()
            + ", source=" + metadata.getSource().name()
            + ", rarity=" + metadata.getRarity().name()
            + ", catalyst=" + metadata.getCatalyst().name()
            + ", seed=" + metadata.getSeed()));
        ctx.sendMessage(Message.raw("[ItemRolls] scalars={rarityX=" + val(resolution.getRarityScalar())
            + ", keep=" + val(resolution.getDroppedPenaltyKeep())
            + ", dropPenalty=" + pct(metadata.getDroppedPenalty()) + "}"));
        ctx.sendMessage(Message.raw("[ItemRolls] summary.base={dmg=" + val(resolution.getBaseStats().getDamage())
            + ", def=" + val(resolution.getBaseStats().getDefence())
            + ", heal=" + val(resolution.getBaseStats().getHealingPower())
            + ", util=" + val(resolution.getBaseStats().getUtilityPower()) + "}"));
        ctx.sendMessage(Message.raw("[ItemRolls] summary.effective={dmg=" + val(effective.getDamage())
            + ", def=" + val(effective.getDefence())
            + ", heal=" + val(effective.getHealingPower())
            + ", util=" + val(effective.getUtilityPower()) + "}"));

        for (ItemizedStat stat : ItemizedStat.values()) {
            double flatRoll = metadata.getFlatStatRoll(stat);
            double percentRoll = metadata.getPercentStatRoll(stat);
            if (Math.abs(flatRoll) <= 1e-9 && Math.abs(percentRoll) <= 1e-9) {
                continue;
            }
            double base = resolution.getBaseSpecializedStats().get(stat);
            double resolved = resolution.getResolvedSpecializedStats().get(stat);
            ctx.sendMessage(Message.raw("[ItemRolls] " + stat.getDisplayName()
                + " -> roll=" + formatRoll(stat, flatRoll, percentRoll)
                + ", base=" + val(base)
                + ", effective=" + val(resolved)));
        }
    }

    private static String pct(double value) {
        return String.format(Locale.US, "%.2f%%", value * 100.0);
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

    private static String formatRoll(ItemizedStat stat, double flatRoll, double percentRoll) {
        boolean hasFlat = Math.abs(flatRoll) > 1e-9;
        boolean hasPercent = Math.abs(percentRoll) > 1e-9;
        if (hasFlat && hasPercent) {
            if (preferPercentPrimary(stat)) {
                return pct(percentRoll) + " + " + flat(flatRoll);
            }
            return flat(flatRoll) + " + " + pct(percentRoll);
        }
        if (hasPercent) {
            return pct(percentRoll);
        }
        return flat(flatRoll);
    }

    private static boolean preferPercentPrimary(ItemizedStat stat) {
        return stat != null && stat.isPercentPrimary();
    }
}
