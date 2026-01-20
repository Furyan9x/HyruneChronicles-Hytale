package dev.hytalemodding.origins.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import dev.hytalemodding.Origins;
import dev.hytalemodding.origins.classes.Classes;
import dev.hytalemodding.origins.playerdata.PlayerLvlData;

import java.util.UUID;
import javax.annotation.Nonnull;

public class CheckLevelCommand extends CommandBase {

    @Nonnull
    private final RequiredArg<PlayerRef> playerArg;

    public CheckLevelCommand() {
        super("checklevel", "Check level and class of a player");
        // Argument: /checklevel <player>
        this.playerArg = this.withRequiredArg("player", "Target Player", ArgTypes.PLAYER_REF);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        var service = Origins.getService();
        if (service == null) {
            ctx.sendMessage(Message.raw("Error: Origins Service not initialized."));
            return;
        }

        PlayerRef targetPlayer = this.playerArg.get(ctx);
        UUID targetUuid = targetPlayer.getUuid();

        // 1. Get Data using the new API
        int globalLvl = service.getAdventurerLevel(targetUuid);
        int classLvl = service.getClassLevel(targetUuid, service.getActiveClassId(targetUuid));
        String classId = service.getActiveClassId(targetUuid);

        String displayClass = "None"; // Default if null
        if (classId != null) {
            Classes rpgClass = Classes.fromId(classId);
            // If the enum exists use DisplayName, otherwise raw ID
            displayClass = (rpgClass != null) ? rpgClass.getDisplayName() : classId;
        }

        // 3. Send Feedback
        ctx.sendMessage(Message.raw("--- " + targetPlayer.getUsername() + " ---"));
        ctx.sendMessage(Message.raw("Global Level: " + globalLvl));

        if (classId != null) {
            ctx.sendMessage(Message.raw("Class: " + displayClass + " (Lvl " + classLvl + ")"));
        } else {
            ctx.sendMessage(Message.raw("Class: None (Adventurer)"));
        }
    }
}