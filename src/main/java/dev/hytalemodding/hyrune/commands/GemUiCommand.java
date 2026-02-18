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
import dev.hytalemodding.hyrune.itemization.GemSocketConfigHelper;
import dev.hytalemodding.hyrune.ui.GemSocketPage;

import javax.annotation.Nonnull;

/**
 * Debug command to open gem socket UI directly from held gem item.
 */
public class GemUiCommand extends AbstractPlayerCommand {
    public GemUiCommand() {
        super("gemui", "Open gem socket UI using held gem.");
        this.addAliases("cui", "gemui", "gui");
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
            ctx.sendMessage(Message.raw("Hold a gem item and try again."));
            return;
        }

        String heldId = held.getItemId();
        if (!GemSocketConfigHelper.isGemItemId(heldId)) {
            ctx.sendMessage(Message.raw("Held item is not recognized as a gem: " + heldId));
            return;
        }

        player.getPageManager().openCustomPage(ref, store, new GemSocketPage(playerRef, heldId));
        ctx.sendMessage(Message.raw("Opened gem socket UI for " + heldId + "."));
    }
}

