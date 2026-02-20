package dev.hytalemodding.hyrune.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.hyrune.itemization.ItemInstanceMetadata;
import dev.hytalemodding.hyrune.itemization.ItemizationEligibilityService;
import dev.hytalemodding.hyrune.repair.HighAlchemyPolicy;
import dev.hytalemodding.hyrune.repair.ItemRarity;

import javax.annotation.Nonnull;

/**
 * Milestone 7 first-pass high alch loop for held itemized items.
 */
public class HighAlchCommand extends AbstractPlayerCommand {
    public HighAlchCommand() {
        super("highalch", "Convert the held itemized item into coins.");
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
                           @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref,
                           @Nonnull PlayerRef playerRef,
                           @Nonnull World world) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            ctx.sendMessage(Message.raw("Player is unavailable."));
            return;
        }

        Inventory inventory = player.getInventory();
        ItemContainer combined = ItemSinkCommandSupport.combinedContainer(player);
        if (inventory == null || combined == null) {
            ctx.sendMessage(Message.raw("Inventory is unavailable."));
            return;
        }

        ItemStack held = inventory.getItemInHand();
        if (held == null || held.isEmpty() || held.getItemId() == null) {
            ctx.sendMessage(Message.raw("Hold an item to high alch."));
            return;
        }
        if (!ItemizationEligibilityService.isEligible(held)) {
            ctx.sendMessage(Message.raw("Held item is not high-alch eligible."));
            return;
        }
        ItemInstanceMetadata metadata = held.getFromMetadataOrNull(ItemInstanceMetadata.KEYED_CODEC);
        if (metadata == null) {
            ctx.sendMessage(Message.raw("Held item has no itemization metadata."));
            return;
        }

        ItemRarity rarity = metadata.getRarity();
        int coins = HighAlchemyPolicy.estimateCoinReturn(held.getItemId(), rarity);
        if (coins <= 0) {
            ctx.sendMessage(Message.raw("No alchemy value for this item."));
            return;
        }

        if (!ItemSinkCommandSupport.consumeOneFromHand(inventory, held)) {
            ctx.sendMessage(Message.raw("Failed to consume held item."));
            return;
        }

        ItemStack payout = new ItemStack(HighAlchemyPolicy.COIN_ITEM_ID, coins);
        ItemSinkCommandSupport.payoutOrDrop(ref, store, combined, payout);

        ctx.sendMessage(Message.raw("[HighAlch] Consumed 1x " + held.getItemId()
            + " (" + rarity.name() + ") -> " + coins + "x " + HighAlchemyPolicy.COIN_ITEM_ID));
    }
}

