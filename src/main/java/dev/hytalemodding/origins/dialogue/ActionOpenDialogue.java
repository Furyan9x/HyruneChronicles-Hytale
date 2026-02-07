package dev.hytalemodding.origins.dialogue;

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
import dev.hytalemodding.Origins;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import dev.hytalemodding.origins.registry.DialogueRegistry;
import dev.hytalemodding.origins.ui.SimpleDialoguePage;

import javax.annotation.Nonnull;
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
            Origins.LOGGER.at(Level.WARNING).log("OpenDialogue action could not resolve dialogue key.");
            return false;
        }

        // Get the dialogue from the registry
        SimpleDialogue dialogue = DialogueRegistry.get(dialogueKey);
        if (dialogue == null) {
            playerRefComponent.sendMessage(Message.raw("This NPC has nothing to say right now."));
            Origins.LOGGER.at(Level.WARNING).log("Missing dialogue for npcId: " + dialogueKey);
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
            String text = nameplate.getText();
            if (text != null && !text.isBlank()) {
                return text.trim().toLowerCase();
            }
        }

        // Try display name
        DisplayNameComponent displayName = store.getComponent(npcRef, DisplayNameComponent.getComponentType());
        if (displayName != null && displayName.getDisplayName() != null) {
            String raw = displayName.getDisplayName().getRawText();
            if (raw != null && !raw.isBlank()) {
                return raw.trim().toLowerCase();
            }
        }

        // Try role name
        String roleName = role.getRoleName();
        if (roleName != null && !roleName.isBlank()) {
            return roleName.trim().toLowerCase();
        }

        // Try appearance name
        String appearance = role.getAppearanceName();
        if (appearance != null && !appearance.isBlank()) {
            return appearance.trim().toLowerCase();
        }

        // Try NPC type ID
        NPCEntity npc = store.getComponent(npcRef, NPCEntity.getComponentType());
        if (npc != null) {
            String typeId = npc.getNPCTypeId();
            if (typeId != null && !typeId.isBlank()) {
                return typeId.trim().toLowerCase();
            }
        }

        // Try translation key as last resort
        String translationKey = role.getNameTranslationKey();
        if (translationKey != null && !translationKey.isBlank()) {
            return translationKey.trim().toLowerCase();
        }

        return null;
    }
}
