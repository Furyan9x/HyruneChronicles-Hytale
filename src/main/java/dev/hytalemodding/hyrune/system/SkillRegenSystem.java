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
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import dev.hytalemodding.Hyrune;
import dev.hytalemodding.hyrune.bonus.SkillStatBonusApplier;
import dev.hytalemodding.hyrune.combat.CombatStateTracker;
import dev.hytalemodding.hyrune.config.HyruneConfig;
import dev.hytalemodding.hyrune.config.HyruneConfigManager;
import dev.hytalemodding.hyrune.itemization.PlayerItemizationStatsService;
import dev.hytalemodding.hyrune.level.LevelingService;
import dev.hytalemodding.hyrune.npc.NpcLevelComponent;
import dev.hytalemodding.hyrune.skills.SkillType;
import dev.hytalemodding.hyrune.util.PlayerEntityAccess;

import javax.annotation.Nonnull;
import java.util.Locale;

/**
 * ECS system for skill regen.
 */
public class SkillRegenSystem extends EntityTickingSystem<EntityStore> {
    private static final String HEALTH_ID = "Health";
    private static final String MANA_ID = "Mana";
    private static final String STAMINA_ID = "Stamina";
    private static final float DEFAULT_PLAYER_HEALTH_REGEN_PER_CONSTITUTION = 20.0f / 99.0f;
    private static final float DEFAULT_PLAYER_HEALTH_REGEN_CAP_PER_SEC = 20.0f;
    private static final float DEFAULT_NPC_HEALTH_REGEN_PER_LEVEL = 20.0f / 99.0f;
    private static final float DEFAULT_NPC_HEALTH_REGEN_CAP_PER_SEC = 20.0f;
    private static final float DEFAULT_BOSS_HEALTH_REGEN_PER_LEVEL = 1000.0f / 99.0f;
    private static final float DEFAULT_BOSS_HEALTH_REGEN_CAP_PER_SEC = 1000.0f;

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
        RegenRuntimeConfig regenCfg = loadRegenConfig();
        Holder<EntityStore> holder = EntityUtils.toHolder(index, archetypeChunk);
        Player player = holder.getComponent(Player.getComponentType());
        EntityStatMap statMap = holder.getComponent(EntityStatMap.getComponentType());
        if (statMap == null) {
            return;
        }

        if (player != null) {
            tickPlayerRegen(holder, statMap, regenCfg, dt);
            return;
        }

        NPCEntity npc = holder.getComponent(NPCEntity.getComponentType());
        NpcLevelComponent npcLevel = holder.getComponent(Hyrune.getNpcLevelComponentType());
        if (npc == null || npcLevel == null) {
            return;
        }

        tickNpcRegen(statMap, npcLevel, regenCfg, dt);
    }

    private static void tickPlayerRegen(Holder<EntityStore> holder,
                                        EntityStatMap statMap,
                                        RegenRuntimeConfig regenCfg,
                                        float dt) {
        Player player = holder.getComponent(Player.getComponentType());
        if (player == null) {
            return;
        }

        PlayerRef playerRef = PlayerEntityAccess.getPlayerRef(player);
        if (playerRef == null) {
            return;
        }

        LevelingService service = Hyrune.getService();
        int magicLevel = service.getSkillLevel(playerRef.getUuid(), SkillType.MAGIC);
        int agilityLevel = service.getSkillLevel(playerRef.getUuid(), SkillType.AGILITY);
        int constitutionLevel = service.getSkillLevel(playerRef.getUuid(), SkillType.CONSTITUTION);
        var itemStats = PlayerItemizationStatsService.getCached(playerRef.getUuid());

        applyManaRegen(statMap, magicLevel, (float) itemStats.getItemManaRegenBonusPerSecond(), dt);

        if (!isSprinting(holder) && !CombatStateTracker.isInCombat(playerRef.getUuid())) {
            applyStaminaRegen(statMap, agilityLevel, (float) itemStats.getItemStaminaRegenBonusPerSecond(), dt);
            float constitutionRegen = constitutionLevel * regenCfg.playerHealthRegenPerConstitution;
            float rawHealthRegen = constitutionRegen + (float) itemStats.getItemHpRegenBonusPerSecond();
            applyHealthRegen(statMap, rawHealthRegen, regenCfg.playerHealthRegenCapPerSecond, dt);
        }
    }

    private static void tickNpcRegen(EntityStatMap statMap,
                                     NpcLevelComponent npcLevel,
                                     RegenRuntimeConfig regenCfg,
                                     float dt) {
        EntityStatValue hpValue = statMap.get(HEALTH_ID);
        if (hpValue == null || hpValue.get() <= 0f) {
            return;
        }

        boolean boss = isBossArchetype(npcLevel);

        int level = Math.max(1, Math.min(99, npcLevel.getLevel()));
        float regenPerSecond = boss
            ? (level * regenCfg.bossHealthRegenPerLevel)
            : (level * regenCfg.npcHealthRegenPerLevel);
        float capPerSecond = boss ? regenCfg.bossHealthRegenCapPerSecond : regenCfg.npcHealthRegenCapPerSecond;
        applyHealthRegen(statMap, regenPerSecond, capPerSecond, dt);
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

    private static float applyHealthRegen(EntityStatMap statMap,
                                          float regenPerSecond,
                                          float capPerSecond,
                                          float dt) {
        float effectivePerSecond = Math.max(0f, Math.min(regenPerSecond, capPerSecond));
        if (effectivePerSecond <= 0f) {
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

        float amountToAdd = effectivePerSecond * dt;
        if (amountToAdd <= 0f) {
            return 0f;
        }

        float newAmount = Math.min(max, current + amountToAdd);
        statMap.setStatValue(hpValue.getIndex(), newAmount);
        return newAmount - current;
    }

    private static boolean isBossArchetype(NpcLevelComponent npcLevel) {
        if (npcLevel == null || npcLevel.getArchetypeId() == null) {
            return false;
        }
        return "BOSS".equalsIgnoreCase(npcLevel.getArchetypeId().trim().toUpperCase(Locale.ROOT));
    }

    private static RegenRuntimeConfig loadRegenConfig() {
        HyruneConfig cfg = HyruneConfigManager.getConfig();
        HyruneConfig.RegenConfig regen = cfg == null ? null : cfg.regen;
        return new RegenRuntimeConfig(
            positiveOrDefault(regen == null ? 0.0 : regen.playerHealthRegenPerConstitution, DEFAULT_PLAYER_HEALTH_REGEN_PER_CONSTITUTION),
            positiveOrDefault(regen == null ? 0.0 : regen.playerHealthRegenCapPerSecond, DEFAULT_PLAYER_HEALTH_REGEN_CAP_PER_SEC),
            positiveOrDefault(regen == null ? 0.0 : regen.npcHealthRegenPerLevel, DEFAULT_NPC_HEALTH_REGEN_PER_LEVEL),
            positiveOrDefault(regen == null ? 0.0 : regen.npcHealthRegenCapPerSecond, DEFAULT_NPC_HEALTH_REGEN_CAP_PER_SEC),
            positiveOrDefault(regen == null ? 0.0 : regen.bossHealthRegenPerLevel, DEFAULT_BOSS_HEALTH_REGEN_PER_LEVEL),
            positiveOrDefault(regen == null ? 0.0 : regen.bossHealthRegenCapPerSecond, DEFAULT_BOSS_HEALTH_REGEN_CAP_PER_SEC)
        );
    }

    private static float positiveOrDefault(double value, float fallback) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value <= 0.0) {
            return fallback;
        }
        return (float) value;
    }

    private record RegenRuntimeConfig(float playerHealthRegenPerConstitution,
                                      float playerHealthRegenCapPerSecond,
                                      float npcHealthRegenPerLevel,
                                      float npcHealthRegenCapPerSecond,
                                      float bossHealthRegenPerLevel,
                                      float bossHealthRegenCapPerSecond) {
    }

    private static boolean isSprinting(Holder<EntityStore> holder) {
        MovementStatesComponent movementStates = holder.getComponent(MovementStatesComponent.getComponentType());
        if (movementStates == null) {
            return false;
        }
        return movementStates.getMovementStates().sprinting;
    }
}
