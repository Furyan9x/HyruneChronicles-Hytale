package dev.hytalemodding.hyrune.clans;

import java.util.UUID;
import java.util.function.Function;

/**
 * Bridge used by CharacterMenu to render clan tab data without compile-time dependency on the clans plugin module.
 */
public final class ClanPanelBridge {
    private static final ClanPanelSnapshot DEFAULT_SNAPSHOT = new ClanPanelSnapshot(
        "Unaligned",
        "Wanderer",
        1,
        1,
        "No clan data available."
    );

    private static volatile Function<UUID, ClanPanelSnapshot> provider = uuid -> DEFAULT_SNAPSHOT;

    private ClanPanelBridge() {
    }

    public static void registerProvider(Function<UUID, ClanPanelSnapshot> snapshotProvider) {
        provider = snapshotProvider != null ? snapshotProvider : uuid -> DEFAULT_SNAPSHOT;
    }

    public static void clearProvider() {
        provider = uuid -> DEFAULT_SNAPSHOT;
    }

    public static ClanPanelSnapshot snapshot(UUID playerUuid) {
        if (playerUuid == null) {
            return DEFAULT_SNAPSHOT;
        }
        try {
            ClanPanelSnapshot value = provider.apply(playerUuid);
            return value != null ? value : DEFAULT_SNAPSHOT;
        } catch (RuntimeException ignored) {
            return DEFAULT_SNAPSHOT;
        }
    }

    public record ClanPanelSnapshot(
        String clanName,
        String rankName,
        int memberCount,
        int onlineCount,
        String motd
    ) {
        public ClanPanelSnapshot {
            clanName = safe(clanName, "Unaligned");
            rankName = safe(rankName, "Wanderer");
            memberCount = Math.max(0, memberCount);
            onlineCount = Math.max(0, onlineCount);
            motd = safe(motd, "No clan message set.");
        }

        private static String safe(String value, String fallback) {
            if (value == null || value.isBlank()) {
                return fallback;
            }
            return value;
        }
    }
}
