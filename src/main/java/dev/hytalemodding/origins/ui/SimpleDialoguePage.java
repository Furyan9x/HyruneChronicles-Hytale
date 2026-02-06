package dev.hytalemodding.origins.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.Origins;
import dev.hytalemodding.origins.dialogue.SimpleDialogue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * SIMPLE DIALOGUE PAGE
 *
 * Displays a SimpleDialogue using the Hytale UI system.
 * Clean, straightforward, no conversion layers.
 */
public class SimpleDialoguePage extends InteractiveCustomUIPage<SimpleDialoguePage.DialogueData> {

    private static final String UI_PATH = "Pages/SimpleDialogue.ui";
    private static final String ACTION_SELECT_OPTION = "SelectOption";
    private static final int MAX_OPTIONS = 8;
    private static final int MAX_DIALOGUE_LINE_LENGTH = 70;

    private final SimpleDialogue dialogue;
    private final PlayerRef playerRef;
    private final Ref<EntityStore> playerEntityRef;
    private final Store<EntityStore> entityStore;

    private SimpleDialogue.Node currentNode;
    private List<SimpleDialogue.Choice> currentChoices = new ArrayList<>();

    /**
     * Create a dialogue page starting at the first node.
     */
    public SimpleDialoguePage(@Nonnull PlayerRef playerRef,
                              @Nonnull Ref<EntityStore> playerEntityRef,
                              @Nonnull Store<EntityStore> store,
                              @Nonnull SimpleDialogue dialogue) {
        this(playerRef, playerEntityRef, store, dialogue, null);
    }

    /**
     * Create a dialogue page starting at a specific node.
     */
    public SimpleDialoguePage(@Nonnull PlayerRef playerRef,
                              @Nonnull Ref<EntityStore> playerEntityRef,
                              @Nonnull Store<EntityStore> store,
                              @Nonnull SimpleDialogue dialogue,
                              @Nullable String startNodeId) {
        super(playerRef, CustomPageLifetime.CanDismiss, DialogueData.CODEC);
        this.playerRef = playerRef;
        this.playerEntityRef = playerEntityRef;
        this.entityStore = store;
        this.dialogue = dialogue;

        // Set starting node
        if (startNodeId != null) {
            this.currentNode = dialogue.getNode(startNodeId);
        }
        if (this.currentNode == null) {
            this.currentNode = dialogue.start();
        }
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder commandBuilder,
                      @Nonnull UIEventBuilder eventBuilder,
                      @Nonnull Store<EntityStore> store) {
        // Load the UI layout
        commandBuilder.append(UI_PATH);

        // Bind click events for all option buttons
        bindOptionEvents(eventBuilder);

        // Display the current node
        displayCurrentNode(commandBuilder, ref, store);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                @Nonnull DialogueData data) {
        super.handleDataEvent(ref, store, data);

        // Only handle option selection events
        if (!ACTION_SELECT_OPTION.equals(data.button)) {
            return;
        }

        // Refresh choices if needed
        if (currentChoices.isEmpty()) {
            refreshCurrentChoices(ref, store);
        }

        // Parse the option index
        int optionIndex = parseOptionIndex(data.optionIndex);
        if (optionIndex < 0 || optionIndex >= currentChoices.size()) {
            Origins.LOGGER.at(Level.WARNING).log("Invalid option index: " + data.optionIndex);
            return;
        }

        // Get the selected choice
        SimpleDialogue.Choice selectedChoice = currentChoices.get(optionIndex);
        // Execute the choice's action (if it has one)
        selectedChoice.execute(playerRef, playerEntityRef, entityStore);
        Player player = store.getComponent(playerEntityRef, Player.getComponentType());

        if (player != null) {
            // If the currently open page is NOT 'this' page, it means the action
            // opened a new UI (like the Slayer Shop).
            // We must STOP here. Calling close() now would close the NEW page.
            if (player.getPageManager().getCustomPage() != this) {
                return;
            }
        }

        // Handle navigation or closing
        if (selectedChoice.closes()) {
            this.close();
            return;
        }
        if (selectedChoice.navigates()) {
            navigateToNode(selectedChoice.nextNode, ref, store);
        }
    }

    /**
     * Bind click events for all option buttons.
     */
    private void bindOptionEvents(UIEventBuilder eventBuilder) {
        for (int i = 0; i < MAX_OPTIONS; i++) {
            eventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#Option" + i,
                    EventData.of("Button", ACTION_SELECT_OPTION).append("OptionIndex", String.valueOf(i)),
                    false
            );
        }
    }

    /**
     * Navigate to a different node in the dialogue.
     */
    private void navigateToNode(@Nullable String nodeId, Ref<EntityStore> ref, Store<EntityStore> store) {
        if (nodeId == null) {
            this.close();
            return;
        }

        SimpleDialogue.Node nextNode = dialogue.getNode(nodeId);
        if (nextNode == null) {
            Origins.LOGGER.at(Level.WARNING).log("Dialogue node not found: " + nodeId);
            showErrorNode(ref, store);
            return;
        }

        // Update current node
        this.currentNode = nextNode;

        // Update the UI
        UICommandBuilder cmd = new UICommandBuilder();
        displayCurrentNode(cmd, ref, store);
        UIEventBuilder evt = new UIEventBuilder();
        bindOptionEvents(evt);
        this.sendUpdate(cmd, evt, false);
    }

    /**
     * Display the current dialogue node.
     */
    private void displayCurrentNode(UICommandBuilder cmd, Ref<EntityStore> ref, Store<EntityStore> store) {
        if (currentNode == null) {
            showErrorNode(cmd);
            return;
        }

        // Get NPC name from dialogue ID (capitalize and replace underscores)
        String npcName = formatNpcName(dialogue.getId());
        cmd.set("#NpcName.Text", npcName);

        // Get the dialogue text (evaluates dynamic text if needed)
        String text = currentNode.getText(playerRef, playerEntityRef, entityStore);
        cmd.set("#DialogueText.Text", wrapText(text));

        // Get visible choices for this player
        List<SimpleDialogue.Choice> choices = currentNode.getVisibleChoices(playerRef, playerEntityRef, entityStore);

        // Limit to max options
        if (choices.size() > MAX_OPTIONS) {
            Origins.LOGGER.at(Level.WARNING).log("Dialogue node has " + choices.size()
                    + " options; only " + MAX_OPTIONS + " will be shown.");
            choices = choices.subList(0, MAX_OPTIONS);
        }

        // Store current choices
        currentChoices = new ArrayList<>(choices);

        // Apply choices to UI
        applyChoices(cmd, currentChoices);
    }

    /**
     * Refresh the current choices (called when handling events).
     */
    private void refreshCurrentChoices(Ref<EntityStore> ref, Store<EntityStore> store) {
        if (currentNode == null) {
            currentNode = dialogue.start();
        }

        if (currentNode == null) {
            currentChoices = List.of();
            return;
        }

        List<SimpleDialogue.Choice> choices = currentNode.getVisibleChoices(playerRef, playerEntityRef, entityStore);
        if (choices.size() > MAX_OPTIONS) {
            choices = choices.subList(0, MAX_OPTIONS);
        }
        currentChoices = new ArrayList<>(choices);
    }

    /**
     * Show an error message when node is missing.
     */
    private void showErrorNode(Ref<EntityStore> ref, Store<EntityStore> store) {
        UICommandBuilder cmd = new UICommandBuilder();
        showErrorNode(cmd);
        UIEventBuilder evt = new UIEventBuilder();
        bindOptionEvents(evt);
        this.sendUpdate(cmd, evt, false);
    }

    /**
     * Show an error message when node is missing.
     */
    private void showErrorNode(UICommandBuilder cmd) {
        String npcName = formatNpcName(dialogue.getId());
        cmd.set("#NpcName.Text", npcName);
        cmd.set("#DialogueText.Text", "This dialogue is unavailable right now.");

        // Create a single "Close" choice
        currentChoices = List.of(SimpleDialogue.choice("Close"));
        applyChoices(cmd, currentChoices);
    }

    /**
     * Apply choices to the UI buttons.
     * Each option has a number label and text label that need to be set separately.
     */
    private void applyChoices(UICommandBuilder cmd, List<SimpleDialogue.Choice> choices) {
        for (int i = 0; i < MAX_OPTIONS; i++) {
            boolean visible = i < choices.size();
            cmd.set("#Option" + i + ".Visible", visible);
            if (visible) {
                // Set the number (1., 2., 3., etc.)
                cmd.set("#Option" + i + "Number.Text", (i + 1) + ".");
                // Set the option text
                cmd.set("#Option" + i + "Text.Text", choices.get(i).text);
            }
        }
    }

    /**
     * Parse option index from string.
     */
    private int parseOptionIndex(@Nullable String optionIndex) {
        if (optionIndex == null) {
            return -1;
        }
        try {
            return Integer.parseInt(optionIndex);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Format NPC name from dialogue ID.
     * Converts "master_hans" to "Master Hans"
     */
    private String formatNpcName(String dialogueId) {
        if (dialogueId == null || dialogueId.isEmpty()) {
            return "NPC";
        }

        String[] parts = dialogueId.split("_");
        StringBuilder name = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            if (i > 0) name.append(" ");
            String part = parts[i];
            if (!part.isEmpty()) {
                name.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    name.append(part.substring(1));
                }
            }
        }

        return name.toString();
    }

    /**
     * Wrap text to fit the dialogue box.
     * Manual wrapping because the UI label does not auto-wrap.
     */
    private String wrapText(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }

        StringBuilder result = new StringBuilder();
        int lineLength = 0;

        for (String word : text.split(" ")) {
            int wordLength = word.length();

            if (lineLength == 0) {
                // First word on the line
                result.append(word);
                lineLength = wordLength;
                continue;
            }

            if (lineLength + 1 + wordLength > MAX_DIALOGUE_LINE_LENGTH) {
                // Word doesn't fit, start new line
                result.append("\n").append(word);
                lineLength = wordLength;
            } else {
                // Word fits on current line
                result.append(" ").append(word);
                lineLength += 1 + wordLength;
            }
        }

        return result.toString();
    }

    /**
     * Data class for handling UI events.
     */
    public static class DialogueData {
        static final String KEY_BUTTON = "Button";
        static final String KEY_OPTION_INDEX = "OptionIndex";

        public static final BuilderCodec<DialogueData> CODEC = BuilderCodec.builder(DialogueData.class, DialogueData::new)
                .addField(new KeyedCodec<>(KEY_BUTTON, Codec.STRING), (d, s) -> d.button = s, d -> d.button)
                .addField(new KeyedCodec<>(KEY_OPTION_INDEX, Codec.STRING), (d, s) -> d.optionIndex = s, d -> d.optionIndex)
                .build();

        private String button;
        private String optionIndex;
    }
}
