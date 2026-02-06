package dev.hytalemodding.origins.ui;

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
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.origins.slayer.ShopItem;
import dev.hytalemodding.origins.slayer.SlayerPlayerData;
import dev.hytalemodding.origins.slayer.SlayerService;

import javax.annotation.Nonnull;
import java.util.List;

public class SlayerVendorPage extends InteractiveCustomUIPage<SlayerVendorPage.VendorData> {

    private static final String MAIN_UI = "Pages/SlayerVendor.ui";
    private static final String ENTRY_UI = "Pages/SlayerVendorEntry.ui";

    private static final String ACTION_CLOSE = "Close";
    private static final String TAB_BUY = "Buy";
    private static final String TAB_LEARN = "Learn";
    private static final String TAB_TASK = "Task";
    private static final String ACTION_BUY_PREFIX = "Buy_";
    private String selectedTab = TAB_BUY;

    private final SlayerService slayerService;

    public SlayerVendorPage(@Nonnull PlayerRef playerRef, SlayerService slayerService) {
        super(playerRef, CustomPageLifetime.CanDismiss, VendorData.CODEC);
        this.slayerService = slayerService;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder evt,
                      @Nonnull Store<EntityStore> store) {

        // 1. Load the Main Frame
        cmd.append(MAIN_UI);

        if (slayerService != null) {
            SlayerPlayerData data = slayerService.getPlayerData(playerRef.getUuid());
            cmd.set("#VendorPoints.Text", String.valueOf(data.getSlayerPoints()));

            // 2. Populate the Buy Tab
            applyTabState(cmd, evt);
        }

        // 3. Bind Global Navigation
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BtnClose",
                EventData.of("Button", ACTION_CLOSE), false);

        evt.addEventBinding(CustomUIEventBindingType.Activating, "#TabBuyBtn",
                EventData.of("Button", TAB_BUY), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#TabLearnBtn",
                EventData.of("Button", TAB_LEARN), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#TabTaskBtn",
                EventData.of("Button", TAB_TASK), false);
    }

    /**
     * Updates the visual state of the tabs and populates the active content.
     */
    private void applyTabState(UICommandBuilder cmd, UIEventBuilder evt) {
        boolean isBuy = TAB_BUY.equals(this.selectedTab);
        boolean isLearn = TAB_LEARN.equals(this.selectedTab);
        boolean isTask = TAB_TASK.equals(this.selectedTab);

        // 1. Update the TabBar widget so the Gold Overlay moves
        cmd.set("#TabBar.SelectedTab", this.selectedTab);

        // 2. Toggle Container Visibility
        cmd.set("#TabBuy.Visible", isBuy);
        cmd.set("#TabLearn.Visible", isLearn);
        cmd.set("#TabTask.Visible", isTask);

        // 3. Populate Content based on active tab
        if (isBuy) {
            populateBuyTab(cmd, evt);
        } else if (isLearn) {
            // populateLearnTab(cmd, evt); // Stub
        } else if (isTask) {
            // populateTaskTab(cmd, evt); // Stub
        }
    }


    private void populateBuyTab(UICommandBuilder cmd, UIEventBuilder evt) {
        cmd.clear("#BuyCol1").clear("#BuyCol2"); // Clear old items

        List<ShopItem> items = slayerService.getShopItems();
        int index = 0;

        for (ShopItem item : items) {
            String targetCol = (index % 2 == 0) ? "#BuyCol1" : "#BuyCol2";

            // 1. Add the UI
            cmd.append(targetCol, ENTRY_UI);

            // 2. Target the specific item we just added
            int rowIndex = index / 2;
            String entryRoot = targetCol + "[" + rowIndex + "]";

            // 3. Set Data
            // Note: Hytale finds nested IDs automatically if unique
            cmd.set(entryRoot + " #ItemName.Text", item.getDisplayName());
            cmd.set(entryRoot + " #ItemCost.Text", item.getCost() + " Points");



            // 4. Bind Button
            String btnId = ACTION_BUY_PREFIX + item.getId();
            evt.addEventBinding(CustomUIEventBindingType.Activating,
                    entryRoot + " #BuyButton",
                    EventData.of("Button", btnId),
                    false
            );

            index++;
        }
    }
    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull VendorData data) {
        super.handleDataEvent(ref, store, data);

        if (data.button == null) return;

        // Handle Close
        if (ACTION_CLOSE.equals(data.button)) {
            this.close();
            return;
        }

        // Handle Tab Switching
        if (TAB_BUY.equals(data.button) || TAB_LEARN.equals(data.button) || TAB_TASK.equals(data.button)) {
            this.selectedTab = data.button;

            UICommandBuilder cmd = new UICommandBuilder();
            UIEventBuilder evt = new UIEventBuilder();

            // Refresh the entire tab state (visuals + content)
            applyTabState(cmd, evt);

            this.sendUpdate(cmd, evt, false);
            return;
        }

        // Handle Buying
        if (data.button.startsWith(ACTION_BUY_PREFIX)) {
            String itemId = data.button.substring(ACTION_BUY_PREFIX.length());
            handlePurchase(itemId, ref, store);
        }
    }

    private void handlePurchase(String itemId, Ref<EntityStore> ref, Store<EntityStore> store) {
        // 1. Get the Player Component (Need this for Inventory access)
        Player player = store.getComponent(ref, Player.getComponentType());

        // 2. Call the service with all required context
        // We pass 'ref' and 'store' so the service can trigger the pickup notification
        if (player != null && slayerService.attemptPurchase(player, itemId, ref, store)) {
            playerRef.sendMessage(Message.raw("Item purchased successfully!"));

            // Refresh the Points UI
            SlayerPlayerData data = slayerService.getPlayerData(playerRef.getUuid());
            UICommandBuilder cmd = new UICommandBuilder();
            cmd.set("#VendorPoints.Text", String.valueOf(data.getSlayerPoints()));
            this.sendUpdate(cmd, new UIEventBuilder(), false);
        } else {
            playerRef.sendMessage(Message.raw("Transaction failed. Check your points and inventory space."));
        }
    }

    public static class VendorData {
        static final String KEY_BUTTON = "Button";
        public static final BuilderCodec<VendorData> CODEC = BuilderCodec.builder(VendorData.class, VendorData::new)
                .addField(new KeyedCodec<>(KEY_BUTTON, Codec.STRING), (d, s) -> d.button = s, d -> d.button)
                .build();
        private String button;
    }
}
