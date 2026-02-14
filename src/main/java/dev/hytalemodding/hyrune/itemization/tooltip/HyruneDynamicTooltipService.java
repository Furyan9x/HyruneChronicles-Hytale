package dev.hytalemodding.hyrune.itemization.tooltip;

import java.util.UUID;

/**
 * Facade for dynamic tooltip packet-layer services.
 */
public final class HyruneDynamicTooltipService {
    private final HyruneDynamicTooltipComposer composer;
    private final HyruneVirtualItemRegistry virtualItemRegistry;
    private final HyruneDynamicTooltipPacketAdapter packetAdapter;

    public HyruneDynamicTooltipService() {
        this.composer = new HyruneDynamicTooltipComposer();
        this.virtualItemRegistry = new HyruneVirtualItemRegistry();
        this.packetAdapter = new HyruneDynamicTooltipPacketAdapter(virtualItemRegistry, composer);
    }

    public void register() {
        packetAdapter.register();
    }

    public void shutdown() {
        packetAdapter.deregister();
        packetAdapter.invalidateAllPlayers();
        composer.clearCache();
        virtualItemRegistry.clearCache();
    }

    public void onPlayerLeave(UUID playerUuid) {
        if (playerUuid == null) {
            return;
        }
        packetAdapter.onPlayerLeave(playerUuid);
    }

    public boolean invalidateAndRefreshPlayer(UUID playerUuid) {
        if (playerUuid == null) {
            return false;
        }
        packetAdapter.invalidatePlayer(playerUuid);
        return packetAdapter.refreshPlayer(playerUuid);
    }

    public void invalidateAllPlayers() {
        packetAdapter.invalidateAllPlayers();
    }
}
