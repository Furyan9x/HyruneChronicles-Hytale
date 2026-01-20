package dev.hytalemodding.origins.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import dev.hytalemodding.Origins;
import dev.hytalemodding.origins.classes.Classes; // Import needed for display names
import java.util.UUID;
import javax.annotation.Nonnull;

public class AddXpCommand extends CommandBase {

    @Nonnull
    private final RequiredArg<PlayerRef> playerArg;
    @Nonnull
    private final RequiredArg<Integer> xpArg;

    public AddXpCommand() {
        super("addxp", "Add XP to a player");
        this.playerArg = this.withRequiredArg("player", "Target Player", ArgTypes.PLAYER_REF);
        this.xpArg = this.withRequiredArg("amount", "Amount of XP", ArgTypes.INTEGER);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        // 1. Get our Service
        var service = Origins.getService();
        if (service == null) {
            ctx.sendMessage(Message.raw("Error: Origins Service not initialized."));
            return;
        }

        // 2. Extract Arguments safely
        PlayerRef targetPlayer = this.playerArg.get(ctx);
        Integer amount = this.xpArg.get(ctx);
        UUID targetUuid = targetPlayer.getUuid();

        // 3. Logic: Add XP
        service.addCombatXp(targetUuid, amount.longValue());

        // 4. Get Updated Data for Feedback
        int globalLvl = service.getAdventurerLevel(targetUuid);
        String classId = service.getActiveClassId(targetUuid);

        // 5. Build the Message
        StringBuilder feedback = new StringBuilder();
        feedback.append("Gave ").append(amount).append(" XP to ").append(targetPlayer.getUsername()).append(". ");
        feedback.append("Global: ").append(globalLvl);

        // Append Class info if they have one
        if (classId != null) {
            int classLvl = service.getClassLevel(targetUuid, classId);

            // Format nice name (e.g. "mage" -> "Mage")
            Classes rpgClass = Classes.fromId(classId);
            String className = (rpgClass != null) ? rpgClass.getDisplayName() : classId;

            feedback.append(", ").append(className).append(": ").append(classLvl);
        }

        // 6. Send Feedback
        ctx.sendMessage(Message.raw(feedback.toString()));
    }
}