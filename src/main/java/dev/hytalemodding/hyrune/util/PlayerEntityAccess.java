package dev.hytalemodding.hyrune.util;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Safe helpers for reading player-linked components from an entity reference.
 */
public final class PlayerEntityAccess {
    private PlayerEntityAccess() {
    }

    @Nullable
    public static PlayerRef getPlayerRef(Player player) {
        if (player == null) {
            return null;
        }
        Ref<EntityStore> ref = player.getReference();
        if (ref == null || !ref.isValid()) {
            return null;
        }
        Store<EntityStore> store = ref.getStore();
        return store == null ? null : store.getComponent(ref, PlayerRef.getComponentType());
    }

    @Nullable
    public static UUID getPlayerUuid(Player player) {
        if (player == null) {
            return null;
        }
        Ref<EntityStore> ref = player.getReference();
        if (ref == null || !ref.isValid()) {
            return null;
        }
        Store<EntityStore> store = ref.getStore();
        if (store == null) {
            return null;
        }
        UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
        return uuidComponent == null ? null : uuidComponent.getUuid();
    }
}
