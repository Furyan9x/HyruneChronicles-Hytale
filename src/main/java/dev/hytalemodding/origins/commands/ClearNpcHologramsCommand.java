package dev.hytalemodding.origins.commands;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.ProjectileComponent;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import javax.annotation.Nonnull;

/**
 * Command handler for clear npc holograms.
 */
public class ClearNpcHologramsCommand extends AbstractWorldCommand {

    public ClearNpcHologramsCommand() {
        super("clearnpcholograms", "Remove orphaned NPC level holograms from the world");
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
                           @Nonnull World world,
                           @Nonnull Store<EntityStore> store) {
        final int[] removed = {0};

        store.forEachChunk((ArchetypeChunk<EntityStore> chunk, CommandBuffer<EntityStore> commandBuffer) -> {
            int size = chunk.size();
            for (int i = 0; i < size; i++) {
                if (chunk.getComponent(i, Player.getComponentType()) != null) {
                    continue;
                }
                if (chunk.getComponent(i, NPCEntity.getComponentType()) != null) {
                    continue;
                }

                ProjectileComponent projectile = chunk.getComponent(i, ProjectileComponent.getComponentType());
                if (projectile == null) {
                    continue;
                }

                Nameplate nameplate = chunk.getComponent(i, Nameplate.getComponentType());
                if (nameplate == null || nameplate.getText() == null) {
                    continue;
                }

                String text = nameplate.getText();
                if (!text.startsWith("[Lvl ") && !text.startsWith("* [Lv ")) {
                    continue;
                }

                Ref<EntityStore> ref = chunk.getReferenceTo(i);
                if (ref != null && ref.isValid()) {
                    commandBuffer.removeEntity(ref, RemoveReason.REMOVE);
                    removed[0]++;
                }
            }
        });

        ctx.sendMessage(Message.raw("Removed " + removed[0] + " NPC level holograms."));
    }
}
