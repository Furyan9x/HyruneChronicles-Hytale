package dev.hytalemodding.origins.dialogue;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.asset.builder.InstructionType;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.Action;

import javax.annotation.Nonnull;
import java.util.EnumSet;

/**
 * 
 */
public class BuilderActionOpenDialogue extends BuilderActionBase {
    @Nonnull
    @Override
    public String getShortDescription() {
        return "Open a registered dialogue tree by NPC id";
    }

    @Nonnull
    @Override
    public String getLongDescription() {
        return "Open a registered dialogue tree by NPC id (npcId)";
    }

    @Nonnull
    @Override
    public Action build(@Nonnull BuilderSupport builderSupport) {
        return new ActionOpenDialogue(this);
    }

    @Nonnull
    @Override
    public BuilderDescriptorState getBuilderDescriptorState() {
        return BuilderDescriptorState.Stable;
    }

    @Nonnull
    @Override
    public BuilderActionOpenDialogue readConfig(@Nonnull JsonElement data) {
        this.requireInstructionType(EnumSet.of(InstructionType.Interaction));
        return this;
    }
}
