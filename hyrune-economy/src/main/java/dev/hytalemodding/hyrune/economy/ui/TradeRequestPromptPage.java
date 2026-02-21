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
import dev.hytalemodding.hyrune.economy.trade.PlayerTradeService;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Accept/decline popup shown to a player when another player requests a trade.
 */
public class TradeRequestPromptPage extends InteractiveCustomUIPage<TradeRequestPromptPage.TradePromptData> {
    private static final String UI_PATH = "Pages/TradeRequestPrompt.ui";
    private static final String ACTION_ACCEPT = "Accept";
    private static final String ACTION_DECLINE = "Decline";
    private static final String ACTION_CLOSE = "Close";

    private final UUID requesterUuid;
    private final String requesterName;
    private final PlayerTradeService tradeService;
    private boolean responded;

    public TradeRequestPromptPage(@Nonnull PlayerRef targetPlayerRef,
                                  @Nonnull UUID requesterUuid,
                                  @Nonnull String requesterName,
                                  @Nonnull PlayerTradeService tradeService) {
        super(targetPlayerRef, CustomPageLifetime.CanDismiss, TradePromptData.CODEC);
        this.requesterUuid = requesterUuid;
        this.requesterName = requesterName;
        this.tradeService = tradeService;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder commandBuilder,
                      @Nonnull UIEventBuilder eventBuilder,
                      @Nonnull Store<EntityStore> store) {
        commandBuilder.append(UI_PATH);
        commandBuilder.set("#TradePromptText.Text", requesterName + " wishes to trade with you.");

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#TradePromptAccept",
            EventData.of("Button", ACTION_ACCEPT), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#TradePromptDecline",
            EventData.of("Button", ACTION_DECLINE), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#TradePromptClose",
            EventData.of("Button", ACTION_CLOSE), false);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                @Nonnull TradePromptData data) {
        super.handleDataEvent(ref, store, data);
        if (data.button == null) {
            return;
        }
        switch (data.button) {
            case ACTION_ACCEPT:
                respond(true);
                close();
                return;
            case ACTION_DECLINE:
            case ACTION_CLOSE:
                respond(false);
                close();
                return;
            default:
        }
    }

    @Override
    public void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        super.onDismiss(ref, store);
        respond(false);
    }

    private void respond(boolean accepted) {
        if (responded) {
            return;
        }
        responded = true;
        tradeService.respondToRequest(this.playerRef.getUuid(), requesterUuid, accepted);
    }

    public static class TradePromptData {
        static final String KEY_BUTTON = "Button";

        public static final BuilderCodec<TradePromptData> CODEC = BuilderCodec.builder(TradePromptData.class, TradePromptData::new)
            .addField(new KeyedCodec<>(KEY_BUTTON, Codec.STRING), (d, s) -> d.button = s, d -> d.button)
            .build();

        private String button;
    }
}
