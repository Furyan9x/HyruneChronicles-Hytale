package dev.hytalemodding.hyrune.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.hyrune.skills.SkillType;
import dev.hytalemodding.hyrune.ui.SkillInfoPage;

import javax.annotation.Nonnull;

/**
 * Debug command to open SkillInfoPage directly.
 */
public class SkillInfoCommand extends AbstractPlayerCommand {

    public SkillInfoCommand() {
        super("skillinfo", "Open the Skills Info page directly");
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
                           @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref,
                           @Nonnull PlayerRef playerRef,
                           @Nonnull World world) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            ctx.sendMessage(Message.raw("Unable to open SkillInfo page."));
            return;
        }
        player.getPageManager().openCustomPage(ref, store, new SkillInfoPage(playerRef, SkillType.ATTACK));
    }
}
