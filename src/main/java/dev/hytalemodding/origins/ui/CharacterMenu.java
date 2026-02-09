package dev.hytalemodding.origins.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.origins.level.LevelingService;
import dev.hytalemodding.origins.quests.Quest;
import dev.hytalemodding.origins.quests.QuestListFilter;
import dev.hytalemodding.origins.quests.QuestManager;
import dev.hytalemodding.origins.playerdata.QuestProgress;
import dev.hytalemodding.origins.quests.QuestRequirement;
import dev.hytalemodding.origins.quests.QuestReward;
import dev.hytalemodding.origins.playerdata.QuestStatus;
import dev.hytalemodding.origins.skills.SkillType;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * UI page for the character menu.
 */
public class CharacterMenu extends InteractiveCustomUIPage<CharacterMenu.SkillMenuData> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final String MAIN_UI = "Pages/SkillEntry.ui";
    private static final String CELL_UI = "Pages/character_stats.ui";
    private static final String ATTR_ROW_UI = "Pages/attribute_row.ui";
    private static final String UNLOCK_ROW_UI = "Pages/skill_unlock_row.ui";
    private static final String QUEST_ITEM_UI = "Pages/quest_list_item.ui";
    private static final String QUEST_STAGE_UI = "Pages/quest_stage_row.ui";
    private static final String QUEST_REQ_UI = "Pages/quest_requirement_row.ui";
    private static final String QUEST_REWARD_UI = "Pages/quest_reward_row.ui";

    private static final String COLOR_YELLOW = "#ffff00";
    private static final String COLOR_ORANGE = "#ff981f"; // For selected skills.
    private static final String COLOR_WHITE = "#ffffff";
    private static final String COLOR_GRAY = "#808080";
    private static final String COLOR_GREEN = "#00ff00";
    private static final String COLOR_RED = "#ff6666";
    private static final int PROGRESS_BAR_WIDTH = 112;
    private static final int PROGRESS_BAR_HEIGHT = 4;
    private static final String TAB_SERVER_INFO = "ServerInfo";
    private static final String TAB_QUESTS = "Quests";
    private static final String TAB_ATTRIBUTES = "Attributes";
    private static final String TAB_SKILLS = "Skills";
    private static final String TAB_FRIENDS = "Friends";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM-dd-yyyy");
    private static final String ATTR_TOGGLE_ACTION = "ToggleAttributeCategory";
    private static final String QUEST_REQ_TOGGLE_ACTION = "ToggleQuestRequirement";
    private static final String ATTR_CATEGORY_COMBAT = "Combat";
    private static final String ATTR_CATEGORY_GATHERING = "Gathering";
    private static final String ATTR_CATEGORY_MISC = "Misc";
    private static final String REQ_CATEGORY_LEVEL = "Level";
    private static final String REQ_CATEGORY_ITEM = "Item";
    private static final String REQ_CATEGORY_QUEST = "Quest";
    private static final String DEFAULT_DETAIL_TITLE = "Select a skill";
    private static final String DEFAULT_DETAIL_DESC = "Click a skill to see its bonuses.";

    private SkillType selectedSkill = null;
    private String selectedTab = TAB_SKILLS;
    private boolean combatExpanded = true;
    private boolean gatheringExpanded = true;
    private boolean miscExpanded = true;
    private String selectedQuestId = null;
    private QuestListFilter currentQuestFilter = QuestListFilter.ALPHABETICAL;
    private boolean levelReqExpanded = true;
    private boolean itemReqExpanded = true;
    private boolean questReqExpanded = true;
    // Filter State
    private boolean hideCompleted = false;
    private boolean hideUnavailable = false;

    public CharacterMenu(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, SkillMenuData.CODEC);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {
        commandBuilder.append(MAIN_UI);

        UUID uuid = this.playerRef.getUuid();
        LevelingService levelingService = LevelingService.get();

        this.applyTabState(commandBuilder, eventBuilder);
        this.populateServerInfo(commandBuilder);
        this.buildSkillGrid(commandBuilder, eventBuilder, uuid, levelingService);
        this.buildAttributes(commandBuilder, eventBuilder, uuid, levelingService);
        this.buildSkillDetailsPanel(commandBuilder, this.selectedSkill);

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BtnClose", EventData.of("Button", "Close"), false);

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#TabServerInfoBtn", EventData.of("Button", "SelectTab").append("TabID", TAB_SERVER_INFO), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#TabQuestsBtn", EventData.of("Button", "SelectTab").append("TabID", TAB_QUESTS), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#TabAttributesBtn", EventData.of("Button", "SelectTab").append("TabID", TAB_ATTRIBUTES), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#TabSkillsBtn", EventData.of("Button", "SelectTab").append("TabID", TAB_SKILLS), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#TabFriendsBtn", EventData.of("Button", "SelectTab").append("TabID", TAB_FRIENDS), false);

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#AttrCombatToggle",
            EventData.of("Button", ATTR_TOGGLE_ACTION).append("Category", ATTR_CATEGORY_COMBAT), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#AttrGatheringToggle",
            EventData.of("Button", ATTR_TOGGLE_ACTION).append("Category", ATTR_CATEGORY_GATHERING), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#AttrMiscToggle",
            EventData.of("Button", ATTR_TOGGLE_ACTION).append("Category", ATTR_CATEGORY_MISC), false);

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BtnAcceptQuest",
            EventData.of("Button", "AcceptQuest"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BtnTrackQuest",
            EventData.of("Button", "TrackQuest"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#LevelReqToggle",
            EventData.of("Button", QUEST_REQ_TOGGLE_ACTION).append("Category", REQ_CATEGORY_LEVEL), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#ItemReqToggle",
            EventData.of("Button", QUEST_REQ_TOGGLE_ACTION).append("Category", REQ_CATEGORY_ITEM), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#QuestReqToggle",
            EventData.of("Button", QUEST_REQ_TOGGLE_ACTION).append("Category", REQ_CATEGORY_QUEST), false);
    }

    /**
     * Populates the 3-column skill grid with icons, levels, and progress bars.
     */
    private void buildSkillGrid(UICommandBuilder cmd, UIEventBuilder evt, UUID uuid, LevelingService service) {
        cmd.clear("#Col1").clear("#Col2").clear("#Col3");

        int totalLevel = 0;
        int index = 0;

        for (SkillType skill : SkillType.values()) {
            int level = service.getSkillLevel(uuid, skill);
            totalLevel += level;
            long currentXp = service.getSkillXp(uuid, skill);
            long currentLevelXp = service.getLevelFormula().getXpForLevel(level);
            long nextLevelXp = service.getLevelFormula().getXpForLevel(level + 1);
            double progressPercent = 0.0;
            if (nextLevelXp > currentLevelXp) {
                progressPercent = (double) (currentXp - currentLevelXp)
                        / (double) (nextLevelXp - currentLevelXp);
            }
            progressPercent = Math.max(0.0, Math.min(1.0, progressPercent));

            int colIndex = index % 3;
            String targetCol = "#Col" + (colIndex + 1);

            cmd.append(targetCol, CELL_UI);

            int rowIndex = index / 3;
            String cellRoot = targetCol + "[" + rowIndex + "]";

            String iconPath = getSkillIconPath(skill);
            if (iconPath != null) {
                cmd.set(cellRoot + " #IconBox.Background", iconPath);
                cmd.set(cellRoot + " #IconText.Visible", false);
            } else {
                cmd.set(cellRoot + " #IconText.Visible", true);
            }

            cmd.set(cellRoot + " #SkillName.Text", skill.getDisplayName());
            cmd.set(cellRoot + " #SkillLevel.Text", level + "/99");
            cmd.set(cellRoot + " #IconText.Text", skill.getIconCode());

            if (level >= 99) {
                progressPercent = 1.0;
            }

            int fillWidth = (int) (PROGRESS_BAR_WIDTH * progressPercent);
            Anchor progressAnchor = new Anchor();
            progressAnchor.setWidth(Value.of(fillWidth));
            progressAnchor.setHeight(Value.of(PROGRESS_BAR_HEIGHT));
            cmd.setObject(cellRoot + " #ProgressFill.Anchor", progressAnchor);

            if (level >= 99) {
                cmd.set(cellRoot + " #ProgressFill.Background", "#FFD700");
            } else {
                cmd.set(cellRoot + " #ProgressFill.Background", "#00FF00");
            }
            boolean isSelected = skill == this.selectedSkill;
            cmd.set(cellRoot + " #SkillName.Style.TextColor", isSelected ? COLOR_ORANGE : COLOR_YELLOW);

            evt.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    cellRoot,
                    EventData.of("Button", "SelectSkill").append("SkillID", skill.name()),
                    false
            );

            index++;
        }

        cmd.set("#TotalLevelVal.Text", String.valueOf(totalLevel));
        cmd.set("#CombatLevelVal.Text", String.valueOf(calculateCombatLevel(uuid, service)));
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull SkillMenuData data) {
        super.handleDataEvent(ref, store, data);
        if (data.button == null) {
            return;
        }

        switch (data.button) {
            case "Close":
                this.close();
                break;

            case "SelectSkill":
                if (data.skillId != null) {
                    try {
                        this.selectedSkill = SkillType.valueOf(data.skillId);
                        UICommandBuilder refreshCmd = new UICommandBuilder();
                        UIEventBuilder refreshEvt = new UIEventBuilder();
                        this.buildSkillGrid(refreshCmd, refreshEvt, this.playerRef.getUuid(), LevelingService.get());
                        this.buildSkillDetailsPanel(refreshCmd, this.selectedSkill);
                        this.sendUpdate(refreshCmd, refreshEvt, false);
                    } catch (IllegalArgumentException e) {
                        LOGGER.at(Level.WARNING).log("Invalid skill selected: " + data.skillId);
                    }
                }
                break;

            case "SelectTab":
                if (data.tabId != null) {
                    this.selectedTab = data.tabId;

                    UICommandBuilder refreshCmd = new UICommandBuilder();
                    UIEventBuilder refreshEvt = new UIEventBuilder();
                    this.applyTabState(refreshCmd, refreshEvt);
                    if (TAB_SERVER_INFO.equals(this.selectedTab)) {
                        this.populateServerInfo(refreshCmd);
                    }
                    if (TAB_ATTRIBUTES.equals(this.selectedTab)) {
                        this.buildAttributes(refreshCmd, refreshEvt, this.playerRef.getUuid(), LevelingService.get());
                    }
                    this.sendUpdate(refreshCmd, refreshEvt, false);
                }
                break;
            case ATTR_TOGGLE_ACTION:
                if (data.category != null) {
                    toggleAttributeCategory(data.category);
                    UICommandBuilder refreshCmd = new UICommandBuilder();
                    UIEventBuilder refreshEvt = new UIEventBuilder();
                    this.buildAttributes(refreshCmd, refreshEvt, this.playerRef.getUuid(), LevelingService.get());
                    this.sendUpdate(refreshCmd, refreshEvt, false);
                }
                break;
            case "SelectQuest":
                if (data.questId != null) {
                    this.selectedQuestId = data.questId;
                    UICommandBuilder refreshCmd = new UICommandBuilder();
                    UIEventBuilder refreshEvt = new UIEventBuilder();
                    this.buildQuestTab(refreshCmd, refreshEvt, this.playerRef.getUuid());
                    this.sendUpdate(refreshCmd, refreshEvt, false);
                }
                break;
            case "AcceptQuest":
                if (this.selectedQuestId != null) {
                    QuestManager manager = QuestManager.get();
                    if (manager.startQuest(this.playerRef.getUuid(), this.selectedQuestId)) {
                        UICommandBuilder refreshCmd = new UICommandBuilder();
                        UIEventBuilder refreshEvt = new UIEventBuilder();
                        this.buildQuestTab(refreshCmd, refreshEvt, this.playerRef.getUuid());
                        this.sendUpdate(refreshCmd, refreshEvt, false);
                    }
                }
                break;
            case "TrackQuest":
                LOGGER.at(Level.FINE).log("Tracking quest: " + this.selectedQuestId);
                break;
            case "QuestFilterChanged":
                if (data.selectedIndex != null) {
                    try {
                        int index = Integer.parseInt(data.selectedIndex);
                        List<QuestListFilter> filters = getQuestFilterOptions();
                        if (index >= 0 && index < filters.size()) {
                            this.currentQuestFilter = filters.get(index);
                        }

                        UICommandBuilder refreshCmd = new UICommandBuilder();
                        UIEventBuilder refreshEvt = new UIEventBuilder();
                        this.buildQuestTab(refreshCmd, refreshEvt, this.playerRef.getUuid());
                        this.sendUpdate(refreshCmd, refreshEvt, false);
                    } catch (NumberFormatException e) {
                        LOGGER.at(Level.WARNING).log("Invalid filter index: " + data.selectedIndex);
                    }
                }
                break;
            case "ToggleHideCompleted":
                this.hideCompleted = !this.hideCompleted; // Toggle boolean

                // Rebuild the tab to update list and checkbox visual
                UICommandBuilder refreshCmd1 = new UICommandBuilder();
                UIEventBuilder refreshEvt1 = new UIEventBuilder();
                this.buildQuestTab(refreshCmd1, refreshEvt1, this.playerRef.getUuid());
                this.sendUpdate(refreshCmd1, refreshEvt1, false);
                break;

            case "ToggleHideUnavailable":
                this.hideUnavailable = !this.hideUnavailable; // Toggle boolean

                UICommandBuilder refreshCmd2 = new UICommandBuilder();
                UIEventBuilder refreshEvt2 = new UIEventBuilder();
                this.buildQuestTab(refreshCmd2, refreshEvt2, this.playerRef.getUuid());
                this.sendUpdate(refreshCmd2, refreshEvt2, false);
                break;
            case QUEST_REQ_TOGGLE_ACTION:
                if (data.category != null) {
                    toggleQuestRequirement(data.category);
                    UICommandBuilder refreshCmd = new UICommandBuilder();
                    UIEventBuilder refreshEvt = new UIEventBuilder();
                    this.buildQuestTab(refreshCmd, refreshEvt, this.playerRef.getUuid());
                    this.sendUpdate(refreshCmd, refreshEvt, false);
                }
                break;
        }
    }

    /**
     * Calculates Combat Level based on standard weights.
     */
    private int calculateCombatLevel(UUID uuid, LevelingService service) {
        int def = service.getSkillLevel(uuid, SkillType.DEFENCE);
        int hp = service.getSkillLevel(uuid, SkillType.CONSTITUTION);
        int div = service.getSkillLevel(uuid, SkillType.RESTORATION); // Prayer/Healer equivalent.

        int att = service.getSkillLevel(uuid, SkillType.ATTACK);
        int str = service.getSkillLevel(uuid, SkillType.STRENGTH);
        int range = service.getSkillLevel(uuid, SkillType.RANGED);
        int magic = service.getSkillLevel(uuid, SkillType.MAGIC);

        double base = 0.25 * (def + hp + div);

        double melee = 0.325 * (att + str);

        double ranged = 0.325 * (range * 1.5);
        double mage = 0.325 * (magic * 1.5);

        double maxOffense = Math.max(melee, Math.max(ranged, mage));

        return (int) (base + maxOffense);
    }

    private void applyTabState(UICommandBuilder cmd, UIEventBuilder evt) {
        boolean showServerInfo = TAB_SERVER_INFO.equals(this.selectedTab);
        boolean showQuests = TAB_QUESTS.equals(this.selectedTab);
        boolean showAttributes = TAB_ATTRIBUTES.equals(this.selectedTab);
        boolean showSkills = TAB_SKILLS.equals(this.selectedTab);
        boolean showFriends = TAB_FRIENDS.equals(this.selectedTab);

        cmd.set("#PageTitle.Text", getTabTitle());
        cmd.set("#TabServerInfo.Visible", showServerInfo);
        cmd.set("#TabQuests.Visible", showQuests);
        cmd.set("#TabAttributes.Visible", showAttributes);
        cmd.set("#TabSkills.Visible", showSkills);
        cmd.set("#TabFriends.Visible", showFriends);
        cmd.set("#Footer.Visible", showSkills);
        cmd.set("#TabBar.SelectedTab", this.selectedTab);
        if (showQuests) {
            this.buildQuestTab(cmd, evt, this.playerRef.getUuid());
        }
    }

    private void buildQuestTab(UICommandBuilder cmd, UIEventBuilder evt, UUID uuid) {
        QuestManager questManager = QuestManager.get();

        buildQuestFilterDropdown(cmd, evt);
        cmd.set("#HideCompletedToggle #CheckBox.Value", this.hideCompleted);
        cmd.set("#HideUnavailableToggle #CheckBox.Value", this.hideUnavailable);

        evt.addEventBinding(CustomUIEventBindingType.ValueChanged, "#HideCompletedToggle #CheckBox",
                EventData.of("Button", "ToggleHideCompleted"), false);
        evt.addEventBinding(CustomUIEventBindingType.ValueChanged, "#HideUnavailableToggle #CheckBox",
                EventData.of("Button", "ToggleHideUnavailable"), false);

        buildQuestList(cmd, evt, uuid, questManager);
        if (this.selectedQuestId != null) {
            Quest quest = questManager.getQuest(this.selectedQuestId);
            if (quest != null) {
                buildQuestDetailPanel(cmd, evt, uuid, quest, questManager);
            }
        } else {
            cmd.set("#QuestDetailTitle.Text", "Select a quest");
            cmd.set("#QuestDetailDesc.Text", "Click a quest to see its details.");
            cmd.set("#QuestJournalSection.Visible", false);
            cmd.set("#QuestRequirementsSection.Visible", false);
            cmd.set("#QuestRewardsSection.Visible", false);
            cmd.set("#QuestActionButtons.Visible", false);
        }
        int currentPoints = questManager.getQuestPoints(uuid);
        int totalPoints = questManager.getAllQuests().stream().mapToInt(Quest::getQuestPoints).sum();
        cmd.set("#QuestPointsLabel.Text", "Quest Points: " + currentPoints + " / " + totalPoints);
        cmd.set("#QuestPointsVal.Text", String.valueOf(questManager.getQuestPoints(uuid)));
    }

    private void buildQuestFilterDropdown(UICommandBuilder cmd, UIEventBuilder evt) {
        List<QuestListFilter> filters = getQuestFilterOptions();
        List<DropdownEntryInfo> filterOptions = new ArrayList<>();
        for (QuestListFilter filter : filters) {
            filterOptions.add(new DropdownEntryInfo(
                LocalizableString.fromString(filter.getDisplayName()),
                filter.name()
            ));
        }

        if (!filters.contains(this.currentQuestFilter)) {
            this.currentQuestFilter = filters.get(0);
        }
        cmd.set("#QuestFilterRow #Input.Entries", filterOptions);
        cmd.set("#QuestFilterRow #Input.Value", this.currentQuestFilter.name());
        evt.addEventBinding(CustomUIEventBindingType.ValueChanged, "#QuestFilterRow #Input",
            EventData.of("Dropdown", "QuestFilterChanged"), false);
    }

    private List<QuestListFilter> getQuestFilterOptions() {
        List<QuestListFilter> filters = new ArrayList<>();
        filters.add(QuestListFilter.ALPHABETICAL);
        filters.add(QuestListFilter.BY_LENGTH);
        return filters;
    }

    private void buildQuestList(UICommandBuilder cmd, UIEventBuilder evt, UUID uuid, QuestManager manager) {
        cmd.clear("#QuestList");

        List<Quest> quests = manager.getFilteredQuests(uuid, this.currentQuestFilter);
        int index = 0;
        for (Quest quest : quests) {
            QuestStatus status = manager.getQuestStatus(uuid, quest.getId());
            if (this.hideCompleted && status == QuestStatus.COMPLETED) {
                continue;
            }
            if (this.hideUnavailable && status == QuestStatus.NOT_STARTED && !quest.meetsRequirements(uuid)) {
                continue;
            }
            cmd.append("#QuestList", QUEST_ITEM_UI);

            String itemRoot = "#QuestList[" + index + "]";
            cmd.set(itemRoot + " #QuestName.Text", quest.getName());

            String statusColor;
            switch (status) {
                case NOT_STARTED:
                    statusColor = COLOR_RED;
                    break;
                case IN_PROGRESS:
                    statusColor = COLOR_YELLOW;
                    break;
                case COMPLETED:
                    statusColor = COLOR_GREEN;
                    break;
                default:
                    statusColor = COLOR_YELLOW;
                    break;
            }

            boolean isSelected = quest.getId().equals(this.selectedQuestId);
            String textColor = isSelected ? COLOR_WHITE : statusColor;
            cmd.set(itemRoot + " #QuestName.Style.TextColor", textColor);
            cmd.set(itemRoot + " #QuestName.Style.RenderBold", isSelected);
            cmd.set(itemRoot + " #QuestStatus.Visible", status == QuestStatus.COMPLETED);

            evt.addEventBinding(
                CustomUIEventBindingType.Activating,
                itemRoot,
                EventData.of("Button", "SelectQuest").append("QuestID", quest.getId()),
                false
            );

            index++;
        }
    }

    private void buildQuestDetailPanel(UICommandBuilder cmd, UIEventBuilder evt,
                                       UUID uuid, Quest quest, QuestManager manager) {
        QuestProgress progress = manager.getQuestProgress(uuid, quest.getId());
        QuestStatus status = progress != null ? progress.getStatus() : QuestStatus.NOT_STARTED;

        cmd.set("#QuestDetailTitle.Text", quest.getName());
        if (status == QuestStatus.NOT_STARTED) {
            cmd.set("#QuestDetailDesc.Text", quest.getDescription());
            cmd.set("#QuestDescScroll.Visible", true);
            cmd.set("#QuestJournalSection.Visible", false);
        } else {
            cmd.set("#QuestDetailDesc.Text", "");
            cmd.set("#QuestDescScroll.Visible", false);
            cmd.set("#QuestJournalSection.Visible", true);
            buildQuestJournal(cmd, quest, progress, status);
        }

        cmd.set("#QuestRequirementsSection.Visible", true);
        buildQuestRequirements(cmd, evt, uuid, quest);

        cmd.set("#QuestRewardsSection.Visible", true);
        buildQuestRewards(cmd, quest);

        buildQuestActionButtons(cmd, uuid, quest, status);
    }

    private void buildQuestJournal(UICommandBuilder cmd, Quest quest, QuestProgress progress, QuestStatus status) {
        cmd.clear("#QuestJournalList");

        List<Quest.StageInfo> stages = quest.getStageList(progress);
        int index = 0;
        boolean shownCurrent = false;

        for (Quest.StageInfo stage : stages) {
            if (status == QuestStatus.IN_PROGRESS && !stage.isCompleted() && shownCurrent) {
                break;
            }

            cmd.append("#QuestJournalList", QUEST_STAGE_UI);
            String stageRoot = "#QuestJournalList[" + index + "]";
            cmd.set(stageRoot + " #StageText.Text", stage.getText());

            if (status == QuestStatus.COMPLETED || stage.isCompleted()) {
                cmd.set(stageRoot + " #StageText.Style.TextColor", COLOR_GRAY);
            }

            if (!stage.isCompleted()) {
                shownCurrent = true;
            }

            index++;
        }
    }

    private void buildQuestRequirements(UICommandBuilder cmd, UIEventBuilder evt,
                                        UUID uuid, Quest quest) {
        LevelingService service = LevelingService.get();

        cmd.set("#LevelReqToggle.Text", levelReqExpanded ? "-" : "+");
        cmd.set("#ItemReqToggle.Text", itemReqExpanded ? "-" : "+");
        cmd.set("#QuestReqToggle.Text", questReqExpanded ? "-" : "+");

        cmd.set("#LevelReqContent.Visible", levelReqExpanded);
        cmd.set("#ItemReqContent.Visible", itemReqExpanded);
        cmd.set("#QuestReqContent.Visible", questReqExpanded);

        cmd.clear("#LevelReqContent");
        cmd.clear("#ItemReqContent");
        cmd.clear("#QuestReqContent");

        if (levelReqExpanded) {
            int row = 0;
            for (QuestRequirement.LevelRequirement req : quest.getLevelRequirements()) {
                boolean met = service.getSkillLevel(uuid, req.getSkill()) >= req.getLevel();
                appendRequirementRow(cmd, "#LevelReqContent", row++, req.getDisplayText(), met);
            }
        }

        if (itemReqExpanded) {
            int row = 0;
            for (QuestRequirement.ItemRequirement req : quest.getItemRequirements()) {
                appendRequirementRow(cmd, "#ItemReqContent", row++, req.getDisplayText(), false);
            }
        }

        if (questReqExpanded) {
            int row = 0;
            QuestManager manager = QuestManager.get();
            for (String questId : quest.getPrerequisiteQuests()) {
                Quest prereq = manager.getQuest(questId);
                String label = prereq != null ? prereq.getName() : questId;
                QuestProgress progress = manager.getQuestProgress(uuid, questId);
                boolean met = progress != null && progress.getStatus() == QuestStatus.COMPLETED;
                appendRequirementRow(cmd, "#QuestReqContent", row++, label, met);
            }
        }
    }

    private void appendRequirementRow(UICommandBuilder cmd, String container, int index, String text, boolean met) {
        cmd.append(container, QUEST_REQ_UI);
        String rowRoot = container + "[" + index + "]";
        cmd.set(rowRoot + " #RequirementText.Text", text);
        cmd.set(rowRoot + " #RequirementText.Style.TextColor", met ? COLOR_GREEN : COLOR_RED);
        cmd.set(rowRoot + " #RequirementIcon.Text", met ? "v" : "-");
        cmd.set(rowRoot + " #RequirementIcon.Style.TextColor", met ? COLOR_GREEN : COLOR_RED);
    }

    private void buildQuestRewards(UICommandBuilder cmd, Quest quest) {
        cmd.clear("#RewardsContent");

        int row = 0;
        cmd.append("#RewardsContent", QUEST_REWARD_UI);
        cmd.set("#RewardsContent[" + row + "] #RewardText.Text",
            quest.getQuestPoints() + " Quest Point" + (quest.getQuestPoints() != 1 ? "s" : ""));
        row++;

        for (QuestReward reward : quest.getRewards()) {
            cmd.append("#RewardsContent", QUEST_REWARD_UI);
            cmd.set("#RewardsContent[" + row + "] #RewardText.Text", reward.getDisplayText());
            row++;
        }
    }

    private void buildQuestActionButtons(UICommandBuilder cmd, UUID uuid, Quest quest, QuestStatus status) {
        boolean canStart = status == QuestStatus.NOT_STARTED && quest.meetsRequirements(uuid);
        boolean inProgress = status == QuestStatus.IN_PROGRESS;

        cmd.set("#QuestActionButtons.Visible", canStart || inProgress);
        cmd.set("#QuestAcceptRow.Visible", canStart);
        cmd.set("#QuestTrackRow.Visible", inProgress);
        cmd.set("#BtnAcceptQuest.Visible", canStart);
        cmd.set("#BtnTrackQuest.Visible", inProgress);
    }

    private void toggleQuestRequirement(String category) {
        switch (category) {
            case REQ_CATEGORY_LEVEL:
                levelReqExpanded = !levelReqExpanded;
                break;
            case REQ_CATEGORY_ITEM:
                itemReqExpanded = !itemReqExpanded;
                break;
            case REQ_CATEGORY_QUEST:
                questReqExpanded = !questReqExpanded;
                break;
            default:
                break;
        }
    }

    private void buildAttributes(UICommandBuilder cmd,
                                 UIEventBuilder evt,
                                 UUID uuid,
                                 LevelingService service) {
        cmd.set("#AttrCombatToggle.Text", combatExpanded ? "-" : "+");
        cmd.set("#AttrGatheringToggle.Text", gatheringExpanded ? "-" : "+");
        cmd.set("#AttrMiscToggle.Text", miscExpanded ? "-" : "+");

        cmd.set("#AttrCombatContent.Visible", combatExpanded);
        cmd.set("#AttrGatheringContent.Visible", gatheringExpanded);
        cmd.set("#AttrMiscContent.Visible", miscExpanded);

        cmd.clear("#AttrCombatContent");
        cmd.clear("#AttrGatheringContent");
        cmd.clear("#AttrMiscContent");

        if (combatExpanded) {
            int row = 0;
            for (AttributeEntry entry : buildCombatAttributes(uuid, service)) {
                appendAttributeRow(cmd, "#AttrCombatContent", row++, entry);
            }
        }

        if (gatheringExpanded) {
            int row = 0;
            for (AttributeEntry entry : buildGatheringAttributes(uuid, service)) {
                appendAttributeRow(cmd, "#AttrGatheringContent", row++, entry);
            }
        }

        if (miscExpanded) {
            int row = 0;
            for (AttributeEntry entry : buildMiscAttributes(uuid, service)) {
                appendAttributeRow(cmd, "#AttrMiscContent", row++, entry);
            }
        }
    }

    private void buildSkillDetailsPanel(UICommandBuilder cmd, SkillType skill) {
        SkillDetailRegistry.SkillDetail detail = SkillDetailRegistry.getDetail(skill);
        if (detail == null) {
            cmd.set("#SkillDetailTitle.Text", DEFAULT_DETAIL_TITLE);
            cmd.set("#SkillDetailDesc.Text", DEFAULT_DETAIL_DESC);
            cmd.set("#SkillUnlockHeader.Visible", false);
            cmd.set("#SkillUnlockScroll.Visible", false);
            cmd.clear("#SkillUnlockList");
            appendUnlockRow(cmd, "#SkillUnlockList", 0,
                new SkillDetailRegistry.SkillUnlock(1, "No unlocks yet."));
            return;
        }

        cmd.set("#SkillUnlockHeader.Visible", true);
        cmd.set("#SkillUnlockScroll.Visible", true);
        cmd.set("#SkillDetailTitle.Text", detail.title);
        cmd.set("#SkillDetailDesc.Text", detail.description);
        cmd.clear("#SkillUnlockList");

        int row = 0;
        for (SkillDetailRegistry.SkillUnlock unlock : detail.unlocks) {
            appendUnlockRow(cmd, "#SkillUnlockList", row++, unlock);
        }
    }

    private void appendUnlockRow(UICommandBuilder cmd, String container, int index, SkillDetailRegistry.SkillUnlock unlock) {
        cmd.append(container, UNLOCK_ROW_UI);
        String rowRoot = container + "[" + index + "]";
        cmd.set(rowRoot + " #UnlockLevel.Text", "Lv " + unlock.level);
        cmd.set(rowRoot + " #UnlockText.Text", unlock.text);
    }

    private void appendAttributeRow(UICommandBuilder cmd,
                                    String container,
                                    int index,
                                    AttributeEntry entry) {
        cmd.append(container, ATTR_ROW_UI);
        String rowRoot = container + "[" + index + "]";
        cmd.set(rowRoot + " #AttrLabel.Text", entry.label);
        cmd.set(rowRoot + " #AttrValue.Text", entry.value);
    }

    private AttributeEntry[] buildCombatAttributes(UUID uuid, LevelingService service) {
        int attack = service.getSkillLevel(uuid, SkillType.ATTACK);
        int strength = service.getSkillLevel(uuid, SkillType.STRENGTH);
        int defence = service.getSkillLevel(uuid, SkillType.DEFENCE);
        int ranged = service.getSkillLevel(uuid, SkillType.RANGED);
        int magic = service.getSkillLevel(uuid, SkillType.MAGIC);

        double meleeDamageBonus = attack * dev.hytalemodding.origins.system.SkillCombatBonusSystem.ATTACK_DAMAGE_PER_LEVEL;
        double meleeCritChance = Math.min(
            dev.hytalemodding.origins.system.SkillCombatBonusSystem.STRENGTH_CRIT_CHANCE_CAP,
            strength * dev.hytalemodding.origins.system.SkillCombatBonusSystem.STRENGTH_CRIT_CHANCE_PER_LEVEL
        );
        double meleeCritMultiplier = dev.hytalemodding.origins.system.SkillCombatBonusSystem.STRENGTH_CRIT_BASE_MULTIPLIER
            + (strength * dev.hytalemodding.origins.system.SkillCombatBonusSystem.STRENGTH_CRIT_DAMAGE_BONUS_PER_LEVEL);
        double damageReduction = Math.min(
            dev.hytalemodding.origins.system.SkillCombatBonusSystem.DEFENCE_DAMAGE_REDUCTION_CAP,
            defence * dev.hytalemodding.origins.system.SkillCombatBonusSystem.DEFENCE_DAMAGE_REDUCTION_PER_LEVEL
        );

        double rangedDamageBonus = ranged * dev.hytalemodding.origins.system.SkillCombatBonusSystem.RANGED_DAMAGE_PER_LEVEL;
        double rangedCritChance = Math.min(
            dev.hytalemodding.origins.system.SkillCombatBonusSystem.RANGED_CRIT_CHANCE_CAP,
            ranged * dev.hytalemodding.origins.system.SkillCombatBonusSystem.RANGED_CRIT_CHANCE_PER_LEVEL
        );
        double rangedCritMultiplier = dev.hytalemodding.origins.system.SkillCombatBonusSystem.RANGED_CRIT_BASE_MULTIPLIER
            + (ranged * dev.hytalemodding.origins.system.SkillCombatBonusSystem.RANGED_CRIT_DAMAGE_BONUS_PER_LEVEL);

        double magicDamageBonus = magic * dev.hytalemodding.origins.system.SkillCombatBonusSystem.MAGIC_DAMAGE_PER_LEVEL;
        double magicCritChance = Math.min(
            dev.hytalemodding.origins.system.SkillCombatBonusSystem.MAGIC_CRIT_CHANCE_CAP,
            magic * dev.hytalemodding.origins.system.SkillCombatBonusSystem.MAGIC_CRIT_CHANCE_PER_LEVEL
        );
        double magicCritMultiplier = dev.hytalemodding.origins.system.SkillCombatBonusSystem.MAGIC_CRIT_BASE_MULTIPLIER
            + (magic * dev.hytalemodding.origins.system.SkillCombatBonusSystem.MAGIC_CRIT_DAMAGE_BONUS_PER_LEVEL);

        return new AttributeEntry[]{
            new AttributeEntry("Melee damage bonus", formatBonusPercent(meleeDamageBonus)),
            new AttributeEntry("Melee crit chance", formatPercent(meleeCritChance)),
            new AttributeEntry("Melee crit multiplier", formatMultiplier(meleeCritMultiplier)),
            new AttributeEntry("Damage reduction", formatPercent(damageReduction)),
            new AttributeEntry("Ranged damage bonus", formatBonusPercent(rangedDamageBonus)),
            new AttributeEntry("Ranged crit chance", formatPercent(rangedCritChance)),
            new AttributeEntry("Ranged crit multiplier", formatMultiplier(rangedCritMultiplier)),
            new AttributeEntry("Magic damage bonus", formatBonusPercent(magicDamageBonus)),
            new AttributeEntry("Magic crit chance", formatPercent(magicCritChance)),
            new AttributeEntry("Magic crit multiplier", formatMultiplier(magicCritMultiplier))
        };
    }

    private AttributeEntry[] buildGatheringAttributes(UUID uuid, LevelingService service) {
        int mining = service.getSkillLevel(uuid, SkillType.MINING);
        int woodcutting = service.getSkillLevel(uuid, SkillType.WOODCUTTING);
        int farming = service.getSkillLevel(uuid, SkillType.FARMING);
        int cooking = service.getSkillLevel(uuid, SkillType.COOKING);
        int alchemy = service.getSkillLevel(uuid, SkillType.ALCHEMY);

        double miningSpeed = mining * dev.hytalemodding.origins.system.MiningSpeedSystem.MINING_DAMAGE_PER_LEVEL;
        double miningDurability = Math.min(
            dev.hytalemodding.origins.system.MiningDurabilitySystem.MINING_DURABILITY_REDUCTION_CAP,
            mining * dev.hytalemodding.origins.system.MiningDurabilitySystem.MINING_DURABILITY_REDUCTION_PER_LEVEL
        );
        double woodcuttingSpeed = woodcutting * dev.hytalemodding.origins.system.WoodcuttingSpeedSystem.WOODCUTTING_DAMAGE_PER_LEVEL;
        double woodcuttingDurability = Math.min(
            dev.hytalemodding.origins.system.WoodcuttingDurabilitySystem.WOODCUTTING_DURABILITY_REDUCTION_CAP,
            woodcutting * dev.hytalemodding.origins.system.WoodcuttingDurabilitySystem.WOODCUTTING_DURABILITY_REDUCTION_PER_LEVEL
        );
        double farmingYieldBonus = dev.hytalemodding.origins.system.FarmingHarvestPickupSystem.MAX_YIELD_BONUS
            * (Math.min(farming, 99) / 99.0);
        double sickleXpBonus = dev.hytalemodding.origins.system.FarmingHarvestPickupSystem.SICKLE_XP_BONUS - 1.0;
        double cookingDoubleProc = Math.min(
            dev.hytalemodding.origins.system.TimedCraftingXpSystem.DOUBLE_PROC_CHANCE_CAP,
            cooking * dev.hytalemodding.origins.system.TimedCraftingXpSystem.DOUBLE_PROC_CHANCE_PER_LEVEL
        );
        double alchemyDoubleProc = Math.min(
            dev.hytalemodding.origins.system.TimedCraftingXpSystem.DOUBLE_PROC_CHANCE_CAP,
            alchemy * dev.hytalemodding.origins.system.TimedCraftingXpSystem.DOUBLE_PROC_CHANCE_PER_LEVEL
        );

        return new AttributeEntry[]{
            new AttributeEntry("Mining speed bonus", formatBonusPercent(miningSpeed)),
            new AttributeEntry("Mining durability reduction", formatPercent(miningDurability)),
            new AttributeEntry("Woodcutting speed bonus", formatBonusPercent(woodcuttingSpeed)),
            new AttributeEntry("Woodcutting durability reduction", formatPercent(woodcuttingDurability)),
            new AttributeEntry("Farming yield bonus", formatBonusPercent(farmingYieldBonus)),
            new AttributeEntry("Farming sickle XP bonus", formatBonusPercent(sickleXpBonus)),
            new AttributeEntry("Cooking double proc chance", formatPercent(cookingDoubleProc)),
            new AttributeEntry("Alchemy double proc chance", formatPercent(alchemyDoubleProc))
        };
    }

    private AttributeEntry[] buildMiscAttributes(UUID uuid, LevelingService service) {
        int constitution = service.getSkillLevel(uuid, SkillType.CONSTITUTION);
        int magic = service.getSkillLevel(uuid, SkillType.MAGIC);
        int agility = service.getSkillLevel(uuid, SkillType.AGILITY);

        double healthBonus = constitution * dev.hytalemodding.origins.bonus.SkillStatBonusApplier.HEALTH_PER_CONSTITUTION;
        double manaBonus = magic * dev.hytalemodding.origins.bonus.SkillStatBonusApplier.MANA_MAX_PER_MAGIC;
        double staminaBonus = agility * dev.hytalemodding.origins.bonus.SkillStatBonusApplier.STAMINA_MAX_PER_AGILITY;
        double manaRegen = magic * dev.hytalemodding.origins.bonus.SkillStatBonusApplier.MANA_REGEN_PER_MAGIC;
        double staminaRegen = agility * dev.hytalemodding.origins.bonus.SkillStatBonusApplier.STAMINA_REGEN_PER_AGILITY;
        double moveSpeedBonus = (agility / 99.0) * dev.hytalemodding.origins.bonus.SkillStatBonusApplier.MOVEMENT_SPEED_BONUS_AT_99;

        return new AttributeEntry[]{
            new AttributeEntry("Max health bonus", formatFlat(healthBonus)),
            new AttributeEntry("Max mana bonus", formatFlat(manaBonus)),
            new AttributeEntry("Max stamina bonus", formatFlat(staminaBonus)),
            new AttributeEntry("Mana regen per sec", formatFlat(manaRegen)),
            new AttributeEntry("Stamina regen per sec", formatFlat(staminaRegen)),
            new AttributeEntry("Movement speed bonus", formatBonusPercent(moveSpeedBonus))
        };
    }

    private void toggleAttributeCategory(String category) {
        switch (category) {
            case ATTR_CATEGORY_COMBAT:
                combatExpanded = !combatExpanded;
                break;
            case ATTR_CATEGORY_GATHERING:
                gatheringExpanded = !gatheringExpanded;
                break;
            case ATTR_CATEGORY_MISC:
                miscExpanded = !miscExpanded;
                break;
            default:
                break;
        }
    }

    private String formatPercent(double value) {
        return String.format(java.util.Locale.US, "%.1f%%", value * 100.0);
    }

    private String formatBonusPercent(double value) {
        return String.format(java.util.Locale.US, "+%.1f%%", value * 100.0);
    }

    private String formatMultiplier(double value) {
        return String.format(java.util.Locale.US, "x%.2f", value);
    }

    private String formatFlat(double value) {
        return String.format(java.util.Locale.US, "+%.2f", value);
    }


    private String getTabTitle() {
        switch (this.selectedTab) {
            case TAB_SERVER_INFO:
                return "Server Info";
            case TAB_QUESTS:
                return "Quests";
            case TAB_ATTRIBUTES:
                return "Attributes";
            case TAB_FRIENDS:
                return "Friends List";
            case TAB_SKILLS:
            default:
                return "Skills";
        }
    }

    private String getSkillIconPath(SkillType skill) {
        switch (skill) {
            case ATTACK:
                return "Pages/attack.png";
            case DEFENCE:
                return "Pages/defence.png";
            case STRENGTH:
                return "Pages/strength.png";
            case FISHING:
                return "Pages/fishing.png";
            case MINING:
                return "Pages/mining.png";
            case SMELTING:
                return "Pages/smelting.png";
            case ARCANE_ENGINEERING:
                return "Pages/arcane_engineering.png";
            case ARMORSMITHING:
                return "Pages/armorsmithing.png";
            case WEAPONSMITHING:
                return "Pages/weaponsmithing.png";
            case LEATHERWORKING:
                return "Pages/leatherworking.png";
            case ARCHITECT:
                return "Pages/architect.png";
            case WOODCUTTING:
                return "Pages/woodcutting.png";
            case RANGED:
                return "Pages/ranged.png";
            case CONSTITUTION:
                return "Pages/constitution.png";
            case MAGIC:
                return "Pages/magic.png";
            case RESTORATION:
                return "Pages/restoration.png";
            case SLAYER:
                return "Pages/slayer.png";
            case AGILITY:
                return "Pages/agility.png";
            case FARMING:
                return "Pages/farming.png";
            case COOKING:
                return "Pages/cooking.png";
            case ALCHEMY:
                return "Pages/alchemy.png";
            default:
                return null;
        }
    }

    private void populateServerInfo(UICommandBuilder cmd) {
        int onlineCount = getOnlinePlayerCount();
        String onlineText = onlineCount >= 0 ? String.valueOf(onlineCount) : "Unknown";

        ZonedDateTime now = ZonedDateTime.now();
        String serverTime = TIME_FORMATTER.format(now);
        String serverDate = DATE_FORMATTER.format(now);
        String serverZone = now.getZone().getId();

        cmd.set("#ServerInfoPlayersOnline.Text", onlineText);
        cmd.set("#ServerInfoServerTime.Text", serverTime);
        cmd.set("#ServerInfoLocalTime.Text", "Unknown");
        cmd.set("#ServerInfoServerDate.Text", serverDate);
        cmd.set("#ServerInfoServerZone.Text", serverZone);
    }

    private int getOnlinePlayerCount() {
        Universe universe = Universe.get();
        if (universe == null) {
            return -1;
        }

        Integer count = tryGetOnlineCount(universe, "getPlayerCount");
        if (count != null) {
            return count;
        }

        try {
            Method getPlayers = universe.getClass().getMethod("getPlayers");
            Object result = getPlayers.invoke(universe);
            if (result instanceof Collection) {
                return ((Collection<?>) result).size();
            }
        } catch (ReflectiveOperationException ignored) {
            // Fallback below.
        }

        return -1;
    }

    private Integer tryGetOnlineCount(Object universe, String methodName) {
        try {
            Method method = universe.getClass().getMethod(methodName);
            Object result = method.invoke(universe);
            if (result instanceof Number) {
                return ((Number) result).intValue();
            }
        } catch (ReflectiveOperationException ignored) {
            // Fallback handled by caller.
        }
        return null;
    }

    // --- CODEC ---
    public static class SkillMenuData {
        static final String KEY_BUTTON = "Button";
        static final String KEY_SKILL_ID = "SkillID";
        static final String KEY_TAB_ID = "TabID";
        static final String KEY_CATEGORY = "Category";
        static final String KEY_QUEST_ID = "QuestID";
        static final String KEY_SELECTED_INDEX = "SelectedIndex";

        public static final BuilderCodec<SkillMenuData> CODEC = BuilderCodec.builder(SkillMenuData.class, SkillMenuData::new)
                .addField(new KeyedCodec<>(KEY_BUTTON, Codec.STRING), (d, s) -> d.button = s, d -> d.button)
                .addField(new KeyedCodec<>(KEY_SKILL_ID, Codec.STRING), (d, s) -> d.skillId = s, d -> d.skillId)
                .addField(new KeyedCodec<>(KEY_TAB_ID, Codec.STRING), (d, s) -> d.tabId = s, d -> d.tabId)
                .addField(new KeyedCodec<>(KEY_CATEGORY, Codec.STRING), (d, s) -> d.category = s, d -> d.category)
                .addField(new KeyedCodec<>(KEY_QUEST_ID, Codec.STRING), (d, s) -> d.questId = s, d -> d.questId)
                .addField(new KeyedCodec<>(KEY_SELECTED_INDEX, Codec.STRING), (d, s) -> d.selectedIndex = s, d -> d.selectedIndex)
                .build();

        private String button;
        private String skillId;
        private String tabId;
        private String category;
        private String questId;
        private String selectedIndex;
    }

    private static final class AttributeEntry {
        private final String label;
        private final String value;

        private AttributeEntry(String label, String value) {
            this.label = label;
            this.value = value;
        }
    }

}

