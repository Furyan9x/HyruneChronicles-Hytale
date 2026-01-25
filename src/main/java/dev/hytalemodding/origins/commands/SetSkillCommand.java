package dev.hytalemodding.origins.commands;

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
import dev.hytalemodding.Origins;
import dev.hytalemodding.origins.bonus.SkillStatBonusApplier;
import dev.hytalemodding.origins.level.LevelingService;
import dev.hytalemodding.origins.skills.SkillType;
import dev.hytalemodding.origins.util.NameplateManager;

import javax.annotation.Nonnull;
import java.util.Locale;
import java.util.UUID;

/**
 * Debug/admin command to set a skill level for the executing player.
 * Usage: /setskill <skill> <level>
 */
public class SetSkillCommand extends AbstractPlayerCommand {
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

        LevelingService service = Origins.getService();
        UUID uuid = playerRef.getUuid();
        service.setSkillLevel(uuid, skill, level);
        NameplateManager.update(uuid);
        SkillStatBonusApplier.apply(playerRef);
        SkillStatBonusApplier.applyMovementSpeed(playerRef);

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
}
