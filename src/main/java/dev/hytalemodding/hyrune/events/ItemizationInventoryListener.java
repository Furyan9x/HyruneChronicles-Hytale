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

        UUID uuid = player.getUuid();
        if (!ACTIVE.add(uuid)) {
            return;
        }
        if (HyruneConfigManager.getConfig().itemizationDebugLogging) {
            LOGGER.at(Level.INFO).log("[Itemization] InventoryChange event player=" + uuid);
        }

        World world = player.getReference() != null ? player.getReference().getStore().getExternalData().getWorld() : null;
        if (world == null) {
            ACTIVE.remove(uuid);
            return;
        }

        world.execute(() -> {
            try {
                ItemRollCoordinator.applyPendingCraftRolls(player);
                PlayerItemizationStatsService.recompute(player);
                SkillStatBonusApplier.apply(player.getPlayerRef());
                SkillStatBonusApplier.applyMovementSpeed(player.getPlayerRef());
                HyruneDynamicTooltipService tooltipService = Hyrune.getDynamicTooltipService();
                if (tooltipService != null) {
                    boolean refreshed = tooltipService.invalidateAndRefreshPlayer(uuid);
                    if (HyruneConfigManager.getConfig().dynamicTooltipCacheDebug) {
                        LOGGER.at(Level.INFO).log("[Itemization] Dynamic tooltip invalidate+refresh player=" + uuid + ", refreshed=" + refreshed);
                    }
                }
            } finally {
                ACTIVE.remove(uuid);
            }
        });
    }
}
