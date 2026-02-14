package dev.hytalemodding.hyrune.interaction;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.hyrune.itemization.CatalystAffinity;
import dev.hytalemodding.hyrune.itemization.CatalystCatalog;
import dev.hytalemodding.hyrune.ui.CatalystImbuePage;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

/**
 * Custom item interaction for catalyst items.
 */
public class CatalystImbueInteraction extends SimpleInstantInteraction {
    public static final BuilderCodec<CatalystImbueInteraction> CODEC = BuilderCodec.builder(
            CatalystImbueInteraction.class,
            CatalystImbueInteraction::new,
            SimpleInstantInteraction.CODEC
        )
        .documentation("Opens the catalyst imbuement UI from catalyst items.")
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

        ItemStack held = interactionContext.getHeldItem();
        if ((held == null || held.getItemId() == null) && player.getInventory() != null) {
            held = player.getInventory().getItemInHand();
        }
        if (held == null || held.isEmpty() || held.getItemId() == null) {
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        String catalystItemId = held.getItemId();
        CatalystAffinity affinity = CatalystCatalog.resolveAffinity(catalystItemId);
        if (affinity == CatalystAffinity.NONE) {
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        player.getPageManager().openCustomPage(
            entityRef,
            commandBuffer.getStore(),
            new CatalystImbuePage(playerRef, catalystItemId, affinity)
        );
    }
}

