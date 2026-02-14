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
import dev.hytalemodding.hyrune.skills.SkillType;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

/**
 * Full skill information page with left skill selector and right detail panel.
 */
public class SkillInfoPage extends InteractiveCustomUIPage<SkillInfoPage.SkillInfoData> {
    private static final String MAIN_UI = "Pages/SkillInfoPage.ui";
    private static final String SKILL_ICON_CELL_UI = "Pages/skill_info_icon_cell.ui";
    private static final String UNLOCK_ROW_UI = "Pages/skill_info_unlock_row.ui";

    private static final String ACTION_CLOSE = "Close";
    private static final String ACTION_BACK = "Back";
    private static final String ACTION_SELECT_SKILL = "SelectSkill";

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
                refreshSkillView();
                return;
            default:
                return;
        }
    }

    private void refreshSkillView() {
        UICommandBuilder updateCmd = new UICommandBuilder();
        UIEventBuilder updateEvt = new UIEventBuilder();
        buildSkillList(updateCmd, updateEvt);
        buildSkillDetail(updateCmd);
        this.sendUpdate(updateCmd, updateEvt, false);
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

            String iconPath = SkillUiAssets.getSkillIconPath(skill);
            if (iconPath == null) {
                iconPath = "Pages/skills.png";
            }
            if (iconPath != null) {
                cmd.set(rowRoot + " #SkillIcon.Background", iconPath);
            }

            boolean selected = skill == selectedSkill;
            cmd.set(rowRoot + " #SelectedUnderline.Visible", selected);

            evt.addEventBinding(CustomUIEventBindingType.Activating,
                rowRoot + " #SkillIconButton",
                EventData.of("Button", ACTION_SELECT_SKILL)
                    .append("SkillID", skill.name()),
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
