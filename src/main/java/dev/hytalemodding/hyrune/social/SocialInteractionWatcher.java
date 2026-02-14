package dev.hytalemodding.hyrune.social;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketWatcher;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import dev.hytalemodding.hyrune.ui.SocialMenuPage;

/**
 * Opens the Social Menu when a player presses the interaction key on another player.
 */
public class SocialInteractionWatcher implements PlayerPacketWatcher {
    @Override
    public void accept(PlayerRef sender, Packet packet) {
        if (!(packet instanceof SyncInteractionChains chains)) {
            return;
        }

        boolean shouldOpen = false;
        for (SyncInteractionChain chain : chains.updates) {
            if (chain != null && chain.interactionType == InteractionType.Pick) {
                shouldOpen = true;
                break;
            }
        }
        if (!shouldOpen) {
            return;
        }

        Universe universe = Universe.get();
        if (universe == null) {
            return;
        }
        World world = universe.getWorld(sender.getWorldUuid());
        if (world == null) {
            return;
        }

        world.execute(() -> openSocialMenu(sender, universe));
    }

    private void openSocialMenu(PlayerRef sender, Universe universe) {
        Ref<EntityStore> senderRef = sender.getReference();
        if (senderRef == null || !senderRef.isValid()) {
            return;
        }

        Store<EntityStore> senderStore = senderRef.getStore();
        Ref<EntityStore> targetEntityRef = TargetUtil.getTargetEntity(senderRef, senderStore);
        if (targetEntityRef == null || !targetEntityRef.isValid()) {
            return;
        }

        Store<EntityStore> targetStore = targetEntityRef.getStore();
        if (universe == null) {
            return;
        }
        PlayerRef targetPlayerRef = targetStore.getComponent(targetEntityRef, universe.getPlayerRefComponentType());
        if (targetPlayerRef == null || targetPlayerRef.getUuid().equals(sender.getUuid())) {
            return;
        }

        Player senderPlayer = senderStore.getComponent(senderRef, Player.getComponentType());
        if (senderPlayer == null) {
            return;
        }

        senderPlayer.getPageManager().openCustomPage(senderRef, senderStore, new SocialMenuPage(sender, targetPlayerRef));
    }
}
