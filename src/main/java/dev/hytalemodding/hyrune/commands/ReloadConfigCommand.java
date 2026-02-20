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
        HyruneConfig.RegenConfig regen = cfg.regen == null ? new HyruneConfig.RegenConfig() : cfg.regen;
        ctx.sendMessage(Message.raw(
            "Hyrune config reloaded."
                + " regen[p:" + fmt(regen.playerHealthRegenPerConstitution) + "/con, cap " + fmt(regen.playerHealthRegenCapPerSecond)
                + " | npc:" + fmt(regen.npcHealthRegenPerLevel) + "/lvl, cap " + fmt(regen.npcHealthRegenCapPerSecond)
                + " | boss:" + fmt(regen.bossHealthRegenPerLevel) + "/lvl, cap " + fmt(regen.bossHealthRegenCapPerSecond) + "]"
                + " durabilityDebugLogging=" + cfg.durabilityDebugLogging
                + ", animalHusbandryGating=" + cfg.enableAnimalHusbandryGating
                + ", seedRules=" + cfg.farmingSeedLevelRequirements.size()
                + ", animalRules=" + cfg.farmingAnimalLevelRequirements.size()
                + ", npcNameOverrides=" + cfg.npcNameOverrides.size()
        ));
    }

    private static String fmt(double value) {
        return String.format(java.util.Locale.US, "%.3f", value);
    }
}
