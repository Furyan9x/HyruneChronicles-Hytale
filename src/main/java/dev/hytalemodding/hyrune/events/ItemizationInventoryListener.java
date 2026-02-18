package dev.hytalemodding.hyrune.events;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.LivingEntity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.entity.LivingEntityInventoryChangeEvent;
import com.hypixel.hytale.server.core.universe.world.World;
import dev.hytalemodding.Hyrune;
import dev.hytalemodding.hyrune.bonus.SkillStatBonusApplier;
import dev.hytalemodding.hyrune.config.HyruneConfigManager;
import dev.hytalemodding.hyrune.itemization.ItemRollCoordinator;
import dev.hytalemodding.hyrune.itemization.PlayerItemizationStatsService;
import dev.hytalemodding.hyrune.itemization.tooltip.HyruneDynamicTooltipService;
import dev.hytalemodding.hyrune.util.PlayerEntityAccess;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Applies pending crafted-item rolls once outputs land in inventory.
 */
public class ItemizationInventoryListener {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final Set<UUID> ACTIVE = ConcurrentHashMap.newKeySet();

    public void onInventoryChange(LivingEntityInventoryChangeEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Player player)) {
            return;
        }

        UUID uuid = PlayerEntityAccess.getPlayerUuid(player);
        if (uuid == null) {
            return;
        }
        if (!ACTIVE.add(uuid)) {
            return;
        }

        World world = player.getReference() != null ? player.getReference().getStore().getExternalData().getWorld() : null;
        if (world == null) {
            ACTIVE.remove(uuid);
            return;
        }

        world.execute(() -> {
            long startedAt = System.currentTimeMillis();
            try {
                int appliedStacks = ItemRollCoordinator.applyPendingCraftRolls(player);
                PlayerItemizationStatsService.recompute(player);
                var playerRef = PlayerEntityAccess.getPlayerRef(player);
                if (playerRef != null) {
                    SkillStatBonusApplier.apply(playerRef);
                    SkillStatBonusApplier.applyMovementSpeed(playerRef);
                }
                boolean refreshed = false;
                HyruneDynamicTooltipService tooltipService = Hyrune.getDynamicTooltipService();
                if (tooltipService != null) {
                    refreshed = tooltipService.invalidateAndRefreshPlayer(uuid);
                }
                if (HyruneConfigManager.getConfig().itemizationDebugLogging) {
                    LOGGER.at(Level.INFO).log("[Itemization][Inventory] p=" + shortUuid(uuid)
                        + ", rolled=" + appliedStacks
                        + ", tooltipRefresh=" + refreshed
                        + ", ms=" + Math.max(0, System.currentTimeMillis() - startedAt));
                }
            } finally {
                ACTIVE.remove(uuid);
            }
        });
    }

    private static String shortUuid(UUID uuid) {
        if (uuid == null) {
            return "null";
        }
        String raw = uuid.toString();
        return raw.length() <= 8 ? raw : raw.substring(0, 8);
    }
}
