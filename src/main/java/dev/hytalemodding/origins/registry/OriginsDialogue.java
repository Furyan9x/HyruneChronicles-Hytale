package dev.hytalemodding.origins.registry;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.asset.builder.BuilderFactory;
import com.hypixel.hytale.server.npc.instructions.Action;
import dev.hytalemodding.Origins;
import dev.hytalemodding.origins.component.GameModeDataComponent;
import dev.hytalemodding.origins.dialogue.*;
import dev.hytalemodding.origins.level.LevelingService;
import dev.hytalemodding.origins.skills.SkillType;
import dev.hytalemodding.origins.slayer.SlayerService;
import dev.hytalemodding.origins.slayer.SlayerTaskAssignment;
import dev.hytalemodding.origins.slayer.SlayerTaskState;
import dev.hytalemodding.origins.slayer.SlayerTurnInResult;
import dev.hytalemodding.origins.ui.GameModeSelectionPage;
import dev.hytalemodding.origins.ui.SlayerVendorPage;

import java.util.logging.Level;

import static dev.hytalemodding.origins.dialogue.SimpleDialogue.choice;

/**
 *
 *
 *
 *
 *
 */
public class OriginsDialogue {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static LevelingService levelingService;
    private static SlayerService slayerService;

    public static void init(LevelingService leveling, SlayerService slayer) {
        levelingService = leveling;
        slayerService = slayer;

        registerNpcActions();
        registerDialogues();
    }

    private static void registerNpcActions() {
        NPCPlugin npcPlugin = NPCPlugin.get();
        if (npcPlugin == null) {
            return;
        }

        BuilderFactory<Action> actionFactory = npcPlugin.getBuilderManager().getFactory(Action.class);
        if (actionFactory == null) {
            actionFactory = new BuilderFactory<>(Action.class, NPCPlugin.FACTORY_CLASS_ACTION);
            npcPlugin.getBuilderManager().registerFactory(actionFactory);
        }

        actionFactory.add("OpenDialogue", BuilderActionOpenDialogue::new);
    }

    private static void registerDialogues() {
        registerMasterHans();
        registerSlayerMaster();
    }

    // ==================== MASTER HANS (10 LINES!) ====================

    private static void registerMasterHans() {
        var hans = new SimpleDialogue("master_hans")
                .add("greeting", "Hello, adventurer! Welcome to -server-. ",
                        choice("Tell me about this place", "info"),
                        choice("I want to choose my game mode")
                                .showIf((playerRef, playerEntityRef, store) -> {
                                    // 1. Try to get the component
                                    GameModeDataComponent component = store.getComponent(playerEntityRef, Origins.getGameModeDataComponentType());

                                    // 2. Visible if component is missing OR setup isn't done
                                    return component == null || !component.hasCompletedSetup();
                                })
                                .onSelect((playerRef, playerEntityRef, store) -> {
                                    Player player = store.getComponent(playerEntityRef, Player.getComponentType());
                                    if (player != null) {
                                        player.getPageManager().openCustomPage(playerEntityRef, store, new GameModeSelectionPage(playerRef));
                                    }
                                }),
                        choice("Goodbye"))
                .add("info", "Welcome to our village! Feel free to explore.",
                        choice("Thanks", "greeting"));

        // Add action to open game mode UI (opens then closes dialogue)
        hans.node("greeting").choice("I want to choose my game mode")
                .onSelect((playerRef, playerEntityRef, store) -> {
                    Player player = store.getComponent(playerEntityRef, Player.getComponentType());
                    if (player != null) {
                        player.getPageManager().openCustomPage(playerEntityRef, store, new GameModeSelectionPage(playerRef));
                    }
                });

        DialogueRegistry.register(hans);
    }

    // ==================== SLAYER MASTER (35 LINES!) ====================

    private static void registerSlayerMaster() {
        if (slayerService == null) {
            LOGGER.at(Level.WARNING).log("SlayerService is null, skipping Slayer Master dialogue");
            return;
        }

        var slayer = new SimpleDialogue("slayer_master")
                .add("greeting", "Greetings.",
                        choice("I have completed my task.", "turn_in"),
                        choice("What is the Slayer skill?", "explain"),
                        choice("Give me a Slayer task.", "request"),
                        choice("View Slayer shop."),
                        choice("Nevermind."))
                .add("explain",
                        "Slayer tasks assign specific foes to defeat. Complete tasks to earn Slayer XP and points. " +
                                "Points can be spent at the Slayer shop for rare rewards.",
                        choice("Back.", "greeting"),
                        choice("Nevermind."))
                .add("request", "I have no tasks for you right now.",
                        choice("Understood.", "greeting"),
                        choice("Thanks."))
                .add("turn_in", "You have not completed your task yet.",
                        choice("Understood.", "greeting"),
                        choice("Thanks."));

        // === DYNAMIC TEXT ===

        slayer.node("greeting").setText((playerRef, playerEntityRef, store) -> {
            SlayerTaskAssignment assignment = slayerService.getAssignment(playerRef.getUuid());
            if (assignment == null) {
                return "Greetings. Looking for a Slayer task, or just curious about the skill?";
            }
            if (assignment.getState() == SlayerTaskState.COMPLETED) {
                return "You have completed your task. Would you like to turn it in?";
            }
            String target = assignment.getTargetNpcTypeId();
            return "You are assigned to slay " + target + "s. Remaining: " + assignment.getRemainingKills() + ".";
        });

        slayer.node("request").setText((playerRef, playerEntityRef, store) -> {
            int slayerLevel = getSlayerLevel(playerRef);
            SlayerTaskAssignment assignment = slayerService.assignTask(playerRef.getUuid(), slayerLevel);
            if (assignment == null) {
                return "I have no tasks suitable for you right now.";
            }
            String target = assignment.getTargetNpcTypeId();
            return "Your task is to slay " + assignment.getTotalKills() + " " + target + "s. Return when you are done.";
        });

        slayer.node("turn_in").setText((playerRef, playerEntityRef, store) -> {
            Player player = store.getComponent(playerEntityRef, Player.getComponentType());
            if (player == null) {
                return "I can't find your records right now.";
            }
            SlayerTurnInResult result = slayerService.turnInTask(player);
            if (result == null) {
                return "You have not completed your task yet.";
            }
            String message = "Well done. You earned " + result.getPointsAwarded()
                    + " Slayer points and " + result.getSlayerXpAwarded() + " Slayer XP.";
            if (result.isItemRewarded()) {
                message += " A special item reward is pending.";
            }
            return message;
        });

        // === CONDITIONAL VISIBILITY ===

        slayer.node("greeting").choice("I have completed my task.")
                .showIf((playerRef, playerEntityRef, store) -> {
                    SlayerTaskAssignment assignment = slayerService.getAssignment(playerRef.getUuid());
                    return assignment != null && assignment.getState() == SlayerTaskState.COMPLETED;
                });

        slayer.node("greeting").choice("Give me a Slayer task.")
                .showIf((playerRef, playerEntityRef, store) ->
                        slayerService.getAssignment(playerRef.getUuid()) == null);

        // === ACTIONS ===

        // Open slayer shop (then close dialogue)
        slayer.node("greeting").choice("View Slayer shop.")
                .onSelect((playerRef, playerEntityRef, store) -> {
                    Player player = store.getComponent(playerEntityRef, Player.getComponentType());
                    if (player != null) {
                        player.getPageManager().openCustomPage(playerEntityRef, store, new SlayerVendorPage(playerRef, slayerService));
                    }
                });

        DialogueRegistry.register(slayer);
    }

    // ==================== HELPER METHODS ====================

    private static int getSlayerLevel(com.hypixel.hytale.server.core.universe.PlayerRef playerRef) {
        if (levelingService == null) {
            LOGGER.at(Level.WARNING).log("LevelingService is null during dialogue. Defaulting to level 1.");
            return 1;
        }
        try {
            return levelingService.getSkillLevel(playerRef.getUuid(), SkillType.SLAYER);
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Failed to get Slayer level for " + playerRef.getUsername() + ": " + e.getMessage());
            return 1;
        }
    }
}
