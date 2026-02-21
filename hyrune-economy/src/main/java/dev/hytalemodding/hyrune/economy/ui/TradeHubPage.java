package dev.hytalemodding.hyrune.economy.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * First-pass trade UI shell for economy iteration.
 */
public class TradeHubPage extends InteractiveCustomUIPage<TradeHubPage.TradeHubData> {
    private static final String UI_PATH = "Pages/TradeHubPage.ui";
    private static final String ACTION_CLOSE = "Close";
    private static final String ACTION_TAB_LOCAL = "TabLocal";
    private static final String ACTION_TAB_MARKET = "TabMarket";
    private static final String TAB_LOCAL = "LocalTrade";
    private static final String TAB_MARKET = "MarketBoard";

    private String selectedTab = TAB_LOCAL;

    public TradeHubPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, TradeHubData.CODEC);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder commandBuilder,
                      @Nonnull UIEventBuilder eventBuilder,
                      @Nonnull Store<EntityStore> store) {
        commandBuilder.append(UI_PATH);
        render(commandBuilder, eventBuilder);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                @Nonnull TradeHubData data) {
        super.handleDataEvent(ref, store, data);
        if (data.button == null) {
            return;
        }
        switch (data.button) {
            case ACTION_CLOSE:
                this.close();
                return;
            case ACTION_TAB_LOCAL:
                this.selectedTab = TAB_LOCAL;
                break;
            case ACTION_TAB_MARKET:
                this.selectedTab = TAB_MARKET;
                break;
            default:
                return;
        }

        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder evt = new UIEventBuilder();
        render(cmd, evt);
        this.sendUpdate(cmd, evt, false);
    }

    private void render(UICommandBuilder cmd, UIEventBuilder evt) {
        boolean local = TAB_LOCAL.equals(this.selectedTab);
        cmd.set("#LocalTradePanel.Visible", local);
        cmd.set("#MarketBoardPanel.Visible", !local);
        cmd.set("#TradeTabLocal.Text", local ? "Local Trade (Selected)" : "Local Trade");
        cmd.set("#TradeTabMarket.Text", local ? "Market Board" : "Market Board (Selected)");

        evt.addEventBinding(CustomUIEventBindingType.Activating, "#TradeClose",
            EventData.of("Button", ACTION_CLOSE), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#TradeTabLocal",
            EventData.of("Button", ACTION_TAB_LOCAL), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#TradeTabMarket",
            EventData.of("Button", ACTION_TAB_MARKET), false);
    }

    public static class TradeHubData {
        static final String KEY_BUTTON = "Button";

        public static final BuilderCodec<TradeHubData> CODEC = BuilderCodec.builder(TradeHubData.class, TradeHubData::new)
            .addField(new KeyedCodec<>(KEY_BUTTON, Codec.STRING), (d, s) -> d.button = s, d -> d.button)
            .build();

        private String button;
    }
}
