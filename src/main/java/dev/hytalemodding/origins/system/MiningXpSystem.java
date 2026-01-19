package dev.hytalemodding.origins.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.Origins;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class MiningXpSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {

    // --- CONFIGURATION AREA ---
    // We store all our XP values here. Add as many as you want!
    private static final Map<String, Long> ORE_XP_MAP = new HashMap<>();

    static {
        // Tier 1: Common
        ORE_XP_MAP.put("copper", 5L);
        ORE_XP_MAP.put("iron", 8L);

        // Tier 2: Uncommon
        ORE_XP_MAP.put("gold", 20L);
        ORE_XP_MAP.put("cobalt", 25L);
        ORE_XP_MAP.put("Thorium", 35L);
        ORE_XP_MAP.put("Silver", 40L);

        // Tier 3: Rare
        ORE_XP_MAP.put("diamond", 60L);
        ORE_XP_MAP.put("emerald", 45L);
        ORE_XP_MAP.put("ruby", 45L);
        ORE_XP_MAP.put("sapphire", 45L);

        // Tier 4: Legendary / Hytale Specific
        ORE_XP_MAP.put("thorium", 80L);
        ORE_XP_MAP.put("cobalt", 80L);
        ORE_XP_MAP.put("Adamantite", 150L);
    }
    // --------------------------

    public MiningXpSystem() {
        super(BreakBlockEvent.class);
    }

    @Override
    @Nullable
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }

    @Override
    public void handle(int index,
                       @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull BreakBlockEvent event) {

        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return;

        // 1. Get ID and simplify
        String blockId = event.getBlockType().getId().toLowerCase();

        // 2. Loop through our Map to find a match
        long xpToGive = 0;

        for (Map.Entry<String, Long> entry : ORE_XP_MAP.entrySet()) {
            // If the block name contains our keyword (e.g. "deepslate_diamond_ore" contains "diamond")
            if (blockId.contains(entry.getKey())) {
                xpToGive = entry.getValue();
                break; // Stop looking after the first match
            }
        }

        // 3. Reward
        if (xpToGive > 0) {
            //Origins.getService().addXp(player.getUuid(), xpToGive);
            player.sendMessage(Message.raw("Mining: +" + xpToGive + " XP"));
        }
    }
}