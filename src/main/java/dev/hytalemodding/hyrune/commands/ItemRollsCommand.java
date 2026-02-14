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
import dev.hytalemodding.hyrune.itemization.ItemStatResolver;

import javax.annotation.Nonnull;
import java.util.Locale;

/**
 * Shows roll and effective-stat detail for the held item.
 */
public class ItemRollsCommand extends AbstractPlayerCommand {
    public ItemRollsCommand() {
        super("itemrolls", "Show roll and effective stat details for the held item.");
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

        EffectiveItemStats effective = ItemStatResolver.resolve(held);
        ctx.sendMessage(Message.raw("[ItemRolls] item=" + held.getItemId()
            + ", version=" + metadata.getVersion()
            + ", source=" + metadata.getSource().name()
            + ", rarity=" + metadata.getRarity().name()
            + ", catalyst=" + metadata.getCatalyst().name()
            + ", seed=" + metadata.getSeed()));
        ctx.sendMessage(Message.raw("[ItemRolls] rolls={dmg=" + pct(metadata.getDamageRoll())
            + ", def=" + pct(metadata.getDefenceRoll())
            + ", heal=" + pct(metadata.getHealingRoll())
            + ", util=" + pct(metadata.getUtilityRoll())
            + ", droppedPenalty=" + pct(metadata.getDroppedPenalty()) + "}"));
        ctx.sendMessage(Message.raw("[ItemRolls] effective={dmg=" + val(effective.getDamage())
            + ", def=" + val(effective.getDefence())
            + ", heal=" + val(effective.getHealingPower())
            + ", util=" + val(effective.getUtilityPower()) + "}"));
    }

    private static String pct(double value) {
        return String.format(Locale.US, "%.2f%%", value * 100.0);
    }

    private static String val(double value) {
        return String.format(Locale.US, "%.2f", value);
    }
}

