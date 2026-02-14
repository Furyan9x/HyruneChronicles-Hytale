package dev.hytalemodding.hyrune.itemization.tooltip;

import dev.hytalemodding.Hyrune;
import dev.hytalemodding.hyrune.events.LevelUpListener;

import java.util.UUID;

/**
 * Keeps dynamic tooltips in sync with skill-driven damage scaling.
 */
public final class DynamicTooltipLevelUpListener implements LevelUpListener {
    @Override
    public void onLevelUp(UUID uuid, int newLevel, String source) {
        if (uuid == null) {
            return;
        }
        Hyrune.refreshDynamicTooltipsForPlayer(uuid);
    }
}
