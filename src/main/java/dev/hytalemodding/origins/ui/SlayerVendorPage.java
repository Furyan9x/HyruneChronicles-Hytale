package dev.hytalemodding.origins.ui;

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
import dev.hytalemodding.origins.slayer.SlayerPlayerData;
import dev.hytalemodding.origins.slayer.SlayerService;

import javax.annotation.Nonnull;

public class SlayerVendorPage extends InteractiveCustomUIPage<SlayerVendorPage.VendorData> {

    private static final String UI_PATH = "Pages/SlayerVendor.ui";

    private final SlayerService slayerService;

    public SlayerVendorPage(@Nonnull PlayerRef playerRef, SlayerService slayerService) {
        super(playerRef, CustomPageLifetime.CanDismiss, VendorData.CODEC);
        this.slayerService = slayerService;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder commandBuilder,
                      @Nonnull UIEventBuilder eventBuilder,
                      @Nonnull Store<EntityStore> store) {
        commandBuilder.append(UI_PATH);

        if (slayerService != null) {
            SlayerPlayerData data = slayerService.getPlayerData(playerRef.getUuid());
            commandBuilder.set("#VendorPoints.Text", String.valueOf(data.getSlayerPoints()));
            commandBuilder.set("#VendorTasks.Text", String.valueOf(data.getCompletedTasks()));
        }

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#VendorClose",
                EventData.of("Button", "Close"), false);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull VendorData data) {
        super.handleDataEvent(ref, store, data);
        if ("Close".equals(data.button)) {
            this.close();
        }
    }

    public static class VendorData {
        static final String KEY_BUTTON = "Button";

        public static final BuilderCodec<VendorData> CODEC = BuilderCodec.builder(VendorData.class, VendorData::new)
                .addField(new KeyedCodec<>(KEY_BUTTON, Codec.STRING), (d, s) -> d.button = s, d -> d.button)
                .build();

        private String button;
    }
}
