package dev.hytalemodding.origins.events;

import com.hypixel.hytale.server.core.entity.LivingEntity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.entity.LivingEntityInventoryChangeEvent;
import com.hypixel.hytale.server.core.universe.world.World;
import dev.hytalemodding.origins.tradepack.TradePackManager;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Event listener for trade pack inventory.
 */
public class TradePackInventoryListener {
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

        World world = player.getReference() != null ? player.getReference().getStore().getExternalData().getWorld() : null;
        if (world == null) {
            ACTIVE.remove(uuid);
            return;
        }

        world.execute(() -> {
            try {

                TradePackManager.sync(player);
            } finally {
                ACTIVE.remove(uuid);
            }
        });
    }
}
