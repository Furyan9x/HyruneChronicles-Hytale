package dev.hytalemodding.hyrune.registry;

import dev.hytalemodding.hyrune.dialogue.SimpleDialogue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * SIMPLE DIALOGUE REGISTRY
 *
 * Just stores SimpleDialogue instances by ID. That's it.
 * No conversion, no compatibility layers, no nonsense.
 */
public class DialogueRegistry {

    private static final Map<String, SimpleDialogue> DIALOGUES = new HashMap<>();

    /**
     * Register a dialogue.
     */
    public static void register(@Nonnull String id, @Nonnull SimpleDialogue dialogue) {
        DIALOGUES.put(id.toLowerCase(), dialogue);
    }

    /**
     * Register a dialogue using its own ID.
     */
    public static void register(@Nonnull SimpleDialogue dialogue) {
        DIALOGUES.put(dialogue.getId().toLowerCase(), dialogue);
    }

    /**
     * Get a dialogue by ID.
     */
    @Nullable
    public static SimpleDialogue get(@Nonnull String id) {
        return DIALOGUES.get(id.toLowerCase());
    }

    /**
     * Check if a dialogue exists.
     */
    public static boolean has(@Nonnull String id) {
        return DIALOGUES.containsKey(id.toLowerCase());
    }

    /**
     * Remove a dialogue.
     */
    public static void unregister(@Nonnull String id) {
        DIALOGUES.remove(id.toLowerCase());
    }

    /**
     * Clear all dialogues.
     */
    public static void clear() {
        DIALOGUES.clear();
    }

    /**
     * Get the number of registered dialogues.
     */
    public static int count() {
        return DIALOGUES.size();
    }
}