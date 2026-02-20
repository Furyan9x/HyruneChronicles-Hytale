package dev.hytalemodding.hyrune.events;

import com.hypixel.hytale.server.core.event.events.ecs.InteractivelyPickupItemEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import dev.hytalemodding.hyrune.itemization.ItemGenerationService;
import dev.hytalemodding.hyrune.itemization.ItemRarityRollModel;
import dev.hytalemodding.hyrune.itemization.ItemRollSource;

/**
 * Fallback generation hook for items entering inventory from world pickup interactions.
 * Covers container loot / mob drops / world-generated items that are not routed through explicit systems.
 */
public class WorldItemGenerationListener {
    public void onInteractivelyPickupItem(InteractivelyPickupItemEvent event) {
        if (event == null) {
            return;
        }
        ItemStack itemStack = event.getItemStack();
        if (itemStack == null || itemStack.isEmpty()) {
            return;
        }

        ItemStack rolled = ItemGenerationService.rollIfEligible(
            itemStack,
            ItemRollSource.WORLD_PICKUP,
            ItemRarityRollModel.GenerationContext.of("interactive_pickup_world")
        );
        event.setItemStack(rolled);
    }
}
