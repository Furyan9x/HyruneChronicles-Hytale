package dev.hytalemodding.hyrune.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.hyrune.itemization.ItemGenerationDiagnostics;

import javax.annotation.Nonnull;

/**
 * Shows generation diagnostics counters.
 */
public class ItemDiagCommand extends AbstractPlayerCommand {
    public ItemDiagCommand() {
        super("itemdiag", "Show item generation diagnostics counters.");
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
                           @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref,
                           @Nonnull PlayerRef playerRef,
                           @Nonnull World world) {
        ctx.sendMessage(Message.raw("[ItemDiag] generation.attempts=" + ItemGenerationDiagnostics.attemptsBySourceSnapshot()
            + ", ineligible=" + ItemGenerationDiagnostics.ineligibleBySourceSnapshot()
            + ", alreadyRolled=" + ItemGenerationDiagnostics.alreadyRolledBySourceSnapshot()
            + ", rolled=" + ItemGenerationDiagnostics.rolledBySourceSnapshot()));
        ctx.sendMessage(Message.raw("[ItemDiag] generation.rolledByRarity=" + ItemGenerationDiagnostics.rolledBySourceAndRaritySnapshot()));
    }
}

