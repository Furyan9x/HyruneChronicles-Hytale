package dev.hytalemodding.hyrune.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.EntityUtils;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.Hyrune;
import dev.hytalemodding.hyrune.bonus.SkillStatBonusApplier;
import dev.hytalemodding.hyrune.combat.CombatStateTracker;
import dev.hytalemodding.hyrune.itemization.PlayerItemizationStatsService;
import dev.hytalemodding.hyrune.level.LevelingService;
import dev.hytalemodding.hyrune.skills.SkillType;
import dev.hytalemodding.hyrune.util.PlayerEntityAccess;

import javax.annotation.Nonnull;

/**
 * ECS system for skill regen.
 */
public class SkillRegenSystem extends EntityTickingSystem<EntityStore> {
    private static final String HEALTH_ID = "Health";
    private static final String MANA_ID = "Mana";
    private static final String STAMINA_ID = "Stamina";

    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public void tick(float dt,
                     int index,
                     @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                     @Nonnull Store<EntityStore> store,
                     @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        Holder<EntityStore> holder = EntityUtils.toHolder(index, archetypeChunk);
        Player player = holder.getComponent(Player.getComponentType());
        if (player == null) {
            return;
        }

        EntityStatMap statMap = holder.getComponent(EntityStatMap.getComponentType());
        if (statMap == null) {
            return;
        }

        PlayerRef playerRef = PlayerEntityAccess.getPlayerRef(player);
        if (playerRef == null) {
            return;
        }

        LevelingService service = Hyrune.getService();
        int magicLevel = service.getSkillLevel(playerRef.getUuid(), SkillType.MAGIC);
        int agilityLevel = service.getSkillLevel(playerRef.getUuid(), SkillType.AGILITY);
        var itemStats = PlayerItemizationStatsService.getCached(playerRef.getUuid());

        applyManaRegen(statMap, magicLevel, (float) itemStats.getItemManaRegenBonusPerSecond(), dt);

        if (!isSprinting(holder) && !CombatStateTracker.isInCombat(playerRef.getUuid())) {
            applyStaminaRegen(statMap, agilityLevel, (float) itemStats.getItemStaminaRegenBonusPerSecond(), dt);
            applyHealthRegen(statMap, (float) itemStats.getItemHpRegenBonusPerSecond(), dt);
        }
    }

    private static float applyManaRegen(EntityStatMap statMap, int magicLevel, float itemBonus, float dt) {
        EntityStatValue manaValue = statMap.get(MANA_ID);
        if (manaValue == null) {
            return 0f;
        }

        float current = manaValue.get();
        float max = manaValue.getMax();
        if (current >= max) {
            return 0f;
        }

        float amountToAdd = ((magicLevel * SkillStatBonusApplier.MANA_REGEN_PER_MAGIC) + itemBonus) * dt;
        if (amountToAdd <= 0f) {
            return 0f;
        }

        float newAmount = Math.min(max, current + amountToAdd);
        statMap.setStatValue(manaValue.getIndex(), newAmount);
        return newAmount - current;
    }

    private static float applyStaminaRegen(EntityStatMap statMap, int agilityLevel, float itemBonus, float dt) {
        EntityStatValue staminaValue = statMap.get(STAMINA_ID);
        if (staminaValue == null) {
            return 0f;
        }

        float current = staminaValue.get();
        float max = staminaValue.getMax();
        if (current >= max) {
            return 0f;
        }

        float amountToAdd = ((agilityLevel * SkillStatBonusApplier.STAMINA_REGEN_PER_AGILITY) + itemBonus) * dt;
        if (amountToAdd <= 0f) {
            return 0f;
        }

        float newAmount = Math.min(max, current + amountToAdd);
        statMap.setStatValue(staminaValue.getIndex(), newAmount);
        return newAmount - current;
    }

    private static float applyHealthRegen(EntityStatMap statMap, float itemHpRegenBonus, float dt) {
        if (itemHpRegenBonus <= 0f) {
            return 0f;
        }
        EntityStatValue hpValue = statMap.get(HEALTH_ID);
        if (hpValue == null) {
            return 0f;
        }

        float current = hpValue.get();
        float max = hpValue.getMax();
        if (current >= max) {
            return 0f;
        }

        float amountToAdd = itemHpRegenBonus * dt;
        if (amountToAdd <= 0f) {
            return 0f;
        }

        float newAmount = Math.min(max, current + amountToAdd);
        statMap.setStatValue(hpValue.getIndex(), newAmount);
        return newAmount - current;
    }

    private static boolean isSprinting(Holder<EntityStore> holder) {
        MovementStatesComponent movementStates = holder.getComponent(MovementStatesComponent.getComponentType());
        if (movementStates == null) {
            return false;
        }
        return movementStates.getMovementStates().sprinting;
    }
}
