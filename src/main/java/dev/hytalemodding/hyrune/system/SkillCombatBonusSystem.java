package dev.hytalemodding.hyrune.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.dependency.SystemGroupDependency;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.EntityUtils;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.Message;
import dev.hytalemodding.Hyrune;
import dev.hytalemodding.hyrune.registry.CombatRequirementRegistry;
import dev.hytalemodding.hyrune.level.LevelingService;
import dev.hytalemodding.hyrune.skills.SkillType;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

import static dev.hytalemodding.Hyrune.LOGGER;

/**
 * ECS system for skill combat bonus.
 */
public class SkillCombatBonusSystem extends EntityEventSystem<EntityStore, Damage> {
    public static final float ATTACK_DAMAGE_PER_LEVEL = 0.02f;
    public static final float DEFENCE_DAMAGE_REDUCTION_PER_LEVEL = 0.30f / 99.0f;
    public static final float DEFENCE_DAMAGE_REDUCTION_CAP = 0.30f;
    public static final float RANGED_DAMAGE_PER_LEVEL = 0.03f;
    public static final float MAGIC_DAMAGE_PER_LEVEL = 0.03f;
    public static final float STRENGTH_CRIT_CHANCE_PER_LEVEL = 0.35f / 99.0f;
    public static final float STRENGTH_CRIT_CHANCE_CAP = 0.35f;
    public static final float STRENGTH_CRIT_DAMAGE_BONUS_PER_LEVEL = 1.0f / 99.0f;
    public static final float STRENGTH_CRIT_BASE_MULTIPLIER = 1.5f;
    public static final float RANGED_CRIT_CHANCE_PER_LEVEL = 0.35f / 99.0f;
    public static final float RANGED_CRIT_CHANCE_CAP = 0.35f;
    public static final float RANGED_CRIT_DAMAGE_BONUS_PER_LEVEL = 1.0f / 99.0f;
    public static final float RANGED_CRIT_BASE_MULTIPLIER = 1.5f;
    public static final float MAGIC_CRIT_CHANCE_PER_LEVEL = 0.35f / 99.0f;
    public static final float MAGIC_CRIT_CHANCE_CAP = 0.35f;
    public static final float MAGIC_CRIT_DAMAGE_BONUS_PER_LEVEL = 1.0f / 99.0f;
    public static final float MAGIC_CRIT_BASE_MULTIPLIER = 1.5f;
    private static final boolean DEBUG_PROJECTILE = false;
    private static final long WEAPON_WARNING_COOLDOWN_MS = 2000;
    private static final Map<UUID, Long> LAST_WEAPON_WARNING = new ConcurrentHashMap<>();

    public SkillCombatBonusSystem() {
        super(Damage.class);
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
                       @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull Damage damage) {
        if (damage.isCancelled()) {
            return;
        }

        applyDefenceBonus(index, archetypeChunk, damage);

        Damage.Source source = damage.getSource();
        if (source instanceof Damage.ProjectileSource projectileSource) {
            applyRangedMagicBonuses(projectileSource, store, damage);
            return;
        }

        if (!(source instanceof Damage.EntitySource entitySource)) {
            return;
        }

        DamageCause cause = damage.getCause();
        if (isProjectileCause(cause)) {
            applyRangedMagicBonusesFromEntity(entitySource, store, damage);
            return;
        }

        Ref<EntityStore> attackerRef = entitySource.getRef();
        if (attackerRef == null || !attackerRef.isValid()) {
            return;
        }

        Player attacker = store.getComponent(attackerRef, Player.getComponentType());
        if (attacker == null) {
            return;
        }

        PlayerRef attackerPlayerRef = attacker.getPlayerRef();
        if (attackerPlayerRef == null) {
            return;
        }

        String weaponId = getHeldItemIdentifier(attacker);
        if (isWeaponRestricted(attackerPlayerRef, weaponId)) {
            damage.getMetaStore().putMetaObject(Damage.BLOCKED, Boolean.TRUE);
            //damage.setAmount(0f);
            damage.setCancelled(true);
            return;
        }

        LevelingService service = Hyrune.getService();
        int attackLevel = service.getSkillLevel(attackerPlayerRef.getUuid(), SkillType.ATTACK);
        if (attackLevel <= 0) {
            return;
        }

        float multiplier = 1.0f + (attackLevel * ATTACK_DAMAGE_PER_LEVEL);
        float before = damage.getAmount();
        float after = before * multiplier;
        damage.setAmount(after);

        applyStrengthCrit(attackerPlayerRef, damage, cause);
    }

    private static void applyDefenceBonus(int index,
                                          ArchetypeChunk<EntityStore> archetypeChunk,
                                          Damage damage) {
        var holder = EntityUtils.toHolder(index, archetypeChunk);
        Player victim = holder.getComponent(Player.getComponentType());
        if (victim == null) {
            return;
        }

        PlayerRef victimRef = victim.getPlayerRef();
        if (victimRef == null) {
            return;
        }

        LevelingService service = Hyrune.getService();
        int defenceLevel = service.getSkillLevel(victimRef.getUuid(), SkillType.DEFENCE);
        if (defenceLevel <= 0) {
            return;
        }

        float reduction = Math.min(DEFENCE_DAMAGE_REDUCTION_CAP,
            defenceLevel * DEFENCE_DAMAGE_REDUCTION_PER_LEVEL);
        float before = damage.getAmount();
        float after = before * (1.0f - reduction);
        damage.setAmount(after);
    }

    private static void applyStrengthCrit(PlayerRef attackerPlayerRef, Damage damage, DamageCause cause) {
        if (attackerPlayerRef == null) {
            return;
        }
        if (isProjectileCause(cause)) {
            return;
        }

        LevelingService service = Hyrune.getService();
        int strengthLevel = service.getSkillLevel(attackerPlayerRef.getUuid(), SkillType.STRENGTH);
        if (strengthLevel <= 0) {
            return;
        }

        float chance = Math.min(STRENGTH_CRIT_CHANCE_CAP,
            strengthLevel * STRENGTH_CRIT_CHANCE_PER_LEVEL);
        if (ThreadLocalRandom.current().nextFloat() >= chance) {
            return;
        }

        float multiplier = STRENGTH_CRIT_BASE_MULTIPLIER
            + (strengthLevel * STRENGTH_CRIT_DAMAGE_BONUS_PER_LEVEL);
        float before = damage.getAmount();
        float after = before * multiplier;
        damage.setAmount(after);
    }

    private static void applyRangedMagicBonuses(Damage.ProjectileSource projectileSource,
                                                Store<EntityStore> store,
                                                Damage damage) {
        Ref<EntityStore> attackerRef = projectileSource.getRef();
        applyRangedMagicBonusesFromRef(attackerRef, store, damage);
    }

    private static void applyRangedMagicBonusesFromEntity(Damage.EntitySource entitySource,
                                                          Store<EntityStore> store,
                                                          Damage damage) {
        Ref<EntityStore> attackerRef = entitySource.getRef();
        applyRangedMagicBonusesFromRef(attackerRef, store, damage);
    }

    private static void applyRangedMagicBonusesFromRef(Ref<EntityStore> attackerRef,
                                                       Store<EntityStore> store,
                                                       Damage damage) {
        if (attackerRef == null || !attackerRef.isValid()) {
            return;
        }

        Player attacker = store.getComponent(attackerRef, Player.getComponentType());
        if (attacker == null) {
            return;
        }

        PlayerRef attackerPlayerRef = attacker.getPlayerRef();
        if (attackerPlayerRef == null) {
            return;
        }

        String weaponId = getHeldItemIdentifier(attacker);
        if (weaponId == null) {
            return;
        }

        if (isWeaponRestricted(attackerPlayerRef, weaponId)) {
            damage.getMetaStore().putMetaObject(Damage.BLOCKED, Boolean.TRUE);
            //damage.setAmount(0f);
            damage.setCancelled(true);
            return;
        }

        boolean ranged = isRangedWeapon(weaponId);
        boolean magic = !ranged && isMagicWeapon(weaponId);
        if (DEBUG_PROJECTILE) {
            LOGGER.at(Level.INFO).log("Projectile bonus check: weaponId=" + weaponId
                + " ranged=" + ranged
                + " magic=" + magic);
        }

        if (ranged) {
            applyRangedDamage(attackerPlayerRef, damage);
            applyRangedCrit(attackerPlayerRef, damage);
            return;
        }

        if (magic) {
            applyMagicDamage(attackerPlayerRef, damage);
            applyMagicCrit(attackerPlayerRef, damage);
        }
    }

    private static void applyRangedDamage(PlayerRef attackerPlayerRef, Damage damage) {
        LevelingService service = Hyrune.getService();
        int level = service.getSkillLevel(attackerPlayerRef.getUuid(), SkillType.RANGED);
        if (level <= 0) {
            return;
        }

        float multiplier = 1.0f + (level * RANGED_DAMAGE_PER_LEVEL);
        damage.setAmount(damage.getAmount() * multiplier);
    }

    private static void applyMagicDamage(PlayerRef attackerPlayerRef, Damage damage) {
        LevelingService service = Hyrune.getService();
        int level = service.getSkillLevel(attackerPlayerRef.getUuid(), SkillType.MAGIC);
        if (level <= 0) {
            return;
        }

        float multiplier = 1.0f + (level * MAGIC_DAMAGE_PER_LEVEL);
        damage.setAmount(damage.getAmount() * multiplier);
    }

    private static void applyRangedCrit(PlayerRef attackerPlayerRef, Damage damage) {
        LevelingService service = Hyrune.getService();
        int level = service.getSkillLevel(attackerPlayerRef.getUuid(), SkillType.RANGED);
        if (level <= 0) {
            return;
        }

        float chance = Math.min(RANGED_CRIT_CHANCE_CAP,
            level * RANGED_CRIT_CHANCE_PER_LEVEL);
        if (ThreadLocalRandom.current().nextFloat() >= chance) {
            return;
        }

        float multiplier = RANGED_CRIT_BASE_MULTIPLIER
            + (level * RANGED_CRIT_DAMAGE_BONUS_PER_LEVEL);
        damage.setAmount(damage.getAmount() * multiplier);
    }

    private static void applyMagicCrit(PlayerRef attackerPlayerRef, Damage damage) {
        LevelingService service = Hyrune.getService();
        int level = service.getSkillLevel(attackerPlayerRef.getUuid(), SkillType.MAGIC);
        if (level <= 0) {
            return;
        }

        float chance = Math.min(MAGIC_CRIT_CHANCE_CAP,
            level * MAGIC_CRIT_CHANCE_PER_LEVEL);
        if (ThreadLocalRandom.current().nextFloat() >= chance) {
            return;
        }

        float multiplier = MAGIC_CRIT_BASE_MULTIPLIER
            + (level * MAGIC_CRIT_DAMAGE_BONUS_PER_LEVEL);
        damage.setAmount(damage.getAmount() * multiplier);
    }

    public static boolean isRangedWeapon(String weaponId) {
        String id = weaponId.toLowerCase();
        return id.contains("shortbow")
            || id.contains("longbow")
            || id.contains("crossbow")
            || id.contains("gun")
            || id.contains("sling");
    }

    public static boolean isMagicWeapon(String weaponId) {
        String id = weaponId.toLowerCase();
        return id.contains("wand")
            || id.contains("staff")
            || id.contains("spellbook")
            || id.contains("scepter")
            || id.contains("grimoire");
    }

    public static String getHeldItemIdentifier(Player player) {
        if (player == null) {
            return null;
        }

        try {
            Inventory inventory = player.getInventory();
            if (inventory != null) {
                byte activeSlot = inventory.getActiveHotbarSlot();
                if (activeSlot >= 0 && activeSlot <= 8) {
                    ItemContainer hotbar = inventory.getHotbar();
                    if (hotbar != null) {
                        ItemStack heldStack = hotbar.getItemStack(activeSlot);
                        if (heldStack != null && heldStack.getItem() != null) {
                            return heldStack.getItem().getId();
                        }
                    }
                }
            }
        } catch (RuntimeException e) {
            return null;
        }

        return null;
    }

    private static boolean isWeaponRestricted(PlayerRef attackerRef, String weaponId) {
        if (attackerRef == null || weaponId == null) {
            return false;
        }

        Integer required = CombatRequirementRegistry.getRequiredLevel(weaponId);
        if (required == null) {
            return false;
        }

        SkillType skill = SkillType.ATTACK;
        if (isMagicWeapon(weaponId)) {
            skill = SkillType.MAGIC;
        } else if (isRangedWeapon(weaponId)) {
            skill = SkillType.RANGED;
        }

        LevelingService service = Hyrune.getService();
        int level = service.getSkillLevel(attackerRef.getUuid(), skill);
        if (level >= required) {
            return false;
        }

        sendWeaponWarning(attackerRef, skill, required);
        return true;
    }

    private static void sendWeaponWarning(PlayerRef playerRef, SkillType skill, int requiredLevel) {
        long now = System.currentTimeMillis();
        Long last = LAST_WEAPON_WARNING.get(playerRef.getUuid());
        if (last != null && now - last < WEAPON_WARNING_COOLDOWN_MS) {
            return;
        }
        LAST_WEAPON_WARNING.put(playerRef.getUuid(), now);

        if (playerRef.getReference() != null && playerRef.getReference().isValid()) {
            Player player = playerRef.getReference().getStore().getComponent(playerRef.getReference(), Player.getComponentType());
            if (player != null) {
                player.sendMessage(Message.raw(
                    "You need " + skill.getDisplayName() + " level " + requiredLevel + " to use this weapon."
                ));
            }
        }
    }

    private static boolean isProjectileCause(DamageCause cause) {
        if (cause == null) {
            return false;
        }
        if (cause == DamageCause.PROJECTILE) {
            return true;
        }
        String id = cause.getId();
        if (id == null) {
            return false;
        }
        String normalized = id.toLowerCase();
        return normalized.contains("projectile") || normalized.contains("arrow");
    }
}

