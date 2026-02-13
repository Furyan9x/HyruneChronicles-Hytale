package dev.hytalemodding.origins.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.origins.config.OriginsConfig;
import dev.hytalemodding.origins.config.OriginsConfigManager;

import javax.annotation.Nonnull;

/**
 * Reloads Origins runtime gameplay config from disk.
 */
public class ReloadConfigCommand extends AbstractPlayerCommand {

    public ReloadConfigCommand() {
        super("configreload", "Reload Origins gameplay config from disk");
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
                           @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref,
                           @Nonnull PlayerRef playerRef,
                           @Nonnull World world) {
        OriginsConfig cfg = OriginsConfigManager.reload();
        ctx.sendMessage(Message.raw(
            "Origins config reloaded. durabilityDebugLogging=" + cfg.durabilityDebugLogging
                + ", animalHusbandryGating=" + cfg.enableAnimalHusbandryGating
                + ", seedRules=" + cfg.farmingSeedLevelRequirements.size()
                + ", animalRules=" + cfg.farmingAnimalLevelRequirements.size()
                + ", npcNameOverrides=" + cfg.npcNameOverrides.size()
        ));
    }
}
