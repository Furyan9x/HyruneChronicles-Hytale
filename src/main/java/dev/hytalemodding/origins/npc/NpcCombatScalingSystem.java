package dev.hytalemodding.origins.npc;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.dependency.SystemGroupDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.EntityUtils;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import dev.hytalemodding.Origins;
import dev.hytalemodding.origins.system.SkillCombatBonusSystem;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * ECS system for npc combat scaling.
 */
public class NpcCombatScalingSystem extends EntityEventSystem<EntityStore, Damage> {

    private final NpcLevelService levelService;

    public NpcCombatScalingSystem(NpcLevelService levelService) {
        super(Damage.class);
        this.levelService = levelService;
    }

    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return Set.of(
            new SystemGroupDependency<>(Order.AFTER, DamageModule.get().getGatherDamageGroup()),
            new SystemDependency<>(Order.BEFORE, DamageSystems.ApplyDamage.class)
        );
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public void handle(int index,
                       @Nonnull ArchetypeChunk<EntityStore> chunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull Damage damage) {
        if (damage.isCancelled()) {
            return;
        }

        applyNpcDefence(index, chunk, store, damage);
        applyNpcOutgoing(store, damage);
    }

    private void applyNpcDefence(int index,
                                 ArchetypeChunk<EntityStore> chunk,
                                 Store<EntityStore> store,
                                 Damage damage) {
        Ref<EntityStore> victimRef = chunk.getReferenceTo(index);
        if (victimRef == null || !victimRef.isValid()) {
            return;
        }

        NpcLevelComponent npcLevel = store.getComponent(victimRef, Origins.getNpcLevelComponentType());
        if (npcLevel == null) {
            return;
        }

        int level = npcLevel.getLevel();
        if (level <= 0) {
            return;
        }

        float reduction = Math.min(SkillCombatBonusSystem.DEFENCE_DAMAGE_REDUCTION_CAP,
            level * SkillCombatBonusSystem.DEFENCE_DAMAGE_REDUCTION_PER_LEVEL);
        float afterDefence = damage.getAmount() * (1.0f - reduction);

        CombatStyle style = resolveCombatStyle(damage, store);
        CombatStyle weakness = npcLevel.getWeakness() != null
            ? npcLevel.getWeakness()
            : levelService.resolveWeakness(levelService.getConfig().getDefaultWeakness());
        double multiplier = style == weakness
            ? levelService.getConfig().getWeaknessMultiplier()
            : levelService.getConfig().getResistanceMultiplier();

        float finalDamage = (float) (afterDefence * multiplier);
        damage.setAmount(finalDamage);
    }

    private void applyNpcOutgoing(Store<EntityStore> store, Damage damage) {
        Damage.Source source = damage.getSource();
        if (source == null) {
            return;
        }

        Ref<EntityStore> attackerRef = null;
        if (source instanceof Damage.EntitySource entitySource) {
            attackerRef = entitySource.getRef();
        } else if (source instanceof Damage.ProjectileSource projectileSource) {
            attackerRef = projectileSource.getRef();
        }

        if (attackerRef == null || !attackerRef.isValid()) {
            return;
        }

        NpcLevelComponent npcLevel = store.getComponent(attackerRef, Origins.getNpcLevelComponentType());
        NPCEntity npc = store.getComponent(attackerRef, NPCEntity.getComponentType());
        if (npc == null || npcLevel == null) {
            return;
        }

        int level = npcLevel.getLevel();
        if (level <= 0) {
            return;
        }

        CombatStyle style = resolveCombatStyle(damage, store);
        switch (style) {
            case RANGED:
                applyRanged(level, damage);
                break;
            case MAGIC:
                applyMagic(level, damage);
                break;
            case MELEE:
            default:
                applyMelee(level, damage);
                break;
        }
    }

    private static void applyMelee(int level, Damage damage) {
        float multiplier = 1.0f + (level * SkillCombatBonusSystem.ATTACK_DAMAGE_PER_LEVEL);
        damage.setAmount(damage.getAmount() * multiplier);

        float chance = Math.min(SkillCombatBonusSystem.STRENGTH_CRIT_CHANCE_CAP,
            level * SkillCombatBonusSystem.STRENGTH_CRIT_CHANCE_PER_LEVEL);
        if (ThreadLocalRandom.current().nextFloat() >= chance) {
            return;
        }

        float critMultiplier = SkillCombatBonusSystem.STRENGTH_CRIT_BASE_MULTIPLIER
            + (level * SkillCombatBonusSystem.STRENGTH_CRIT_DAMAGE_BONUS_PER_LEVEL);
        damage.setAmount(damage.getAmount() * critMultiplier);
    }

    private static void applyRanged(int level, Damage damage) {
        float multiplier = 1.0f + (level * SkillCombatBonusSystem.RANGED_DAMAGE_PER_LEVEL);
        damage.setAmount(damage.getAmount() * multiplier);

        float chance = Math.min(SkillCombatBonusSystem.RANGED_CRIT_CHANCE_CAP,
            level * SkillCombatBonusSystem.RANGED_CRIT_CHANCE_PER_LEVEL);
        if (ThreadLocalRandom.current().nextFloat() >= chance) {
            return;
        }

        float critMultiplier = SkillCombatBonusSystem.RANGED_CRIT_BASE_MULTIPLIER
            + (level * SkillCombatBonusSystem.RANGED_CRIT_DAMAGE_BONUS_PER_LEVEL);
        damage.setAmount(damage.getAmount() * critMultiplier);
    }

    private static void applyMagic(int level, Damage damage) {
        float multiplier = 1.0f + (level * SkillCombatBonusSystem.MAGIC_DAMAGE_PER_LEVEL);
        damage.setAmount(damage.getAmount() * multiplier);

        float chance = Math.min(SkillCombatBonusSystem.MAGIC_CRIT_CHANCE_CAP,
            level * SkillCombatBonusSystem.MAGIC_CRIT_CHANCE_PER_LEVEL);
        if (ThreadLocalRandom.current().nextFloat() >= chance) {
            return;
        }

        float critMultiplier = SkillCombatBonusSystem.MAGIC_CRIT_BASE_MULTIPLIER
            + (level * SkillCombatBonusSystem.MAGIC_CRIT_DAMAGE_BONUS_PER_LEVEL);
        damage.setAmount(damage.getAmount() * critMultiplier);
    }

    private CombatStyle resolveCombatStyle(Damage damage, Store<EntityStore> store) {
        DamageCause cause = damage.getCause();
        if (cause != null) {
            String id = cause.getId();
            if (id != null) {
                String normalized = id.toLowerCase();
                if (normalized.contains("magic") || normalized.contains("spell")) {
                    return CombatStyle.MAGIC;
                }
                if (normalized.contains("projectile") || normalized.contains("arrow")) {
                    return CombatStyle.RANGED;
                }
            }
        }

        Damage.Source source = damage.getSource();
        if (source instanceof Damage.ProjectileSource) {
            return CombatStyle.RANGED;
        }

        if (source instanceof Damage.EntitySource entitySource) {
            Ref<EntityStore> ref = entitySource.getRef();
            if (ref != null && ref.isValid()) {
                Player player = store.getComponent(ref, Player.getComponentType());
                if (player != null) {
                    String weaponId = SkillCombatBonusSystem.getHeldItemIdentifier(player);
                    if (weaponId != null) {
                        String id = weaponId.toLowerCase();
                        if (id.contains("wand") || id.contains("staff") || id.contains("spellbook") || id.contains("scepter")) {
                            return CombatStyle.MAGIC;
                        }
                        if (id.contains("shortbow") || id.contains("longbow") || id.contains("crossbow")
                            || id.contains("gun") || id.contains("sling")) {
                            return CombatStyle.RANGED;
                        }
                    }
                }
            }
        }

        return CombatStyle.MELEE;
    }
}
