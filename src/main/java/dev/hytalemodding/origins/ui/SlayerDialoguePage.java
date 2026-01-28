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
import dev.hytalemodding.origins.level.LevelingService;
import dev.hytalemodding.origins.skills.SkillType;
import dev.hytalemodding.origins.slayer.SlayerService;
import dev.hytalemodding.origins.slayer.SlayerTaskAssignment;
import dev.hytalemodding.origins.slayer.SlayerTaskState;
import dev.hytalemodding.origins.slayer.SlayerTurnInResult;

import javax.annotation.Nonnull;

public class SlayerDialoguePage extends InteractiveCustomUIPage<SlayerDialoguePage.DialogueData> {

    private static final String UI_PATH = "Pages/SlayerDialogue.ui";
    private static final int MAX_DIALOGUE_LINE_LENGTH = 70;

    private static final String OPTION_TURN_IN = "TurnIn";
    private static final String OPTION_EXPLAIN = "Explain";
    private static final String OPTION_REQUEST = "Request";
    private static final String OPTION_SHOP = "Shop";
    private static final String OPTION_BACK = "Back";
    private static final String OPTION_THANKS = "Thanks";
    private static final String OPTION_NEVERMIND = "Nevermind";

    private final SlayerService slayerService;

    public SlayerDialoguePage(@Nonnull PlayerRef playerRef, SlayerService slayerService) {
        super(playerRef, CustomPageLifetime.CanDismiss, DialogueData.CODEC);
        this.slayerService = slayerService;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder commandBuilder,
                      @Nonnull UIEventBuilder eventBuilder,
                      @Nonnull Store<EntityStore> store) {
        commandBuilder.append(UI_PATH);
        applyRootView(commandBuilder);

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#OptionTurnIn",
                EventData.of("Button", "SelectOption").append("OptionId", OPTION_TURN_IN), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#OptionExplain",
                EventData.of("Button", "SelectOption").append("OptionId", OPTION_EXPLAIN), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#OptionRequest",
                EventData.of("Button", "SelectOption").append("OptionId", OPTION_REQUEST), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#OptionShop",
                EventData.of("Button", "SelectOption").append("OptionId", OPTION_SHOP), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#OptionBack",
                EventData.of("Button", "SelectOption").append("OptionId", OPTION_BACK), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#OptionThanks",
                EventData.of("Button", "SelectOption").append("OptionId", OPTION_THANKS), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#OptionNevermind",
                EventData.of("Button", "SelectOption").append("OptionId", OPTION_NEVERMIND), false);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull DialogueData data) {
        super.handleDataEvent(ref, store, data);
        if (data.button == null) {
            return;
        }

        if ("SelectOption".equals(data.button)) {
            handleOption(ref, store, data.optionId);
        }
    }

    private void handleOption(Ref<EntityStore> ref, Store<EntityStore> store, String optionId) {
        if (optionId == null) {
            return;
        }

        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder evt = new UIEventBuilder();

        switch (optionId) {
            case OPTION_EXPLAIN:
                applyExplainView(cmd);
                break;
            case OPTION_REQUEST:
                applyTaskRequestView(cmd);
                break;
            case OPTION_SHOP:
                openShop(ref, store);
                return;
            case OPTION_TURN_IN:
                applyTurnInView(cmd, ref, store);
                break;
            case OPTION_BACK:
                applyRootView(cmd);
                break;
            case OPTION_THANKS:
            case OPTION_NEVERMIND:
                this.close();
                return;
            default:
                return;
        }

        this.sendUpdate(cmd, evt, false);
    }

    private void applyRootView(UICommandBuilder cmd) {
        SlayerTaskAssignment assignment = slayerService.getAssignment(playerRef.getUuid());
        if (assignment == null) {
            cmd.set("#DialogueText.Text", wrapText("Greetings. Looking for a Slayer task, or just curious about the skill?"));
            setOptionVisibility(cmd, false, true, true, true, false, false, true);
            return;
        }

        if (assignment.getState() == SlayerTaskState.COMPLETED) {
            cmd.set("#DialogueText.Text", wrapText("You have completed your task. Would you like to turn it in?"));
            setOptionVisibility(cmd, true, true, false, true, false, false, true);
            return;
        }

        String target = assignment.getTargetNpcTypeId();
        cmd.set("#DialogueText.Text", wrapText(
                "You are assigned to slay " + target + "s. Remaining: " + assignment.getRemainingKills() + "."));
        setOptionVisibility(cmd, false, true, false, true, false, false, true);
    }

    private void applyExplainView(UICommandBuilder cmd) {
        cmd.set("#DialogueText.Text", wrapText(
                "Slayer tasks assign specific foes to defeat. Complete tasks to earn Slayer XP and points. "
                        + "Points can be spent at the Slayer shop for rare rewards."));
        setOptionVisibility(cmd, false, false, false, false, true, false, true);
    }

    private void applyTaskRequestView(UICommandBuilder cmd) {
        int slayerLevel = getSlayerLevel();
        SlayerTaskAssignment assignment = slayerService.assignTask(playerRef.getUuid(), slayerLevel);
        if (assignment == null) {
            cmd.set("#DialogueText.Text", wrapText("I have no tasks suitable for you right now."));
            setOptionVisibility(cmd, false, false, false, false, true, false, true);
            return;
        }

        String target = assignment.getTargetNpcTypeId();
        cmd.set("#DialogueText.Text", wrapText(
                "Your task is to slay " + assignment.getTotalKills() + " " + target + "s. Return when you are done."));
        setOptionVisibility(cmd, false, false, false, false, false, true, true);
    }

    private void applyTurnInView(UICommandBuilder cmd, Ref<EntityStore> ref, Store<EntityStore> store) {
        Player player = store.getComponent(ref, Player.getComponentType());
        SlayerTurnInResult result = slayerService.turnInTask(player);
        if (result == null) {
            cmd.set("#DialogueText.Text", wrapText("You have not completed your task yet."));
            setOptionVisibility(cmd, false, false, false, false, true, false, true);
            return;
        }

        String message = "Well done. You earned " + result.getPointsAwarded()
                + " Slayer points and " + result.getSlayerXpAwarded() + " Slayer XP.";
        if (result.isItemRewarded()) {
            message += " A special item reward is pending.";
        }
        cmd.set("#DialogueText.Text", wrapText(message));
        setOptionVisibility(cmd, false, false, false, false, false, true, true);
    }

    private String wrapText(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        // Manual wrapping because the UI label does not auto-wrap.
        StringBuilder result = new StringBuilder();
        int lineLength = 0;
        for (String word : text.split(" ")) {
            int wordLength = word.length();
            if (lineLength == 0) {
                result.append(word);
                lineLength = wordLength;
                continue;
            }
            if (lineLength + 1 + wordLength > MAX_DIALOGUE_LINE_LENGTH) {
                result.append("\n").append(word);
                lineLength = wordLength;
            } else {
                result.append(" ").append(word);
                lineLength += 1 + wordLength;
            }
        }
        return result.toString();
    }

    private void openShop(Ref<EntityStore> ref, Store<EntityStore> store) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        player.getPageManager().openCustomPage(
                ref,
                store,
                new SlayerVendorPage(playerRef, slayerService)
        );
    }

    private int getSlayerLevel() {
        LevelingService levelingService = LevelingService.get();
        if (levelingService == null) {
            return 1;
        }
        try {
            return levelingService.getSkillLevel(playerRef.getUuid(), SkillType.SLAYER);
        } catch (Exception ignored) {
            return 1;
        }
    }

    private void setOptionVisibility(UICommandBuilder cmd,
                                     boolean showTurnIn,
                                     boolean showExplain,
                                     boolean showRequest,
                                     boolean showShop,
                                     boolean showBack,
                                     boolean showThanks,
                                     boolean showNevermind) {
        cmd.set("#OptionTurnIn.Visible", showTurnIn);
        cmd.set("#OptionExplain.Visible", showExplain);
        cmd.set("#OptionRequest.Visible", showRequest);
        cmd.set("#OptionShop.Visible", showShop);
        cmd.set("#OptionBack.Visible", showBack);
        cmd.set("#OptionThanks.Visible", showThanks);
        cmd.set("#OptionNevermind.Visible", showNevermind);
    }

    public static class DialogueData {
        static final String KEY_BUTTON = "Button";
        static final String KEY_OPTION_ID = "OptionId";

        public static final BuilderCodec<DialogueData> CODEC = BuilderCodec.builder(DialogueData.class, DialogueData::new)
                .addField(new KeyedCodec<>(KEY_BUTTON, Codec.STRING), (d, s) -> d.button = s, d -> d.button)
                .addField(new KeyedCodec<>(KEY_OPTION_ID, Codec.STRING), (d, s) -> d.optionId = s, d -> d.optionId)
                .build();

        private String button;
        private String optionId;
    }
}
