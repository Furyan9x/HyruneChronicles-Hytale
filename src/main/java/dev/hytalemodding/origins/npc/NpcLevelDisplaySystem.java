package dev.hytalemodding.origins.npc;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.QuerySystem;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import dev.hytalemodding.Origins;

import javax.annotation.Nonnull;

public class NpcLevelDisplaySystem extends EntityTickingSystem<EntityStore> implements QuerySystem<EntityStore> {
    private static final int TICK_INTERVAL = 5;
    private int tickCounter = 0;

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
        if (npc == null) {
            return;
        }

        NpcLevelService service = Origins.getNpcLevelService();
        String baseName = resolveBaseName(store, ref, npc);
        if (service != null && service.isExcluded(npc.getNPCTypeId(), baseName)) {
            Nameplate nameplate = store.getComponent(ref, Nameplate.getComponentType());
            if (nameplate == null) {
                commandBuffer.addComponent(ref, Nameplate.getComponentType(), new Nameplate(baseName));
            } else if (!baseName.equals(nameplate.getText())) {
                nameplate.setText(baseName);
            }
            return;
        }

        NpcLevelComponent levelComponent = store.getComponent(ref, Origins.getNpcLevelComponentType());
        if (levelComponent == null) {
            return;
        }

        tickCounter++;
        if (tickCounter < TICK_INTERVAL) {
            return;
        }
        tickCounter = 0;

        String label = NpcLevelHologram.buildLabel(
            levelComponent.getBaseName(),
            levelComponent.getLevel(),
            levelComponent.isElite()
        );

        String displayKey = label;
        if (displayKey.equals(levelComponent.getLastDisplayKey())) {
            return;
        }

        Nameplate nameplate = store.getComponent(ref, Nameplate.getComponentType());
        if (nameplate == null) {
            commandBuffer.addComponent(ref, Nameplate.getComponentType(), new Nameplate(label));
        } else {
            nameplate.setText(label);
        }
        levelComponent.setLastDisplayKey(displayKey);
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
        return typeId != null ? typeId : "NPC";
    }
}
