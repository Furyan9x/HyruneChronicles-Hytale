package dev.hytalemodding.hyrune.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.Hyrune;
import dev.hytalemodding.hyrune.component.GameModeDataComponent;
import dev.hytalemodding.hyrune.gamemode.StarterKitProvider;

import javax.annotation.Nonnull;

/**
 * Interactive UI page for selecting game mode.
 * Streamlined for Phase 3 "Cards + Details" layout.
 */
public class GameModeSelectionPage extends InteractiveCustomUIPage<GameModeSelectionPage.SelectionData> {

    private static final String UI_PATH = "Pages/GameModeSelection.ui";

    private static final String ACTION_CLOSE = "Close";
    private static final String ACTION_SELECT_NORMAL = "SelectNormal";
    private static final String ACTION_SELECT_IRONMAN = "SelectIronman";
    private static final String ACTION_SELECT_HARDCORE = "SelectHardcore";
    private static final String ACTION_CONFIRM = "Confirm";

    private static final String MODE_SOUND_ID = "SFX_Mode_Sound";

    private String pendingGameMode = null;

    public GameModeSelectionPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, SelectionData.CODEC);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder commandBuilder,
                      @Nonnull UIEventBuilder eventBuilder,
                      @Nonnull Store<EntityStore> store) {

        // 1. Safety Check: Ensure component exists and isn't locked
        GameModeDataComponent component = store.getComponent(ref, Hyrune.getGameModeDataComponentType());
        if (component == null) {
            component = new GameModeDataComponent();
            store.putComponent(ref, Hyrune.getGameModeDataComponentType(), component);
        }

        if (component.hasCompletedSetup()) {
            playerRef.sendMessage(Message.raw("You have already selected a game mode!"));
            this.close();
            return;
        }

        // 2. Load UI
        commandBuilder.append(UI_PATH);

        // 3. Initialize State (Hidden Details, Visible Cards)
        initUI(commandBuilder, eventBuilder);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                @Nonnull SelectionData data) {
        super.handleDataEvent(ref, store, data);

        if (data.button == null) return;

        switch (data.button) {
            case ACTION_CLOSE:
                this.close();
                break;
            case ACTION_SELECT_NORMAL:
                handleGameModePreview(StarterKitProvider.MODE_NORMAL);
                break;
            case ACTION_SELECT_IRONMAN:
                handleGameModePreview(StarterKitProvider.MODE_IRONMAN);
                break;
            case ACTION_SELECT_HARDCORE:
                handleGameModePreview(StarterKitProvider.MODE_HARDCORE_IRONMAN);
                break;
            case ACTION_CONFIRM:
                handleConfirmSelection(ref, store);
                break;
        }
    }

    /**
     * Set initial state: Details hidden, bindings active.
     */
    private void initUI(UICommandBuilder cmd, UIEventBuilder evt) {
        // Hide details initially
        cmd.set("#DetailsPanel.Visible", false);

        // Ensure selection overlays are hidden
        cmd.set("#NormalSelected.Visible", false);
        cmd.set("#IronmanSelected.Visible", false);
        cmd.set("#HardcoreSelected.Visible", false);

        // Bind Card Clicks
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#NormalButton",
                EventData.of("Button", ACTION_SELECT_NORMAL), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#IronmanButton",
                EventData.of("Button", ACTION_SELECT_IRONMAN), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#HardcoreButton",
                EventData.of("Button", ACTION_SELECT_HARDCORE), false);

        // Bind Close
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton",
                EventData.of("Button", ACTION_CLOSE), false);
    }

    /**
     * Updates the UI to show the selected card and its details.
     */
    private void handleGameModePreview(String gameMode) {
        this.pendingGameMode = gameMode;
        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder evt = new UIEventBuilder();

        // 1. VISUALS: Exclusive Selection Logic
        cmd.set("#NormalSelected.Visible", false);
        cmd.set("#IronmanSelected.Visible", false);
        cmd.set("#HardcoreSelected.Visible", false);

        if (gameMode.equals(StarterKitProvider.MODE_NORMAL)) {
            cmd.set("#NormalSelected.Visible", true);
        } else if (gameMode.equals(StarterKitProvider.MODE_IRONMAN)) {
            cmd.set("#IronmanSelected.Visible", true);
        } else if (gameMode.equals(StarterKitProvider.MODE_HARDCORE_IRONMAN)) {
            cmd.set("#HardcoreSelected.Visible", true);
        }

        // 2. CONTENT: Populate Details Panel
        cmd.set("#DetailsPanel.Visible", true);

        String displayName = StarterKitProvider.getGameModeDisplayName(gameMode);
        String description = StarterKitProvider.getGameModeDescription(gameMode);

        cmd.set("#ConfirmationTitle.Text", displayName);
        cmd.set("#ConfirmationDescription.Text", description);

        // 3. WARNINGS: specific logic for Hardcore
        if (gameMode.equals(StarterKitProvider.MODE_HARDCORE_IRONMAN)) {
            cmd.set("#ConfirmationWarning.Text", "DEATH IS PERMANENT. NO RESPAWNS.");
            cmd.set("#ConfirmationWarning.Visible", true);
        } else {
            cmd.set("#ConfirmationWarning.Visible", false);
        }

        // 4. BINDING: Bind the 'Begin Adventure' button
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#ConfirmButton",
                EventData.of("Button", ACTION_CONFIRM), false);

        this.sendUpdate(cmd, evt, false);
    }

    /**
     * Locks in the choice, gives items, and closes.
     */
    private void handleConfirmSelection(Ref<EntityStore> ref, Store<EntityStore> store) {
        if (pendingGameMode == null) return;

        // Re-verify state to prevent exploits
        GameModeDataComponent component = store.getComponent(ref, Hyrune.getGameModeDataComponentType());
        if (component == null || !component.canSelectGameMode()) {
            this.close();
            return;
        }

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return;

        // Lock it in
        component.setGameMode(pendingGameMode);
        component.lockSelection();

        // Give Loot
        boolean success = StarterKitProvider.giveStarterKit(player.getInventory(), pendingGameMode);
        component.markStarterReceived();

        // Feedback
        String displayName = StarterKitProvider.getGameModeDisplayName(pendingGameMode);
        playerRef.sendMessage(Message.raw("You have selected " + displayName + "!"));

        int modeSound = SoundEvent.getAssetMap().getIndex(MODE_SOUND_ID);
        SoundUtil.playSoundEvent2dToPlayer(playerRef, modeSound, SoundCategory.SFX, 2.0f, 1.0f);

        if (!success) {
            playerRef.sendMessage(Message.raw("Warning: Inventory full. Some starter items may be missing."));
        }

        this.close();
    }

    public static class SelectionData {
        static final String KEY_BUTTON = "Button";

        public static final BuilderCodec<SelectionData> CODEC = BuilderCodec.builder(SelectionData.class, SelectionData::new)
                .addField(new KeyedCodec<>(KEY_BUTTON, Codec.STRING), (d, s) -> d.button = s, d -> d.button)
                .build();

        private String button;
    }
}
