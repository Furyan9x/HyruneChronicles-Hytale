package dev.hytalemodding.hyrune.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.hyrune.itemization.GatheringUtilityDropService;
import dev.hytalemodding.hyrune.itemization.ItemGenerationService;
import dev.hytalemodding.hyrune.itemization.ItemizedStatRuntimeContracts;
import dev.hytalemodding.hyrune.system.MiningSpeedSystem;
import dev.hytalemodding.hyrune.system.SkillCombatBonusSystem;
import dev.hytalemodding.hyrune.system.WoodcuttingSpeedSystem;

import javax.annotation.Nonnull;
import java.util.Locale;

/**
 * Shows generation diagnostics counters.
 */
public class ItemDiagCommand extends AbstractPlayerCommand {
    public ItemDiagCommand() {
        super("itemdiag", "Show item generation diagnostics counters.");
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
                           @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref,
                           @Nonnull PlayerRef playerRef,
                           @Nonnull World world) {
        ctx.sendMessage(Message.raw("[ItemDiag] generation.attempts=" + ItemGenerationService.Diagnostics.attemptsBySourceSnapshot()
            + ", ineligible=" + ItemGenerationService.Diagnostics.ineligibleBySourceSnapshot()
            + ", alreadyRolled=" + ItemGenerationService.Diagnostics.alreadyRolledBySourceSnapshot()
            + ", rolled=" + ItemGenerationService.Diagnostics.rolledBySourceSnapshot()));
        ctx.sendMessage(Message.raw("[ItemDiag] generation.rolledByRarity=" + ItemGenerationService.Diagnostics.rolledBySourceAndRaritySnapshot()));
        ctx.sendMessage(Message.raw("[ItemDiag] contract.count=" + ItemizedStatRuntimeContracts.all().size()));

        String checks = String.format(Locale.US,
            "combat[armor=%.1f,crit=%.2f,mana=%.2f,cadence=%d,block=%.2f] "
                + "regen[passiveHpOnly=true] "
                + "movement[speedSoftCap=%.3f] "
                + "gather[mining=%.2f,wood=%.2f] "
                + "gatherLoot[doubleDrop=%s]",
            SkillCombatBonusSystem.computeEffectiveArmorAfterPenetration(1000.0, 100.0),
            SkillCombatBonusSystem.suppressCritChance(0.40, 0.15),
            SkillCombatBonusSystem.computeManaCost(8.0, 0.25),
            SkillCombatBonusSystem.computeCadenceIntervalMs(SkillCombatBonusSystem.BASE_ATTACK_CADENCE_MS, 0.25),
            SkillCombatBonusSystem.computeStaminaDrainMultiplier(0.30),
            dev.hytalemodding.hyrune.bonus.SkillStatBonusApplier.applyItemMovementSpeedSoftCap(0.35),
            MiningSpeedSystem.computeMiningDamageMultiplier(50, 0.25),
            WoodcuttingSpeedSystem.computeWoodcuttingDamageMultiplier(50, 0.25),
            GatheringUtilityDropService.shouldDoubleDrop(1.0, 0.10)
        );
        ctx.sendMessage(Message.raw("[ItemDiag] integration.checks=" + checks));
    }
}
