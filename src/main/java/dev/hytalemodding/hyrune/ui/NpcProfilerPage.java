package dev.hytalemodding.hyrune.ui;

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
import dev.hytalemodding.hyrune.npc.NpcProfilerService;

import javax.annotation.Nonnull;

/**
 * UI page for inspecting plugin-driven NPC combat/runtime data.
 */
public class NpcProfilerPage extends InteractiveCustomUIPage<NpcProfilerPage.NpcProfilerData> {
    private static final String UI_PATH = "Pages/NpcProfiler.ui";
    private static final String SECTION_ROW_UI = "Pages/npc_profiler_section_row.ui";
    private static final String STAT_ROW_UI = "Pages/npc_profiler_stat_row.ui";
    private static final String ACTION_CLOSE = "Close";
    private static final String ACTION_REFRESH = "Refresh";

    private final Ref<EntityStore> targetNpcRef;
    private NpcProfilerService.NpcProfilerSnapshot snapshot;

    public NpcProfilerPage(@Nonnull PlayerRef playerRef,
                           @Nonnull Ref<EntityStore> targetNpcRef,
                           @Nonnull NpcProfilerService.NpcProfilerSnapshot snapshot) {
        super(playerRef, CustomPageLifetime.CanDismiss, NpcProfilerData.CODEC);
        this.targetNpcRef = targetNpcRef;
        this.snapshot = snapshot;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder commandBuilder,
                      @Nonnull UIEventBuilder eventBuilder,
                      @Nonnull Store<EntityStore> store) {
        commandBuilder.append(UI_PATH);
        render(commandBuilder, eventBuilder);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                @Nonnull NpcProfilerData data) {
        super.handleDataEvent(ref, store, data);
        if (data.button == null) {
            return;
        }

        if (ACTION_CLOSE.equals(data.button)) {
            this.close();
            return;
        }

        if (ACTION_REFRESH.equals(data.button)) {
            NpcProfilerService.NpcProfilerSnapshot rebuilt = NpcProfilerService.buildSnapshot(targetNpcRef);
            if (rebuilt != null) {
                this.snapshot = rebuilt;
            }
            refreshView();
        }
    }

    private void refreshView() {
        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder evt = new UIEventBuilder();
        render(cmd, evt);
        this.sendUpdate(cmd, evt, false);
    }

    private void render(UICommandBuilder cmd, UIEventBuilder evt) {
        String title = snapshot == null ? "NPC" : snapshot.title();
        String subtitle = snapshot == null ? "Profiler" : snapshot.subtitle();
        cmd.set("#ProfilerTitle.Text", title);
        cmd.set("#ProfilerSubtitle.Text", subtitle);

        cmd.clear("#StatsList");
        int rowIndex = 0;
        if (snapshot != null && snapshot.sections() != null) {
            for (NpcProfilerService.ProfilerSection section : snapshot.sections()) {
                cmd.append("#StatsList", SECTION_ROW_UI);
                String sectionRoot = "#StatsList[" + rowIndex + "]";
                cmd.set(sectionRoot + " #SectionTitle.Text", section.title());
                rowIndex++;

                if (section.rows() == null) {
                    continue;
                }
                for (NpcProfilerService.ProfilerRow row : section.rows()) {
                    cmd.append("#StatsList", STAT_ROW_UI);
                    String statRoot = "#StatsList[" + rowIndex + "]";
                    cmd.set(statRoot + " #StatLabel.Text", row.label());
                    cmd.set(statRoot + " #StatValue.Text", row.value());
                    rowIndex++;
                }
            }
        }

        evt.addEventBinding(CustomUIEventBindingType.Activating, "#ProfilerClose",
            EventData.of("Button", ACTION_CLOSE), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#ProfilerRefresh",
            EventData.of("Button", ACTION_REFRESH), false);
    }

    public static class NpcProfilerData {
        static final String KEY_BUTTON = "Button";

        public static final BuilderCodec<NpcProfilerData> CODEC = BuilderCodec.builder(NpcProfilerData.class, NpcProfilerData::new)
            .addField(new KeyedCodec<>(KEY_BUTTON, Codec.STRING), (d, s) -> d.button = s, d -> d.button)
            .build();

        private String button;
    }
}
