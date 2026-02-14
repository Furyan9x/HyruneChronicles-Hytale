package dev.hytalemodding.hyrune.dialogue;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.corecomponents.ActionBase;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import dev.hytalemodding.Hyrune;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import dev.hytalemodding.hyrune.registry.DialogueRegistry;
import dev.hytalemodding.hyrune.ui.SimpleDialoguePage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;

/**
 * SIMPLE ACTION OPEN DIALOGUE
 *
 * Opens a SimpleDialogue when player presses F on an NPC.
 * No conversion, no old system, just works directly with SimpleDialogue.
 */
public class ActionOpenDialogue extends ActionBase {

    public ActionOpenDialogue(@Nonnull BuilderActionOpenDialogue builder) {
        super(builder);
    }

    @Override
    public boolean canExecute(@Nonnull Ref<EntityStore> ref,
                              @Nonnull Role role,
                              InfoProvider sensorInfo,
                              double dt,
                              @Nonnull Store<EntityStore> store) {
        return super.canExecute(ref, role, sensorInfo, dt, store)
                && role.getStateSupport().getInteractionIterationTarget() != null;
    }

    @Override
    public boolean execute(@Nonnull Ref<EntityStore> ref,
                           @Nonnull Role role,
                           InfoProvider sensorInfo,
                           double dt,
                           @Nonnull Store<EntityStore> store) {
        super.execute(ref, role, sensorInfo, dt, store);

        Ref<EntityStore> playerReference = role.getStateSupport().getInteractionIterationTarget();
        if (playerReference == null) {
            return false;
        }

        PlayerRef playerRefComponent = store.getComponent(playerReference, PlayerRef.getComponentType());
        Player playerComponent = store.getComponent(playerReference, Player.getComponentType());
        if (playerRefComponent == null || playerComponent == null) {
            return false;
        }

        // Resolve which dialogue to use based on NPC name/ID
        String dialogueKey = resolveDialogueKey(ref, role, store);
        if (dialogueKey == null || dialogueKey.isBlank()) {
            Hyrune.LOGGER.at(Level.WARNING).log("OpenDialogue action could not resolve dialogue key.");
            return false;
        }

        // Get the dialogue from the registry
        SimpleDialogue dialogue = DialogueRegistry.get(dialogueKey);
        if (dialogue == null) {
            playerRefComponent.sendMessage(Message.raw("This NPC has nothing to say right now."));
            Hyrune.LOGGER.at(Level.WARNING).log("Missing dialogue for npcId: " + dialogueKey);
            return false;
        }

        // Open the dialogue UI
        playerComponent.getPageManager().openCustomPage(
                playerReference,
                store,
                new SimpleDialoguePage(playerRefComponent, playerReference, store, dialogue)
        );

        return true;
    }

    /**
     * Figure out which dialogue to use based on the NPC's name/ID.
     * Tries multiple sources in priority order.
     */
    private String resolveDialogueKey(@Nonnull Ref<EntityStore> npcRef,
                                      @Nonnull Role role,
                                      @Nonnull Store<EntityStore> store) {
        // Try nameplate first
        Nameplate nameplate = store.getComponent(npcRef, Nameplate.getComponentType());
        if (nameplate != null) {
            String resolved = firstMatchingDialogueKey(nameplate.getText());
            if (resolved != null) {
                return resolved;
            }
        }

        // Try display name
        DisplayNameComponent displayName = store.getComponent(npcRef, DisplayNameComponent.getComponentType());
        if (displayName != null && displayName.getDisplayName() != null) {
            String resolved = firstMatchingDialogueKey(displayName.getDisplayName().getRawText());
            if (resolved != null) {
                return resolved;
            }
        }

        // Try role name
        String roleName = role.getRoleName();
        String resolvedRole = firstMatchingDialogueKey(roleName);
        if (resolvedRole != null) {
            return resolvedRole;
        }

        // Try appearance name
        String appearance = role.getAppearanceName();
        String resolvedAppearance = firstMatchingDialogueKey(appearance);
        if (resolvedAppearance != null) {
            return resolvedAppearance;
        }

        // Try NPC type ID
        NPCEntity npc = store.getComponent(npcRef, NPCEntity.getComponentType());
        if (npc != null) {
            String resolvedType = firstMatchingDialogueKey(npc.getNPCTypeId());
            if (resolvedType != null) {
                return resolvedType;
            }
        }

        // Try translation key as last resort
        String translationKey = role.getNameTranslationKey();
        String resolvedTranslation = firstMatchingDialogueKey(translationKey);
        if (resolvedTranslation != null) {
            return resolvedTranslation;
        }

        return null;
    }

    @Nullable
    private static String firstMatchingDialogueKey(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        for (String candidate : toCandidates(raw)) {
            if (DialogueRegistry.has(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static Set<String> toCandidates(String raw) {
        Set<String> candidates = new LinkedHashSet<>();

        String base = raw.trim().toLowerCase();
        if (base.isEmpty()) {
            return candidates;
        }

        candidates.add(base);

        int closeBracket = base.indexOf(']');
        if (closeBracket >= 0 && closeBracket + 1 < base.length()) {
            String withoutPrefix = base.substring(closeBracket + 1).trim();
            if (!withoutPrefix.isEmpty()) {
                candidates.add(withoutPrefix);
            }
        }

        Set<String> extra = new LinkedHashSet<>();
        for (String value : candidates) {
            extra.add(value.replace(' ', '_'));
            extra.add(value.replace('-', '_'));
            extra.add(value.replace(' ', '_').replace('-', '_'));
            extra.add(value.replace("_", " "));
        }
        candidates.addAll(extra);

        Set<String> normalized = new LinkedHashSet<>();
        for (String value : candidates) {
            String cleaned = value.replaceAll("[^a-z0-9_ ]", "")
                .replaceAll("\\s+", " ")
                .trim();
            if (!cleaned.isEmpty()) {
                normalized.add(cleaned);
                normalized.add(cleaned.replace(' ', '_'));
            }
        }

        return normalized;
    }
}
