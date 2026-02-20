package dev.hytalemodding.hyrune.interaction;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.hyrune.npc.NpcProfilerService;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

/**
 * Item interaction entry point that opens the NPC profiler for the currently targeted NPC.
 */
public class NpcProfilerInteraction extends SimpleInstantInteraction {
    public static final BuilderCodec<NpcProfilerInteraction> CODEC = BuilderCodec.builder(
            NpcProfilerInteraction.class,
            NpcProfilerInteraction::new,
            SimpleInstantInteraction.CODEC
        )
        .documentation("Opens the NPC profiler UI from dedicated utility items.")
        .build();

    @Override
    protected void firstRun(@NonNullDecl InteractionType interactionType,
                            com.hypixel.hytale.server.core.entity.InteractionContext interactionContext,
                            @NonNullDecl CooldownHandler cooldownHandler) {
        CommandBuffer<EntityStore> commandBuffer = interactionContext.getCommandBuffer();
        if (commandBuffer == null) {
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        Ref<EntityStore> entityRef = interactionContext.getEntity();
        if (entityRef == null || !entityRef.isValid()) {
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        Player player = commandBuffer.getComponent(entityRef, Player.getComponentType());
        PlayerRef playerRef = commandBuffer.getComponent(entityRef, PlayerRef.getComponentType());
        if (player == null || playerRef == null) {
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        boolean opened = NpcProfilerService.tryOpenNpcProfiler(playerRef, entityRef, commandBuffer.getStore(), player, true);
        if (!opened) {
            interactionContext.getState().state = InteractionState.Failed;
        }
    }
}
