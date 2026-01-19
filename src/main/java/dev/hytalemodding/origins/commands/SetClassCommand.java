package dev.hytalemodding.origins.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import dev.hytalemodding.Origins;
import dev.hytalemodding.origins.classes.Classes; // Your Enum
import dev.hytalemodding.origins.level.LevelingService.ClassChangeResult;
import java.util.UUID;
import javax.annotation.Nonnull;

public class SetClassCommand extends CommandBase {

    @Nonnull
    private final RequiredArg<PlayerRef> playerArg;
    @Nonnull
    private final RequiredArg<String> classArg;

    public SetClassCommand() {
        super("setclass", "Change a player's RPG class");
        this.playerArg = this.withRequiredArg("player", "Target Player", ArgTypes.PLAYER_REF);
        this.classArg = this.withRequiredArg("class", "Class Name (e.g. warrior, none)", ArgTypes.STRING);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        var service = Origins.getService();
        if (service == null) {
            ctx.sendMessage(Message.raw("Error: Origins Service not initialized."));
            return;
        }

        PlayerRef targetPlayer = this.playerArg.get(ctx);
        String className = this.classArg.get(ctx);
        UUID targetUuid = targetPlayer.getUuid();

        // 1. Attempt the change
        ClassChangeResult result = service.changeClass(targetUuid, className);

        // 2. Handle the result
        switch (result) {
            case SUCCESS:
                String display = className.equalsIgnoreCase("none") ? "Adventurer" : className;
                ctx.sendMessage(Message.raw("§aSuccess! " + targetPlayer.getUsername() + " is now: " + display));
                break;
            case INVALID_CLASS:
                ctx.sendMessage(Message.raw("§cError: Class '" + className + "' does not exist."));
                break;
            case LEVEL_TOO_LOW:
                ctx.sendMessage(Message.raw("§cError: Level too low to join " + className));
                break;
            case WRONG_PARENT:
                Classes target = Classes.fromId(className);
                String parent = (target != null && target.getParent() != null)
                        ? target.getParent().getDisplayName()
                        : "Unknown";
                ctx.sendMessage(Message.raw("§cError: Must be a " + parent + " to switch to " + className));
                break;
            case ALREADY_THIS_CLASS:
                ctx.sendMessage(Message.raw("§eWarning: Player is already a " + className));
                break;
        }
    }
}