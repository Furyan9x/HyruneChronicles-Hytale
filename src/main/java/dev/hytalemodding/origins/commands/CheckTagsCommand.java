package dev.hytalemodding.origins.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * Debug command to check the current DisplayNameComponent (nameplate) of the executing player.
 */
public class CheckTagsCommand extends AbstractPlayerCommand {

    public CheckTagsCommand() {
        super("checktags", "Debug: Display your current nameplate text");
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        DisplayNameComponent comp = store.getComponent(ref, DisplayNameComponent.getComponentType());

        if (comp != null) {
            Message currentName = comp.getDisplayName();
            ctx.sendMessage(Message.raw("Your current nameplate:"));
            ctx.sendMessage(currentName);
        } else {
            ctx.sendMessage(Message.raw("Error: No DisplayNameComponent found!"));
        }
    }
}