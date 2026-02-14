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
import dev.hytalemodding.hyrune.itemization.ItemInstanceMetadata;
import dev.hytalemodding.hyrune.itemization.ItemizationEligibilityService;

import javax.annotation.Nonnull;

/**
 * Shows a concise metadata summary for the held item.
 */
public class ItemMetaCommand extends AbstractPlayerCommand {
    public ItemMetaCommand() {
        super("itemmeta", "Show concise metadata for the held item.");
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

        String itemId = held.getItemId();
        ItemInstanceMetadata metadata = held.getFromMetadataOrNull(ItemInstanceMetadata.KEYED_CODEC);
        boolean eligible = ItemizationEligibilityService.isEligible(held);
        ctx.sendMessage(Message.raw("[ItemMeta] item=" + itemId
            + ", eligible=" + eligible
            + ", hasMetadata=" + (metadata != null)));

        if (metadata != null) {
            ctx.sendMessage(Message.raw("[ItemMeta] version=" + metadata.getVersion()
                + ", source=" + metadata.getSource().name()
                + ", rarity=" + metadata.getRarity().name()
                + ", catalyst=" + metadata.getCatalyst().name()
                + ", seed=" + metadata.getSeed()));
        }
        ctx.sendMessage(Message.raw("[ItemMeta] use /itemrolls for roll/effective stats, /itemdiag for generation counters."));
    }
}
