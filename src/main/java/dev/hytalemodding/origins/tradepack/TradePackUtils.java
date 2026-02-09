package dev.hytalemodding.origins.tradepack;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Utility helpers for trade pack.
 */
public final class TradePackUtils {
    public static final String TRADE_PACK_ITEM_ID = "TradePack_Basic";
    public static final String TRADE_PACK_MODEL = "Items/Back/BackpackBig.blockymodel";
    public static final String TRADE_PACK_TEXTURE = "Items/Back/BackpackBig_Texture.png";
    public static final float TRADE_PACK_SPEED_MULTIPLIER = 0.70f;

    private TradePackUtils() {
    }

    public static boolean hasTradePack(PlayerRef playerRef) {
        if (playerRef == null) {
            return false;
        }

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return false;
        }

        Store<EntityStore> store = ref.getStore();
        Player player = store.getComponent(ref, Player.getComponentType());
        return hasTradePack(player);
    }

    public static boolean hasTradePack(Player player) {
        if (player == null) {
            return false;
        }

        Inventory inventory = player.getInventory();
        if (inventory == null) {
            return false;
        }

        ItemContainer container = inventory.getCombinedEverything();
        if (container == null) {
            return false;
        }

        short capacity = container.getCapacity();
        for (short slot = 0; slot < capacity; slot++) {
            ItemStack stack = container.getItemStack(slot);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            if (TRADE_PACK_ITEM_ID.equals(stack.getItemId())) {
                return true;
            }
        }

        return false;
    }
}
