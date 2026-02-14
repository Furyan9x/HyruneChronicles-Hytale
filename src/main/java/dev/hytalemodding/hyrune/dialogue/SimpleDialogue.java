package dev.hytalemodding.hyrune.dialogue;

import com.google.gson.*;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

/**
 * SIMPLE DIALOGUE SYSTEM
 * 
 * One class for all your dialogue needs. No builders, no registries, no complexity.
 * 
 * Usage:
 * 
 * // In-code dialogue
 * var hans = new SimpleDialogue("hans")
 *     .add("greeting", "Hello!",
 *         choice("Info", "info"),
 *         choice("Bye", null))
 *     .add("info", "Welcome!",
 *         choice("Back", "greeting"));
 * 
 * // Or load from JSON and add dynamic parts
 * var slayer = SimpleDialogue.fromJson("/dialogues/slayer.json");
 * slayer.node("greeting").setText((p,e,s) -> hasTask(p) ? "Turn in?" : "Want task?");
 * slayer.node("greeting").choice("Turn in").showIf((p,e,s) -> taskDone(p));
 */
public class SimpleDialogue {
    
    private final String id;
    private final Map<String, Node> nodes = new HashMap<>();
    private String startNode;
    
    public SimpleDialogue(String id) {
        this.id = id;
    }
    
    public String getId() {
        return id;
    }
    
    // ========== BUILDING DIALOGUES ==========
    
    /**
     * Add a node with static text and choices.
     */
    public SimpleDialogue add(String nodeId, String text, Choice... choices) {
        Node node = new Node(text);
        for (Choice c : choices) node.addChoice(c);
        nodes.put(nodeId, node);
        if (startNode == null) startNode = nodeId;
        return this;
    }
    
    /**
     * Add a node with dynamic text (changes based on player state).
     */
    public SimpleDialogue addDynamic(String nodeId, TextProvider textProvider, Choice... choices) {
        Node node = new Node(textProvider);
        for (Choice c : choices) node.addChoice(c);
        nodes.put(nodeId, node);
        if (startNode == null) startNode = nodeId;
        return this;
    }
    
    /**
     * Get a node to modify it after creation.
     */
    public Node node(String id) {
        return nodes.get(id);
    }
    
    /**
     * Get the starting node.
     */
    public Node start() {
        return nodes.get(startNode);
    }
    
    /**
     * Get a specific node by ID.
     */
    public Node getNode(String nodeId) {
        return nodes.get(nodeId);
    }
    
    /**
     * Set which node starts the dialogue.
     */
    public SimpleDialogue setStart(String nodeId) {
        this.startNode = nodeId;
        return this;
    }
    
    // ========== LOADING FROM JSON ==========
    
    /**
     * Load dialogue from JSON resource.
     * 
     * JSON format:
     * {
     *   "greeting": {
     *     "text": "Hello!",
     *     "choices": [
     *       {"text": "Info", "goto": "info"},
     *       {"text": "Bye"}
     *     ]
     *   },
     *   "info": {
     *     "text": "Information here",
     *     "choices": [{"text": "Back", "goto": "greeting"}]
     *   }
     * }
     */
    public static SimpleDialogue fromJson(String resourcePath) {
        try (var in = SimpleDialogue.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new RuntimeException("Resource not found: " + resourcePath);
            }
            
            JsonObject json = JsonParser.parseReader(new InputStreamReader(in)).getAsJsonObject();
            
            // Extract ID if present, otherwise use filename
            String id = json.has("id") ? json.get("id").getAsString() : 
                        resourcePath.substring(resourcePath.lastIndexOf('/') + 1, resourcePath.lastIndexOf('.'));
            
            SimpleDialogue dialogue = new SimpleDialogue(id);
            
            // Extract start node if specified
            String startNodeId = json.has("start") ? json.get("start").getAsString() : null;
            
            // Parse all nodes
            for (String nodeId : json.keySet()) {
                if (nodeId.equals("id") || nodeId.equals("start")) continue;
                
                JsonObject nodeJson = json.getAsJsonObject(nodeId);
                String text = nodeJson.get("text").getAsString();
                
                List<Choice> choices = new ArrayList<>();
                if (nodeJson.has("choices")) {
                    for (JsonElement elem : nodeJson.getAsJsonArray("choices")) {
                        JsonObject choiceJson = elem.getAsJsonObject();
                        String choiceText = choiceJson.get("text").getAsString();
                        String goTo = choiceJson.has("goto") ? choiceJson.get("goto").getAsString() : null;
                        choices.add(new Choice(choiceText, goTo));
                    }
                }
                
                dialogue.add(nodeId, text, choices.toArray(new Choice[0]));
            }
            
            // Set start node
            if (startNodeId != null) {
                dialogue.setStart(startNodeId);
            }
            
            return dialogue;
        } catch (RuntimeException | IOException e) {
            throw new RuntimeException("Failed to load dialogue: " + resourcePath, e);
        }
    }
    
    // ========== HELPER METHODS ==========
    
    /**
     * Create a choice that navigates to another node.
     */
    public static Choice choice(String text, String goToNode) {
        return new Choice(text, goToNode);
    }
    
    /**
     * Create a choice that closes the dialogue.
     */
    public static Choice choice(String text) {
        return new Choice(text, null);
    }
    
    // ========== INNER CLASSES ==========
    
    /**
     * A single dialogue node containing text and player choices.
     */
    public static class Node {
        private Object textProvider; // String or TextProvider
        private final List<Choice> choices = new ArrayList<>();
        
        public Node(String staticText) {
            this.textProvider = staticText;
        }
        
        public Node(TextProvider dynamicText) {
            this.textProvider = dynamicText;
        }
        
        /**
         * Change to static text.
         */
        public void setText(String text) {
            this.textProvider = text;
        }
        
        /**
         * Change to dynamic text that depends on player state.
         */
        public void setText(TextProvider textProvider) {
            this.textProvider = textProvider;
        }
        
        /**
         * Get the text to display (evaluates dynamic text if needed).
         */
        public String getText(PlayerRef playerRef, Ref<EntityStore> playerEntityRef, Store<EntityStore> store) {
            if (textProvider instanceof String) {
                return (String) textProvider;
            } else if (textProvider instanceof TextProvider) {
                return ((TextProvider) textProvider).getText(playerRef, playerEntityRef, store);
            }
            return "";
        }
        
        /**
         * Add a choice to this node.
         */
        public void addChoice(Choice choice) {
            choices.add(choice);
        }
        
        /**
         * Get a specific choice by its text.
         */
        public Choice choice(String text) {
            for (Choice c : choices) {
                if (c.text.equals(text)) return c;
            }
            return null;
        }
        
        /**
         * Get all visible choices for a player.
         */
        public List<Choice> getVisibleChoices(PlayerRef playerRef, Ref<EntityStore> playerEntityRef, Store<EntityStore> store) {
            List<Choice> visible = new ArrayList<>();
            for (Choice c : choices) {
                if (c.isVisible(playerRef, playerEntityRef, store)) {
                    visible.add(c);
                }
            }
            return visible;
        }
        
        /**
         * Get all choices (even hidden ones).
         */
        public List<Choice> getAllChoices() {
            return new ArrayList<>(choices);
        }
    }
    
    /**
     * A player choice/option in the dialogue.
     */
    public static class Choice {
        public final String text;
        public final String nextNode; // null = close dialogue
        private VisibilityCondition visibilityCheck;
        private ChoiceAction action;
        
        public Choice(String text, String nextNode) {
            this.text = text;
            this.nextNode = nextNode;
        }
        
        /**
         * Only show this choice if the condition is true.
         */
        public Choice showIf(VisibilityCondition condition) {
            this.visibilityCheck = condition;
            return this;
        }
        
        /**
         * Execute an action when this choice is selected.
         * Action is called BEFORE navigating to next node.
         */
        public Choice onSelect(ChoiceAction action) {
            this.action = action;
            return this;
        }
        
        /**
         * Check if this choice should be visible to the player.
         */
        public boolean isVisible(PlayerRef playerRef, Ref<EntityStore> playerEntityRef, Store<EntityStore> store) {
            return visibilityCheck == null || visibilityCheck.isVisible(playerRef, playerEntityRef, store);
        }
        
        /**
         * Execute the choice's action (if it has one).
         */
        public void execute(PlayerRef playerRef, Ref<EntityStore> playerEntityRef, Store<EntityStore> store) {
            if (action != null) {
                action.execute(playerRef, playerEntityRef, store);
            }
        }
        
        /**
         * Does this choice close the dialogue?
         */
        public boolean closes() {
            return nextNode == null;
        }
        
        /**
         * Does this choice navigate to another node?
         */
        public boolean navigates() {
            return nextNode != null;
        }
    }
    
    // ========== FUNCTIONAL INTERFACES ==========
    
    /**
     * Provides dynamic text based on player state.
     */
    @FunctionalInterface
    public interface TextProvider {
        String getText(PlayerRef playerRef, Ref<EntityStore> playerEntityRef, Store<EntityStore> store);
    }
    
    /**
     * Determines if a choice should be visible.
     */
    @FunctionalInterface
    public interface VisibilityCondition {
        boolean isVisible(PlayerRef playerRef, Ref<EntityStore> playerEntityRef, Store<EntityStore> store);
    }
    
    /**
     * Action executed when a choice is selected.
     */
    @FunctionalInterface
    public interface ChoiceAction {
        void execute(PlayerRef playerRef, Ref<EntityStore> playerEntityRef, Store<EntityStore> store);
    }
}

