package dev.hytalemodding.hyrune.ui;

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
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.hyrune.level.LevelingService;
import dev.hytalemodding.hyrune.quests.QuestManager;
import dev.hytalemodding.hyrune.skills.SkillType;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Placeholder inspect page for viewing another player's profile summary.
 */
public class InspectPlayerPage extends InteractiveCustomUIPage<InspectPlayerPage.InspectData> {
    private static final String UI_PATH = "Pages/InspectPlayerPage.ui";
    private static final String SKILL_CELL_UI = "Pages/inspect_skill_cell.ui";

    private static final String ACTION_CLOSE = "Close";
    private static final String ACTION_BACK = "Back";
    private static final String ACTION_TAB_OVERVIEW = "TabOverview";
    private static final String ACTION_TAB_SKILLS = "TabSkills";
    private static final String TAB_OVERVIEW = "Overview";
    private static final String TAB_SKILLS = "Skills";

    private final PlayerRef targetPlayerRef;
    private String selectedTab = TAB_OVERVIEW;

    public InspectPlayerPage(@Nonnull PlayerRef viewerRef, @Nonnull PlayerRef targetPlayerRef) {
        super(viewerRef, CustomPageLifetime.CanDismiss, InspectData.CODEC);
        this.targetPlayerRef = targetPlayerRef;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder commandBuilder,
                      @Nonnull UIEventBuilder eventBuilder,
                      @Nonnull Store<EntityStore> store) {
        commandBuilder.append(UI_PATH);

        UUID targetUuid = targetPlayerRef.getUuid();
        LevelingService levels = LevelingService.get();
        QuestManager quests = QuestManager.get();

        int totalLevel = 0;
        long totalXp = 0L;
        int attack = 1;
        int strength = 1;
        int defence = 1;
        int ranged = 1;
        int magic = 1;
        int constitution = 1;
        int restoration = 1;

        if (levels != null) {
            for (SkillType skill : SkillType.values()) {
                totalLevel += levels.getSkillLevel(targetUuid, skill);
                totalXp += Math.max(0L, levels.getSkillXp(targetUuid, skill));
            }
            attack = levels.getSkillLevel(targetUuid, SkillType.ATTACK);
            strength = levels.getSkillLevel(targetUuid, SkillType.STRENGTH);
            defence = levels.getSkillLevel(targetUuid, SkillType.DEFENCE);
            ranged = levels.getSkillLevel(targetUuid, SkillType.RANGED);
            magic = levels.getSkillLevel(targetUuid, SkillType.MAGIC);
            constitution = levels.getSkillLevel(targetUuid, SkillType.CONSTITUTION);
            restoration = levels.getSkillLevel(targetUuid, SkillType.RESTORATION);
        }

        int combatLevel = calculateCombatLevel(attack, strength, defence, ranged, magic, constitution, restoration);
        int questPoints = quests != null ? quests.getQuestPoints(targetUuid) : 0;

        commandBuilder.set("#InspectPlayerName.Text", resolveTargetName());
        commandBuilder.set("#InspectCombatLevel.Text", String.valueOf(combatLevel));
        commandBuilder.set("#InspectTotalLevel.Text", String.valueOf(totalLevel));
        commandBuilder.set("#InspectTotalXp.Text", String.valueOf(totalXp));
        commandBuilder.set("#InspectQuestPoints.Text", String.valueOf(questPoints));
        commandBuilder.set("#InspectClan.Text", "N/A");
        commandBuilder.set("#InspectAchievementPoints.Text", "Coming Soon");
        commandBuilder.set("#InspectGearPlaceholder.Text", "Gear view placeholder");
        buildSkillMiniGrid(commandBuilder, levels, targetUuid);
        applyTabState(commandBuilder);

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BtnClose",
            EventData.of("Button", ACTION_CLOSE), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BtnBack",
            EventData.of("Button", ACTION_BACK), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#InspectTabOverview",
            EventData.of("Button", ACTION_TAB_OVERVIEW), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#InspectTabSkills",
            EventData.of("Button", ACTION_TAB_SKILLS), false);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                @Nonnull InspectData data) {
        super.handleDataEvent(ref, store, data);
        if (data.button == null) {
            return;
        }
        switch (data.button) {
            case ACTION_CLOSE:
                this.close();
                break;
            case ACTION_BACK:
                openSocialMenu(ref, store);
                break;
            case ACTION_TAB_OVERVIEW:
                this.selectedTab = TAB_OVERVIEW;
                updateTabState();
                break;
            case ACTION_TAB_SKILLS:
                this.selectedTab = TAB_SKILLS;
                updateTabState();
                break;
            default:
                break;
        }
    }

    private void openSocialMenu(Ref<EntityStore> ref, Store<EntityStore> store) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        World world = store.getExternalData().getWorld();
        CompletableFuture.runAsync(
            () -> player.getPageManager().openCustomPage(ref, store, new SocialMenuPage(this.playerRef, this.targetPlayerRef)),
            world
        );
    }

    private String resolveTargetName() {
        String username = targetPlayerRef.getUsername();
        return (username == null || username.isBlank()) ? "PLAYER" : username;
    }

    private int calculateCombatLevel(int attack,
                                     int strength,
                                     int defence,
                                     int ranged,
                                     int magic,
                                     int constitution,
                                     int restoration) {
        double base = 0.25 * (defence + constitution + restoration);
        double melee = 0.325 * (attack + strength);
        double rangedStyle = 0.325 * (ranged * 1.5);
        double magicStyle = 0.325 * (magic * 1.5);
        return (int) (base + Math.max(melee, Math.max(rangedStyle, magicStyle)));
    }

    private void buildSkillMiniGrid(UICommandBuilder commandBuilder, LevelingService levels, UUID targetUuid) {
        commandBuilder.clear("#InspectSkillCol1");
        commandBuilder.clear("#InspectSkillCol2");
        commandBuilder.clear("#InspectSkillCol3");

        SkillType[] values = SkillType.values();
        for (int i = 0; i < values.length; i++) {
            SkillType skill = values[i];
            int level = levels != null ? levels.getSkillLevel(targetUuid, skill) : 1;
            int col = i % 3;
            int row = i / 3;
            String root = "#InspectSkillCol" + (col + 1);

            commandBuilder.append(root, SKILL_CELL_UI);
            String cell = root + "[" + row + "]";
            String iconPath = SkillUiAssets.getSkillIconPath(skill);
            if (iconPath != null) {
                commandBuilder.set(cell + " #InspectCellIcon.Background", iconPath);
            }
            commandBuilder.set(cell + " #InspectCellValue.Text", String.valueOf(level));
        }
    }

    private void updateTabState() {
        UICommandBuilder commandBuilder = new UICommandBuilder();
        applyTabState(commandBuilder);
        sendUpdate(commandBuilder, null, false);
    }

    private void applyTabState(UICommandBuilder commandBuilder) {
        boolean overview = TAB_OVERVIEW.equals(this.selectedTab);
        commandBuilder.set("#InspectOverviewContent.Visible", overview);
        commandBuilder.set("#InspectSkillsContent.Visible", !overview);
        commandBuilder.set("#InspectTabOverviewSelected.Visible", overview);
        commandBuilder.set("#InspectTabSkillsSelected.Visible", !overview);
        commandBuilder.set("#InspectTabOverview.Disabled", overview);
        commandBuilder.set("#InspectTabSkills.Disabled", !overview);
    }

    public static class InspectData {
        static final String KEY_BUTTON = "Button";

        public static final BuilderCodec<InspectData> CODEC = BuilderCodec.builder(InspectData.class, InspectData::new)
            .addField(new KeyedCodec<>(KEY_BUTTON, Codec.STRING), (d, s) -> d.button = s, d -> d.button)
            .build();

        private String button;
    }
}
