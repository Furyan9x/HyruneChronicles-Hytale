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
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Gem socket UI.
 */
public class GemSocketPage extends InteractiveCustomUIPage<GemSocketPage.GemSocketData> {
    private static final String UI_PATH = "Pages/GemSocket.ui";
    private static final String INVENTORY_CARD_ROW_UI = "Pages/gem_inventory_card_row.ui";
    private static final String SOCKETED_ROW_UI = "Pages/gem_socketed_row.ui";
    private static final int CARDS_PER_ROW = 5;

    private static final String ACTION_CLOSE = "Close";
    private static final String ACTION_SELECT_RECEIVER = "SelectReceiver";
    private static final String ACTION_SELECT_MATERIAL = "SelectMaterial";
    private static final String ACTION_SOCKET = "Socket";
    private static final String ACTION_REMOVE = "Remove";
    private static final String ACTION_TAB_ADD = "TabAdd";
    private static final String ACTION_TAB_REMOVE = "TabRemove";
    private static final String TAB_ADD = "Add";
    private static final String TAB_REMOVE = "Remove";
    private static final String GEM_REMOVER_ITEM_ID = "Ingredient_Bar_Copper";
    private static final String VIRTUAL_ITEM_SEPARATOR = "__hyrunedtt_";

    private String selectedGemItemId;
    private String selectedTab = TAB_ADD;
    private short selectedReceiverSlot = -1;
    private String selectedRemoveItemId = GEM_REMOVER_ITEM_ID;

    public GemSocketPage(@Nonnull PlayerRef playerRef, @Nonnull String gemItemId) {
        super(playerRef, CustomPageLifetime.CanDismiss, GemSocketData.CODEC);
        this.selectedGemItemId = gemItemId;
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
            case ACTION_SELECT_RECEIVER:
                this.selectedReceiverSlot = parseSlot(data.slot);
                rebuild(ref, store);
                return;
            case ACTION_SELECT_MATERIAL:
                if (TAB_ADD.equals(this.selectedTab)) {
                    this.selectedGemItemId = data.itemId;
                } else {
                    this.selectedRemoveItemId = data.itemId;
                }
                rebuild(ref, store);
                return;
            case ACTION_TAB_ADD:
                this.selectedTab = TAB_ADD;
                rebuild(ref, store);
                return;
            case ACTION_TAB_REMOVE:
                this.selectedTab = TAB_REMOVE;
                rebuild(ref, store);
                return;
            case ACTION_SOCKET:
                handleSocket(ref, store);
                return;
            case ACTION_REMOVE:
                handleRemove(ref, store);
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
            this.playerRef.sendMessage(Message.raw("Select an equipment item first."));
            rebuild(ref, store);
            return;
        }
        if (selectedGemItemId == null || selectedGemItemId.isBlank()) {
            this.playerRef.sendMessage(Message.raw("Select a gem from the right inventory list."));
            rebuild(ref, store);
            return;
        }

        Inventory inventory = player.getInventory();
        GemSocketApplicationService.Result result =
            GemSocketApplicationService.applyGemToSlot(inventory, selectedReceiverSlot, selectedGemItemId);
        this.playerRef.sendMessage(Message.raw(result.message()));
        rebuild(ref, store);
    }

    private void handleRemove(Ref<EntityStore> ref, Store<EntityStore> store) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        if (selectedReceiverSlot < 0) {
            this.playerRef.sendMessage(Message.raw("Select an equipment item first."));
            rebuild(ref, store);
            return;
        }
        if (selectedRemoveItemId == null || selectedRemoveItemId.isBlank()) {
            this.playerRef.sendMessage(Message.raw("Select a removal material first."));
            rebuild(ref, store);
            return;
        }

        Inventory inventory = player.getInventory();
        GemSocketApplicationService.Result result =
            GemSocketApplicationService.removeAllGemsAndDestroySlot(inventory, selectedReceiverSlot, selectedRemoveItemId);
        this.playerRef.sendMessage(Message.raw(result.message()));
        if (result.success()) {
            selectedReceiverSlot = -1;
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
        ItemContainer container = inventory == null ? null : inventory.getCombinedEverything();

        List<GemSocketApplicationService.ReceiverEntry> equipmentEntries = inventory == null
            ? new ArrayList<>()
            : GemSocketApplicationService.findEligibleReceiverEntries(inventory);
        List<MaterialEntry> gemEntries = buildGemEntries(container);
        List<MaterialEntry> removeEntries = buildRemoveEntries(container);

        ensureSelections(equipmentEntries, gemEntries, removeEntries);

        boolean isAdd = TAB_ADD.equals(this.selectedTab);
        cmd.set("#TabBar.SelectedTab", this.selectedTab);
        cmd.set("#AddActionRow.Visible", isAdd);
        cmd.set("#RemoveActionRow.Visible", !isAdd);

        GemSocketApplicationService.ReceiverEntry selectedReceiver = findReceiverEntry(equipmentEntries, selectedReceiverSlot);
        MaterialEntry selectedMaterial = isAdd
            ? findMaterialById(gemEntries, selectedGemItemId)
            : findMaterialById(removeEntries, selectedRemoveItemId);

        renderTopCards(cmd, selectedReceiver, selectedMaterial, isAdd);
        renderSocketedGemList(cmd, selectedReceiver);
        renderLeftEquipmentInventory(cmd, evt, equipmentEntries);
        renderRightMaterialInventory(cmd, evt, isAdd ? gemEntries : removeEntries, isAdd);

        evt.addEventBinding(CustomUIEventBindingType.Activating, "#GemClose",
            EventData.of("Button", ACTION_CLOSE), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#TabAddBtn",
            EventData.of("Button", ACTION_TAB_ADD), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#TabRemoveBtn",
            EventData.of("Button", ACTION_TAB_REMOVE), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#SocketButton",
            EventData.of("Button", ACTION_SOCKET), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#RemoveButton",
            EventData.of("Button", ACTION_REMOVE), false);
    }

    private void renderTopCards(UICommandBuilder cmd,
                                GemSocketApplicationService.ReceiverEntry receiver,
                                MaterialEntry material,
                                boolean isAdd) {
        if (receiver == null) {
            cmd.set("#TopEquipName.Text", "No equipment selected");
            cmd.set("#TopEquipIcon.Visible", false);
            cmd.set("#ReceiverPreviewGem.Text", "Sockets: 0/0");
            cmd.set("#ReceiverPreviewSocketed.Text", "Selected Effect: -");
        } else {
            String receiverId = receiver.stack().getItemId();
            cmd.set("#TopEquipName.Text", safeDisplayName(receiverId));
            cmd.set("#TopEquipIcon.Visible", true);
            cmd.set("#TopEquipIcon.ItemId", receiverId);
            cmd.set("#ReceiverPreviewGem.Text",
                "Sockets: " + receiver.metadata().getSocketedGemCount() + "/" + receiver.metadata().getSocketCapacity());
        }

        if (material == null) {
            cmd.set("#TopMaterialName.Text", isAdd ? "No gem selected" : "No removal item selected");
            cmd.set("#TopMaterialIcon.Visible", false);
            cmd.set("#TopMaterialOwned.Text", "Owned: 0");
            cmd.set("#ReceiverPreviewSocketed.Text", "Selected Effect: -");
        } else {
            cmd.set("#TopMaterialName.Text", safeDisplayName(material.itemId()));
            cmd.set("#TopMaterialIcon.Visible", true);
            cmd.set("#TopMaterialIcon.ItemId", material.itemId());
            cmd.set("#TopMaterialOwned.Text", "Owned: " + material.quantity());
            String selectedEffect = receiver == null
                ? "-"
                : GemSocketConfigHelper.describeGemBonusForItem(material.itemId(), receiver.stack().getItemId());
            cmd.set("#ReceiverPreviewSocketed.Text", "Selected Effect: " + selectedEffect);
        }

        cmd.set("#TopMaterialModeHint.Text", isAdd
            ? "Select a gem from the right inventory and socket it into the selected item."
            : "Use a removal material to recover gems, but the host item is destroyed.");
    }

    private void renderSocketedGemList(UICommandBuilder cmd, GemSocketApplicationService.ReceiverEntry receiver) {
        cmd.clear("#SocketedGemList");
        if (receiver == null) {
            cmd.set("#SocketedGemEmpty.Visible", true);
            return;
        }

        List<String> socketed = receiver.metadata().getSocketedGems();
        cmd.set("#SocketedGemEmpty.Visible", socketed.isEmpty());
        int row = 0;
        for (String gemId : socketed) {
            cmd.append("#SocketedGemList", SOCKETED_ROW_UI);
            String root = "#SocketedGemList[" + row + "]";
            cmd.set(root + " #SocketedGemIcon.ItemId", gemId);
            cmd.set(root + " #SocketedGemText.Text",
                GemSocketConfigHelper.displayGemName(gemId)
                    + " - "
                    + GemSocketConfigHelper.describeGemBonusForItem(gemId, receiver.stack().getItemId()));
            row++;
        }
    }

    private void renderLeftEquipmentInventory(UICommandBuilder cmd,
                                              UIEventBuilder evt,
                                              List<GemSocketApplicationService.ReceiverEntry> equipmentEntries) {
        cmd.clear("#EquipInventoryGrid");
        cmd.set("#EquipInventoryEmpty.Visible", equipmentEntries.isEmpty());
        for (int start = 0, row = 0; start < equipmentEntries.size(); start += CARDS_PER_ROW, row++) {
            cmd.append("#EquipInventoryGrid", INVENTORY_CARD_ROW_UI);
            String root = "#EquipInventoryGrid[" + row + "]";
            for (int col = 0; col < CARDS_PER_ROW; col++) {
                int index = start + col;
                String slotRoot = root + " #Slot" + col;
                if (index >= equipmentEntries.size()) {
                    cmd.set(slotRoot + ".Visible", false);
                    continue;
                }

                GemSocketApplicationService.ReceiverEntry entry = equipmentEntries.get(index);
                ItemInstanceMetadata meta = entry.metadata();
                cmd.set(slotRoot + ".Visible", true);
                cmd.set(slotRoot + " #CardIcon" + col + ".ItemId", entry.stack().getItemId());
                cmd.set(slotRoot + " #CardMeta" + col + ".Text",
                    meta.getSocketedGemCount() + "/" + meta.getSocketCapacity());
                cmd.set(slotRoot + " #CardSel" + col + ".Visible", entry.slot() == selectedReceiverSlot);

                evt.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    slotRoot + " #CardBtn" + col,
                    EventData.of("Button", ACTION_SELECT_RECEIVER).append("Slot", String.valueOf(entry.slot())),
                    false
                );
            }
        }
    }

    private void renderRightMaterialInventory(UICommandBuilder cmd,
                                              UIEventBuilder evt,
        List<MaterialEntry> materials,
                                              boolean isAdd) {
        cmd.set("#RightInventoryTitle.Text", isAdd ? "Gem Inventory" : "Removal Inventory");
        cmd.clear("#MaterialInventoryGrid");
        cmd.set("#MaterialInventoryEmpty.Visible", materials.isEmpty());

        String selectedId = isAdd ? selectedGemItemId : selectedRemoveItemId;
        for (int start = 0, row = 0; start < materials.size(); start += CARDS_PER_ROW, row++) {
            cmd.append("#MaterialInventoryGrid", INVENTORY_CARD_ROW_UI);
            String root = "#MaterialInventoryGrid[" + row + "]";
            for (int col = 0; col < CARDS_PER_ROW; col++) {
                int index = start + col;
                String slotRoot = root + " #Slot" + col;
                if (index >= materials.size()) {
                    cmd.set(slotRoot + ".Visible", false);
                    continue;
                }

                MaterialEntry entry = materials.get(index);
                cmd.set(slotRoot + ".Visible", true);
                cmd.set(slotRoot + " #CardIcon" + col + ".ItemId", entry.itemId());
                cmd.set(slotRoot + " #CardMeta" + col + ".Text", String.valueOf(entry.quantity()));
                cmd.set(slotRoot + " #CardSel" + col + ".Visible", entry.itemId().equalsIgnoreCase(selectedId));

                evt.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    slotRoot + " #CardBtn" + col,
                    EventData.of("Button", ACTION_SELECT_MATERIAL).append("ItemId", entry.itemId()),
                    false
                );
            }
        }
    }

    private void ensureSelections(List<GemSocketApplicationService.ReceiverEntry> equipmentEntries,
                                  List<MaterialEntry> gemEntries,
                                  List<MaterialEntry> removeEntries) {
        if (findReceiverEntry(equipmentEntries, selectedReceiverSlot) == null) {
            selectedReceiverSlot = equipmentEntries.isEmpty() ? -1 : equipmentEntries.get(0).slot();
        }
        if (findMaterialById(gemEntries, selectedGemItemId) == null) {
            selectedGemItemId = gemEntries.isEmpty() ? null : gemEntries.get(0).itemId();
        }
        if (findMaterialById(removeEntries, selectedRemoveItemId) == null) {
            selectedRemoveItemId = removeEntries.isEmpty() ? GEM_REMOVER_ITEM_ID : removeEntries.get(0).itemId();
        }
    }

    private static List<MaterialEntry> buildGemEntries(ItemContainer container) {
        Map<String, Integer> quantities = new LinkedHashMap<>();
        if (container == null) {
            return List.of();
        }
        container.forEach((slot, stack) -> {
            if (stack == null || stack.isEmpty() || stack.getItemId() == null) {
                return;
            }
            if (!GemSocketConfigHelper.isGemItemId(stack.getItemId())) {
                return;
            }
            quantities.merge(stack.getItemId(), stack.getQuantity(), Integer::sum);
        });
        List<MaterialEntry> out = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : quantities.entrySet()) {
            out.add(new MaterialEntry(entry.getKey(), entry.getValue()));
        }
        return out;
    }

    private static List<MaterialEntry> buildRemoveEntries(ItemContainer container) {
        int count = countItem(container, GEM_REMOVER_ITEM_ID);
        List<MaterialEntry> out = new ArrayList<>();
        if (count > 0) {
            out.add(new MaterialEntry(GEM_REMOVER_ITEM_ID, count));
        }
        return out;
    }

    private static int countItem(ItemContainer container, String itemId) {
        if (container == null || itemId == null || itemId.isBlank()) {
            return 0;
        }
        final String wanted = itemId.trim();
        return container.countItemStacks(stack ->
            stack != null && !stack.isEmpty() && stack.getItemId() != null && wanted.equalsIgnoreCase(stack.getItemId()));
    }

    private static GemSocketApplicationService.ReceiverEntry findReceiverEntry(List<GemSocketApplicationService.ReceiverEntry> entries, short slot) {
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

    private static MaterialEntry findMaterialById(List<MaterialEntry> entries, String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return null;
        }
        for (MaterialEntry entry : entries) {
            if (entry.itemId().equalsIgnoreCase(itemId)) {
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

    private record MaterialEntry(String itemId, int quantity) {
    }

    public static class GemSocketData {
        static final String KEY_BUTTON = "Button";
        static final String KEY_SLOT = "Slot";
        static final String KEY_ITEM_ID = "ItemId";

        public static final BuilderCodec<GemSocketData> CODEC = BuilderCodec.builder(GemSocketData.class, GemSocketData::new)
            .addField(new KeyedCodec<>(KEY_BUTTON, Codec.STRING), (d, s) -> d.button = s, d -> d.button)
            .addField(new KeyedCodec<>(KEY_SLOT, Codec.STRING), (d, s) -> d.slot = s, d -> d.slot)
            .addField(new KeyedCodec<>(KEY_ITEM_ID, Codec.STRING), (d, s) -> d.itemId = s, d -> d.itemId)
            .build();

        private String button;
        private String slot;
        private String itemId;
    }
}
