package dev.hytalemodding.hyrune.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
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
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.Hyrune;
import dev.hytalemodding.hyrune.clans.ClanPanelBridge;
import dev.hytalemodding.hyrune.level.CombatXpStyle;
import dev.hytalemodding.hyrune.level.LevelingService;
import dev.hytalemodding.hyrune.quests.Quest;
import dev.hytalemodding.hyrune.quests.QuestListFilter;
import dev.hytalemodding.hyrune.quests.QuestManager;
import dev.hytalemodding.hyrune.registry.FishingRegistry;
import dev.hytalemodding.hyrune.playerdata.QuestProgress;
import dev.hytalemodding.hyrune.quests.QuestRequirement;
import dev.hytalemodding.hyrune.quests.QuestReward;
import dev.hytalemodding.hyrune.playerdata.QuestStatus;
import dev.hytalemodding.hyrune.social.SocialActionResult;
import dev.hytalemodding.hyrune.social.SocialInteractionRules;
import dev.hytalemodding.hyrune.social.SocialService;
import dev.hytalemodding.hyrune.skills.SkillType;
import dev.hytalemodding.hyrune.ui.attributes.CharacterAttributeComputationService;
import dev.hytalemodding.hyrune.ui.attributes.CharacterAttributeComputationService.DetailCategory;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * UI page for the character menu.
 */
public class CharacterMenu extends InteractiveCustomUIPage<CharacterMenu.SkillMenuData> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final String MAIN_UI = "Pages/SkillEntry.ui";
    private static final String CELL_UI = "Pages/character_stats.ui";
    private static final String ATTR_ROW_UI = "Pages/attribute_row.ui";
    private static final String QUEST_ITEM_UI = "Pages/quest_list_item.ui";
    private static final String QUEST_HEADER_UI = "Pages/quest_list_header.ui";
    private static final String QUEST_STAGE_UI = "Pages/quest_stage_row.ui";
    private static final String QUEST_REQ_UI = "Pages/quest_requirement_row.ui";
    private static final String QUEST_REWARD_UI = "Pages/quest_reward_row.ui";
    private static final String FRIEND_ROW_UI = "Pages/friend_row.ui";
    private static final String FRIEND_REQUEST_ROW_UI = "Pages/friend_request_row.ui";
    private static final String IGNORED_ROW_UI = "Pages/ignored_row.ui";

    private static final String COLOR_YELLOW = "#ffff00";
    private static final String COLOR_ORANGE = "#ff981f";
    private static final String COLOR_WHITE = "#ffffff";
    private static final String COLOR_GRAY = "#808080";
    private static final String COLOR_GREEN = "#00ff00";
    private static final String COLOR_RED = "#ff6666";
    private static final int PROGRESS_BAR_WIDTH = 98;
    private static final int PROGRESS_BAR_HEIGHT = 4;
    private static final String TAB_SERVER_INFO = "ServerInfo";
    private static final String TAB_COMBAT_SETTINGS = "CombatSettings";
    private static final String TAB_QUESTS = "Quests";
    private static final String TAB_ATTRIBUTES = "Attributes";
    private static final String TAB_SKILLS = "Skills";
    private static final String TAB_FRIENDS = "Friends";
    private static final String TAB_CLAN = "Clan";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM-dd-yyyy");
    private static final DateTimeFormatter RESTART_FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm");
    private static final int DAILY_RESTART_HOUR = 4;
    private static final int DAILY_RESTART_MINUTE = 0;
    private static final String SERVER_PATCH_VERSION = "Hyrune v1.0.0";
    private static final String DEFAULT_EVENT_STATUS = "No event active";
    private static final String DEFAULT_EVENT_DESCRIPTION = "No event is currently active. Check back soon for announcements.";
    private static final String ATTR_DETAIL_SELECT_ACTION = "SelectAttributeDetailCategory";
    private static final String QUEST_REQ_TOGGLE_ACTION = "ToggleQuestRequirement";
    private static final String REQ_CATEGORY_LEVEL = "Level";
    private static final String REQ_CATEGORY_ITEM = "Item";
    private static final String REQ_CATEGORY_QUEST = "Quest";
    private static final String COMBAT_STYLE_CHANGED_ACTION = "CombatStyleChanged";
    private static final String QUEST_FILTER_CHANGED_ACTION = "QuestFilterChanged";
    private static final String OPEN_SKILL_INFO_ACTION = "OpenSkillInfo";
    private static final String FRIEND_INTERACT_ACTION = "FriendInteract";
    private static final String FRIEND_REMOVE_ACTION = "FriendRemove";
    private static final String FRIEND_ACCEPT_ACTION = "FriendAccept";
    private static final String FRIEND_DENY_ACTION = "FriendDeny";
    private static final String FRIEND_UNIGNORE_ACTION = "FriendUnignore";
    private static final int FRIEND_ROW_NAME_MAX_CHARS = 18;
    private static final int REQUEST_ROW_NAME_MAX_CHARS = 12;
    private static final int IGNORED_ROW_NAME_MAX_CHARS = 14;

    private String selectedTab = TAB_SKILLS;
    private DetailCategory selectedAttrDetailCategory = DetailCategory.OFFENSE;
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
        this.buildAttributes(commandBuilder, eventBuilder, ref, store, uuid, levelingService);

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BtnClose", EventData.of("Button", "Close"), false);

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#TabCombatSettingsBtn", EventData.of("Button", "SelectTab").append("TabID", TAB_COMBAT_SETTINGS), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#TabServerInfoBtn", EventData.of("Button", "SelectTab").append("TabID", TAB_SERVER_INFO), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#TabQuestsBtn", EventData.of("Button", "SelectTab").append("TabID", TAB_QUESTS), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#TabAttributesBtn", EventData.of("Button", "SelectTab").append("TabID", TAB_ATTRIBUTES), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#TabSkillsBtn", EventData.of("Button", "SelectTab").append("TabID", TAB_SKILLS), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#TabFriendsBtn", EventData.of("Button", "SelectTab").append("TabID", TAB_FRIENDS), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#TabClanBtn", EventData.of("Button", "SelectTab").append("TabID", TAB_CLAN), false);

        bindAttributeDetailCategoryButton(eventBuilder, "#AttrDetailOffenseBtn", DetailCategory.OFFENSE);
        bindAttributeDetailCategoryButton(eventBuilder, "#AttrDetailDefenseBtn", DetailCategory.DEFENSE);
        bindAttributeDetailCategoryButton(eventBuilder, "#AttrDetailResourcesBtn", DetailCategory.RESOURCES);
        bindAttributeDetailCategoryButton(eventBuilder, "#AttrDetailMobilityBtn", DetailCategory.MOBILITY);
        bindAttributeDetailCategoryButton(eventBuilder, "#AttrDetailGatheringBtn", DetailCategory.GATHERING);
        bindAttributeDetailCategoryButton(eventBuilder, "#AttrDetailHealingBtn", DetailCategory.HEALING);

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

            String iconPath = SkillUiAssets.getSkillIconPath(skill);
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
            cmd.set(cellRoot + " #SkillName.Style.TextColor", COLOR_YELLOW);
            evt.addEventBinding(
                CustomUIEventBindingType.Activating,
                cellRoot,
                EventData.of("Button", OPEN_SKILL_INFO_ACTION).append("SkillID", skill.name()),
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

        LOGGER.at(Level.FINE).log("Event Data: " + data.toString());

        // Now check for button events
        if (data.Button == null) {
            return;
        }

        switch (data.Button) {
            case "Close":
                this.close();
                break;

            case "SelectTab":
                if (data.TabID != null) {
                    this.selectedTab = data.TabID;

                    UICommandBuilder refreshCmd = new UICommandBuilder();
                    UIEventBuilder refreshEvt = new UIEventBuilder();
                    this.applyTabState(refreshCmd, refreshEvt);
                    if (TAB_SERVER_INFO.equals(this.selectedTab)) {
                        this.populateServerInfo(refreshCmd);
                    }
                    if (TAB_COMBAT_SETTINGS.equals(this.selectedTab)) {
                        this.buildCombatSettings(refreshCmd, refreshEvt, this.playerRef.getUuid(), LevelingService.get());
                    }
                    if (TAB_ATTRIBUTES.equals(this.selectedTab)) {
                        this.buildAttributes(refreshCmd, refreshEvt, ref, store, this.playerRef.getUuid(), LevelingService.get());
                    }
                    if (TAB_FRIENDS.equals(this.selectedTab)) {
                        this.buildFriendsTab(refreshCmd, refreshEvt, this.playerRef.getUuid());
                    }
                    this.sendUpdate(refreshCmd, refreshEvt, false);
                }
                break;
            case ATTR_DETAIL_SELECT_ACTION:
                if (data.Category != null) {
                    this.selectedAttrDetailCategory = DetailCategory.fromIdOrDefault(data.Category, DetailCategory.OFFENSE);
                    UICommandBuilder refreshCmd = new UICommandBuilder();
                    UIEventBuilder refreshEvt = new UIEventBuilder();
                    this.buildAttributes(refreshCmd, refreshEvt, ref, store, this.playerRef.getUuid(), LevelingService.get());
                    this.sendUpdate(refreshCmd, refreshEvt, false);
                }
                break;
            case "SelectQuest":
                if (data.QuestID != null) {
                    this.selectedQuestId = data.QuestID;
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
            case "ToggleHideCompleted":
                this.hideCompleted = !this.hideCompleted; // Toggle boolean
                QuestManager.get().setHideCompleted(this.playerRef.getUuid(), this.hideCompleted);

                // Rebuild the tab to update list and checkbox visual
                UICommandBuilder refreshCmd1 = new UICommandBuilder();
                UIEventBuilder refreshEvt1 = new UIEventBuilder();
                this.buildQuestTab(refreshCmd1, refreshEvt1, this.playerRef.getUuid());
                this.sendUpdate(refreshCmd1, refreshEvt1, false);
                break;

            case "ToggleHideUnavailable":
                this.hideUnavailable = !this.hideUnavailable; // Toggle boolean
                QuestManager.get().setHideUnavailable(this.playerRef.getUuid(), this.hideUnavailable);

                UICommandBuilder refreshCmd2 = new UICommandBuilder();
                UIEventBuilder refreshEvt2 = new UIEventBuilder();
                this.buildQuestTab(refreshCmd2, refreshEvt2, this.playerRef.getUuid());
                this.sendUpdate(refreshCmd2, refreshEvt2, false);
                break;
            case QUEST_REQ_TOGGLE_ACTION:
                if (data.Category != null) {
                    toggleQuestRequirement(data.Category);
                    UICommandBuilder refreshCmd = new UICommandBuilder();
                    UIEventBuilder refreshEvt = new UIEventBuilder();
                    this.buildQuestTab(refreshCmd, refreshEvt, this.playerRef.getUuid());
                    this.sendUpdate(refreshCmd, refreshEvt, false);
                }
                break;
            case COMBAT_STYLE_CHANGED_ACTION:
                handleCombatStyleChange(data);
                break;
            case QUEST_FILTER_CHANGED_ACTION:
                handleQuestFilterChange(data);
                break;
            case OPEN_SKILL_INFO_ACTION:
                openSkillInfoPage(ref, store, data.SkillID);
                break;
            case FRIEND_INTERACT_ACTION:
                openSocialMenuFromTarget(ref, store, data.TargetUuid);
                break;
            case FRIEND_REMOVE_ACTION:
                handleFriendAction(data.TargetUuid, (service, target) -> service.removeFriend(this.playerRef.getUuid(), target));
                refreshFriendsTab();
                break;
            case FRIEND_ACCEPT_ACTION:
                handleFriendAction(data.TargetUuid, (service, target) -> service.acceptFriendRequest(this.playerRef.getUuid(), target));
                refreshFriendsTab();
                break;
            case FRIEND_DENY_ACTION:
                handleFriendAction(data.TargetUuid, (service, target) -> service.denyFriendRequest(this.playerRef.getUuid(), target));
                refreshFriendsTab();
                break;
            case FRIEND_UNIGNORE_ACTION:
                handleFriendAction(data.TargetUuid, (service, target) -> service.unignore(this.playerRef.getUuid(), target));
                refreshFriendsTab();
                break;
        }
    }

    private interface FriendActionExecutor {
        SocialActionResult execute(SocialService service, UUID targetUuid);
    }

    private void handleFriendAction(String targetUuidText, FriendActionExecutor executor) {
        SocialService socialService = Hyrune.getSocialService();
        if (socialService == null) {
            this.playerRef.sendMessage(Message.raw("Social service unavailable."));
            return;
        }
        UUID targetUuid = parseUuid(targetUuidText);
        if (targetUuid == null) {
            this.playerRef.sendMessage(Message.raw("Invalid player target."));
            return;
        }
        SocialActionResult result = executor.execute(socialService, targetUuid);
        if (result != null && result.message() != null) {
            this.playerRef.sendMessage(Message.raw(result.message()));
        }
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private void refreshFriendsTab() {
        UICommandBuilder refreshCmd = new UICommandBuilder();
        UIEventBuilder refreshEvt = new UIEventBuilder();
        this.buildFriendsTab(refreshCmd, refreshEvt, this.playerRef.getUuid());
        this.sendUpdate(refreshCmd, refreshEvt, false);
    }

    private void openSocialMenuFromTarget(@Nonnull Ref<EntityStore> ref,
                                          @Nonnull Store<EntityStore> store,
                                          String targetUuidText) {
        UUID targetUuid = parseUuid(targetUuidText);
        if (targetUuid == null) {
            this.playerRef.sendMessage(Message.raw("Invalid player target."));
            return;
        }

        Universe universe = Universe.get();
        PlayerRef targetPlayerRef = universe != null ? universe.getPlayer(targetUuid) : null;
        if (targetPlayerRef == null) {
            this.playerRef.sendMessage(Message.raw("That player is offline."));
            return;
        }
        if (!SocialInteractionRules.isWithinInteractionRange(this.playerRef, targetPlayerRef)) {
            this.playerRef.sendMessage(Message.raw("You must be within 5 blocks to use social actions."));
            return;
        }

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        World world = store.getExternalData().getWorld();
        CompletableFuture.runAsync(
            () -> player.getPageManager().openCustomPage(ref, store, new SocialMenuPage(this.playerRef, targetPlayerRef)),
            world
        );
    }

    private void openSkillInfoPage(@Nonnull Ref<EntityStore> ref,
                                   @Nonnull Store<EntityStore> store,
                                   String skillId) {
        SkillType selected = SkillType.ATTACK;
        if (skillId != null) {
            try {
                selected = SkillType.valueOf(skillId);
            } catch (IllegalArgumentException ignored) {
                selected = SkillType.ATTACK;
            }
        }

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        World world = store.getExternalData().getWorld();
        final SkillType targetSkill = selected;
        CompletableFuture.runAsync(() ->
            player.getPageManager().openCustomPage(ref, store, new SkillInfoPage(this.playerRef, targetSkill)),
            world
        );
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
        boolean showCombatSettings = TAB_COMBAT_SETTINGS.equals(this.selectedTab);
        boolean showQuests = TAB_QUESTS.equals(this.selectedTab);
        boolean showAttributes = TAB_ATTRIBUTES.equals(this.selectedTab);
        boolean showSkills = TAB_SKILLS.equals(this.selectedTab);
        boolean showFriends = TAB_FRIENDS.equals(this.selectedTab);
        boolean showClan = TAB_CLAN.equals(this.selectedTab);

        cmd.set("#PageTitle.Text", getTabTitle());
        cmd.set("#TabCombatSettings.Visible", showCombatSettings);
        cmd.set("#TabServerInfo.Visible", showServerInfo);
        cmd.set("#TabQuests.Visible", showQuests);
        cmd.set("#TabAttributes.Visible", showAttributes);
        cmd.set("#TabSkills.Visible", showSkills);
        cmd.set("#TabFriends.Visible", showFriends);
        cmd.set("#TabClan.Visible", showClan);
        cmd.set("#Footer.Visible", showSkills);
        cmd.set("#TabBar.SelectedTab", this.selectedTab);
        if (showCombatSettings) {
            this.buildCombatSettings(cmd, evt, this.playerRef.getUuid(), LevelingService.get());
        }
        if (showQuests) {
            this.buildQuestTab(cmd, evt, this.playerRef.getUuid());
        }
        if (showFriends) {
            this.buildFriendsTab(cmd, evt, this.playerRef.getUuid());
        }
        if (showClan) {
            this.buildClanTab(cmd, this.playerRef.getUuid());
        }
    }

    private void buildClanTab(UICommandBuilder cmd, UUID uuid) {
        ClanPanelBridge.ClanPanelSnapshot snapshot = ClanPanelBridge.snapshot(uuid);
        cmd.set("#ClanNameValue.Text", snapshot.clanName());
        cmd.set("#ClanRankValue.Text", snapshot.rankName());
        cmd.set("#ClanMembersValue.Text", String.format(Locale.US, "%d total / %d online", snapshot.memberCount(), snapshot.onlineCount()));
        cmd.set("#ClanMotdValue.Text", snapshot.motd());
    }

    private void buildFriendsTab(UICommandBuilder cmd, UIEventBuilder evt, UUID uuid) {
        cmd.clear("#FriendsList");
        cmd.clear("#FriendRequestsList");
        cmd.clear("#IgnoredList");

        SocialService socialService = Hyrune.getSocialService();
        if (socialService == null) {
            cmd.set("#FriendsPromptLabel.Text", "Social service unavailable.");
            cmd.set("#FriendsSummaryLabel.Text", "Friends: 0 | Online: 0 | Requests: 0");
            return;
        }

        List<UUID> friends = socialService.getFriends(uuid);
        List<UUID> incomingRequests = socialService.getIncomingRequests(uuid);
        List<UUID> outgoingRequests = socialService.getOutgoingRequests(uuid);
        List<UUID> ignored = socialService.getIgnored(uuid);

        int onlineCount = 0;
        for (UUID friendId : friends) {
            if (socialService.isOnline(friendId)) {
                onlineCount++;
            }
        }

        cmd.set("#FriendsPromptLabel.Text", "Look at a player and press F to interact. Outgoing requests: " + outgoingRequests.size());
        cmd.set("#FriendsSummaryLabel.Text", String.format(Locale.US,
            "Friends: %d | Online: %d | Requests: %d",
            friends.size(), onlineCount, incomingRequests.size()));

        for (int i = 0; i < friends.size(); i++) {
            UUID friendId = friends.get(i);
            String friendName = truncateForUi(socialService.resolveDisplayName(friendId), FRIEND_ROW_NAME_MAX_CHARS);
            boolean online = socialService.isOnline(friendId);
            cmd.append("#FriendsList", FRIEND_ROW_UI);
            String rowRoot = "#FriendsList[" + i + "]";
            cmd.set(rowRoot + " #FriendName.Text", friendName);
            cmd.set(rowRoot + " #FriendStatus.Text", online ? "Online" : "Offline");
            evt.addEventBinding(CustomUIEventBindingType.Activating, rowRoot + " #FriendInteractBtn",
                EventData.of("Button", FRIEND_INTERACT_ACTION).append(SkillMenuData.KEY_TARGET_UUID, friendId.toString()), false);
            evt.addEventBinding(CustomUIEventBindingType.Activating, rowRoot + " #FriendRemoveBtn",
                EventData.of("Button", FRIEND_REMOVE_ACTION).append(SkillMenuData.KEY_TARGET_UUID, friendId.toString()), false);
        }

        for (int i = 0; i < incomingRequests.size(); i++) {
            UUID requester = incomingRequests.get(i);
            cmd.append("#FriendRequestsList", FRIEND_REQUEST_ROW_UI);
            String rowRoot = "#FriendRequestsList[" + i + "]";
            cmd.set(rowRoot + " #RequestName.Text",
                truncateForUi(socialService.resolveDisplayName(requester), REQUEST_ROW_NAME_MAX_CHARS));
            evt.addEventBinding(CustomUIEventBindingType.Activating, rowRoot + " #RequestAcceptBtn",
                EventData.of("Button", FRIEND_ACCEPT_ACTION).append(SkillMenuData.KEY_TARGET_UUID, requester.toString()), false);
            evt.addEventBinding(CustomUIEventBindingType.Activating, rowRoot + " #RequestDenyBtn",
                EventData.of("Button", FRIEND_DENY_ACTION).append(SkillMenuData.KEY_TARGET_UUID, requester.toString()), false);
        }

        for (int i = 0; i < ignored.size(); i++) {
            UUID ignoredUuid = ignored.get(i);
            cmd.append("#IgnoredList", IGNORED_ROW_UI);
            String rowRoot = "#IgnoredList[" + i + "]";
            cmd.set(rowRoot + " #IgnoredName.Text",
                truncateForUi(socialService.resolveDisplayName(ignoredUuid), IGNORED_ROW_NAME_MAX_CHARS));
            evt.addEventBinding(CustomUIEventBindingType.Activating, rowRoot + " #IgnoredUnignoreBtn",
                EventData.of("Button", FRIEND_UNIGNORE_ACTION).append(SkillMenuData.KEY_TARGET_UUID, ignoredUuid.toString()), false);
        }
    }

    private String truncateForUi(String value, int maxChars) {
        if (value == null) {
            return "";
        }
        if (maxChars <= 3 || value.length() <= maxChars) {
            return value;
        }
        return value.substring(0, maxChars - 3) + "...";
    }

    private void buildQuestTab(UICommandBuilder cmd, UIEventBuilder evt, UUID uuid) {
        QuestManager questManager = QuestManager.get();
        syncQuestUiState(uuid, questManager);

        List<DropdownEntryInfo> filterEntries = new ArrayList<>();
        filterEntries.add(new DropdownEntryInfo(LocalizableString.fromString("A-Z"), QuestListFilter.ALPHABETICAL.name()));
        filterEntries.add(new DropdownEntryInfo(LocalizableString.fromString("Length"), QuestListFilter.BY_LENGTH.name()));
        filterEntries.add(new DropdownEntryInfo(LocalizableString.fromString("Difficulty"), QuestListFilter.BY_DIFFICULTY.name()));
        cmd.set("#QuestFilterInput.Entries", filterEntries);
        cmd.set("#QuestFilterInput.Value", this.currentQuestFilter.name());

        cmd.set("#HideCompletedToggle #CheckBox.Value", this.hideCompleted);
        cmd.set("#HideUnavailableToggle #CheckBox.Value", this.hideUnavailable);

        evt.addEventBinding(CustomUIEventBindingType.ValueChanged, "#QuestFilterInput",
                EventData.of("Button", QUEST_FILTER_CHANGED_ACTION)
                        .append(SkillMenuData.KEY_QUEST_FILTER, "#QuestFilterInput.Value"), false);
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
            cmd.set("#QuestDetailTitle.Text", "Select a Quest");
            cmd.set("#QuestDetailDesc.Text", "Click a quest to see its details.");
            cmd.set("#QuestJournalSection.Visible", false);
            cmd.set("#QuestRequirementsSection.Visible", false);
            cmd.set("#QuestRewardsSection.Visible", false);
            cmd.set("#QuestActionButtons.Visible", false);
            cmd.set("#QuestActionHint.Visible", false);
        }
        int currentPoints = questManager.getQuestPoints(uuid);
        int totalPoints = questManager.getAllQuests().stream().mapToInt(Quest::getQuestPoints).sum();
        cmd.set("#QuestPointsLabel.Text", "Quest Points: " + currentPoints + " / " + totalPoints);
        cmd.set("#QuestPointsVal.Text", String.valueOf(questManager.getQuestPoints(uuid)));
    }

    private void buildCombatSettings(UICommandBuilder cmd,
                                     UIEventBuilder evt,
                                     UUID uuid,
                                     LevelingService service) {
        if (service == null) {
            return;
        }
        List<DropdownEntryInfo> meleeEntries = new ArrayList<>();
        meleeEntries.add(createCombatEntry(CombatXpStyle.ATTACK));
        meleeEntries.add(createCombatEntry(CombatXpStyle.STRENGTH));
        meleeEntries.add(createCombatEntry(CombatXpStyle.DEFENCE));
        meleeEntries.add(createCombatEntry(CombatXpStyle.SHARED));
        cmd.set("#MeleeCombatStyle.Entries", meleeEntries);
        cmd.set("#MeleeCombatStyle.Value", service.getMeleeXpStyle(uuid).name());

        List<DropdownEntryInfo> rangedEntries = new ArrayList<>();
        rangedEntries.add(createCombatEntry(CombatXpStyle.RANGED));
        rangedEntries.add(createCombatEntry(CombatXpStyle.DEFENCE));
        rangedEntries.add(createCombatEntry(CombatXpStyle.SHARED));
        cmd.set("#RangedCombatStyle.Entries", rangedEntries);
        cmd.set("#RangedCombatStyle.Value", service.getRangedXpStyle(uuid).name());

        List<DropdownEntryInfo> magicEntries = new ArrayList<>();
        magicEntries.add(createCombatEntry(CombatXpStyle.MAGIC));
        magicEntries.add(createCombatEntry(CombatXpStyle.DEFENCE));
        magicEntries.add(createCombatEntry(CombatXpStyle.SHARED));
        cmd.set("#MagicCombatStyle.Entries", magicEntries);
        cmd.set("#MagicCombatStyle.Value", service.getMagicXpStyle(uuid).name());

        evt.addEventBinding(CustomUIEventBindingType.ValueChanged, "#MeleeCombatStyle",
            EventData.of("Button", COMBAT_STYLE_CHANGED_ACTION)
                .append(SkillMenuData.KEY_MELEE_COMBAT_STYLE, "#MeleeCombatStyle.Value"),
            false);
        evt.addEventBinding(CustomUIEventBindingType.ValueChanged, "#RangedCombatStyle",
            EventData.of("Button", COMBAT_STYLE_CHANGED_ACTION)
                .append(SkillMenuData.KEY_RANGED_COMBAT_STYLE, "#RangedCombatStyle.Value"),
            false);
        evt.addEventBinding(CustomUIEventBindingType.ValueChanged, "#MagicCombatStyle",
            EventData.of("Button", COMBAT_STYLE_CHANGED_ACTION)
                .append(SkillMenuData.KEY_MAGIC_COMBAT_STYLE, "#MagicCombatStyle.Value"),
            false);
    }

    private DropdownEntryInfo createCombatEntry(CombatXpStyle style) {
        return new DropdownEntryInfo(LocalizableString.fromString(style.getDisplayName()), style.name());
    }

    private void handleCombatStyleChange(SkillMenuData data) {
        LevelingService service = LevelingService.get();
        if (service == null) {
            return;
        }

        UUID uuid = this.playerRef.getUuid();
        boolean changed = false;

        if (data.MeleeCombatStyle != null) {
            CombatXpStyle style = CombatXpStyle.fromString(data.MeleeCombatStyle);
            if (style != null) {
                service.setMeleeXpStyle(uuid, style);
                changed = true;
            }
        }

        if (data.RangedCombatStyle != null) {
            CombatXpStyle style = CombatXpStyle.fromString(data.RangedCombatStyle);
            if (style != null) {
                service.setRangedXpStyle(uuid, style);
                changed = true;
            }
        }

        if (data.MagicCombatStyle != null) {
            CombatXpStyle style = CombatXpStyle.fromString(data.MagicCombatStyle);
            if (style != null) {
                service.setMagicXpStyle(uuid, style);
                changed = true;
            }
        }

        if (changed) {
            UICommandBuilder refreshCmd = new UICommandBuilder();
            UIEventBuilder refreshEvt = new UIEventBuilder();
            this.buildCombatSettings(refreshCmd, refreshEvt, uuid, service);
            this.sendUpdate(refreshCmd, refreshEvt, false);
        }
    }

    private void handleQuestFilterChange(SkillMenuData data) {
        if (data.QuestFilter == null) {
            return;
        }
        QuestManager manager = QuestManager.get();
        QuestListFilter filter = QuestListFilter.fromString(data.QuestFilter, QuestListFilter.ALPHABETICAL);
        this.currentQuestFilter = filter;
        manager.setQuestListFilter(this.playerRef.getUuid(), filter);

        UICommandBuilder refreshCmd = new UICommandBuilder();
        UIEventBuilder refreshEvt = new UIEventBuilder();
        this.buildQuestTab(refreshCmd, refreshEvt, this.playerRef.getUuid());
        this.sendUpdate(refreshCmd, refreshEvt, false);
    }

    private void syncQuestUiState(UUID uuid, QuestManager questManager) {
        if (uuid == null || questManager == null) {
            return;
        }
        this.hideCompleted = questManager.isHideCompleted(uuid);
        this.hideUnavailable = questManager.isHideUnavailable(uuid);
        this.currentQuestFilter = questManager.getQuestListFilter(uuid);
    }



    private void buildQuestList(UICommandBuilder cmd, UIEventBuilder evt, UUID uuid, QuestManager manager) {
        cmd.clear("#QuestList");

        List<Quest> quests = manager.getFilteredQuests(uuid, this.currentQuestFilter);
        int rowIndex = 0;
        String lastHeader = null;
        for (Quest quest : quests) {
            QuestStatus status = manager.getQuestStatus(uuid, quest.getId());
            if (this.hideCompleted && status == QuestStatus.COMPLETED) {
                continue;
            }
            if (this.hideUnavailable && status == QuestStatus.NOT_STARTED && !quest.meetsRequirements(uuid)) {
                continue;
            }

            String groupHeader = getQuestGroupHeader(quest);
            if (groupHeader != null && !groupHeader.equals(lastHeader)) {
                cmd.append("#QuestList", QUEST_HEADER_UI);
                String headerRoot = "#QuestList[" + rowIndex + "]";
                cmd.set(headerRoot + " #HeaderText.Text", groupHeader);
                rowIndex++;
                lastHeader = groupHeader;
            }

            cmd.append("#QuestList", QUEST_ITEM_UI);

            String itemRoot = "#QuestList[" + rowIndex + "]";
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

            rowIndex++;
        }
    }

    private String getQuestGroupHeader(Quest quest) {
        if (quest == null) {
            return null;
        }
        switch (this.currentQuestFilter) {
            case BY_LENGTH:
                return quest.getLength().getDisplayName();
            case BY_DIFFICULTY:
                return quest.getDifficulty().getDisplayName();
            case ALPHABETICAL:
            default:
                return getAlphabeticalHeader(quest.getName());
        }
    }

    private String getAlphabeticalHeader(String questName) {
        if (questName == null || questName.isBlank()) {
            return "#";
        }
        String normalized = questName.trim();
        if (normalized.length() > 4 && normalized.toLowerCase(Locale.ROOT).startsWith("the ")) {
            normalized = normalized.substring(4).trim();
        }
        if (normalized.isEmpty()) {
            return "#";
        }
        char first = Character.toUpperCase(normalized.charAt(0));
        if (!Character.isLetter(first)) {
            return "#";
        }
        return String.valueOf(first);
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

        cmd.set("#QuestActionButtons.Visible", true);
        cmd.set("#QuestAcceptRow.Visible", canStart);
        cmd.set("#QuestTrackRow.Visible", inProgress);
        cmd.set("#BtnAcceptQuest.Visible", canStart);
        cmd.set("#BtnTrackQuest.Visible", inProgress);
        cmd.set("#QuestActionHint.Text",
            "Length: " + quest.getLength().getDisplayName() + "  |  Difficulty: " + quest.getDifficulty().getDisplayName());
        cmd.set("#QuestActionHint.Style.TextColor", getQuestDifficultyColor(quest.getDifficulty()));
        cmd.set("#QuestActionHint.Visible", true);
    }

    private String getQuestDifficultyColor(dev.hytalemodding.hyrune.quests.QuestDifficulty difficulty) {
        switch (difficulty) {
            case TUTORIAL:
                return "#7ec8ff";
            case EASY:
                return COLOR_GREEN;
            case MEDIUM:
                return COLOR_YELLOW;
            case HARD:
                return COLOR_ORANGE;
            case ELITE:
                return COLOR_RED;
            default:
                return COLOR_WHITE;
        }
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

    private void bindAttributeDetailCategoryButton(UIEventBuilder eventBuilder, String target, DetailCategory category) {
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            target,
            EventData.of("Button", ATTR_DETAIL_SELECT_ACTION).append(SkillMenuData.KEY_CATEGORY, category.getId()),
            false
        );
    }

    private void buildAttributes(UICommandBuilder cmd,
                                 UIEventBuilder evt,
                                 Ref<EntityStore> ref,
                                 Store<EntityStore> store,
                                 UUID uuid,
                                 LevelingService service) {
        Player player = (ref == null || store == null) ? null : store.getComponent(ref, Player.getComponentType());
        CharacterAttributeComputationService.AttributeModel model = CharacterAttributeComputationService.compute(
            uuid,
            player,
            service,
            this.selectedAttrDetailCategory,
            movementSpeedMetersPerSecond(ref, store)
        );

        cmd.set("#AttrDetailCategoryTitle.Text", model.detailHeader());
        cmd.set("#AttrDetailHint.Text", model.detailHint());

        setDetailButtonLabel(cmd, "#AttrDetailOffenseBtn", DetailCategory.OFFENSE);
        setDetailButtonLabel(cmd, "#AttrDetailDefenseBtn", DetailCategory.DEFENSE);
        setDetailButtonLabel(cmd, "#AttrDetailResourcesBtn", DetailCategory.RESOURCES);
        setDetailButtonLabel(cmd, "#AttrDetailMobilityBtn", DetailCategory.MOBILITY);
        setDetailButtonLabel(cmd, "#AttrDetailGatheringBtn", DetailCategory.GATHERING);
        setDetailButtonLabel(cmd, "#AttrDetailHealingBtn", DetailCategory.HEALING);

        cmd.clear("#AttrOverviewContent");
        cmd.clear("#AttrDetailContent");

        int overviewRow = 0;
        for (CharacterAttributeComputationService.AttributeRow row : model.overviewRows()) {
            appendAttributeRow(cmd, "#AttrOverviewContent", overviewRow++, row);
        }

        int detailRow = 0;
        for (CharacterAttributeComputationService.AttributeRow row : model.detailRows()) {
            appendAttributeRow(cmd, "#AttrDetailContent", detailRow++, row);
        }
    }

    private void setDetailButtonLabel(UICommandBuilder cmd, String buttonPath, DetailCategory category) {
        cmd.set(buttonPath + ".Text", category.getDisplayName());
    }

    private Double movementSpeedMetersPerSecond(Ref<EntityStore> ref, Store<EntityStore> store) {
        if (ref == null || store == null) {
            return null;
        }
        MovementManager movementManager = store.getComponent(ref, MovementManager.getComponentType());
        if (movementManager == null || movementManager.getSettings() == null) {
            return null;
        }
        return (double) movementManager.getSettings().baseSpeed;
    }

    private void appendAttributeRow(UICommandBuilder cmd,
                                    String container,
                                    int index,
                                    CharacterAttributeComputationService.AttributeRow row) {
        cmd.append(container, ATTR_ROW_UI);
        String rowRoot = container + "[" + index + "]";
        cmd.set(rowRoot + " #AttrLabel.Text", row.label());
        cmd.set(rowRoot + " #AttrValue.Text", row.value());
    }


    private String getTabTitle() {
        switch (this.selectedTab) {
            case TAB_COMBAT_SETTINGS:
                return "Combat Settings";
            case TAB_SERVER_INFO:
                return "Server Info";
            case TAB_QUESTS:
                return "Quests";
            case TAB_ATTRIBUTES:
                return "Attributes";
            case TAB_FRIENDS:
                return "Friends List";
            case TAB_CLAN:
                return "Clan";
            case TAB_SKILLS:
            default:
                return "Skills";
        }
    }

    private void populateServerInfo(UICommandBuilder cmd) {
        int onlineCount = getOnlinePlayerCount();
        String onlineText = onlineCount >= 0 ? String.valueOf(onlineCount) : "Unknown";

        ZonedDateTime now = ZonedDateTime.now();
        String serverTime = TIME_FORMATTER.format(now);
        String serverDate = DATE_FORMATTER.format(now);
        String nextRestartTime = formatNextRestart(now);

        cmd.set("#ServerInfoPlayersOnline.Text", onlineText);
        cmd.set("#ServerInfoServerTime.Text", serverTime);
        cmd.set("#ServerInfoServerDate.Text", serverDate);
        cmd.set("#ServerInfoNextRestart.Text", nextRestartTime);
        cmd.set("#ServerInfoEventStatus.Text", DEFAULT_EVENT_STATUS);
        cmd.set("#ServerInfoEventDescription.Text", DEFAULT_EVENT_DESCRIPTION);
        cmd.set("#ServerInfoPatchVersion.Text", "Patch: " + SERVER_PATCH_VERSION);
    }

    private String formatNextRestart(ZonedDateTime now) {
        ZonedDateTime nextRestart = now
            .withHour(DAILY_RESTART_HOUR)
            .withMinute(DAILY_RESTART_MINUTE)
            .withSecond(0)
            .withNano(0);

        if (!nextRestart.isAfter(now)) {
            nextRestart = nextRestart.plusDays(1);
        }

        return RESTART_FORMATTER.format(nextRestart);
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
        static final String KEY_TARGET_UUID = "TargetUUID";
        static final String KEY_QUEST_FILTER = "@QuestFilter";
        static final String KEY_MELEE_COMBAT_STYLE = "@MeleeCombatStyle";
        static final String KEY_RANGED_COMBAT_STYLE = "@RangedCombatStyle";
        static final String KEY_MAGIC_COMBAT_STYLE = "@MagicCombatStyle";
        static final String KEY_QUEST_FILTER_FALLBACK = "QuestFilter";
        static final String KEY_MELEE_COMBAT_STYLE_FALLBACK = "MeleeCombatStyle";
        static final String KEY_RANGED_COMBAT_STYLE_FALLBACK = "RangedCombatStyle";
        static final String KEY_MAGIC_COMBAT_STYLE_FALLBACK = "MagicCombatStyle";

        public static final BuilderCodec<SkillMenuData> CODEC = BuilderCodec.builder(SkillMenuData.class, SkillMenuData::new)
                .addField(new KeyedCodec<>(KEY_BUTTON, Codec.STRING), (d, s) -> d.Button = s, d -> d.Button)
                .addField(new KeyedCodec<>(KEY_SKILL_ID, Codec.STRING), (d, s) -> d.SkillID = s, d -> d.SkillID)
                .addField(new KeyedCodec<>(KEY_TAB_ID, Codec.STRING), (d, s) -> d.TabID = s, d -> d.TabID)
                .addField(new KeyedCodec<>(KEY_CATEGORY, Codec.STRING), (d, s) -> d.Category = s, d -> d.Category)
                .addField(new KeyedCodec<>(KEY_QUEST_ID, Codec.STRING), (d, s) -> d.QuestID = s, d -> d.QuestID)
                .addField(new KeyedCodec<>(KEY_TARGET_UUID, Codec.STRING), (d, s) -> d.TargetUuid = s, d -> d.TargetUuid)
                .addField(new KeyedCodec<>(KEY_QUEST_FILTER, Codec.STRING), (d, s) -> d.QuestFilter = s, d -> d.QuestFilter)
                .addField(new KeyedCodec<>(KEY_MELEE_COMBAT_STYLE, Codec.STRING), (d, s) -> d.MeleeCombatStyle = s, d -> d.MeleeCombatStyle)
                .addField(new KeyedCodec<>(KEY_RANGED_COMBAT_STYLE, Codec.STRING), (d, s) -> d.RangedCombatStyle = s, d -> d.RangedCombatStyle)
                .addField(new KeyedCodec<>(KEY_MAGIC_COMBAT_STYLE, Codec.STRING), (d, s) -> d.MagicCombatStyle = s, d -> d.MagicCombatStyle)
                .addField(new KeyedCodec<>(KEY_QUEST_FILTER_FALLBACK, Codec.STRING), (d, s) -> d.QuestFilter = s, d -> d.QuestFilter)
                .addField(new KeyedCodec<>(KEY_MELEE_COMBAT_STYLE_FALLBACK, Codec.STRING), (d, s) -> d.MeleeCombatStyle = s, d -> d.MeleeCombatStyle)
                .addField(new KeyedCodec<>(KEY_RANGED_COMBAT_STYLE_FALLBACK, Codec.STRING), (d, s) -> d.RangedCombatStyle = s, d -> d.RangedCombatStyle)
                .addField(new KeyedCodec<>(KEY_MAGIC_COMBAT_STYLE_FALLBACK, Codec.STRING), (d, s) -> d.MagicCombatStyle = s, d -> d.MagicCombatStyle)
                .build();

        private String Button;
        private String SkillID;
        private String TabID;
        private String Category;
        private String QuestID;
        private String TargetUuid;
        private String QuestFilter;
        private String MeleeCombatStyle;
        private String RangedCombatStyle;
        private String MagicCombatStyle;

        @Override
        public String toString() {
            return "SkillMenuData{" +
                "Button='" + Button + '\'' +
                ", SkillID='" + SkillID + '\'' +
                ", TabID='" + TabID + '\'' +
                ", Category='" + Category + '\'' +
                ", QuestID='" + QuestID + '\'' +
                ", TargetUuid='" + TargetUuid + '\'' +
                ", QuestFilter='" + QuestFilter + '\'' +
                ", MeleeCombatStyle='" + MeleeCombatStyle + '\'' +
                ", RangedCombatStyle='" + RangedCombatStyle + '\'' +
                ", MagicCombatStyle='" + MagicCombatStyle + '\'' +
                '}';
        }
    }

}

