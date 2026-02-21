package dev.hytalemodding.hyrune.clans;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import dev.hytalemodding.Hyrune;
import dev.hytalemodding.hyrune.social.SocialService;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * First-pass clans plugin that wires CharacterMenu clan-tab data.
 */
public class HyruneClans extends JavaPlugin {

    public HyruneClans(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        ClanPanelBridge.registerProvider(this::buildSnapshot);
    }

    @Override
    protected void shutdown() {
        ClanPanelBridge.clearProvider();
    }

    private ClanPanelBridge.ClanPanelSnapshot buildSnapshot(UUID playerUuid) {
        SocialService socialService = Hyrune.getSocialService();
        if (socialService == null) {
            return new ClanPanelBridge.ClanPanelSnapshot(
                "Unaligned",
                "Wanderer",
                1,
                1,
                "Clans service booting..."
            );
        }

        int allies = socialService.getFriends(playerUuid).size();
        int online = 1;
        for (UUID friendId : socialService.getFriends(playerUuid)) {
            if (socialService.isOnline(friendId)) {
                online++;
            }
        }

        return new ClanPanelBridge.ClanPanelSnapshot(
            "Aurora Vanguard",
            "Vanguard",
            Math.max(1, allies + 1),
            online,
            "Stand together. Trade routes open at dusk."
        );
    }
}
