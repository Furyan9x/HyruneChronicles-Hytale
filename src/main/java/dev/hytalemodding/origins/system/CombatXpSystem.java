package dev.hytalemodding.origins.system;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import dev.hytalemodding.Origins;
import javax.annotation.Nonnull;

public class CombatXpSystem extends DeathSystems.OnDeathSystem {

    @Override
    public Query<EntityStore> getQuery() {
        // We want to listen to ANY entity dying (Monsters, Players, Animals)
        return Query.any();
    }

    @Override
    public void onComponentAdded(@Nonnull Ref<EntityStore> victimRef,
                                 @Nonnull DeathComponent deathComponent,
                                 @Nonnull Store<EntityStore> store,
                                 @Nonnull CommandBuffer<EntityStore> commandBuffer) {

        // 1. Check the Cause of Death
        Damage deathInfo = deathComponent.getDeathInfo();
        if (deathInfo == null) return;

        // 2. Was it caused by an Entity? (Not fall damage, lava, etc.)
        Damage.Source source = deathInfo.getSource();
        if (source instanceof Damage.EntitySource entitySource) {
            Ref<EntityStore> killerRef = entitySource.getRef();

            // 3. Was that entity a Player?
            if (killerRef.isValid()) {
                // Note: We use the server.core Player class from your imports
                Player player = store.getComponent(killerRef, Player.getComponentType());

                if (player != null) {
                    handlePlayerKill(player, victimRef, store);
                }
            }
        }
    }

    private void handlePlayerKill(Player player, Ref<EntityStore> victimRef, Store<EntityStore> store) {
        // 4. Calculate XP based on Victim's Max Health
        long xpToGive = 10; // Default fallback

        // Try to get stats
        EntityStatMap statMap = store.getComponent(victimRef, EntityStatMap.getComponentType());
        if (statMap != null) {
            try {
                // Look up the "Health" stat index
                int healthIndex = EntityStatType.getAssetMap().getIndex("Health");
                EntityStatValue healthStat = statMap.get(healthIndex);

                if (healthStat != null) {
                    float maxHealth = healthStat.getMax();
                    // Formula: 1 XP per HP
                    xpToGive = (long) maxHealth;
                }
            } catch (Exception e) {
                // If assets aren't loaded or stat is missing, ignore and use default
                System.out.println("[Origins] Could not read entity stats: " + e.getMessage());
            }
        }

        // 5. Award XP via our Service
        var service = Origins.getService();
        if (service != null) {
            service.addCombatXp(player.getUuid(), xpToGive);

            // 6. Feedback
            player.sendMessage(Message.raw("+" + xpToGive + " XP"));

            // Optional: Title
            // You can use EventTitleUtil here if you have the PlayerRef,
            // but PlayerRef is a separate component.
            // For now, Action Bar is safer and less intrusive for every kill.
        }
    }
}