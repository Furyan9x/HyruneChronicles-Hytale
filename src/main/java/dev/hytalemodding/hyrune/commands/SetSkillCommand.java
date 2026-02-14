package dev.hytalemodding.hyrune.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.logger.HytaleLogger;
import dev.hytalemodding.Hyrune;
import dev.hytalemodding.hyrune.bonus.SkillStatBonusApplier;
import dev.hytalemodding.hyrune.level.LevelingService;
import dev.hytalemodding.hyrune.quests.QuestManager;
import dev.hytalemodding.hyrune.skills.SkillType;
import dev.hytalemodding.hyrune.util.NameplateManager;

import javax.annotation.Nonnull;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Debug/admin command to set a skill level for the executing player.
 * Usage: /setskill <skill> <level>
 */
public class SetSkillCommand extends AbstractPlayerCommand {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final RequiredArg<String> skillArg;
    private final RequiredArg<Integer> levelArg;

    public SetSkillCommand() {
        super("setskill", "Debug: Set a skill level for your character");
        this.skillArg = withRequiredArg("skill", "Skill name", ArgTypes.STRING);
        this.levelArg = withRequiredArg("level", "Level", ArgTypes.INTEGER);
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
                           @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref,
                           @Nonnull PlayerRef playerRef,
                           @Nonnull World world) {
        String rawSkill = ctx.get(skillArg);
        SkillType skill = parseSkill(rawSkill);
        if (skill == null) {
            ctx.sendMessage(Message.raw("Unknown skill: " + rawSkill));
            return;
        }

        int level = ctx.get(levelArg);

        LevelingService service = Hyrune.getService();
        if (service == null) {
            ctx.sendMessage(Message.raw("Leveling service is not available."));
            return;
        }
        UUID uuid = playerRef.getUuid();
        service.setSkillLevel(uuid, skill, level);
        NameplateManager.update(uuid);
        SkillStatBonusApplier.apply(playerRef);
        SkillStatBonusApplier.applyMovementSpeed(playerRef);
        flushQuestData(uuid);
        ctx.sendMessage(Message.raw("Set " + skill.getDisplayName() + " to level " + level + "."));
    }

    private static SkillType parseSkill(String raw) {
        if (raw == null) {
            return null;
        }

        String normalized = normalize(raw);
        for (SkillType skill : SkillType.values()) {
            if (normalize(skill.name()).equals(normalized)) {
                return skill;
            }
            if (normalize(skill.getDisplayName()).equals(normalized)) {
                return skill;
            }
        }
        return null;
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replace("_", "").replace(" ", "");
    }

    private void flushQuestData(UUID playerUuid) {
        QuestManager qm = QuestManager.get();
        LOGGER.at(Level.FINE).log("Flushing quest data for " + playerUuid);
        qm.unload(playerUuid);
    }
}
