package dev.hytalemodding.hyrune.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.hyrune.config.HyruneConfig;
import dev.hytalemodding.hyrune.config.HyruneConfigManager;

import javax.annotation.Nonnull;

/**
 * Reloads Hyrune runtime gameplay config from disk.
 */
public class ReloadConfigCommand extends AbstractPlayerCommand {

    public ReloadConfigCommand() {
        super("configreload", "Reload Hyrune gameplay config from disk");
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
                           @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref,
                           @Nonnull PlayerRef playerRef,
                           @Nonnull World world) {
        HyruneConfig cfg = HyruneConfigManager.reload();
        ctx.sendMessage(Message.raw(
            "Hyrune config reloaded. durabilityDebugLogging=" + cfg.durabilityDebugLogging
                + ", animalHusbandryGating=" + cfg.enableAnimalHusbandryGating
                + ", seedRules=" + cfg.farmingSeedLevelRequirements.size()
                + ", animalRules=" + cfg.farmingAnimalLevelRequirements.size()
                + ", npcNameOverrides=" + cfg.npcNameOverrides.size()
        ));
    }
}
