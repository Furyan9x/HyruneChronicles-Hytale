package dev.hytalemodding.origins.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.origins.level.LevelingService;
import dev.hytalemodding.origins.skills.SkillType;

import javax.annotation.Nonnull;
import java.util.logging.Level;
import java.util.concurrent.CompletableFuture;

/**
 * Full skill information page with left skill selector and right detail panel.
 */
public class SkillInfoPage extends InteractiveCustomUIPage<SkillInfoPage.SkillInfoData> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String MAIN_UI = "Pages/SkillInfoPage.ui";
    private static final String SKILL_ICON_CELL_UI = "Pages/skill_info_icon_cell.ui";
    private static final String UNLOCK_ROW_UI = "Pages/skill_info_unlock_row.ui";

    private static final String ACTION_CLOSE = "Close";
    private static final String ACTION_BACK = "Back";
    private static final String ACTION_SELECT_SKILL = "SelectSkill";
    private static final String ACTION_SELECT_SKILL_PREFIX = "SelectSkill_";

    private SkillType selectedSkill;

    public SkillInfoPage(@Nonnull PlayerRef playerRef, SkillType initiallySelectedSkill) {
        super(playerRef, CustomPageLifetime.CanDismiss, SkillInfoData.CODEC);
        this.selectedSkill = initiallySelectedSkill != null ? initiallySelectedSkill : SkillType.ATTACK;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder evt,
                      @Nonnull Store<EntityStore> store) {
        cmd.append(MAIN_UI);
        buildSkillList(cmd, evt);
        buildSkillDetail(cmd);

        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BtnClose",
            EventData.of("Button", ACTION_CLOSE), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BtnBack",
            EventData.of("Button", ACTION_BACK), false);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                @Nonnull SkillInfoData data) {
        super.handleDataEvent(ref, store, data);
        if (data.button == null) {
            return;
        }
        LOGGER.at(Level.INFO).log("SkillInfo event: button=" + data.button + ", skillId=" + data.skillId);

        switch (data.button) {
            case ACTION_CLOSE:
                this.close();
                return;
            case ACTION_BACK:
                goBackToCharacterSkills(ref, store);
                return;
            case ACTION_SELECT_SKILL:
                if (data.skillId == null) {
                    return;
                }
                try {
                    this.selectedSkill = SkillType.valueOf(data.skillId);
                } catch (IllegalArgumentException ignored) {
                    return;
                }
                UICommandBuilder updateCmd = new UICommandBuilder();
                UIEventBuilder updateEvt = new UIEventBuilder();
                buildSkillList(updateCmd, updateEvt);
                buildSkillDetail(updateCmd);
                this.sendUpdate(updateCmd, updateEvt, false);
                return;
            default:
                if (data.button.startsWith(ACTION_SELECT_SKILL_PREFIX)) {
                    String skillKey = data.button.substring(ACTION_SELECT_SKILL_PREFIX.length());
                    try {
                        this.selectedSkill = SkillType.valueOf(skillKey);
                    } catch (IllegalArgumentException ignored) {
                        return;
                    }

                    UICommandBuilder cmd = new UICommandBuilder();
                    UIEventBuilder evt = new UIEventBuilder();
                    buildSkillList(cmd, evt);
                    buildSkillDetail(cmd);
                    this.sendUpdate(cmd, evt, false);
                }
                return;
        }
    }

    private void goBackToCharacterSkills(@Nonnull Ref<EntityStore> ref,
                                         @Nonnull Store<EntityStore> store) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        World world = store.getExternalData().getWorld();
        CompletableFuture.runAsync(() ->
            player.getPageManager().openCustomPage(ref, store, new CharacterMenu(this.playerRef)),
            world
        );
    }

    private void buildSkillList(UICommandBuilder cmd, UIEventBuilder evt) {
        cmd.clear("#IconCol1").clear("#IconCol2").clear("#IconCol3");
        int index = 0;
        for (SkillType skill : SkillType.values()) {
            int colIndex = index % 3;
            String targetCol = "#IconCol" + (colIndex + 1);
            cmd.append(targetCol, SKILL_ICON_CELL_UI);
            int rowIndex = index / 3;
            String rowRoot = targetCol + "[" + rowIndex + "]";

            String iconPath = getSkillIconPath(skill);
            if (iconPath != null) {
                cmd.set(rowRoot + " #SkillIcon.Background", iconPath);
            }

            boolean selected = skill == selectedSkill;
            cmd.set(rowRoot + " #SelectedUnderline.Visible", selected);

            evt.addEventBinding(CustomUIEventBindingType.Activating,
                rowRoot + " #SkillIconButton",
                EventData.of("Button", ACTION_SELECT_SKILL)
                    .append("SkillID", skill.name())
                    .append("SkillKey", ACTION_SELECT_SKILL_PREFIX + skill.name()),
                false);
            index++;
        }
    }

    private void buildSkillDetail(UICommandBuilder cmd) {
        SkillDetailRegistry.SkillDetail detail = SkillDetailRegistry.getDetail(selectedSkill);
        if (detail == null) {
            cmd.set("#SkillTitle.Text", selectedSkill.getDisplayName());
            cmd.set("#SkillDescription.Text", "No skill details configured yet.");
            cmd.clear("#UnlockList");
            return;
        }

        int playerSkillLevel = 1;
        LevelingService service = LevelingService.get();
        if (service != null) {
            playerSkillLevel = service.getSkillLevel(this.playerRef.getUuid(), selectedSkill);
        }

        cmd.set("#SkillTitle.Text", detail.title);
        cmd.set("#SkillDescription.Text", detail.description);
        cmd.clear("#UnlockList");

        int row = 0;
        for (SkillDetailRegistry.SkillUnlock unlock : detail.unlocks) {
            cmd.append("#UnlockList", UNLOCK_ROW_UI);
            String rowRoot = "#UnlockList[" + row + "]";
            cmd.set(rowRoot + " #UnlockLevel.Text", String.valueOf(unlock.level));
            cmd.set(rowRoot + " #UnlockText.Text", unlock.text);

            boolean unlocked = playerSkillLevel >= unlock.level;
            cmd.set(rowRoot + " #UnlockLevel.Style.TextColor", unlocked ? "#f2d38a" : "#6e6e6e");
            cmd.set(rowRoot + " #UnlockText.Style.TextColor", unlocked ? "#f7e9cd" : "#6e6e6e");

            row++;
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
                return "Pages/skills.png";
        }
    }

    /**
     * UI binding payload.
     */
    public static class SkillInfoData {
        static final String KEY_BUTTON = "Button";
        static final String KEY_SKILL_ID = "SkillID";

        public static final BuilderCodec<SkillInfoData> CODEC = BuilderCodec.builder(SkillInfoData.class, SkillInfoData::new)
            .addField(new KeyedCodec<>(KEY_BUTTON, Codec.STRING), (d, s) -> d.button = s, d -> d.button)
            .addField(new KeyedCodec<>(KEY_SKILL_ID, Codec.STRING), (d, s) -> d.skillId = s, d -> d.skillId)
            .build();

        private String button;
        private String skillId;
    }
}
