package dev.hytalemodding.hyrune.economy.trade;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackSlotTransaction;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.hyrune.economy.TradeRequestBridge;
import dev.hytalemodding.hyrune.economy.ui.PlayerTradePage;
import dev.hytalemodding.hyrune.economy.ui.TradeRequestPromptPage;
import dev.hytalemodding.hyrune.social.SocialInteractionRules;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime trade orchestration for request flow and active two-player sessions.
 */
public final class PlayerTradeService {
    private final Map<UUID, TradeRequest> pendingByTarget = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> pendingTargetByRequester = new ConcurrentHashMap<>();
    private final Map<UUID, TradeSession> sessionsById = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> sessionByPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerTradePage> openTradePagesByPlayer = new ConcurrentHashMap<>();

    @Nonnull
    public synchronized TradeRequestBridge.TradeRequestResult requestTrade(@Nonnull PlayerRef requester,
                                                                           @Nonnull PlayerRef target) {
        UUID requesterUuid = requester.getUuid();
        UUID targetUuid = target.getUuid();

        if (requesterUuid == null || targetUuid == null || requesterUuid.equals(targetUuid)) {
            return TradeRequestBridge.TradeRequestResult.failure("Invalid trade target.");
        }
        if (!isPlayerOnline(requesterUuid) || !isPlayerOnline(targetUuid)) {
            return TradeRequestBridge.TradeRequestResult.failure("That player is offline.");
        }
        if (!SocialInteractionRules.isWithinInteractionRange(requester, target)) {
            return TradeRequestBridge.TradeRequestResult.failure("You must be within 5 blocks to trade.");
        }
        if (sessionByPlayer.containsKey(requesterUuid) || sessionByPlayer.containsKey(targetUuid)) {
            return TradeRequestBridge.TradeRequestResult.failure("One of those players is already in a trade.");
        }

        UUID existingPendingTarget = pendingTargetByRequester.get(requesterUuid);
        if (existingPendingTarget != null) {
            if (existingPendingTarget.equals(targetUuid)) {
                return TradeRequestBridge.TradeRequestResult.failure("Trade request already sent.");
            }
            return TradeRequestBridge.TradeRequestResult.failure("You already have a pending trade request.");
        }

        TradeRequest existingForTarget = pendingByTarget.get(targetUuid);
        if (existingForTarget != null && !existingForTarget.requesterUuid().equals(requesterUuid)) {
            return TradeRequestBridge.TradeRequestResult.failure("That player already has a pending trade request.");
        }

        String requesterName = safeName(requester);
        String targetName = safeName(target);
        pendingByTarget.put(targetUuid, new TradeRequest(requesterUuid, targetUuid, requesterName));
        pendingTargetByRequester.put(requesterUuid, targetUuid);

        requester.sendMessage(Message.raw("Attempting to trade with " + targetName + "..."));
        target.sendMessage(Message.raw(requesterName + " wishes to trade with you."));
        openPromptPage(target, requesterUuid, requesterName);
        return TradeRequestBridge.TradeRequestResult.success(null);
    }

    public synchronized void respondToRequest(@Nonnull UUID targetUuid, @Nonnull UUID requesterUuid, boolean accepted) {
        TradeRequest pending = pendingByTarget.get(targetUuid);
        if (pending == null || !pending.requesterUuid().equals(requesterUuid)) {
            return;
        }
        pendingByTarget.remove(targetUuid);
        pendingTargetByRequester.remove(requesterUuid);

        PlayerRef targetRef = getOnlinePlayer(targetUuid);
        PlayerRef requesterRef = getOnlinePlayer(requesterUuid);
        if (!accepted) {
            if (requesterRef != null) {
                requesterRef.sendMessage(Message.raw(safeName(targetRef, targetUuid) + " has declined the trade request."));
            }
            return;
        }

        if (targetRef == null || requesterRef == null) {
            if (targetRef != null) {
                targetRef.sendMessage(Message.raw("Trade failed: the other player is no longer online."));
            }
            if (requesterRef != null) {
                requesterRef.sendMessage(Message.raw("Trade failed: the other player is no longer online."));
            }
            return;
        }
        if (sessionByPlayer.containsKey(requesterUuid) || sessionByPlayer.containsKey(targetUuid)) {
            targetRef.sendMessage(Message.raw("Trade failed: one player is already in an active trade."));
            requesterRef.sendMessage(Message.raw("Trade failed: one player is already in an active trade."));
            return;
        }
        if (!SocialInteractionRules.isWithinInteractionRange(requesterRef, targetRef)) {
            targetRef.sendMessage(Message.raw("Trade failed: players must be within 5 blocks."));
            requesterRef.sendMessage(Message.raw("Trade failed: players must be within 5 blocks."));
            return;
        }

        TradeSession session = new TradeSession(UUID.randomUUID(), requesterUuid, targetUuid);
        sessionsById.put(session.sessionId(), session);
        sessionByPlayer.put(requesterUuid, session.sessionId());
        sessionByPlayer.put(targetUuid, session.sessionId());

        requesterRef.sendMessage(Message.raw("Trade started with " + safeName(targetRef) + "."));
        targetRef.sendMessage(Message.raw("Trade started with " + safeName(requesterRef) + "."));
        openTradePage(requesterRef, session.sessionId());
        openTradePage(targetRef, session.sessionId());
    }

    public synchronized void onTradePageOpened(@Nonnull UUID playerUuid, @Nonnull PlayerTradePage page) {
        openTradePagesByPlayer.put(playerUuid, page);
    }

    public synchronized void onTradePageDismissed(@Nonnull UUID sessionId, @Nonnull UUID playerUuid, @Nonnull PlayerTradePage page) {
        PlayerTradePage current = openTradePagesByPlayer.get(playerUuid);
        if (current == page) {
            openTradePagesByPlayer.remove(playerUuid);
        }
        TradeSession session = sessionsById.get(sessionId);
        if (session == null || session.closing()) {
            return;
        }
        cancelSession(session, "Trade cancelled.");
    }

    @Nullable
    public synchronized TradeSnapshot getSnapshotFor(@Nonnull UUID playerUuid, @Nonnull UUID expectedSessionId) {
        TradeSession session = sessionForPlayer(playerUuid);
        if (session == null || !session.sessionId().equals(expectedSessionId)) {
            return null;
        }
        UUID otherUuid = session.other(playerUuid);
        List<TradeOfferEntry> selfOffers = new ArrayList<>(session.offersFor(playerUuid).values());
        List<TradeOfferEntry> otherOffers = new ArrayList<>(session.offersFor(otherUuid).values());
        Set<TradeInventorySlot> offeredSlots = Set.copyOf(session.offersFor(playerUuid).keySet());

        return new TradeSnapshot(
            session.sessionId(),
            playerUuid,
            otherUuid,
            safeName(getOnlinePlayer(otherUuid), otherUuid),
            session.isAccepted(playerUuid),
            session.isAccepted(otherUuid),
            selfOffers,
            otherOffers,
            offeredSlots
        );
    }

    public synchronized void handleInventorySlotClick(@Nonnull UUID playerUuid,
                                                      @Nonnull TradeContainerSection section,
                                                      int slotIndex) {
        TradeSession session = sessionForPlayer(playerUuid);
        if (session == null) {
            return;
        }
        Inventory inventory = resolveInventory(playerUuid);
        if (inventory == null) {
            cancelSession(session, "Trade cancelled: player inventory unavailable.");
            return;
        }
        ItemContainer container = section.resolve(inventory);
        if (container == null || slotIndex < 0 || slotIndex >= container.getCapacity()) {
            return;
        }

        TradeInventorySlot key = new TradeInventorySlot(section, (short) slotIndex);
        LinkedHashMap<TradeInventorySlot, TradeOfferEntry> offers = session.offersFor(playerUuid);
        if (offers.containsKey(key)) {
            offers.remove(key);
            resetAcceptance(session);
            refreshSession(session);
            return;
        }

        ItemStack slotStack = container.getItemStack((short) slotIndex);
        if (slotStack == null || slotStack.isEmpty() || slotStack.getItemId() == null) {
            return;
        }
        offers.put(key, new TradeOfferEntry(key, slotStack, slotStack.getQuantity()));
        resetAcceptance(session);
        refreshSession(session);
    }

    public synchronized void handleOfferSlotClick(@Nonnull UUID playerUuid, int offerIndex) {
        TradeSession session = sessionForPlayer(playerUuid);
        if (session == null) {
            return;
        }
        LinkedHashMap<TradeInventorySlot, TradeOfferEntry> offers = session.offersFor(playerUuid);
        if (offerIndex < 0 || offerIndex >= offers.size()) {
            return;
        }
        TradeInventorySlot key = new ArrayList<>(offers.keySet()).get(offerIndex);
        offers.remove(key);
        resetAcceptance(session);
        refreshSession(session);
    }

    public synchronized void handleAccept(@Nonnull UUID playerUuid) {
        TradeSession session = sessionForPlayer(playerUuid);
        if (session == null) {
            return;
        }
        session.setAccepted(playerUuid, true);
        if (session.bothAccepted()) {
            if (!completeTrade(session)) {
                refreshSession(session);
            }
            return;
        }
        refreshSession(session);
    }

    public synchronized void handleDecline(@Nonnull UUID playerUuid) {
        TradeSession session = sessionForPlayer(playerUuid);
        if (session == null) {
            return;
        }
        cancelSession(session, "Trade declined.");
    }

    public synchronized void handlePlayerDisconnect(@Nullable PlayerRef playerRef) {
        if (playerRef == null || playerRef.getUuid() == null) {
            return;
        }
        UUID playerUuid = playerRef.getUuid();

        UUID pendingTarget = pendingTargetByRequester.remove(playerUuid);
        if (pendingTarget != null) {
            TradeRequest pending = pendingByTarget.remove(pendingTarget);
            if (pending != null) {
                PlayerRef target = getOnlinePlayer(pendingTarget);
                if (target != null) {
                    target.sendMessage(Message.raw("Trade request cancelled."));
                }
            }
        }

        TradeRequest pendingForTarget = pendingByTarget.remove(playerUuid);
        if (pendingForTarget != null) {
            pendingTargetByRequester.remove(pendingForTarget.requesterUuid());
            PlayerRef requester = getOnlinePlayer(pendingForTarget.requesterUuid());
            if (requester != null) {
                requester.sendMessage(Message.raw("Trade request failed: player disconnected."));
            }
        }

        TradeSession session = sessionForPlayer(playerUuid);
        if (session != null) {
            cancelSession(session, "Trade cancelled: other player disconnected.");
        }
    }

    private boolean completeTrade(TradeSession session) {
        UUID leftUuid = session.leftPlayerUuid();
        UUID rightUuid = session.rightPlayerUuid();

        PlayerRef leftRef = getOnlinePlayer(leftUuid);
        PlayerRef rightRef = getOnlinePlayer(rightUuid);
        if (leftRef == null || rightRef == null) {
            cancelSession(session, "Trade cancelled: player disconnected.");
            return true;
        }
        if (!SocialInteractionRules.isWithinInteractionRange(leftRef, rightRef)) {
            leftRef.sendMessage(Message.raw("Trade cancelled: players moved too far apart."));
            rightRef.sendMessage(Message.raw("Trade cancelled: players moved too far apart."));
            cancelSession(session, null);
            return true;
        }

        Inventory leftInventory = resolveInventory(leftUuid);
        Inventory rightInventory = resolveInventory(rightUuid);
        if (leftInventory == null || rightInventory == null) {
            cancelSession(session, "Trade cancelled: inventory unavailable.");
            return true;
        }

        if (!pruneInvalidOffers(session, leftUuid, leftInventory) || !pruneInvalidOffers(session, rightUuid, rightInventory)) {
            resetAcceptance(session);
            leftRef.sendMessage(Message.raw("Trade offer changed. Acceptances reset."));
            rightRef.sendMessage(Message.raw("Trade offer changed. Acceptances reset."));
            return false;
        }

        List<ItemStack> incomingForLeft = extractIncoming(session.offersFor(rightUuid).values());
        List<ItemStack> incomingForRight = extractIncoming(session.offersFor(leftUuid).values());
        if (!canReceiveAfterOffering(leftInventory, session.offersFor(leftUuid).values(), incomingForLeft)) {
            resetAcceptance(session);
            leftRef.sendMessage(Message.raw("You do not have enough inventory space for this trade."));
            rightRef.sendMessage(Message.raw(safeName(leftRef) + " does not have enough inventory space."));
            return false;
        }
        if (!canReceiveAfterOffering(rightInventory, session.offersFor(rightUuid).values(), incomingForRight)) {
            resetAcceptance(session);
            rightRef.sendMessage(Message.raw("You do not have enough inventory space for this trade."));
            leftRef.sendMessage(Message.raw(safeName(rightRef) + " does not have enough inventory space."));
            return false;
        }

        List<RemovedOfferItem> leftRemoved = removeOfferedItems(leftInventory, session.offersFor(leftUuid).values());
        List<RemovedOfferItem> rightRemoved = removeOfferedItems(rightInventory, session.offersFor(rightUuid).values());
        if (leftRemoved == null || rightRemoved == null) {
            restoreRemovedItems(leftInventory, leftRemoved);
            restoreRemovedItems(rightInventory, rightRemoved);
            resetAcceptance(session);
            leftRef.sendMessage(Message.raw("Trade failed while collecting offered items. Acceptances reset."));
            rightRef.sendMessage(Message.raw("Trade failed while collecting offered items. Acceptances reset."));
            return false;
        }

        ItemContainer leftCombined = leftInventory.getCombinedBackpackStorageHotbar();
        ItemContainer rightCombined = rightInventory.getCombinedBackpackStorageHotbar();
        if (leftCombined == null || rightCombined == null) {
            restoreRemovedItems(leftInventory, leftRemoved);
            restoreRemovedItems(rightInventory, rightRemoved);
            cancelSession(session, "Trade cancelled: inventory unavailable.");
            return true;
        }

        if (!addAll(rightCombined, leftRemoved) || !addAll(leftCombined, rightRemoved)) {
            restoreRemovedItems(leftInventory, leftRemoved);
            restoreRemovedItems(rightInventory, rightRemoved);
            resetAcceptance(session);
            leftRef.sendMessage(Message.raw("Trade failed while transferring items. Acceptances reset."));
            rightRef.sendMessage(Message.raw("Trade failed while transferring items. Acceptances reset."));
            return false;
        }

        leftRef.sendMessage(Message.raw("Trade completed successfully."));
        rightRef.sendMessage(Message.raw("Trade completed successfully."));
        closeSession(session, null, null);
        return true;
    }

    private boolean canReceiveAfterOffering(Inventory inventory,
                                            Collection<TradeOfferEntry> ownOffers,
                                            List<ItemStack> incoming) {
        ItemContainer backpack = cloneContainer(inventory.getBackpack());
        ItemContainer storage = cloneContainer(inventory.getStorage());
        ItemContainer hotbar = cloneContainer(inventory.getHotbar());
        if (backpack == null || storage == null || hotbar == null) {
            return false;
        }

        for (TradeOfferEntry offer : ownOffers) {
            ItemContainer sectionContainer = offer.slot().section().resolve(backpack, storage, hotbar);
            if (sectionContainer == null) {
                return false;
            }
            ItemStackSlotTransaction removeTx =
                sectionContainer.removeItemStackFromSlot(
                    offer.slot().slot(),
                    offer.expectedStack(),
                    offer.quantity(),
                    true,
                    true
                );
            if (removeTx == null || !removeTx.succeeded() || ItemStack.isEmpty(removeTx.getOutput())) {
                return false;
            }
        }

        CombinedItemContainer simulated = new CombinedItemContainer(backpack, storage, hotbar);
        for (ItemStack stack : incoming) {
            ItemStackTransaction addTx = simulated.addItemStack(stack, true, false, true);
            ItemStack remainder = addTx == null ? stack : addTx.getRemainder();
            if (addTx == null || !addTx.succeeded() || (remainder != null && !remainder.isEmpty())) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    private static ItemContainer cloneContainer(@Nullable ItemContainer source) {
        if (source == null) {
            return null;
        }
        try {
            return source.clone();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    @Nullable
    private List<RemovedOfferItem> removeOfferedItems(Inventory inventory, Collection<TradeOfferEntry> offers) {
        List<RemovedOfferItem> removed = new ArrayList<>();
        for (TradeOfferEntry offer : offers) {
            ItemContainer container = offer.slot().section().resolve(inventory);
            if (container == null) {
                return null;
            }
            ItemStackSlotTransaction removeTx =
                container.removeItemStackFromSlot(
                    offer.slot().slot(),
                    offer.expectedStack(),
                    offer.quantity(),
                    true,
                    true
                );
            if (removeTx == null || !removeTx.succeeded() || ItemStack.isEmpty(removeTx.getOutput())) {
                return null;
            }
            removed.add(new RemovedOfferItem(offer.slot(), removeTx.getOutput()));
        }
        return removed;
    }

    private void restoreRemovedItems(@Nullable Inventory inventory, @Nullable List<RemovedOfferItem> removedItems) {
        if (inventory == null || removedItems == null || removedItems.isEmpty()) {
            return;
        }
        ItemContainer combined = inventory.getCombinedBackpackStorageHotbar();
        for (RemovedOfferItem removed : removedItems) {
            ItemStack item = removed.stack();
            if (item == null || item.isEmpty()) {
                continue;
            }
            ItemContainer original = removed.slot().section().resolve(inventory);
            ItemStack remaining = item;
            if (original != null) {
                ItemStackSlotTransaction addToOriginal = original.addItemStackToSlot(removed.slot().slot(), remaining, true, true);
                remaining = addToOriginal == null ? remaining : addToOriginal.getRemainder();
            }
            if (combined != null && remaining != null && !remaining.isEmpty()) {
                remaining = combined.addItemStack(remaining).getRemainder();
            }
        }
    }

    private boolean addAll(ItemContainer destination, List<RemovedOfferItem> removed) {
        for (RemovedOfferItem entry : removed) {
            ItemStack stack = entry.stack();
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            ItemStackTransaction addTx = destination.addItemStack(stack, true, false, true);
            ItemStack remainder = addTx == null ? stack : addTx.getRemainder();
            if (addTx == null || !addTx.succeeded() || (remainder != null && !remainder.isEmpty())) {
                return false;
            }
        }
        return true;
    }

    private boolean pruneInvalidOffers(TradeSession session, UUID ownerUuid, Inventory inventory) {
        boolean valid = true;
        LinkedHashMap<TradeInventorySlot, TradeOfferEntry> offers = session.offersFor(ownerUuid);
        List<TradeInventorySlot> invalid = new ArrayList<>();
        for (Map.Entry<TradeInventorySlot, TradeOfferEntry> entry : offers.entrySet()) {
            if (!isOfferValid(inventory, entry.getValue())) {
                invalid.add(entry.getKey());
                valid = false;
            }
        }
        for (TradeInventorySlot slot : invalid) {
            offers.remove(slot);
        }
        return valid;
    }

    private boolean isOfferValid(Inventory inventory, TradeOfferEntry offer) {
        ItemContainer container = offer.slot().section().resolve(inventory);
        if (container == null) {
            return false;
        }
        ItemStack current = container.getItemStack(offer.slot().slot());
        return current != null
            && !current.isEmpty()
            && ItemStack.isEquivalentType(current, offer.expectedStack())
            && current.getQuantity() >= offer.quantity();
    }

    private List<ItemStack> extractIncoming(Collection<TradeOfferEntry> offers) {
        List<ItemStack> incoming = new ArrayList<>(offers.size());
        for (TradeOfferEntry offer : offers) {
            incoming.add(offer.expectedStack().withQuantity(offer.quantity()));
        }
        return incoming;
    }

    @Nullable
    private TradeSession sessionForPlayer(UUID playerUuid) {
        UUID sessionId = sessionByPlayer.get(playerUuid);
        return sessionId == null ? null : sessionsById.get(sessionId);
    }

    private void resetAcceptance(TradeSession session) {
        session.leftAccepted = false;
        session.rightAccepted = false;
    }

    private void refreshSession(TradeSession session) {
        PlayerTradePage leftPage = openTradePagesByPlayer.get(session.leftPlayerUuid());
        PlayerTradePage rightPage = openTradePagesByPlayer.get(session.rightPlayerUuid());
        if (leftPage != null) {
            leftPage.refreshFromService();
        }
        if (rightPage != null && rightPage != leftPage) {
            rightPage.refreshFromService();
        }
    }

    private void cancelSession(TradeSession session, @Nullable String reason) {
        closeSession(session, reason, reason);
    }

    private void closeSession(TradeSession session, @Nullable String leftMessage, @Nullable String rightMessage) {
        session.closing = true;
        sessionsById.remove(session.sessionId());
        sessionByPlayer.remove(session.leftPlayerUuid());
        sessionByPlayer.remove(session.rightPlayerUuid());

        PlayerRef leftRef = getOnlinePlayer(session.leftPlayerUuid());
        PlayerRef rightRef = getOnlinePlayer(session.rightPlayerUuid());
        if (leftRef != null && leftMessage != null && !leftMessage.isBlank()) {
            leftRef.sendMessage(Message.raw(leftMessage));
        }
        if (rightRef != null && rightMessage != null && !rightMessage.isBlank()) {
            rightRef.sendMessage(Message.raw(rightMessage));
        }

        closeTradePage(leftRef, session.leftPlayerUuid(), session.sessionId());
        closeTradePage(rightRef, session.rightPlayerUuid(), session.sessionId());
    }

    private void closeTradePage(@Nullable PlayerRef playerRef, UUID playerUuid, UUID sessionId) {
        PlayerTradePage openPage = openTradePagesByPlayer.remove(playerUuid);
        if (playerRef == null) {
            return;
        }
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return;
        }
        Store<EntityStore> store = ref.getStore();
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        if (openPage == null || !sessionId.equals(openPage.getSessionId())) {
            return;
        }
        player.getPageManager().setPage(ref, store, com.hypixel.hytale.protocol.packets.interface_.Page.None);
    }

    private void openPromptPage(PlayerRef targetRef, UUID requesterUuid, String requesterName) {
        Ref<EntityStore> ref = targetRef.getReference();
        if (ref == null || !ref.isValid()) {
            return;
        }
        Store<EntityStore> store = ref.getStore();
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        World world = store.getExternalData().getWorld();
        CompletableFuture.runAsync(
            () -> player.getPageManager().openCustomPage(ref, store, new TradeRequestPromptPage(targetRef, requesterUuid, requesterName, this)),
            world
        );
    }

    private void openTradePage(PlayerRef playerRef, UUID sessionId) {
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return;
        }
        Store<EntityStore> store = ref.getStore();
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        World world = store.getExternalData().getWorld();
        CompletableFuture.runAsync(
            () -> player.getPageManager().openCustomPage(ref, store, new PlayerTradePage(playerRef, sessionId, this)),
            world
        );
    }

    private boolean isPlayerOnline(@Nullable UUID playerUuid) {
        return playerUuid != null && getOnlinePlayer(playerUuid) != null;
    }

    @Nullable
    private PlayerRef getOnlinePlayer(@Nullable UUID playerUuid) {
        if (playerUuid == null) {
            return null;
        }
        Universe universe = Universe.get();
        return universe == null ? null : universe.getPlayer(playerUuid);
    }

    @Nullable
    private Inventory resolveInventory(@Nullable UUID playerUuid) {
        PlayerRef playerRef = getOnlinePlayer(playerUuid);
        if (playerRef == null) {
            return null;
        }
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return null;
        }
        Store<EntityStore> store = ref.getStore();
        Player player = store.getComponent(ref, Player.getComponentType());
        return player == null ? null : player.getInventory();
    }

    private static String safeName(@Nullable PlayerRef ref) {
        if (ref == null) {
            return "Unknown";
        }
        String name = ref.getUsername();
        return (name == null || name.isBlank()) ? shortUuid(ref.getUuid()) : name;
    }

    private static String safeName(@Nullable PlayerRef ref, @Nullable UUID fallbackUuid) {
        if (ref != null) {
            return safeName(ref);
        }
        return shortUuid(fallbackUuid);
    }

    private static String shortUuid(@Nullable UUID uuid) {
        if (uuid == null) {
            return "Unknown";
        }
        String value = uuid.toString();
        return value.substring(0, Math.min(8, value.length()));
    }

    private record TradeRequest(UUID requesterUuid, UUID targetUuid, String requesterName) {
    }

    private record RemovedOfferItem(TradeInventorySlot slot, ItemStack stack) {
    }

    private static final class TradeSession {
        private final UUID sessionId;
        private final UUID leftPlayerUuid;
        private final UUID rightPlayerUuid;
        private final LinkedHashMap<TradeInventorySlot, TradeOfferEntry> leftOffers = new LinkedHashMap<>();
        private final LinkedHashMap<TradeInventorySlot, TradeOfferEntry> rightOffers = new LinkedHashMap<>();
        private boolean leftAccepted;
        private boolean rightAccepted;
        private boolean closing;

        private TradeSession(UUID sessionId, UUID leftPlayerUuid, UUID rightPlayerUuid) {
            this.sessionId = sessionId;
            this.leftPlayerUuid = leftPlayerUuid;
            this.rightPlayerUuid = rightPlayerUuid;
        }

        private UUID sessionId() {
            return sessionId;
        }

        private UUID leftPlayerUuid() {
            return leftPlayerUuid;
        }

        private UUID rightPlayerUuid() {
            return rightPlayerUuid;
        }

        private boolean closing() {
            return closing;
        }

        private UUID other(UUID playerUuid) {
            return leftPlayerUuid.equals(playerUuid) ? rightPlayerUuid : leftPlayerUuid;
        }

        private LinkedHashMap<TradeInventorySlot, TradeOfferEntry> offersFor(UUID playerUuid) {
            return leftPlayerUuid.equals(playerUuid) ? leftOffers : rightOffers;
        }

        private void setAccepted(UUID playerUuid, boolean accepted) {
            if (leftPlayerUuid.equals(playerUuid)) {
                leftAccepted = accepted;
            } else {
                rightAccepted = accepted;
            }
        }

        private boolean isAccepted(UUID playerUuid) {
            return leftPlayerUuid.equals(playerUuid) ? leftAccepted : rightAccepted;
        }

        private boolean bothAccepted() {
            return leftAccepted && rightAccepted;
        }
    }

    public enum TradeContainerSection {
        BACKPACK("Backpack"),
        STORAGE("Storage"),
        HOTBAR("Hotbar");

        private final String displayName;

        TradeContainerSection(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }

        @Nullable
        public ItemContainer resolve(@Nullable Inventory inventory) {
            if (inventory == null) {
                return null;
            }
            return switch (this) {
                case BACKPACK -> inventory.getBackpack();
                case STORAGE -> inventory.getStorage();
                case HOTBAR -> inventory.getHotbar();
            };
        }

        @Nullable
        private ItemContainer resolve(@Nullable ItemContainer backpack,
                                      @Nullable ItemContainer storage,
                                      @Nullable ItemContainer hotbar) {
            return switch (this) {
                case BACKPACK -> backpack;
                case STORAGE -> storage;
                case HOTBAR -> hotbar;
            };
        }
    }

    public record TradeInventorySlot(TradeContainerSection section, short slot) {
    }

    public record TradeOfferEntry(TradeInventorySlot slot, ItemStack expectedStack, int quantity) {
    }

    public record TradeSnapshot(UUID sessionId,
                                UUID selfUuid,
                                UUID otherUuid,
                                String otherName,
                                boolean selfAccepted,
                                boolean otherAccepted,
                                List<TradeOfferEntry> selfOffers,
                                List<TradeOfferEntry> otherOffers,
                                Set<TradeInventorySlot> selfOfferedSlots) {
    }
}
