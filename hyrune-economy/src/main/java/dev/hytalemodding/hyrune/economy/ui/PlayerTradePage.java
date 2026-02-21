package dev.hytalemodding.hyrune.economy.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.ui.ItemGridSlot;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.hyrune.economy.trade.PlayerTradeService;
import dev.hytalemodding.hyrune.economy.trade.PlayerTradeService.TradeContainerSection;
import dev.hytalemodding.hyrune.economy.trade.PlayerTradeService.TradeInventorySlot;
import dev.hytalemodding.hyrune.economy.trade.PlayerTradeService.TradeOfferEntry;
import dev.hytalemodding.hyrune.economy.trade.PlayerTradeService.TradeSnapshot;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Main player-to-player trading page.
 */
public class PlayerTradePage extends InteractiveCustomUIPage<PlayerTradePage.TradePageData> {
    private static final String UI_PATH = "Pages/PlayerTradePage.ui";
    private static final String ACTION_CLOSE = "Close";
    private static final String ACTION_ACCEPT = "Accept";
    private static final String ACTION_DECLINE = "Decline";
    private static final String ACTION_CLICK_BACKPACK = "ClickBackpack";
    private static final String ACTION_CLICK_STORAGE = "ClickStorage";
    private static final String ACTION_CLICK_HOTBAR = "ClickHotbar";
    private static final String ACTION_CLICK_SELF_OFFER = "ClickSelfOffer";
    private static final String VIRTUAL_ITEM_SEPARATOR = "__hyrunedtt_";

    private final UUID sessionId;
    private final PlayerTradeService tradeService;

    public PlayerTradePage(@Nonnull PlayerRef playerRef,
                           @Nonnull UUID sessionId,
                           @Nonnull PlayerTradeService tradeService) {
        super(playerRef, CustomPageLifetime.CanDismiss, TradePageData.CODEC);
        this.sessionId = sessionId;
        this.tradeService = tradeService;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder commandBuilder,
                      @Nonnull UIEventBuilder eventBuilder,
                      @Nonnull Store<EntityStore> store) {
        tradeService.onTradePageOpened(this.playerRef.getUuid(), this);
        render(ref, store, commandBuilder, eventBuilder);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                @Nonnull TradePageData data) {
        super.handleDataEvent(ref, store, data);
        if (data.button == null) {
            return;
        }

        UUID selfUuid = this.playerRef.getUuid();
        int selectedIndex = resolveSelectedIndex(data);

        switch (data.button) {
            case ACTION_CLOSE:
            case ACTION_DECLINE:
                tradeService.handleDecline(selfUuid);
                return;
            case ACTION_ACCEPT:
                tradeService.handleAccept(selfUuid);
                return;
            case ACTION_CLICK_BACKPACK:
                if (selectedIndex >= 0) {
                    tradeService.handleInventorySlotClick(selfUuid, TradeContainerSection.BACKPACK, selectedIndex);
                }
                return;
            case ACTION_CLICK_STORAGE:
                if (selectedIndex >= 0) {
                    tradeService.handleInventorySlotClick(selfUuid, TradeContainerSection.STORAGE, selectedIndex);
                }
                return;
            case ACTION_CLICK_HOTBAR:
                if (selectedIndex >= 0) {
                    tradeService.handleInventorySlotClick(selfUuid, TradeContainerSection.HOTBAR, selectedIndex);
                }
                return;
            case ACTION_CLICK_SELF_OFFER:
                if (selectedIndex >= 0) {
                    tradeService.handleOfferSlotClick(selfUuid, selectedIndex);
                }
                return;
            default:
        }
    }

    @Override
    public void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        super.onDismiss(ref, store);
        tradeService.onTradePageDismissed(sessionId, this.playerRef.getUuid(), this);
    }

    public void refreshFromService() {
        Ref<EntityStore> ref = this.playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return;
        }
        Store<EntityStore> store = ref.getStore();
        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder evt = new UIEventBuilder();
        render(ref, store, cmd, evt);
        sendUpdate(cmd, evt, false);
    }

    public UUID getSessionId() {
        return sessionId;
    }

    private void render(Ref<EntityStore> ref,
                        Store<EntityStore> store,
                        UICommandBuilder cmd,
                        UIEventBuilder evt) {
        TradeSnapshot snapshot = tradeService.getSnapshotFor(this.playerRef.getUuid(), sessionId);
        if (snapshot == null) {
            close();
            return;
        }

        Player player = store.getComponent(ref, Player.getComponentType());
        Inventory inventory = player == null ? null : player.getInventory();
        if (inventory == null) {
            close();
            return;
        }

        cmd.append(UI_PATH);
        cmd.set("#TradeTitle.Text", "TRADING WITH: " + snapshot.otherName());
        cmd.set("#SelfOfferAccepted.Visible", snapshot.selfAccepted());
        cmd.set("#OtherOfferAccepted.Visible", snapshot.otherAccepted());
        cmd.set("#SelfOfferPanel.Background", snapshot.selfAccepted() ? "#1a4a2f(0.92)" : "#071b2a(0.88)");
        cmd.set("#OtherOfferPanel.Background", snapshot.otherAccepted() ? "#1a4a2f(0.92)" : "#071b2a(0.88)");
        cmd.set("#TradeStateLabel.Text", snapshot.selfAccepted() ? "Waiting for other player..." : "Select items and click Accept.");
        cmd.set("#TradeAccept.Text", snapshot.selfAccepted() ? "Accepted" : "Accept");

        renderOfferGrid(cmd, "#SelfOfferGrid", snapshot.selfOffers(), true);
        renderOfferGrid(cmd, "#OtherOfferGrid", snapshot.otherOffers(), false);
        renderInventoryGrid(cmd, "#BackpackGrid", inventory.getBackpack(), snapshot.selfOfferedSlots(), TradeContainerSection.BACKPACK);
        renderInventoryGrid(cmd, "#StorageGrid", inventory.getStorage(), snapshot.selfOfferedSlots(), TradeContainerSection.STORAGE);
        renderInventoryGrid(cmd, "#HotbarGrid", inventory.getHotbar(), snapshot.selfOfferedSlots(), TradeContainerSection.HOTBAR);

        evt.addEventBinding(CustomUIEventBindingType.Activating, "#TradeClose",
            EventData.of("Button", ACTION_CLOSE), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#TradeAccept",
            EventData.of("Button", ACTION_ACCEPT), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#TradeDecline",
            EventData.of("Button", ACTION_DECLINE), false);
        evt.addEventBinding(CustomUIEventBindingType.SlotClicking, "#SelfOfferGrid",
            EventData.of("Button", ACTION_CLICK_SELF_OFFER), false);
        evt.addEventBinding(CustomUIEventBindingType.SlotClicking, "#BackpackGrid",
            EventData.of("Button", ACTION_CLICK_BACKPACK), false);
        evt.addEventBinding(CustomUIEventBindingType.SlotClicking, "#StorageGrid",
            EventData.of("Button", ACTION_CLICK_STORAGE), false);
        evt.addEventBinding(CustomUIEventBindingType.SlotClicking, "#HotbarGrid",
            EventData.of("Button", ACTION_CLICK_HOTBAR), false);
    }

    private void renderOfferGrid(UICommandBuilder cmd,
                                 String selector,
                                 List<TradeOfferEntry> offers,
                                 boolean removable) {
        ItemGridSlot[] slots = new ItemGridSlot[offers.size()];
        for (int index = 0; index < offers.size(); index++) {
            TradeOfferEntry offer = offers.get(index);
            ItemStack stack = offer.expectedStack().withQuantity(offer.quantity());
            ItemGridSlot slot = new ItemGridSlot(stack);
            slot.setName(safeDisplayName(stack.getItemId()));
            slot.setDescription(offer.slot().section().displayName() + " slot " + offer.slot().slot()
                + "\nQty: " + offer.quantity()
                + (removable ? "\nClick to remove from offer" : ""));
            slot.setActivatable(removable);
            slots[index] = slot;
        }
        cmd.set(selector + ".Slots", slots);
    }

    private void renderInventoryGrid(UICommandBuilder cmd,
                                     String selector,
                                     ItemContainer container,
                                     Set<TradeInventorySlot> offeredSlots,
                                     TradeContainerSection section) {
        if (container == null) {
            cmd.set(selector + ".Slots", new ItemGridSlot[0]);
            return;
        }

        ItemGridSlot[] slots = new ItemGridSlot[container.getCapacity()];
        for (short slotIndex = 0; slotIndex < container.getCapacity(); slotIndex++) {
            ItemStack stack = container.getItemStack(slotIndex);
            if (stack == null || stack.isEmpty() || stack.getItemId() == null) {
                slots[slotIndex] = new ItemGridSlot();
                continue;
            }

            TradeInventorySlot key = new TradeInventorySlot(section, slotIndex);
            boolean offered = offeredSlots.contains(key);
            ItemGridSlot slot = new ItemGridSlot(stack);
            slot.setName(safeDisplayName(stack.getItemId()));
            slot.setDescription("Qty: " + stack.getQuantity()
                + (offered ? "\nAlready in your offer." : "\nClick to add full stack to offer."));
            slot.setItemIncompatible(offered);
            slot.setActivatable(true);
            slots[slotIndex] = slot;
        }

        cmd.set(selector + ".Slots", slots);
    }

    private static int resolveSelectedIndex(TradePageData data) {
        if (data.slotIndex != null) {
            return data.slotIndex;
        }
        if (data.selectedSlotIndex != null) {
            return data.selectedSlotIndex;
        }
        if (data.slot == null) {
            return -1;
        }
        try {
            return Integer.parseInt(data.slot);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static String safeDisplayName(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return "Unknown";
        }
        String value = itemId.trim();
        int virtualIdx = value.toLowerCase(Locale.ROOT).indexOf(VIRTUAL_ITEM_SEPARATOR);
        if (virtualIdx > 0) {
            value = value.substring(0, virtualIdx);
        }
        String[] parts = value.split("_");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            String lower = part.toLowerCase(Locale.ROOT);
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(lower.charAt(0)));
            if (lower.length() > 1) {
                out.append(lower.substring(1));
            }
        }
        String normalized = out.length() == 0 ? value : out.toString();
        return normalized.length() > 34 ? normalized.substring(0, 31) + "..." : normalized;
    }

    public static class TradePageData {
        static final String KEY_BUTTON = "Button";
        static final String KEY_SLOT = "Slot";
        static final String KEY_SLOT_INDEX = "SlotIndex";
        static final String KEY_SELECTED_SLOT_INDEX = "SelectedSlotIndex";

        public static final BuilderCodec<TradePageData> CODEC = BuilderCodec.builder(TradePageData.class, TradePageData::new)
            .addField(new KeyedCodec<>(KEY_BUTTON, Codec.STRING), (d, s) -> d.button = s, d -> d.button)
            .addField(new KeyedCodec<>(KEY_SLOT, Codec.STRING), (d, s) -> d.slot = s, d -> d.slot)
            .addField(new KeyedCodec<>(KEY_SLOT_INDEX, Codec.INTEGER), (d, i) -> d.slotIndex = i, d -> d.slotIndex)
            .addField(new KeyedCodec<>(KEY_SELECTED_SLOT_INDEX, Codec.INTEGER), (d, i) -> d.selectedSlotIndex = i, d -> d.selectedSlotIndex)
            .build();

        private String button;
        private String slot;
        private Integer slotIndex;
        private Integer selectedSlotIndex;
    }
}
