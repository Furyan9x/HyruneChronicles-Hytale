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
import dev.hytalemodding.hyrune.itemization.CatalystAffinity;
import dev.hytalemodding.hyrune.itemization.CatalystApplicationService;
import dev.hytalemodding.hyrune.itemization.CatalystNamingResolver;
import dev.hytalemodding.hyrune.itemization.ItemInstanceMetadata;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Catalyst application UI (Milestone 4 phase 1).
 */
public class CatalystImbuePage extends InteractiveCustomUIPage<CatalystImbuePage.CatalystImbueData> {
    private static final String UI_PATH = "Pages/CatalystImbue.ui";
    private static final String ROW_UI = "Pages/catalyst_receiver_row.ui";

    private static final String ACTION_CLOSE = "Close";
    private static final String ACTION_SELECT = "SelectReceiver";
    private static final String ACTION_IMBUE = "Imbue";

    private final String catalystItemId;
    private final CatalystAffinity catalystAffinity;
    private short selectedReceiverSlot = -1;

    public CatalystImbuePage(@Nonnull PlayerRef playerRef, @Nonnull String catalystItemId, @Nonnull CatalystAffinity catalystAffinity) {
        super(playerRef, CustomPageLifetime.CanDismiss, CatalystImbueData.CODEC);
        this.catalystItemId = catalystItemId;
        this.catalystAffinity = catalystAffinity;
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
                                @Nonnull CatalystImbueData data) {
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
            case ACTION_IMBUE:
                handleImbue(ref, store);
                return;
            default:
                return;
        }
    }

    private void handleImbue(Ref<EntityStore> ref, Store<EntityStore> store) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        if (selectedReceiverSlot < 0) {
            this.playerRef.sendMessage(Message.raw("Select a receiver item first."));
            return;
        }

        Inventory inventory = player.getInventory();
        CatalystApplicationService.Result result = CatalystApplicationService.applyCatalystToSlot(
            inventory,
            selectedReceiverSlot,
            catalystItemId,
            catalystAffinity
        );
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
        List<CatalystApplicationService.ReceiverEntry> entries = inventory == null
            ? new ArrayList<>()
            : CatalystApplicationService.findEligibleReceiverEntries(inventory);

        cmd.set("#CatalystIcon.ItemId", catalystItemId);
        cmd.set("#CatalystName.Text", catalystItemId);
        cmd.set("#CatalystAffinity.Text", "Affinity: " + title(catalystAffinity.name()));

        CatalystApplicationService.ReceiverEntry selected = findEntry(entries, selectedReceiverSlot);
        if (selected == null) {
            cmd.set("#ReceiverPreviewName.Text", "Receiver: none");
            cmd.set("#ReceiverPreviewCatalyst.Text", "Current Catalyst: none");
            cmd.set("#ReceiverPreviewImbued.Text", "Imbued Name: -");
        } else {
            ItemInstanceMetadata meta = selected.metadata();
            String previewName = CatalystNamingResolver.resolveDisplayName(selected.stack().getItemId(), catalystAffinity);
            cmd.set("#ReceiverPreviewName.Text", "Receiver: " + selected.stack().getItemId());
            cmd.set("#ReceiverPreviewCatalyst.Text", "Current Catalyst: " + title(meta.getCatalyst().name()));
            cmd.set("#ReceiverPreviewImbued.Text", "Imbued Name: " + previewName);
        }

        cmd.clear("#ReceiverList");
        cmd.set("#ReceiverEmpty.Visible", entries.isEmpty());
        int row = 0;
        for (CatalystApplicationService.ReceiverEntry entry : entries) {
            cmd.append("#ReceiverList", ROW_UI);
            String root = "#ReceiverList[" + row + "]";

            ItemInstanceMetadata meta = entry.metadata();
            cmd.set(root + " #ReceiverName.Text", entry.stack().getItemId());
            cmd.set(root + " #ReceiverRarity.Text", "Rarity: " + title(meta.getRarity().name()));
            cmd.set(root + " #ReceiverCatalyst.Text", "Catalyst: " + title(meta.getCatalyst().name()));
            cmd.set(root + " #ReceiverButton.Text", entry.slot() == selectedReceiverSlot ? "Selected" : "Use");

            evt.addEventBinding(
                CustomUIEventBindingType.Activating,
                root + " #ReceiverButton",
                EventData.of("Button", ACTION_SELECT).append("Slot", String.valueOf(entry.slot())),
                false
            );
            row++;
        }

        evt.addEventBinding(CustomUIEventBindingType.Activating, "#CatalystClose",
            EventData.of("Button", ACTION_CLOSE), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#ImbueButton",
            EventData.of("Button", ACTION_IMBUE), false);
    }

    private static CatalystApplicationService.ReceiverEntry findEntry(List<CatalystApplicationService.ReceiverEntry> entries, short slot) {
        if (slot < 0) {
            return null;
        }
        for (CatalystApplicationService.ReceiverEntry entry : entries) {
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

    public static class CatalystImbueData {
        static final String KEY_BUTTON = "Button";
        static final String KEY_SLOT = "Slot";

        public static final BuilderCodec<CatalystImbueData> CODEC = BuilderCodec.builder(CatalystImbueData.class, CatalystImbueData::new)
            .addField(new KeyedCodec<>(KEY_BUTTON, Codec.STRING), (d, s) -> d.button = s, d -> d.button)
            .addField(new KeyedCodec<>(KEY_SLOT, Codec.STRING), (d, s) -> d.slot = s, d -> d.slot)
            .build();

        private String button;
        private String slot;
    }
}

