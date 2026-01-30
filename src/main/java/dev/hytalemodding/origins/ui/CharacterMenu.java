package dev.hytalemodding.origins.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.origins.level.LevelingService;
import dev.hytalemodding.origins.skills.SkillType;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.UUID;

public class CharacterMenu extends InteractiveCustomUIPage<CharacterMenu.SkillMenuData> {

    private static final String MAIN_UI = "Pages/SkillEntry.ui";
    private static final String CELL_UI = "Pages/character_stats.ui";
    private static final String ATTR_ROW_UI = "Pages/attribute_row.ui";
    private static final String UNLOCK_ROW_UI = "Pages/skill_unlock_row.ui";

    private static final String COLOR_YELLOW = "#ffff00";
    private static final String COLOR_ORANGE = "#ff981f"; // For selected skills.
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
    private static final String ATTR_CATEGORY_COMBAT = "Combat";
    private static final String ATTR_CATEGORY_GATHERING = "Gathering";
    private static final String ATTR_CATEGORY_MISC = "Misc";
    private static final String DEFAULT_DETAIL_TITLE = "Select a skill";
    private static final String DEFAULT_DETAIL_DESC = "Click a skill to see its bonuses.";

    private SkillType selectedSkill = null;
    private String selectedTab = TAB_SKILLS;
    private boolean combatExpanded = true;
    private boolean gatheringExpanded = true;
    private boolean miscExpanded = true;

    public CharacterMenu(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, SkillMenuData.CODEC);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {
        commandBuilder.append(MAIN_UI);

        UUID uuid = this.playerRef.getUuid();
        LevelingService levelingService = LevelingService.get();

        this.applyTabState(commandBuilder);
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
                        System.err.println("Invalid skill selected: " + data.skillId);
                    }
                }
                break;

            case "SelectTab":
                if (data.tabId != null) {
                    this.selectedTab = data.tabId;

                    UICommandBuilder refreshCmd = new UICommandBuilder();
                    this.applyTabState(refreshCmd);
                    if (TAB_SERVER_INFO.equals(this.selectedTab)) {
                        this.populateServerInfo(refreshCmd);
                    }
                    if (TAB_ATTRIBUTES.equals(this.selectedTab)) {
                        this.buildAttributes(refreshCmd, new UIEventBuilder(), this.playerRef.getUuid(), LevelingService.get());
                    }
                    this.sendUpdate(refreshCmd, new UIEventBuilder(), false);
                }
                break;
            case ATTR_TOGGLE_ACTION:
                if (data.category != null) {
                    toggleAttributeCategory(data.category);
                    UICommandBuilder refreshCmd = new UICommandBuilder();
                    this.buildAttributes(refreshCmd, new UIEventBuilder(), this.playerRef.getUuid(), LevelingService.get());
                    this.sendUpdate(refreshCmd, new UIEventBuilder(), false);
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

    private void applyTabState(UICommandBuilder cmd) {
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

        public static final BuilderCodec<SkillMenuData> CODEC = BuilderCodec.builder(SkillMenuData.class, SkillMenuData::new)
                .addField(new KeyedCodec<>(KEY_BUTTON, Codec.STRING), (d, s) -> d.button = s, d -> d.button)
                .addField(new KeyedCodec<>(KEY_SKILL_ID, Codec.STRING), (d, s) -> d.skillId = s, d -> d.skillId)
                .addField(new KeyedCodec<>(KEY_TAB_ID, Codec.STRING), (d, s) -> d.tabId = s, d -> d.tabId)
                .addField(new KeyedCodec<>(KEY_CATEGORY, Codec.STRING), (d, s) -> d.category = s, d -> d.category)
                .build();

        private String button;
        private String skillId;
        private String tabId;
        private String category;
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
