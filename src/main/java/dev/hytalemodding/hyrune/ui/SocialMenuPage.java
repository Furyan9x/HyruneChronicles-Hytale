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
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.Hyrune;
import dev.hytalemodding.hyrune.component.GameModeDataComponent;
import dev.hytalemodding.hyrune.economy.TradeRequestBridge;
import dev.hytalemodding.hyrune.gamemode.StarterKitProvider;
import dev.hytalemodding.hyrune.social.SocialActionResult;
import dev.hytalemodding.hyrune.social.SocialInteractionRules;
import dev.hytalemodding.hyrune.social.SocialService;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Player context menu opened when interacting with another player.
 */
public class SocialMenuPage extends InteractiveCustomUIPage<SocialMenuPage.SocialData> {
    private static final String UI_PATH = "Pages/SocialMenu.ui";

    private static final String ACTION_CLOSE = "Close";
    private static final String ACTION_ADD_FRIEND = "AddFriend";
    private static final String ACTION_ACCEPT_FRIEND = "AcceptFriend";
    private static final String ACTION_DENY_FRIEND = "DenyFriend";
    private static final String ACTION_REMOVE_FRIEND = "RemoveFriend";
    private static final String ACTION_IGNORE = "Ignore";
    private static final String ACTION_UNIGNORE = "Unignore";
    private static final String ACTION_TRADE = "Trade";
    private static final String ACTION_INSPECT = "Inspect";
    private static final String ACTION_INVITE = "Invite";

    private final PlayerRef targetPlayerRef;

    public SocialMenuPage(@Nonnull PlayerRef playerRef, @Nonnull PlayerRef targetPlayerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, SocialData.CODEC);
        this.targetPlayerRef = targetPlayerRef;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder commandBuilder,
                      @Nonnull UIEventBuilder eventBuilder,
                      @Nonnull Store<EntityStore> store) {
        if (!ensureTargetAvailable(true)) {
            return;
        }
        commandBuilder.append(UI_PATH);
        populateState(commandBuilder, eventBuilder);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                @Nonnull SocialData data) {
        super.handleDataEvent(ref, store, data);

        if (data.button == null) {
            return;
        }

        UUID viewer = this.playerRef.getUuid();
        UUID target = this.targetPlayerRef.getUuid();
        SocialService socialService = Hyrune.getSocialService();
        if (socialService == null) {
            this.playerRef.sendMessage(Message.raw("Social service is not available."));
            this.close();
            return;
        }

        if (!ACTION_CLOSE.equals(data.button) && !ensureTargetAvailable(true)) {
            return;
        }

        switch (data.button) {
            case ACTION_CLOSE:
                this.close();
                return;
            case ACTION_ADD_FRIEND:
                sendResult(socialService.sendFriendRequest(viewer, target));
                break;
            case ACTION_ACCEPT_FRIEND:
                sendResult(socialService.acceptFriendRequest(viewer, target));
                break;
            case ACTION_DENY_FRIEND:
                sendResult(socialService.denyFriendRequest(viewer, target));
                break;
            case ACTION_REMOVE_FRIEND:
                sendResult(socialService.removeFriend(viewer, target));
                break;
            case ACTION_IGNORE:
                sendResult(socialService.ignore(viewer, target));
                break;
            case ACTION_UNIGNORE:
                sendResult(socialService.unignore(viewer, target));
                break;
            case ACTION_TRADE:
                handleTradeRequest();
                return;
            case ACTION_INVITE:
                this.playerRef.sendMessage(Message.raw("Party/Group invites are not implemented yet."));
                break;
            case ACTION_INSPECT:
                openInspectPage(ref, store);
                return;
            default:
                return;
        }

        this.rebuild();
    }

    private void populateState(UICommandBuilder cmd, UIEventBuilder evt) {
        SocialService socialService = Hyrune.getSocialService();
        if (socialService == null) {
            return;
        }

        UUID viewer = this.playerRef.getUuid();
        UUID target = this.targetPlayerRef.getUuid();

        boolean isFriend = socialService.isFriend(viewer, target);
        boolean isIgnored = socialService.isIgnored(viewer, target);
        boolean hasIncoming = socialService.hasIncomingRequest(viewer, target);
        boolean hasOutgoing = socialService.hasOutgoingRequest(viewer, target);
        boolean targetOnline = socialService.isOnline(target);

        String relationship;
        if (isIgnored) {
            relationship = "Ignored";
        } else if (isFriend) {
            relationship = targetOnline ? "Friend (Online)" : "Friend (Offline)";
        } else if (hasIncoming) {
            relationship = "Pending request from player";
        } else if (hasOutgoing) {
            relationship = "Friend request sent";
        } else {
            relationship = "No social relationship";
        }

        cmd.set("#SocialTargetName.Text", socialService.resolveDisplayName(target));
        cmd.set("#SocialRelationship.Text", relationship);
        cmd.set("#SocialHint.Text", "Tip: Look at a player and press F to open this menu.");

        cmd.set("#BtnAddFriend.Visible", !isFriend && !isIgnored && !hasIncoming && !hasOutgoing);
        cmd.set("#BtnAcceptFriend.Visible", hasIncoming);
        cmd.set("#BtnDenyFriend.Visible", hasIncoming);
        cmd.set("#BtnRemoveFriend.Visible", isFriend);
        cmd.set("#BtnIgnore.Visible", !isIgnored);
        cmd.set("#BtnUnignore.Visible", isIgnored);
        cmd.set("#AdminHeader.Visible", true);
        cmd.set("#AdminPlaceholder.Visible", true);

        bind(evt, "#BtnClose", ACTION_CLOSE);
        bind(evt, "#BtnAddFriend", ACTION_ADD_FRIEND);
        bind(evt, "#BtnAcceptFriend", ACTION_ACCEPT_FRIEND);
        bind(evt, "#BtnDenyFriend", ACTION_DENY_FRIEND);
        bind(evt, "#BtnRemoveFriend", ACTION_REMOVE_FRIEND);
        bind(evt, "#BtnIgnore", ACTION_IGNORE);
        bind(evt, "#BtnUnignore", ACTION_UNIGNORE);
        bind(evt, "#BtnTrade", ACTION_TRADE);
        bind(evt, "#BtnInspect", ACTION_INSPECT);
        bind(evt, "#BtnInvite", ACTION_INVITE);
    }

    private void bind(UIEventBuilder evt, String selector, String action) {
        evt.addEventBinding(CustomUIEventBindingType.Activating, selector, EventData.of("Button", action), false);
    }

    private void handleTradeRequest() {
        if (isTradeBlocked(this.playerRef) || isTradeBlocked(this.targetPlayerRef)) {
            this.playerRef.sendMessage(Message.raw("Trade is blocked for Ironman/Hardcore Ironman accounts."));
            this.close();
            return;
        }
        TradeRequestBridge.TradeRequestResult result =
            TradeRequestBridge.requestTrade(this.playerRef, this.targetPlayerRef);
        if (result.message() != null && !result.message().isBlank()) {
            this.playerRef.sendMessage(Message.raw(result.message()));
        }
        this.close();
    }

    private boolean isTradeBlocked(PlayerRef playerRef) {
        if (playerRef == null || Hyrune.getGameModeDataComponentType() == null) {
            return false;
        }
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return false;
        }
        Store<EntityStore> store = ref.getStore();
        GameModeDataComponent data = store.getComponent(ref, Hyrune.getGameModeDataComponentType());
        if (data == null || data.getGameMode() == null) {
            return false;
        }
        String mode = data.getGameMode();
        return StarterKitProvider.MODE_IRONMAN.equalsIgnoreCase(mode)
            || StarterKitProvider.MODE_HARDCORE_IRONMAN.equalsIgnoreCase(mode);
    }

    private void openInspectPage(Ref<EntityStore> ref, Store<EntityStore> store) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        World world = store.getExternalData().getWorld();
        CompletableFuture.runAsync(
            () -> player.getPageManager().openCustomPage(ref, store, new InspectPlayerPage(this.playerRef, this.targetPlayerRef)),
            world
        );
    }

    private void sendResult(SocialActionResult result) {
        if (result == null || result.message() == null) {
            return;
        }
        this.playerRef.sendMessage(Message.raw(result.message()));
    }

    private boolean ensureTargetAvailable(boolean sendFeedback) {
        if (this.targetPlayerRef == null || this.targetPlayerRef.getUuid() == null) {
            if (sendFeedback) {
                this.playerRef.sendMessage(Message.raw("Invalid social target."));
            }
            this.close();
            return false;
        }
        if (!SocialInteractionRules.isOnline(this.targetPlayerRef.getUuid())) {
            if (sendFeedback) {
                this.playerRef.sendMessage(Message.raw("That player is offline."));
            }
            this.close();
            return false;
        }
        if (!SocialInteractionRules.isWithinInteractionRange(this.playerRef, this.targetPlayerRef)) {
            if (sendFeedback) {
                this.playerRef.sendMessage(Message.raw("You must be within 5 blocks to use social actions."));
            }
            this.close();
            return false;
        }
        return true;
    }

    public static class SocialData {
        static final String KEY_BUTTON = "Button";

        public static final BuilderCodec<SocialData> CODEC = BuilderCodec.builder(SocialData.class, SocialData::new)
            .addField(new KeyedCodec<>(KEY_BUTTON, Codec.STRING), (d, s) -> d.button = s, d -> d.button)
            .build();

        private String button;
    }
}
