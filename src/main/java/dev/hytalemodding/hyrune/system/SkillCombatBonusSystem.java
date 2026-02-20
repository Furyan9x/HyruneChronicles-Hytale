package dev.hytalemodding.hyrune.system;

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
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.EntityUtils;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.Hyrune;
import dev.hytalemodding.hyrune.itemization.ItemizedStat;
import dev.hytalemodding.hyrune.itemization.PlayerItemizationStats;
import dev.hytalemodding.hyrune.itemization.PlayerItemizationStatsService;
import dev.hytalemodding.hyrune.level.LevelingService;
import dev.hytalemodding.hyrune.registry.CombatRequirementRegistry;
import dev.hytalemodding.hyrune.skills.SkillType;
import dev.hytalemodding.hyrune.util.PlayerEntityAccess;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

import static dev.hytalemodding.Hyrune.LOGGER;

/**
 * ECS system for skill combat bonus.
 */
public class SkillCombatBonusSystem extends EntityEventSystem<EntityStore, Damage> {
    public static final float STRENGTH_DAMAGE_PER_LEVEL = 0.02f;
    public static final float ATTACK_DAMAGE_PER_LEVEL = STRENGTH_DAMAGE_PER_LEVEL;
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
    public static final long BASE_ATTACK_CADENCE_MS = 650L;
    public static final long BASE_CAST_CADENCE_MS = 900L;
    public static final long MIN_CADENCE_MS = 120L;
    public static final long MAX_CADENCE_MS = 2500L;
    public static final float BASE_MAGIC_MANA_COST = 8.0f;
    private static final boolean DEBUG_PROJECTILE = false;
    private static final long WEAPON_WARNING_COOLDOWN_MS = 2000L;
    private static final long OOM_WARNING_COOLDOWN_MS = 1200L;
    private static final Map<UUID, Long> LAST_WEAPON_WARNING = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_MANA_WARNING = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_ATTACK_CADENCE = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_CAST_CADENCE = new ConcurrentHashMap<>();

    public SkillCombatBonusSystem() {
        super(Damage.class);
    }

    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return Set.of(
            new SystemGroupDependency<>(Order.AFTER, DamageModule.get().getGatherDamageGroup()),
            new SystemDependency<>(Order.BEFORE, DamageSystems.DamageStamina.class),
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

        Player victim = archetypeChunk.getComponent(index, Player.getComponentType());
        applyDefenceBonus(index, archetypeChunk, store, commandBuffer, damage);
        if (damage.isCancelled()) {
            return;
        }

        Damage.Source source = damage.getSource();
        if (source instanceof Damage.ProjectileSource projectileSource) {
            applyRangedMagicBonuses(projectileSource, store, damage, victim, false);
            return;
        }

        if (!(source instanceof Damage.EntitySource entitySource)) {
            return;
        }

        DamageCause cause = damage.getCause();
        if (isProjectileCause(cause) || isMagicalCause(cause)) {
            applyRangedMagicBonusesFromEntity(entitySource, store, damage, victim, isMagicalCause(cause));
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

        PlayerRef attackerPlayerRef = PlayerEntityAccess.getPlayerRef(attacker);
        if (attackerPlayerRef == null) {
            return;
        }

        String weaponId = getHeldItemIdentifier(attacker);
        if (isWeaponRestricted(attackerPlayerRef, weaponId)) {
            markBlocked(damage);
            return;
        }

        PlayerItemizationStats itemStats = PlayerItemizationStatsService.getOrRecompute(attacker);
        if (!passesCadence(attackerPlayerRef.getUuid(), itemStats.getItemAttackSpeedBonus(), false)) {
            markBlocked(damage);
            return;
        }

        applyItemDamageBonus(damage, false, itemStats);

        LevelingService service = Hyrune.getService();
        int strengthLevel = service.getSkillLevel(attackerPlayerRef.getUuid(), SkillType.STRENGTH);
        if (strengthLevel > 0) {
            float multiplier = 1.0f + (strengthLevel * STRENGTH_DAMAGE_PER_LEVEL);
            damage.setAmount(damage.getAmount() * multiplier);
        }

        applyItemCritBonus(attacker, victim, damage, false, itemStats);
    }

    private static void applyDefenceBonus(int index,
                                          ArchetypeChunk<EntityStore> archetypeChunk,
                                          Store<EntityStore> store,
                                          CommandBuffer<EntityStore> commandBuffer,
                                          Damage damage) {
        var holder = EntityUtils.toHolder(index, archetypeChunk);
        Player victim = holder.getComponent(Player.getComponentType());
        if (victim == null) {
            return;
        }

        PlayerRef victimRef = PlayerEntityAccess.getPlayerRef(victim);
        if (victimRef == null) {
            return;
        }

        PlayerItemizationStats victimStats = PlayerItemizationStatsService.getOrRecompute(victim);
        LevelingService service = Hyrune.getService();
        int defenceLevel = service.getSkillLevel(victimRef.getUuid(), SkillType.DEFENCE);
        float skillReduction = defenceLevel <= 0
            ? 0.0f
            : Math.min(DEFENCE_DAMAGE_REDUCTION_CAP, defenceLevel * DEFENCE_DAMAGE_REDUCTION_PER_LEVEL);

        boolean magicalIncoming = isMagicalIncomingDamage(damage, store);
        double defenderArmor = PlayerItemizationStatsService.getDefensiveStat(
            victim,
            victimStats,
            magicalIncoming ? ItemizedStat.MAGICAL_DEFENCE : ItemizedStat.PHYSICAL_DEFENCE
        );
        double attackerPenetration = resolveIncomingPenetration(damage, store, magicalIncoming);
        double effectiveArmor = computeEffectiveArmorAfterPenetration(defenderArmor, attackerPenetration);
        float itemReduction = (float) computeDefenceReductionFromArmor(effectiveArmor);
        float reduction = clampf(skillReduction + itemReduction, -0.50f, 0.85f);
        float afterMitigation = damage.getAmount() * (1.0f - reduction);
        damage.setAmount(afterMitigation);

        double blockEfficiency = PlayerItemizationStatsService.getDefensiveStat(victim, victimStats, ItemizedStat.BLOCK_EFFICIENCY);
        damage.putMetaObject(Damage.STAMINA_DRAIN_MULTIPLIER, (float) computeStaminaDrainMultiplier(blockEfficiency));

        double reflectPercent = PlayerItemizationStatsService.getDefensiveStat(victim, victimStats, ItemizedStat.REFLECT_DAMAGE);
        Ref<EntityStore> victimEntityRef = archetypeChunk.getReferenceTo(index);
        applyReflectDamage(victimEntityRef, store, commandBuffer, damage, afterMitigation, reflectPercent);
    }

    private static double resolveIncomingPenetration(Damage damage,
                                                     Store<EntityStore> store,
                                                     boolean magicalIncoming) {
        Ref<EntityStore> attackerRef = resolveAttackerRef(damage == null ? null : damage.getSource());
        if (attackerRef == null || !attackerRef.isValid()) {
            return 0.0;
        }

        Player attacker = store.getComponent(attackerRef, Player.getComponentType());
        if (attacker == null) {
            return 0.0;
        }

        PlayerItemizationStats attackerStats = PlayerItemizationStatsService.getOrRecompute(attacker);
        ItemizedStat penetrationStat = magicalIncoming
            ? ItemizedStat.MAGICAL_PENETRATION
            : ItemizedStat.PHYSICAL_PENETRATION;
        return Math.max(0.0, attackerStats.getHeldResolvedSpecialized().get(penetrationStat));
    }

    private static void applyReflectDamage(Ref<EntityStore> victimRef,
                                           Store<EntityStore> store,
                                           CommandBuffer<EntityStore> commandBuffer,
                                           Damage incomingDamage,
                                           float mitigatedDamageAmount,
                                           double reflectPercent) {
        if (incomingDamage == null || store == null || commandBuffer == null) {
            return;
        }
        if (victimRef == null || !victimRef.isValid()) {
            return;
        }
        if (mitigatedDamageAmount <= 0f || reflectPercent <= 0.0) {
            return;
        }

        Ref<EntityStore> attackerRef = resolveAttackerRef(incomingDamage.getSource());
        if (attackerRef == null || !attackerRef.isValid() || attackerRef.equals(victimRef)) {
            return;
        }

        EntityStatMap attackerStatMap = store.getComponent(attackerRef, EntityStatMap.getComponentType());
        if (attackerStatMap == null) {
            return;
        }
        EntityStatValue health = attackerStatMap.get("Health");
        if (health == null || health.get() <= health.getMin()) {
            return;
        }

        float reflectedAmount = (float) (mitigatedDamageAmount * clamp(reflectPercent, 0.0, 0.75));
        if (reflectedAmount <= 0f) {
            return;
        }

        float after = attackerStatMap.subtractStatValue(health.getIndex(), reflectedAmount);
        if (after <= health.getMin()) {
            Damage reflectedDamage = new Damage(
                new Damage.EntitySource(victimRef),
                incomingDamage.getCause(),
                reflectedAmount
            );
            DeathComponent.tryAddComponent(commandBuffer, attackerRef, reflectedDamage);
        }
    }

    private static void applyRangedMagicBonuses(Damage.ProjectileSource projectileSource,
                                                Store<EntityStore> store,
                                                Damage damage,
                                                Player victim,
                                                boolean forceMagic) {
        Ref<EntityStore> attackerRef = projectileSource.getRef();
        applyRangedMagicBonusesFromRef(attackerRef, store, damage, victim, forceMagic);
    }

    private static void applyRangedMagicBonusesFromEntity(Damage.EntitySource entitySource,
                                                          Store<EntityStore> store,
                                                          Damage damage,
                                                          Player victim,
                                                          boolean forceMagic) {
        Ref<EntityStore> attackerRef = entitySource.getRef();
        applyRangedMagicBonusesFromRef(attackerRef, store, damage, victim, forceMagic);
    }

    private static void applyRangedMagicBonusesFromRef(Ref<EntityStore> attackerRef,
                                                       Store<EntityStore> store,
                                                       Damage damage,
                                                       Player victim,
                                                       boolean forceMagic) {
        if (attackerRef == null || !attackerRef.isValid()) {
            return;
        }

        Player attacker = store.getComponent(attackerRef, Player.getComponentType());
        if (attacker == null) {
            return;
        }

        PlayerRef attackerPlayerRef = PlayerEntityAccess.getPlayerRef(attacker);
        if (attackerPlayerRef == null) {
            return;
        }

        String weaponId = getHeldItemIdentifier(attacker);
        if (!forceMagic && weaponId == null) {
            return;
        }
        if (weaponId != null && isWeaponRestricted(attackerPlayerRef, weaponId)) {
            markBlocked(damage);
            return;
        }

        PlayerItemizationStats itemStats = PlayerItemizationStatsService.getOrRecompute(attacker);
        boolean ranged = !forceMagic && weaponId != null && isRangedWeapon(weaponId);
        boolean magic = forceMagic || (!ranged && weaponId != null && isMagicWeapon(weaponId));
        if (!ranged && !magic) {
            return;
        }

        double speedBonus = magic ? itemStats.getItemCastSpeedBonus() : itemStats.getItemAttackSpeedBonus();
        if (!passesCadence(attackerPlayerRef.getUuid(), speedBonus, magic)) {
            markBlocked(damage);
            return;
        }

        if (magic && !consumeMagicCost(attacker, attackerRef, store, itemStats)) {
            markBlocked(damage);
            return;
        }

        applyItemDamageBonus(damage, magic, itemStats);
        if (DEBUG_PROJECTILE) {
            LOGGER.at(Level.INFO).log("Projectile bonus check: weaponId=" + weaponId
                + " ranged=" + ranged
                + " magic=" + magic);
        }

        if (ranged) {
            applyRangedDamage(attackerPlayerRef, damage);
            applyItemCritBonus(attacker, victim, damage, false, itemStats);
            return;
        }

        applyMagicDamage(attackerPlayerRef, damage);
        applyItemCritBonus(attacker, victim, damage, true, itemStats);
    }

    private static boolean consumeMagicCost(Player attacker,
                                            Ref<EntityStore> attackerRef,
                                            Store<EntityStore> store,
                                            PlayerItemizationStats itemStats) {
        if (attacker == null || attackerRef == null || store == null || itemStats == null) {
            return true;
        }
        EntityStatMap statMap = store.getComponent(attackerRef, EntityStatMap.getComponentType());
        if (statMap == null) {
            return true;
        }
        EntityStatValue mana = statMap.get("Mana");
        if (mana == null) {
            return true;
        }

        float manaCost = (float) computeManaCost(BASE_MAGIC_MANA_COST, itemStats.getItemManaCostReduction());
        if (manaCost <= 0f) {
            return true;
        }
        if (mana.get() + 1e-6f < manaCost) {
            sendOutOfManaWarning(attacker);
            return false;
        }

        statMap.subtractStatValue(mana.getIndex(), manaCost);
        return true;
    }

    private static void sendOutOfManaWarning(Player attacker) {
        UUID uuid = PlayerEntityAccess.getPlayerUuid(attacker);
        if (uuid == null) {
            return;
        }
        long now = System.currentTimeMillis();
        Long previous = LAST_MANA_WARNING.get(uuid);
        if (previous != null && now - previous < OOM_WARNING_COOLDOWN_MS) {
            return;
        }
        LAST_MANA_WARNING.put(uuid, now);
        attacker.sendMessage(Message.raw("Not enough mana."));
    }

    private static boolean passesCadence(UUID attackerUuid, double speedBonus, boolean castCadence) {
        if (attackerUuid == null) {
            return true;
        }
        long base = castCadence ? BASE_CAST_CADENCE_MS : BASE_ATTACK_CADENCE_MS;
        long intervalMs = computeCadenceIntervalMs(base, speedBonus);
        Map<UUID, Long> cadenceMap = castCadence ? LAST_CAST_CADENCE : LAST_ATTACK_CADENCE;
        long now = System.currentTimeMillis();
        Long previous = cadenceMap.get(attackerUuid);
        if (previous != null && now - previous < intervalMs) {
            return false;
        }
        cadenceMap.put(attackerUuid, now);
        return true;
    }

    public static long computeCadenceIntervalMs(double baseIntervalMs, double speedBonus) {
        double effectiveSpeed = clamp(speedBonus, -0.20, 0.80);
        double divisor = Math.max(0.10, 1.0 + effectiveSpeed);
        double raw = baseIntervalMs / divisor;
        return (long) clamp(raw, MIN_CADENCE_MS, MAX_CADENCE_MS);
    }

    public static double computeManaCost(double baseManaCost, double manaCostReduction) {
        double reduction = clamp(manaCostReduction, 0.0, 0.75);
        return Math.max(0.0, baseManaCost * (1.0 - reduction));
    }

    public static double computeDefenceReductionFromArmor(double armorValue) {
        double armor = Math.max(0.0, armorValue);
        return clamp(armor * 0.004, 0.0, 0.70);
    }

    public static double computeEffectiveArmorAfterPenetration(double armorValue, double penetrationValue) {
        return Math.max(0.0, armorValue - Math.max(0.0, penetrationValue));
    }

    public static double suppressCritChance(double critChance, double critReduction) {
        return clamp(critChance - Math.max(0.0, critReduction), 0.0, 0.95);
    }

    public static double computeStaminaDrainMultiplier(double blockEfficiency) {
        return clamp(1.0 - Math.max(0.0, blockEfficiency), 0.05, 1.0);
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

    private static void applyItemDamageBonus(Damage damage, boolean magical, PlayerItemizationStats stats) {
        if (damage == null || stats == null) {
            return;
        }
        float itemMultiplier = (float) (magical
            ? stats.getMagicalDamageMultiplier()
            : stats.getPhysicalDamageMultiplier());
        if (itemMultiplier <= 0f) {
            return;
        }
        damage.setAmount(damage.getAmount() * itemMultiplier);
    }

    private static void applyItemCritBonus(Player attacker,
                                           Player victim,
                                           Damage damage,
                                           boolean magical,
                                           PlayerItemizationStats attackerStats) {
        if (attacker == null || damage == null || attackerStats == null) {
            return;
        }
        double chance = magical ? attackerStats.getMagicalCritChanceBonus() : attackerStats.getPhysicalCritChanceBonus();
        if (victim != null) {
            PlayerItemizationStats victimStats = PlayerItemizationStatsService.getOrRecompute(victim);
            double suppression = PlayerItemizationStatsService.getDefensiveStat(victim, victimStats, ItemizedStat.CRIT_REDUCTION);
            chance = suppressCritChance(chance, suppression);
        } else {
            chance = clamp(chance, 0.0, 0.95);
        }
        if (chance <= 0.0 || ThreadLocalRandom.current().nextDouble() >= chance) {
            return;
        }
        float multiplier = (float) attackerStats.getCritBonusMultiplier();
        if (multiplier > 1.0f) {
            damage.setAmount(damage.getAmount() * multiplier);
        }
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

    private static boolean isMagicalCause(DamageCause cause) {
        if (cause == null) {
            return false;
        }
        String id = cause.getId();
        if (id == null) {
            return false;
        }
        String normalized = id.toLowerCase();
        return normalized.contains("magic") || normalized.contains("spell");
    }

    private static boolean isMagicalIncomingDamage(Damage damage, Store<EntityStore> store) {
        if (damage == null) {
            return false;
        }
        if (isMagicalCause(damage.getCause())) {
            return true;
        }

        Ref<EntityStore> attackerRef = resolveAttackerRef(damage.getSource());
        if (attackerRef == null || !attackerRef.isValid()) {
            return false;
        }
        Player attacker = store.getComponent(attackerRef, Player.getComponentType());
        if (attacker == null) {
            return false;
        }
        String weaponId = getHeldItemIdentifier(attacker);
        return weaponId != null && isMagicWeapon(weaponId);
    }

    private static Ref<EntityStore> resolveAttackerRef(Damage.Source source) {
        if (source instanceof Damage.EntitySource entitySource) {
            return entitySource.getRef();
        }
        if (source instanceof Damage.ProjectileSource projectileSource) {
            return projectileSource.getRef();
        }
        return null;
    }

    private static void markBlocked(Damage damage) {
        if (damage == null) {
            return;
        }
        damage.putMetaObject(Damage.BLOCKED, Boolean.TRUE);
        damage.setCancelled(true);
    }

    private static float clampf(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
