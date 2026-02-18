package dev.hytalemodding.hyrune.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.hyrune.itemization.GemSocketApplicationService;
import dev.hytalemodding.hyrune.itemization.GemSocketConfigHelper;
import dev.hytalemodding.hyrune.itemization.ItemInstanceMetadata;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Gem socket UI.
 */
public class GemSocketPage extends InteractiveCustomUIPage<GemSocketPage.GemSocketData> {
    private static final String UI_PATH = "Pages/GemSocket.ui";
    private static final String ROW_UI = "Pages/gem_receiver_row.ui";

    private static final String ACTION_CLOSE = "Close";
    private static final String ACTION_SELECT = "SelectReceiver";
    private static final String ACTION_SOCKET = "Socket";

    private final String gemItemId;
    private short selectedReceiverSlot = -1;

    public GemSocketPage(@Nonnull PlayerRef playerRef, @Nonnull String gemItemId) {
        super(playerRef, CustomPageLifetime.CanDismiss, GemSocketData.CODEC);
        this.gemItemId = gemItemId;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder commandBuilder,
                      @Nonnull UIEventBuilder eventBuilder,
                      @Nonnull Store<EntityStore> store) {
        commandBuilder.append(UI_PATH);
        render(ref, store, commandBuilder, eventBuilder);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                @Nonnull GemSocketData data) {
        super.handleDataEvent(ref, store, data);
        if (data.button == null) {
            return;
        }

        switch (data.button) {
            case ACTION_CLOSE:
                this.close();
                return;
            case ACTION_SELECT:
                this.selectedReceiverSlot = parseSlot(data.slot);
                rebuild(ref, store);
                return;
            case ACTION_SOCKET:
                handleSocket(ref, store);
                return;
            default:
                return;
        }
    }

    private void handleSocket(Ref<EntityStore> ref, Store<EntityStore> store) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        if (selectedReceiverSlot < 0) {
            this.playerRef.sendMessage(Message.raw("Select a receiver item first."));
            return;
        }

        Inventory inventory = player.getInventory();
        GemSocketApplicationService.Result result = GemSocketApplicationService.applyGemToSlot(inventory, selectedReceiverSlot, gemItemId);
        this.playerRef.sendMessage(Message.raw(result.message()));
        if (result.success()) {
            this.close();
            return;
        }
        rebuild(ref, store);
    }

    private void rebuild(Ref<EntityStore> ref, Store<EntityStore> store) {
        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder evt = new UIEventBuilder();
        render(ref, store, cmd, evt);
        this.sendUpdate(cmd, evt, false);
    }

    private void render(Ref<EntityStore> ref, Store<EntityStore> store, UICommandBuilder cmd, UIEventBuilder evt) {
        Player player = store.getComponent(ref, Player.getComponentType());
        Inventory inventory = player != null ? player.getInventory() : null;
        List<GemSocketApplicationService.ReceiverEntry> entries = inventory == null
            ? new ArrayList<>()
            : GemSocketApplicationService.findEligibleReceiverEntries(inventory);

        cmd.set("#GemIcon.ItemId", gemItemId);
        cmd.set("#GemName.Text", "Gem: " + gemItemId);
        cmd.set("#GemBonus.Text", "Gem Bonus: select a receiver");

        GemSocketApplicationService.ReceiverEntry selected = findEntry(entries, selectedReceiverSlot);
        if (selected == null) {
            cmd.set("#ReceiverPreviewName.Text", "Receiver: none");
            cmd.set("#ReceiverPreviewGem.Text", "Sockets: 0/0");
            cmd.set("#ReceiverPreviewSocketed.Text", "After Socket: -");
        } else {
            ItemInstanceMetadata meta = selected.metadata();
            cmd.set("#ReceiverPreviewName.Text", "Receiver: " + selected.stack().getItemId());
            cmd.set("#ReceiverPreviewGem.Text", "Sockets: " + meta.getSocketedGemCount() + "/" + meta.getSocketCapacity());
            String bonusSummary = GemSocketConfigHelper.describeGemBonusForItem(gemItemId, selected.stack().getItemId());
            cmd.set("#GemBonus.Text", "Gem Bonus: " + bonusSummary);
            cmd.set("#ReceiverPreviewSocketed.Text", "After Socket: " + bonusSummary);
        }

        cmd.clear("#ReceiverList");
        cmd.set("#ReceiverEmpty.Visible", entries.isEmpty());
        int row = 0;
        for (GemSocketApplicationService.ReceiverEntry entry : entries) {
            cmd.append("#ReceiverList", ROW_UI);
            String root = "#ReceiverList[" + row + "]";

            ItemInstanceMetadata meta = entry.metadata();
            cmd.set(root + " #ReceiverName.Text", entry.stack().getItemId());
            cmd.set(root + " #ReceiverRarity.Text", "Rarity: " + title(meta.getRarity().name()));
            cmd.set(root + " #ReceiverGem.Text", "Sockets: " + meta.getSocketedGemCount() + "/" + meta.getSocketCapacity());
            cmd.set(root + " #ReceiverButton.Text", entry.slot() == selectedReceiverSlot ? "Selected" : "Use");

            evt.addEventBinding(
                CustomUIEventBindingType.Activating,
                root + " #ReceiverButton",
                EventData.of("Button", ACTION_SELECT).append("Slot", String.valueOf(entry.slot())),
                false
            );
            row++;
        }

        evt.addEventBinding(CustomUIEventBindingType.Activating, "#GemClose",
            EventData.of("Button", ACTION_CLOSE), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#SocketButton",
            EventData.of("Button", ACTION_SOCKET), false);
    }

    private static GemSocketApplicationService.ReceiverEntry findEntry(List<GemSocketApplicationService.ReceiverEntry> entries, short slot) {
        if (slot < 0) {
            return null;
        }
        for (GemSocketApplicationService.ReceiverEntry entry : entries) {
            if (entry.slot() == slot) {
                return entry;
            }
        }
        return null;
    }

    private static short parseSlot(String slot) {
        if (slot == null) {
            return -1;
        }
        try {
            return Short.parseShort(slot);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static String title(String raw) {
        if (raw == null || raw.isBlank()) {
            return "None";
        }
        String lower = raw.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    public static class GemSocketData {
        static final String KEY_BUTTON = "Button";
        static final String KEY_SLOT = "Slot";

        public static final BuilderCodec<GemSocketData> CODEC = BuilderCodec.builder(GemSocketData.class, GemSocketData::new)
            .addField(new KeyedCodec<>(KEY_BUTTON, Codec.STRING), (d, s) -> d.button = s, d -> d.button)
            .addField(new KeyedCodec<>(KEY_SLOT, Codec.STRING), (d, s) -> d.slot = s, d -> d.slot)
            .build();

        private String button;
        private String slot;
    }
}

