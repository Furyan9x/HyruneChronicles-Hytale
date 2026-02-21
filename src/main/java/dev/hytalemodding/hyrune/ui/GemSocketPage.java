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
import com.hypixel.hytale.server.core.ui.ItemGridSlot;
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
    private static final String SOCKETED_ROW_UI = "Pages/gem_socketed_row.ui";

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
    private static final int MAX_TOOLTIP_GEMS = 6;

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
                selectReceiverFromEvent(ref, store, data);
                rebuild(ref, store);
                return;
            case ACTION_SELECT_MATERIAL:
                selectMaterialFromEvent(ref, store, data);
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

    private void selectReceiverFromEvent(Ref<EntityStore> ref, Store<EntityStore> store, GemSocketData data) {
        Inventory inventory = resolveInventory(ref, store);
        List<GemSocketApplicationService.ReceiverEntry> entries = inventory == null
            ? List.of()
            : GemSocketApplicationService.findEligibleReceiverEntries(inventory);
        int selectedIndex = resolveSelectedIndex(data);
        if (selectedIndex >= 0 && selectedIndex < entries.size()) {
            this.selectedReceiverSlot = entries.get(selectedIndex).slot();
            return;
        }
        if (data.itemStackId != null && !data.itemStackId.isBlank()) {
            for (GemSocketApplicationService.ReceiverEntry entry : entries) {
                if (data.itemStackId.equalsIgnoreCase(entry.stack().getItemId())) {
                    this.selectedReceiverSlot = entry.slot();
                    return;
                }
            }
        }
        this.selectedReceiverSlot = parseSlot(data.slot);
    }

    private void selectMaterialFromEvent(Ref<EntityStore> ref, Store<EntityStore> store, GemSocketData data) {
        Inventory inventory = resolveInventory(ref, store);
        ItemContainer container = inventory == null ? null : inventory.getCombinedEverything();
        List<MaterialEntry> materials = TAB_ADD.equals(this.selectedTab)
            ? buildGemEntries(container)
            : buildRemoveEntries(container);

        int selectedIndex = resolveSelectedIndex(data);
        if (selectedIndex >= 0 && selectedIndex < materials.size()) {
            String clickedId = materials.get(selectedIndex).itemId();
            if (TAB_ADD.equals(this.selectedTab)) {
                this.selectedGemItemId = clickedId;
            } else {
                this.selectedRemoveItemId = clickedId;
            }
            return;
        }
        if (data.itemStackId != null && !data.itemStackId.isBlank()) {
            if (TAB_ADD.equals(this.selectedTab)) {
                this.selectedGemItemId = data.itemStackId;
            } else {
                this.selectedRemoveItemId = data.itemStackId;
            }
            return;
        }
        if (TAB_ADD.equals(this.selectedTab)) {
            this.selectedGemItemId = data.itemId;
        } else {
            this.selectedRemoveItemId = data.itemId;
        }
    }

    private Inventory resolveInventory(Ref<EntityStore> ref, Store<EntityStore> store) {
        Player player = store.getComponent(ref, Player.getComponentType());
        return player != null ? player.getInventory() : null;
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
        cmd.set("#EquipInventoryEmpty.Visible", equipmentEntries.isEmpty());
        ItemGridSlot[] slots = new ItemGridSlot[equipmentEntries.size()];
        for (int index = 0; index < equipmentEntries.size(); index++) {
            GemSocketApplicationService.ReceiverEntry entry = equipmentEntries.get(index);
            ItemInstanceMetadata meta = entry.metadata();
            ItemGridSlot slot = new ItemGridSlot(new ItemStack(entry.stack().getItemId(), 1));
            slot.setName(safeDisplayName(entry.stack().getItemId()));
            slot.setDescription(buildEquipmentTooltip(entry));
            slot.setActivatable(true);
            slots[index] = slot;
        }
        cmd.set("#EquipInventoryGrid.Slots", slots);
        evt.addEventBinding(
            CustomUIEventBindingType.SlotClicking,
            "#EquipInventoryGrid",
            EventData.of("Button", ACTION_SELECT_RECEIVER),
            false
        );
    }

    private void renderRightMaterialInventory(UICommandBuilder cmd,
                                              UIEventBuilder evt,
                                              List<MaterialEntry> materials,
                                              boolean isAdd) {
        cmd.set("#RightInventoryTitle.Text", isAdd ? "Gems" : "Unsocketing Materials");
        cmd.set("#MaterialInventoryEmpty.Visible", materials.isEmpty());
        ItemGridSlot[] slots = new ItemGridSlot[materials.size()];
        for (int index = 0; index < materials.size(); index++) {
            MaterialEntry entry = materials.get(index);
            ItemGridSlot slot = new ItemGridSlot(new ItemStack(entry.itemId(), 1));
            slot.setName(safeDisplayName(entry.itemId()));
            slot.setDescription("Owned: " + entry.quantity());
            slot.setActivatable(true);
            slots[index] = slot;
        }
        cmd.set("#MaterialInventoryGrid.Slots", slots);
        evt.addEventBinding(
            CustomUIEventBindingType.SlotClicking,
            "#MaterialInventoryGrid",
            EventData.of("Button", ACTION_SELECT_MATERIAL),
            false
        );
    }

    private static String buildEquipmentTooltip(GemSocketApplicationService.ReceiverEntry entry) {
        ItemInstanceMetadata meta = entry.metadata();
        StringBuilder tooltip = new StringBuilder();
        tooltip.append("Sockets: ")
            .append(meta.getSocketedGemCount())
            .append("/")
            .append(meta.getSocketCapacity());

        List<String> socketed = meta.getSocketedGems();
        if (socketed.isEmpty()) {
            tooltip.append("\nSocketed Gems: none");
            return tooltip.toString();
        }

        tooltip.append("\nSocketed Gems:");
        int max = Math.min(socketed.size(), MAX_TOOLTIP_GEMS);
        for (int i = 0; i < max; i++) {
            String gemId = socketed.get(i);
            tooltip.append("\n- ")
                .append(GemSocketConfigHelper.displayGemName(gemId))
                .append(" (")
                .append(GemSocketConfigHelper.describeGemBonusForItem(gemId, entry.stack().getItemId()))
                .append(")");
        }
        if (socketed.size() > max) {
            tooltip.append("\n... +").append(socketed.size() - max).append(" more");
        }
        return tooltip.toString();
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

    private static int resolveSelectedIndex(GemSocketData data) {
        if (data.slotIndex != null) {
            return data.slotIndex;
        }
        if (data.selectedSlotIndex != null) {
            return data.selectedSlotIndex;
        }
        short parsed = parseSlot(data.slot);
        return parsed < 0 ? -1 : parsed;
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
        static final String KEY_SLOT_INDEX = "SlotIndex";
        static final String KEY_SELECTED_SLOT_INDEX = "SelectedSlotIndex";
        static final String KEY_ITEM_ID = "ItemId";
        static final String KEY_ITEM_STACK_ID = "ItemStackId";

        public static final BuilderCodec<GemSocketData> CODEC = BuilderCodec.builder(GemSocketData.class, GemSocketData::new)
            .addField(new KeyedCodec<>(KEY_BUTTON, Codec.STRING), (d, s) -> d.button = s, d -> d.button)
            .addField(new KeyedCodec<>(KEY_SLOT, Codec.STRING), (d, s) -> d.slot = s, d -> d.slot)
            .addField(new KeyedCodec<>(KEY_SLOT_INDEX, Codec.INTEGER), (d, i) -> d.slotIndex = i, d -> d.slotIndex)
            .addField(new KeyedCodec<>(KEY_SELECTED_SLOT_INDEX, Codec.INTEGER), (d, i) -> d.selectedSlotIndex = i, d -> d.selectedSlotIndex)
            .addField(new KeyedCodec<>(KEY_ITEM_ID, Codec.STRING), (d, s) -> d.itemId = s, d -> d.itemId)
            .addField(new KeyedCodec<>(KEY_ITEM_STACK_ID, Codec.STRING), (d, s) -> d.itemStackId = s, d -> d.itemStackId)
            .build();

        private String button;
        private String slot;
        private Integer slotIndex;
        private Integer selectedSlotIndex;
        private String itemId;
        private String itemStackId;
    }
}
