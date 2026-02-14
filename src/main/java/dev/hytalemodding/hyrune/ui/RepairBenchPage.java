package dev.hytalemodding.hyrune.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.hyrune.repair.RepairMaterialCost;
import dev.hytalemodding.hyrune.repair.RepairPlan;
import dev.hytalemodding.hyrune.repair.RepairPlanner;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * UI page for repair bench.
 */
public class RepairBenchPage extends InteractiveCustomUIPage<RepairBenchPage.RepairBenchData> {

    private static final String UI_PATH = "Pages/RepairBench.ui";
    private static final String ROW_UI = "Pages/repair_item_row.ui";

    private static final String ACTION_CLOSE = "Close";
    private static final String ACTION_REPAIR = "RepairItem";

    public RepairBenchPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, RepairBenchData.CODEC);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder commandBuilder,
                      @Nonnull UIEventBuilder eventBuilder,
                      @Nonnull Store<EntityStore> store) {
        commandBuilder.append(UI_PATH);

        Player player = store.getComponent(ref, Player.getComponentType());
        populateRepairList(commandBuilder, eventBuilder, player);

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#RepairClose",
                EventData.of("Button", ACTION_CLOSE), false);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                @Nonnull RepairBenchData data) {
        super.handleDataEvent(ref, store, data);
        if (data.button == null) {
            return;
        }

        switch (data.button) {
            case ACTION_CLOSE:
                this.close();
                return;
            case ACTION_REPAIR:
                handleRepair(ref, store, data.slot);
                return;
            default:
                return;
        }
    }

    private void handleRepair(Ref<EntityStore> ref, Store<EntityStore> store, String slotText) {
        if (slotText == null) {
            return;
        }

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        int slot;
        try {
            slot = Integer.parseInt(slotText);
        } catch (NumberFormatException e) {
            return;
        }

        Inventory inventory = player.getInventory();
        if (inventory == null) {
            return;
        }

        ItemContainer container = inventory.getCombinedEverything();
        if (container == null) {
            return;
        }

        if (slot < 0 || slot >= container.getCapacity()) {
            return;
        }

        ItemStack stack = container.getItemStack((short) slot);
        if (stack == null || stack.isEmpty()) {
            return;
        }

        double maxDurability = stack.getMaxDurability();
        double currentDurability = stack.getDurability();
        if (maxDurability <= 0 || currentDurability >= maxDurability) {
            return;
        }

        RepairPlan plan = RepairPlanner.planFullRepair(stack);
        if (!plan.isRepairable()) {
            playerRef.sendMessage(Message.raw("That item cannot be repaired right now."));
            return;
        }

        RepairMaterialCost missing = findFirstMissingMaterial(player, plan);
        if (missing != null) {
            ItemStack missingStack = new ItemStack(missing.getItemId(), missing.getQuantity());
            String materialKey = missingStack.getItem().getTranslationKey();
            Message errorMsg = Message.translation("server.repair.missing_material.name")
                .param("quantity", missing.getQuantity())
                .param("material", Message.translation(materialKey));
            playerRef.sendMessage(errorMsg);
            return;
        }

        if (!consumeRepairMaterials(container, plan)) {
            playerRef.sendMessage(Message.raw("Repair failed: could not consume required materials."));
            return;
        }

        ItemStack updated = stack.withIncreasedDurability(plan.getRestoreAmount());
        container.replaceItemStackInSlot((short) slot, stack, updated);

        playerRef.sendMessage(Message.raw("Your item has been repaired."));

        UICommandBuilder refreshCmd = new UICommandBuilder();
        UIEventBuilder refreshEvt = new UIEventBuilder();
        populateRepairList(refreshCmd, refreshEvt, player);
        refreshEvt.addEventBinding(CustomUIEventBindingType.Activating, "#RepairClose",
                EventData.of("Button", ACTION_CLOSE), false);
        this.sendUpdate(refreshCmd, refreshEvt, false);
    }

    private void populateRepairList(UICommandBuilder cmd, UIEventBuilder evt, Player player) {
        cmd.clear("#RepairList");

        if (player == null) {
            cmd.set("#RepairEmpty.Visible", true);
            return;
        }

        Inventory inventory = player.getInventory();
        if (inventory == null) {
            cmd.set("#RepairEmpty.Visible", true);
            return;
        }

        ItemContainer container = inventory.getCombinedEverything();
        if (container == null) {
            cmd.set("#RepairEmpty.Visible", true);
            return;
        }

        List<RepairEntry> entries = new ArrayList<>();
        short capacity = container.getCapacity();
        for (short slot = 0; slot < capacity; slot++) {
            ItemStack stack = container.getItemStack(slot);
            if (stack == null || stack.isEmpty()) {
                continue;
            }

            double maxDurability = stack.getMaxDurability();
            if (maxDurability <= 0) {
                continue;
            }

            double currentDurability = stack.getDurability();
            if (currentDurability >= maxDurability) {
                continue;
            }

            RepairPlan plan = RepairPlanner.planFullRepair(stack);
            if (plan.isRepairable()) {
                entries.add(new RepairEntry(slot, stack, plan));
            }
        }

        cmd.set("#RepairEmpty.Visible", entries.isEmpty());

        int row = 0;
        for (RepairEntry entry : entries) {
            cmd.append("#RepairList", ROW_UI);
            String rowRoot = "#RepairList[" + row + "]";

            boolean canAfford = hasAllMaterials(player, entry.repairPlan);

            double maxDurability = entry.stack.getMaxDurability();
            double current = entry.stack.getDurability();
            int pct = (int) Math.round(Math.max(0.0, Math.min(1.0, current / maxDurability)) * 100.0);

            String friendlyName = entry.stack.getItem().getTranslationKey();
            String durabilityText = pct + "%";
            String costColor = canAfford ? "#55FF55" : "#FF5555";
            String durColor = getDurabilityColor(pct);
            RepairMaterialCost primaryCost = entry.repairPlan.getMaterialCosts().get(0);

            cmd.set(rowRoot + " #ItemName.Text", Message.translation(friendlyName));
            cmd.set(rowRoot + " #ItemDurability.Text", durabilityText);
            cmd.set(rowRoot + " #ItemDurability.Style.TextColor", durColor);
            cmd.set(rowRoot + " #CostIcon.ItemId", primaryCost.getItemId());
            cmd.set(rowRoot + " #ItemCost.Text", buildCostSummary(entry.repairPlan.getMaterialCosts()));
            cmd.set(rowRoot + " #ItemCost.Style.TextColor", costColor);

            evt.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    rowRoot + " #RepairButton",
                    EventData.of("Button", ACTION_REPAIR).append("Slot", String.valueOf(entry.slot)),
                    false
            );

            row++;
        }
    }

    private static void playSoundByKey(Ref<EntityStore> entityRef, Store<EntityStore> store, String soundsKey, Vector3d position) {
        int soundIndex = SoundEvent.getAssetMap().getIndex(soundsKey);
        SoundUtil.playSoundEvent3d(soundIndex, SoundCategory.SFX, position.getX(), position.getY(), position.getZ(), 1.0F, 1.0F, store);
    }

    private static String getDurabilityColor(int percent) {
        if (percent >= 90) {
            return "#55FF55"; // Bright Green
        } else if (percent >= 70) {
            return "#FFFF55"; // Yellow
        } else if (percent >= 50) {
            return "#FFAA00"; // Orange (Gold)
        } else {
            return "#FF5555"; // Red
        }
    }

    private String buildCostSummary(List<RepairMaterialCost> costs) {
        StringJoiner joiner = new StringJoiner(" + ");
        for (RepairMaterialCost cost : costs) {
            joiner.add("x" + cost.getQuantity());
        }
        return joiner.toString();
    }

    private boolean hasAllMaterials(Player player, RepairPlan plan) {
        if (player == null || plan == null || plan.getMaterialCosts().isEmpty()) {
            return false;
        }
        for (RepairMaterialCost cost : plan.getMaterialCosts()) {
            if (getInventoryCount(player, cost.getItemId()) < cost.getQuantity()) {
                return false;
            }
        }
        return true;
    }

    private RepairMaterialCost findFirstMissingMaterial(Player player, RepairPlan plan) {
        if (player == null || plan == null) {
            return null;
        }
        for (RepairMaterialCost cost : plan.getMaterialCosts()) {
            if (getInventoryCount(player, cost.getItemId()) < cost.getQuantity()) {
                return cost;
            }
        }
        return null;
    }

    private boolean consumeRepairMaterials(ItemContainer container, RepairPlan plan) {
        if (container == null || plan == null) {
            return false;
        }
        for (RepairMaterialCost cost : plan.getMaterialCosts()) {
            ItemStack payment = new ItemStack(cost.getItemId(), cost.getQuantity());
            var tx = container.removeItemStack(payment);
            if (tx == null || !tx.succeeded()) {
                return false;
            }
        }
        return true;
    }

    private int getInventoryCount(Player player, String itemId) {
        if (player == null || player.getInventory() == null) return 0;

        ItemContainer container = player.getInventory().getCombinedEverything();
        if (container == null) return 0;

        int total = 0;
        for (short i = 0; i < container.getCapacity(); i++) {
            ItemStack stack = container.getItemStack(i);
            if (stack != null && stack.getItemId().equals(itemId)) {
                total += stack.getQuantity();
            }
        }
        return total;
    }

    private static final class RepairEntry {
        private final short slot;
        private final ItemStack stack;
        private final RepairPlan repairPlan;

        private RepairEntry(short slot, ItemStack stack, RepairPlan repairPlan) {
            this.slot = slot;
            this.stack = stack;
            this.repairPlan = repairPlan;
        }
    }

    public static class RepairBenchData {
        static final String KEY_BUTTON = "Button";
        static final String KEY_SLOT = "Slot";

        public static final BuilderCodec<RepairBenchData> CODEC = BuilderCodec.builder(RepairBenchData.class, RepairBenchData::new)
                .addField(new KeyedCodec<>(KEY_BUTTON, Codec.STRING), (d, s) -> d.button = s, d -> d.button)
                .addField(new KeyedCodec<>(KEY_SLOT, Codec.STRING), (d, s) -> d.slot = s, d -> d.slot)
                .build();

        private String button;
        private String slot;
    }
}
