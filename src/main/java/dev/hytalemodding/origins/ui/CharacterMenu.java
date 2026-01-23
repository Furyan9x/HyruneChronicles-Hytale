package dev.hytalemodding.origins.ui;
 // Adjust package as needed

import dev.hytalemodding.origins.level.LevelingService;
import dev.hytalemodding.origins.skills.SkillType;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.UUID;

public class CharacterMenu extends InteractiveCustomUIPage<CharacterMenu.SkillMenuData> {

    // UI Files
    private static final String MAIN_UI = "Pages/SkillEntry.ui";
    private static final String CELL_UI = "Pages/character_stats.ui";

    // Colors (RuneScape Style)
    private static final String COLOR_YELLOW = "#ffff00";
    private static final String COLOR_WHITE  = "#ffffff";
    private static final String COLOR_ORANGE = "#ff981f"; // For maxed skills

    // State
    private SkillType selectedSkill = null;

    public CharacterMenu(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, SkillMenuData.CODEC);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {
        commandBuilder.append(MAIN_UI);

        UUID uuid = this.playerRef.getUuid();
        LevelingService levelingService = LevelingService.get();

        // 1. Build the Grid
        this.buildSkillGrid(commandBuilder, eventBuilder, uuid, levelingService);

        // 2. Global Buttons (Close)
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BtnClose", EventData.of("Button", "Close"), false);

        // 3. Update Detail/Description Box (if you kept it in the UI)
        //this.updateDescription(commandBuilder);
    }

    private void buildSkillGrid(UICommandBuilder cmd, UIEventBuilder evt, UUID uuid, LevelingService service) {
        // Clear columns to prevent duplication on refresh
        cmd.clear("#Col1").clear("#Col2").clear("#Col3");

        int totalLevel = 0;
        int index = 0;

        // Iterate through all defined skills
        for (SkillType skill : SkillType.values()) {
            // Get Level (Assuming your service now accepts SkillType)
            // You will need to update LevelingService to support getLevel(UUID, SkillType)
            int level = service.getSkillLevel(uuid, skill);
            totalLevel += level;

            // Determine Column (0, 1, 2 cycling)
            int colIndex = index % 3;
            String targetCol = "#Col" + (colIndex + 1); // #Col1, #Col2, #Col3

            // Append the Cell
            cmd.append(targetCol, CELL_UI);

            // "List Syntax" allows us to target the specific instance we just added
            // Since we are adding to 3 different lists, we need to track the row index for THAT column
            int rowIndex = index / 3;
            String cellRoot = targetCol + "[" + rowIndex + "]";

            // Set Data
            cmd.set(cellRoot + " #SkillName.Text", skill.getDisplayName());
            cmd.set(cellRoot + " #SkillLevel.Text", level + "/99");
            cmd.set(cellRoot + " #IconText.Text", skill.getIconCode()); // "AT", "ST" placeholder

            // Styling (Highlight if selected)
            boolean isSelected = skill == this.selectedSkill;
            cmd.set(cellRoot + " #SkillName.Style.TextColor", isSelected ? COLOR_ORANGE : COLOR_YELLOW);

            // Bind Click Event
            evt.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    cellRoot, // Bind to the root of the cell (make sure #SkillRoot in UI is a Button!)
                    EventData.of("Button", "SelectSkill").append("SkillID", skill.name()),
                    false
            );

            index++;
        }

        // Update Footer Stats
        cmd.set("#TotalLevelVal.Text", String.valueOf(totalLevel));
        cmd.set("#CombatLevelVal.Text", String.valueOf(calculateCombatLevel(uuid, service)));
    }

//    private void updateDescription(UICommandBuilder cmd) {
//        // Assuming your runescape_menu.ui has a description area,
//        // perhaps appearing when a skill is clicked?
//        if (selectedSkill != null) {
//            cmd.set("#DescTitle.Text", selectedSkill.getDisplayName());
//            cmd.set("#DescText.Text", selectedSkill.getDescription());
//        } else {
//            cmd.set("#DescTitle.Text", "Skill Info");
//            cmd.set("#DescText.Text", "Select a skill to view details.");
//        }
//    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull SkillMenuData data) {
        super.handleDataEvent(ref, store, data);
        if (data.button == null) return;

        switch (data.button) {
            case "Close":
                this.close();
                break;

            case "SelectSkill":
                if (data.skillId != null) {
                    try {
                        this.selectedSkill = SkillType.valueOf(data.skillId);

                        // Refresh UI to show highlight and description
                        UICommandBuilder refreshCmd = new UICommandBuilder();
                        UIEventBuilder refreshEvt = new UIEventBuilder();

                        // Rebuild grid (to update highlights)
                        this.buildSkillGrid(refreshCmd, refreshEvt, this.playerRef.getUuid(), LevelingService.get());
                        //this.updateDescription(refreshCmd);

                        this.sendUpdate(refreshCmd, refreshEvt, false);

                    } catch (IllegalArgumentException e) {
                        System.err.println("Invalid skill selected: " + data.skillId);
                    }
                }
                break;
        }
    }

    /**
     * Calculates Combat Level based on standard weights.
     */
    private int calculateCombatLevel(UUID uuid, LevelingService service) {
        // Fetch Combat Stats
        int def = service.getSkillLevel(uuid, SkillType.DEFENCE);
        int hp = service.getSkillLevel(uuid, SkillType.CONSTITUTION);
        int div = service.getSkillLevel(uuid, SkillType.DIVINITY); // Prayer/Healer equivalent

        int att = service.getSkillLevel(uuid, SkillType.ATTACK);
        int str = service.getSkillLevel(uuid, SkillType.STRENGTH);
        int range = service.getSkillLevel(uuid, SkillType.RANGED);
        int magic = service.getSkillLevel(uuid, SkillType.MAGIC);

        // Formula: Base + Max(Melee, Ranged, Magic)
        // Base = 0.25 * (Def + HP + Prayer)
        double base = 0.25 * (def + hp + div);

        // Melee = 0.325 * (Att + Str)
        double melee = 0.325 * (att + str);

        // Range/Magic = 0.325 * (Level * 1.5)
        double ranged = 0.325 * (range * 1.5);
        double mage   = 0.325 * (magic * 1.5);

        double maxOffense = Math.max(melee, Math.max(ranged, mage));

        return (int) (base + maxOffense);
    }

    // --- CODEC ---
    public static class SkillMenuData {
        static final String KEY_BUTTON = "Button";
        static final String KEY_SKILL_ID = "SkillID";

        public static final BuilderCodec<SkillMenuData> CODEC = BuilderCodec.builder(SkillMenuData.class, SkillMenuData::new)
                .addField(new KeyedCodec<>(KEY_BUTTON, Codec.STRING), (d, s) -> d.button = s, d -> d.button)
                .addField(new KeyedCodec<>(KEY_SKILL_ID, Codec.STRING), (d, s) -> d.skillId = s, d -> d.skillId)
                .build();

        private String button;
        private String skillId;
    }
}