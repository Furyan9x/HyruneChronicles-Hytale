package dev.hytalemodding.origins.interaction;

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
import dev.hytalemodding.origins.ui.RepairBenchPage;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class RepairBenchInteraction extends SimpleInstantInteraction {
    public static final BuilderCodec<RepairBenchInteraction> CODEC = BuilderCodec.builder(
            RepairBenchInteraction.class,
            RepairBenchInteraction::new,
            SimpleInstantInteraction.CODEC
        )
        .documentation("Opens the repair bench UI.")
        .build();

    @Override
    protected void firstRun(@NonNullDecl InteractionType interactionType,
                            com.hypixel.hytale.server.core.entity.InteractionContext context,
                            @NonNullDecl CooldownHandler cooldownHandler) {
        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        if (commandBuffer == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        Ref<EntityStore> entityRef = context.getEntity();
        if (entityRef == null || !entityRef.isValid()) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        Player player = commandBuffer.getComponent(entityRef, Player.getComponentType());
        PlayerRef playerRef = commandBuffer.getComponent(entityRef, PlayerRef.getComponentType());
        if (player == null || playerRef == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        player.getPageManager().openCustomPage(
            entityRef,
            commandBuffer.getStore(),
            new RepairBenchPage(playerRef)
        );
    }
}
