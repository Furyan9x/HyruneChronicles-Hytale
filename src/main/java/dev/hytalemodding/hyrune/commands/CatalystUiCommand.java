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
import dev.hytalemodding.hyrune.itemization.CatalystAffinity;
import dev.hytalemodding.hyrune.itemization.CatalystCatalog;
import dev.hytalemodding.hyrune.ui.CatalystImbuePage;

import javax.annotation.Nonnull;

/**
 * Debug command to open catalyst UI directly from held catalyst item.
 */
public class CatalystUiCommand extends AbstractPlayerCommand {
    public CatalystUiCommand() {
        super("catalystui", "Open catalyst imbuement UI using held catalyst.");
        this.addAliases("cui");
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
            ctx.sendMessage(Message.raw("Hold a catalyst item and try again."));
            return;
        }

        String heldId = held.getItemId();
        CatalystAffinity affinity = CatalystCatalog.resolveAffinity(heldId);
        if (affinity == CatalystAffinity.NONE) {
            ctx.sendMessage(Message.raw("Held item is not recognized as a catalyst: " + heldId));
            return;
        }

        player.getPageManager().openCustomPage(ref, store, new CatalystImbuePage(playerRef, heldId, affinity));
        ctx.sendMessage(Message.raw("Opened catalyst UI for " + heldId + " (" + affinity.name() + ")."));
    }
}

