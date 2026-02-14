package dev.hytalemodding.hyrune.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.Hyrune;
import dev.hytalemodding.hyrune.social.SocialService;
import dev.hytalemodding.hyrune.ui.SocialMenuPage;

import javax.annotation.Nonnull;
import java.util.Locale;

/**
 * Solo testing harness for social systems.
 * Usage: /socialdebug <seed|clear|open|cycle>
 */
public class SocialDebugCommand extends AbstractPlayerCommand {
    private final RequiredArg<String> modeArg;

    public SocialDebugCommand() {
        super("socialdebug", "Solo social debug harness: seed, clear, open, cycle");
        this.modeArg = withRequiredArg("mode", "seed|clear|open|cycle", ArgTypes.STRING);
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
                           @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref,
                           @Nonnull PlayerRef playerRef,
                           @Nonnull World world) {
        SocialService socialService = Hyrune.getSocialService();
        if (socialService == null) {
            ctx.sendMessage(Message.raw("Social service is not available."));
            return;
        }

        String mode = ctx.get(modeArg);
        if (mode == null) {
            sendUsage(ctx);
            return;
        }

        switch (mode.toLowerCase(Locale.ROOT)) {
            case "seed":
                ctx.sendMessage(Message.raw(socialService.debugSeedSolo(playerRef.getUuid())));
                return;
            case "clear":
                ctx.sendMessage(Message.raw(socialService.debugClearSolo(playerRef.getUuid())));
                return;
            case "cycle":
                ctx.sendMessage(Message.raw(socialService.debugCycleSolo(playerRef.getUuid())));
                return;
            case "open":
                openDebugSocialMenu(ctx, store, ref, playerRef);
                return;
            default:
                sendUsage(ctx);
        }
    }

    private void openDebugSocialMenu(CommandContext ctx,
                                     Store<EntityStore> store,
                                     Ref<EntityStore> ref,
                                     PlayerRef playerRef) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            ctx.sendMessage(Message.raw("Unable to open social debug UI."));
            return;
        }

        player.getPageManager().openCustomPage(ref, store, new SocialMenuPage(playerRef, playerRef));
        ctx.sendMessage(Message.raw("Opened social debug menu (self target)."));
    }

    private void sendUsage(CommandContext ctx) {
        ctx.sendMessage(Message.raw("Usage: /socialdebug <seed|clear|open|cycle>"));
    }
}

