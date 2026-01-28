package dev.hytalemodding.origins.slayer;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import javax.annotation.Nonnull;

public class SlayerTaskKillSystem extends DeathSystems.OnDeathSystem {

    private final SlayerService slayerService;

    public SlayerTaskKillSystem(SlayerService slayerService) {
        this.slayerService = slayerService;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public void onComponentAdded(@Nonnull Ref<EntityStore> victimRef,
                                 @Nonnull DeathComponent deathComponent,
                                 @Nonnull Store<EntityStore> store,
                                 @Nonnull CommandBuffer<EntityStore> commandBuffer) {

        Damage deathInfo = deathComponent.getDeathInfo();
        if (deathInfo == null) {
            return;
        }

        Damage.Source source = deathInfo.getSource();
        if (!(source instanceof Damage.EntitySource entitySource)) {
            return;
        }

        Ref<EntityStore> killerRef = entitySource.getRef();
        if (!killerRef.isValid()) {
            return;
        }

        Player killer = store.getComponent(killerRef, Player.getComponentType());
        if (killer == null) {
            return;
        }

        NPCEntity npc = store.getComponent(victimRef, NPCEntity.getComponentType());
        if (npc == null) {
            return;
        }

        String npcTypeId = npc.getNPCTypeId();
        if (npcTypeId == null) {
            return;
        }

        slayerService.onKill(killer.getUuid(), npcTypeId);
    }
}
