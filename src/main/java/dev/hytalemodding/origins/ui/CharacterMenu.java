package dev.hytalemodding.origins.ui;

import dev.hytalemodding.origins.level.LevelingService;
import dev.hytalemodding.origins.classes.Classes;
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
import dev.hytalemodding.origins.playerdata.PlayerAttributes;
import dev.hytalemodding.origins.util.AttributeManager;
import dev.hytalemodding.origins.util.NameplateManager;

import javax.annotation.Nonnull;

/**
 * The main UI menu for viewing character stats and selecting classes.
 */
public class CharacterMenu extends InteractiveCustomUIPage<CharacterMenu.CharacterData> {

    private static final String MAIN_UI = "Pages/character_stats.ui";
    private static final String ENTRY_UI = "Pages/ClassEntry.ui";

    public CharacterMenu(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, CharacterData.CODEC);
    }

    /**
     * Builds the initial UI state when the menu is opened.
     */
    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {
        // Load the base UI layout
        commandBuilder.append(MAIN_UI);

        // Populate header data with player information
        var uuid = this.playerRef.getUuid();
        int globalLevel = LevelingService.get().getAdventurerLevel(uuid);
        String activeClass = LevelingService.get().getActiveClassId(uuid);

        commandBuilder.set("#PlayerName.Text", this.playerRef.getUsername());
        commandBuilder.set("#GlobalLevel.Text", "Global Level: " + globalLevel);
        commandBuilder.set("#ActiveClass.Text", "Active Class: " + (activeClass != null ? activeClass.toUpperCase() : "None"));


        PlayerAttributes stats = AttributeManager.getInstance().getPlayerData(uuid);
        commandBuilder.set("#StrValue.Text", String.valueOf(stats.getStrength()));
        commandBuilder.set("#AgiValue.Text", String.valueOf(stats.getAgility())); // Agility maps to Dex UI
        commandBuilder.set("#IntValue.Text", String.valueOf(stats.getIntellect()));

        // Assuming you have labels for the other 2 stats (Uncomment/Rename as needed):
        commandBuilder.set("#ConValue.Text", String.valueOf(stats.getConstitution()));
        commandBuilder.set("#WisValue.Text", String.valueOf(stats.getWisdom()));

        // Bind global menu buttons
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BtnClose", EventData.of("Button", "Close"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BtnTalents", EventData.of("Button", "Talents"), false);

        // Build the dynamic list of available classes
        this.buildClassList(commandBuilder, eventBuilder);
    }

    /**
     * Populates the class list container with entries for each available class.
     * Updates styling based on active status and level.
     */
    private void buildClassList(@Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder) {
        commandBuilder.clear("#ClassListContainer");

        String activeClassId = LevelingService.get().getActiveClassId(this.playerRef.getUuid());
        var uuid = this.playerRef.getUuid();

        int i = 0;
        for (Classes rpgClass : Classes.values()) {
            commandBuilder.append("#ClassListContainer", ENTRY_UI);

            String rowRoot = "#ClassListContainer[" + i + "]";
            int level = LevelingService.get().getClassLevel(uuid, rpgClass.getId());
            boolean isActive = rpgClass.getId().equalsIgnoreCase(activeClassId);

            // Apply styling based on class state
            if (isActive) {
                // Active Class: Highlighted Gold
                commandBuilder.set(rowRoot + " #ClassName.Style.TextColor", "#ffcc00");
                commandBuilder.set(rowRoot + " #ClassLevel.Style.TextColor", "#ffcc00");
            } else if (level == 0) {
                // Locked/Unvisited: Dimmed Gray
                commandBuilder.set(rowRoot + " #ClassName.Style.TextColor", "#666666");
                commandBuilder.set(rowRoot + " #ClassLevel.Visible", false);
            } else {
                // Discovered: Default Colors
                commandBuilder.set(rowRoot + " #ClassName.Style.TextColor", "#ffffff");
                commandBuilder.set(rowRoot + " #ClassLevel.Style.TextColor", "#93844c");
            }

            // Set text information
            commandBuilder.set(rowRoot + " #ClassName.Text", rpgClass.getDisplayName());
            commandBuilder.set(rowRoot + " #ClassLevel.Text", "Lvl " + level);

            // Bind click event to select the class
            eventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    rowRoot,
                    EventData.of("Button", "SelectClass").append("ClassID", rpgClass.getId()),
                    false
            );

            i++;
        }
    }

    /**
     * Handles data events sent from the client UI.
     */
    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull CharacterData data) {
        super.handleDataEvent(ref, store, data);

        if (data.button == null) return;

        switch (data.button) {
            case "Close":
                this.close();
                break;
            case "Talents":
                this.playerRef.sendMessage(com.hypixel.hytale.server.core.Message.raw("Talent Tree Coming Soon!"));
                break;
            case "SelectClass":
                if (data.classId != null) {
                    handleClassSelection(data.classId);
                }
                break;
        }
    }

    /**
     * Processes class change requests and refreshes the UI if successful.
     */
    private void handleClassSelection(String classId) {
        var result = LevelingService.get().changeClass(this.playerRef.getUuid(), classId);

        if (result == LevelingService.ClassChangeResult.SUCCESS) {
            UICommandBuilder refreshCmd = new UICommandBuilder();
            UIEventBuilder refreshEvent = new UIEventBuilder();

            // Refresh header and class list
            refreshCmd.set("#ActiveClass.Text", "Active Class: " + classId.toUpperCase());
            this.buildClassList(refreshCmd, refreshEvent);
            
            // Update world visuals
            NameplateManager.update(this.playerRef.getUuid());

            // Synchronize update with client
            this.sendUpdate(refreshCmd, refreshEvent, false);
        } else {
            this.playerRef.sendMessage(com.hypixel.hytale.server.core.Message.raw("Cannot switch class: " + result.name()));
        }
    }

    /**
     * Data codec for communicating with the Custom UI client.
     */
    public static class CharacterData {
        static final String KEY_BUTTON = "Button";
        static final String KEY_CLASS_ID = "ClassID";

        public static final BuilderCodec<CharacterData> CODEC = BuilderCodec.builder(CharacterData.class, CharacterData::new)
                .addField(new KeyedCodec<>(KEY_BUTTON, Codec.STRING), (d, s) -> d.button = s, d -> d.button)
                .addField(new KeyedCodec<>(KEY_CLASS_ID, Codec.STRING), (d, s) -> d.classId = s, d -> d.classId)
                .build();

        private String button;
        private String classId;
    }
}