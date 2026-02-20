package dev.hytalemodding.hyrune.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.EntityUtils;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.hyrune.combat.CombatStateTracker;
import dev.hytalemodding.hyrune.util.PlayerEntityAccess;

import java.util.UUID;

import javax.annotation.Nonnull;

/**
 * ECS system for combat state.
 */
public class CombatStateSystem extends EntityEventSystem<EntityStore, Damage> {
    public CombatStateSystem() {
        super(Damage.class);
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public void handle(int index,
                       @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull Damage damage) {
        if (damage.isCancelled()) {
            return;
        }

        // Victim (players and NPCs/entities with UUID component).
        var holder = EntityUtils.toHolder(index, archetypeChunk);
        UUIDComponent victimUuidComponent = holder.getComponent(UUIDComponent.getComponentType());
        if (victimUuidComponent != null && victimUuidComponent.getUuid() != null) {
            CombatStateTracker.markCombat(victimUuidComponent.getUuid());
        }

        // Attacker (entity/projectile sources with UUID component).
        Damage.Source source = damage.getSource();
        if (source instanceof Damage.EntitySource entitySource) {
            markAttackerCombat(store, entitySource.getRef());
        } else if (source instanceof Damage.ProjectileSource projectileSource) {
            markAttackerCombat(store, projectileSource.getRef());
        }
    }

    private static void markAttackerCombat(Store<EntityStore> store, Ref<EntityStore> attackerRef) {
        if (attackerRef == null || !attackerRef.isValid()) {
            return;
        }

        UUIDComponent attackerUuidComponent = store.getComponent(attackerRef, UUIDComponent.getComponentType());
        if (attackerUuidComponent != null && attackerUuidComponent.getUuid() != null) {
            CombatStateTracker.markCombat(attackerUuidComponent.getUuid());
            return;
        }

        Player attackerPlayer = store.getComponent(attackerRef, Player.getComponentType());
        if (attackerPlayer != null) {
            UUID attackerUuid = PlayerEntityAccess.getPlayerUuid(attackerPlayer);
            if (attackerUuid != null) {
                CombatStateTracker.markCombat(attackerUuid);
            }
        }
    }
}
