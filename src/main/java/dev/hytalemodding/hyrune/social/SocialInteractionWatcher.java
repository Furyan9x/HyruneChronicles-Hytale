package dev.hytalemodding.hyrune.social;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketWatcher;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import dev.hytalemodding.hyrune.itemization.GemSocketConfigHelper;
import dev.hytalemodding.hyrune.npc.NpcProfilerService;
import dev.hytalemodding.hyrune.ui.SocialMenuPage;
import dev.hytalemodding.hyrune.ui.GemSocketPage;

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

        world.execute(() -> handlePickInteraction(sender, universe));
    }

    private void handlePickInteraction(PlayerRef sender, Universe universe) {
        Ref<EntityStore> senderRef = sender.getReference();
        if (senderRef == null || !senderRef.isValid()) {
            return;
        }

        Store<EntityStore> senderStore = senderRef.getStore();
        Player senderPlayer = senderStore.getComponent(senderRef, Player.getComponentType());
        if (senderPlayer == null) {
            return;
        }

        if (NpcProfilerService.tryOpenNpcProfiler(sender, senderRef, senderStore, senderPlayer, false)) {
            return;
        }

        if (openGemSocketUiIfHoldingGem(sender, senderRef, senderStore, senderPlayer)) {
            return;
        }

        openSocialMenu(sender, universe, senderRef, senderStore, senderPlayer);
    }

    private void openSocialMenu(PlayerRef sender,
                                Universe universe,
                                Ref<EntityStore> senderRef,
                                Store<EntityStore> senderStore,
                                Player senderPlayer) {
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

        senderPlayer.getPageManager().openCustomPage(senderRef, senderStore, new SocialMenuPage(sender, targetPlayerRef));
    }

    private boolean openGemSocketUiIfHoldingGem(PlayerRef sender,
                                                 Ref<EntityStore> senderRef,
                                                 Store<EntityStore> senderStore,
                                                 Player senderPlayer) {
        Inventory inventory = senderPlayer.getInventory();
        if (inventory == null) {
            return false;
        }

        ItemStack held = inventory.getItemInHand();
        if (held == null || held.isEmpty() || held.getItemId() == null) {
            return false;
        }

        String heldItemId = held.getItemId();
        if (!GemSocketConfigHelper.isGemItemId(heldItemId)) {
            return false;
        }

        senderPlayer.getPageManager().openCustomPage(senderRef, senderStore, new GemSocketPage(sender, heldItemId));
        return true;
    }
}
