package dev.hytalemodding.origins.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import dev.hytalemodding.Origins;
import java.util.UUID;
import javax.annotation.Nonnull;

public class SetCombatLevelCommand extends CommandBase {

    @Nonnull
    private final RequiredArg<PlayerRef> playerArg;
    @Nonnull
    private final RequiredArg<Integer> levelArg;

    public SetCombatLevelCommand() {
        super("setcombatlevel", "Force set a player's combat level");
        // Syntax: /setcombatlevel <player> <level>
        this.playerArg = this.withRequiredArg("player", "Target Player", ArgTypes.PLAYER_REF);
        this.levelArg = this.withRequiredArg("level", "Level (1-100)", ArgTypes.INTEGER);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        var service = Origins.getService();
        if (service == null) {
            ctx.sendMessage(Message.raw("Error: Origins Service not initialized."));
            return;
        }

        PlayerRef targetPlayer = this.playerArg.get(ctx);
        int newLevel = this.levelArg.get(ctx);
        UUID targetUuid = targetPlayer.getUuid();

        // Call the service
        service.setCombatLevel(targetUuid, newLevel);

        // Feedback
        ctx.sendMessage(Message.raw("Set " + targetPlayer.getUsername() + "'s combat level to " + newLevel));
    }
}