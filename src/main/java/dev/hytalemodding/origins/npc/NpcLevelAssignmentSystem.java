package dev.hytalemodding.origins.npc;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.QuerySystem;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import dev.hytalemodding.Origins;

import javax.annotation.Nonnull;

/**
 * ECS system for npc level assignment.
 */
public class NpcLevelAssignmentSystem extends EntityTickingSystem<EntityStore> implements QuerySystem<EntityStore> {

    private final NpcLevelService levelService;

    public NpcLevelAssignmentSystem(NpcLevelService levelService) {
        this.levelService = levelService;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public void tick(float dt,
                     int index,
                     @Nonnull ArchetypeChunk<EntityStore> chunk,
                     @Nonnull Store<EntityStore> store,
                     @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        if (ref == null || !ref.isValid()) {
            return;
        }

        NPCEntity npc = store.getComponent(ref, NPCEntity.getComponentType());
        if (npc == null || npc.getNPCTypeId() == null) {
            return;
        }

        if (store.getComponent(ref, Origins.getNpcLevelComponentType()) != null) {
            return;
        }

        String baseName = resolveBaseName(store, ref, npc);
        NpcLevelComponent component = levelService.buildComponent(npc.getNPCTypeId(), baseName);
        if (component == null) {
            return;
        }
        commandBuffer.putComponent(ref, Origins.getNpcLevelComponentType(), component);

        NpcLevelStatsApplier.apply(store, ref, component);
    }


    private static String resolveBaseName(Store<EntityStore> store, Ref<EntityStore> ref, NPCEntity npc) {
        DisplayNameComponent displayName = store.getComponent(ref, DisplayNameComponent.getComponentType());
        if (displayName != null) {
            Message message = displayName.getDisplayName();
            if (message != null && message.getRawText() != null && !message.getRawText().isBlank()) {
                return message.getRawText();
            }
        }

        String typeId = npc.getNPCTypeId();
        if (typeId == null) {
            return "NPC";
        }
        return typeId;
    }
}
