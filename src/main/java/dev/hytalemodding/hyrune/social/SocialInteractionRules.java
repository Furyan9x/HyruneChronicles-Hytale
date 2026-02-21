package dev.hytalemodding.hyrune.social;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;

/**
 * Shared social interaction rules (range + online checks).
 */
public final class SocialInteractionRules {
    public static final double MAX_INTERACTION_DISTANCE_BLOCKS = 5.0d;
    private static final double MAX_INTERACTION_DISTANCE_SQ =
        MAX_INTERACTION_DISTANCE_BLOCKS * MAX_INTERACTION_DISTANCE_BLOCKS;

    private SocialInteractionRules() {
    }

    public static boolean isOnline(@Nullable UUID playerUuid) {
        if (playerUuid == null) {
            return false;
        }
        Universe universe = Universe.get();
        return universe != null && universe.getPlayer(playerUuid) != null;
    }

    public static boolean isWithinInteractionRange(@Nullable PlayerRef source, @Nullable PlayerRef target) {
        return computeDistanceSquared(source, target) <= MAX_INTERACTION_DISTANCE_SQ;
    }

    public static double computeDistanceSquared(@Nullable PlayerRef source, @Nullable PlayerRef target) {
        if (source == null || target == null) {
            return Double.POSITIVE_INFINITY;
        }
        if (!Objects.equals(source.getWorldUuid(), target.getWorldUuid())) {
            return Double.POSITIVE_INFINITY;
        }

        Vector3d sourcePos = resolvePosition(source);
        Vector3d targetPos = resolvePosition(target);
        if (sourcePos == null || targetPos == null) {
            return Double.POSITIVE_INFINITY;
        }

        double dx = sourcePos.x - targetPos.x;
        double dy = sourcePos.y - targetPos.y;
        double dz = sourcePos.z - targetPos.z;
        return (dx * dx) + (dy * dy) + (dz * dz);
    }

    @Nullable
    private static Vector3d resolvePosition(PlayerRef playerRef) {
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return null;
        }
        Store<EntityStore> store = ref.getStore();
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        return transform == null ? null : transform.getPosition();
    }
}
