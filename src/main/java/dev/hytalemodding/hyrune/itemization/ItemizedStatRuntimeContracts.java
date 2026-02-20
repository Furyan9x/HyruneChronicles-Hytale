package dev.hytalemodding.hyrune.itemization;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Runtime contract metadata for specialized itemized stats.
 */
public final class ItemizedStatRuntimeContracts {
    public record Contract(String meaning, String cap, String formula, String hookLocation) {
    }

    private static final EnumMap<ItemizedStat, Contract> CONTRACTS = new EnumMap<>(ItemizedStat.class);

    static {
        CONTRACTS.put(ItemizedStat.PHYSICAL_DAMAGE, contract(
            "Flat melee/ranged damage scaling from held weapon stat.",
            "Multiplier clamped to [0.20x, 4.00x].",
            "damage *= (1 + physical_damage * 0.02)",
            "SkillCombatBonusSystem.applyItemDamageBonus"
        ));
        CONTRACTS.put(ItemizedStat.MAGICAL_DAMAGE, contract(
            "Flat magic damage scaling from held weapon stat.",
            "Multiplier clamped to [0.20x, 4.00x].",
            "damage *= (1 + magical_damage * 0.02)",
            "SkillCombatBonusSystem.applyItemDamageBonus"
        ));
        CONTRACTS.put(ItemizedStat.PHYSICAL_CRIT_CHANCE, contract(
            "Physical crit chance contribution before defender suppression.",
            "Chance clamped to [0%, 95%] after suppression.",
            "effective_crit = max(0, crit_chance - defender_crit_reduction)",
            "SkillCombatBonusSystem.applyItemCritBonus"
        ));
        CONTRACTS.put(ItemizedStat.MAGICAL_CRIT_CHANCE, contract(
            "Magical crit chance contribution before defender suppression.",
            "Chance clamped to [0%, 95%] after suppression.",
            "effective_crit = max(0, crit_chance - defender_crit_reduction)",
            "SkillCombatBonusSystem.applyItemCritBonus"
        ));
        CONTRACTS.put(ItemizedStat.CRIT_BONUS, contract(
            "Critical hit damage multiplier bonus.",
            "Total multiplier clamped to [1.00x, 4.00x].",
            "crit_multiplier = clamp(1 + crit_bonus, 1, 4)",
            "SkillCombatBonusSystem.applyItemCritBonus"
        ));
        CONTRACTS.put(ItemizedStat.PHYSICAL_PENETRATION, contract(
            "Bypasses physical armour on hit.",
            "Penetration is floored at 0; effective armour floored at 0.",
            "effective_armor = max(0, armor - physical_penetration)",
            "SkillCombatBonusSystem.applyDefenceBonus"
        ));
        CONTRACTS.put(ItemizedStat.MAGICAL_PENETRATION, contract(
            "Bypasses magical armour on hit.",
            "Penetration is floored at 0; effective armour floored at 0.",
            "effective_armor = max(0, armor - magical_penetration)",
            "SkillCombatBonusSystem.applyDefenceBonus"
        ));

        CONTRACTS.put(ItemizedStat.PHYSICAL_DEFENCE, contract(
            "Incoming physical mitigation from defensive armour pool.",
            "Reduction clamped to [0%, 70%] before skill reduction.",
            "reduction = clamp(physical_defence * 0.004, 0, 0.70)",
            "SkillCombatBonusSystem.applyDefenceBonus"
        ));
        CONTRACTS.put(ItemizedStat.MAGICAL_DEFENCE, contract(
            "Incoming magical mitigation from defensive armour pool.",
            "Reduction clamped to [0%, 70%] before skill reduction.",
            "reduction = clamp(magical_defence * 0.004, 0, 0.70)",
            "SkillCombatBonusSystem.applyDefenceBonus"
        ));
        CONTRACTS.put(ItemizedStat.BLOCK_EFFICIENCY, contract(
            "Reduces stamina drained on successful blocks.",
            "Stamina drain multiplier clamped to [0.05, 1.00].",
            "stamina_drain_multiplier = clamp(1 - block_efficiency, 0.05, 1)",
            "SkillCombatBonusSystem.applyDefenceBonus -> Damage.STAMINA_DRAIN_MULTIPLIER"
        ));
        CONTRACTS.put(ItemizedStat.REFLECT_DAMAGE, contract(
            "Reflects a percentage of mitigated incoming damage.",
            "Reflect percent clamped to [0%, 75%].",
            "reflect_damage = mitigated_incoming_damage * reflect_damage_percent",
            "SkillCombatBonusSystem.applyReflectDamage"
        ));
        CONTRACTS.put(ItemizedStat.CRIT_REDUCTION, contract(
            "Suppresses incoming enemy crit chance.",
            "Suppression floored at 0; effective crit chance capped at 95%.",
            "effective_enemy_crit = max(0, enemy_crit - crit_reduction)",
            "SkillCombatBonusSystem.applyItemCritBonus; NpcCombatScalingSystem.applyOutgoing"
        ));
        CONTRACTS.put(ItemizedStat.MAX_HP, contract(
            "Adds to max health as a persistent stat modifier.",
            "Only non-negative bonus is applied.",
            "health_max += max(0, item_max_hp)",
            "SkillStatBonusApplier.apply"
        ));
        CONTRACTS.put(ItemizedStat.HP_REGEN, contract(
            "Adds flat HP regeneration per second.",
            "Non-negative only; still capped by regen config per-second caps.",
            "hp_regen_per_sec += hp_regen",
            "SkillRegenSystem.tickPlayerRegen"
        ));

        CONTRACTS.put(ItemizedStat.HEALING_POWER, contract(
            "Scales outgoing active healing effects from abilities/items.",
            "Healing power multiplier clamped to [1.00x, 3.00x].",
            "healing *= clamp(1 + healing_power, 1, 3)",
            "Pending runtime hook (healing item/weapon effects; not passive HP regen)"
        ));
        CONTRACTS.put(ItemizedStat.HEALING_CRIT_CHANCE, contract(
            "Chance for active healing crits from abilities/items.",
            "Chance clamped to [0%, 75%].",
            "if rand < healing_crit_chance then apply healing crit multiplier",
            "Pending runtime hook (healing item/weapon effects; not passive HP regen)"
        ));
        CONTRACTS.put(ItemizedStat.HEALING_CRIT_BONUS, contract(
            "Bonus to active healing crit multiplier.",
            "Healing crit multiplier clamped to [1.50x, 4.00x].",
            "healing_crit_multiplier = clamp(1.5 + healing_crit_bonus, 1.5, 4)",
            "Pending runtime hook (healing item/weapon effects; not passive HP regen)"
        ));
        CONTRACTS.put(ItemizedStat.MANA_COST_REDUCTION, contract(
            "Reduces mana spent for magic attack cadence events.",
            "Reduction clamped to [0%, 75%].",
            "mana_cost = base_magic_cost * (1 - mana_cost_reduction)",
            "SkillCombatBonusSystem.consumeMagicCost"
        ));

        CONTRACTS.put(ItemizedStat.MANA_REGEN, contract(
            "Adds flat mana regen per second.",
            "Non-negative only.",
            "mana_regen_per_sec += mana_regen",
            "SkillRegenSystem.applyManaRegen"
        ));
        CONTRACTS.put(ItemizedStat.MOVEMENT_SPEED, contract(
            "Adds movement speed bonus with soft-cap behavior.",
            "Soft cap 20%, overflow scaled 25%, hard cap 30%.",
            "movement_speed = agility_bonus + soft_cap(item_move_speed)",
            "SkillStatBonusApplier.applyMovementSpeed"
        ));
        CONTRACTS.put(ItemizedStat.ATTACK_SPEED, contract(
            "Reduces physical/ranged cadence interval.",
            "Speed bonus clamped to [-20%, 80%]; interval clamped to [120ms, 2500ms].",
            "attack_interval = base_attack_interval / (1 + attack_speed)",
            "SkillCombatBonusSystem.passesCadence"
        ));
        CONTRACTS.put(ItemizedStat.CAST_SPEED, contract(
            "Reduces magical cast cadence interval.",
            "Speed bonus clamped to [-20%, 80%]; interval clamped to [120ms, 2500ms].",
            "cast_interval = base_cast_interval / (1 + cast_speed)",
            "SkillCombatBonusSystem.passesCadence"
        ));
        CONTRACTS.put(ItemizedStat.BLOCK_BREAK_SPEED, contract(
            "Held-tool multiplicative gathering speed bonus for mining/woodcutting.",
            "Bonus clamped to [0%, 150%].",
            "gather_damage *= (1 + block_break_speed)",
            "MiningSpeedSystem.handle; WoodcuttingSpeedSystem.handle"
        ));
        CONTRACTS.put(ItemizedStat.RARE_DROP_CHANCE, contract(
            "Held-tool chance bonus for gathering rare table rolls.",
            "Chance contribution clamped by stat cache to [0%, 75%].",
            "rare_roll_chance = clamp(skill_base_chance + rare_drop_chance, 0, 95%)",
            "GatheringUtilityDropService.resolveRareDrops; GatheringXpSystem; FishingInteraction; FarmingHarvestPickupSystem"
        ));
        CONTRACTS.put(ItemizedStat.DOUBLE_DROP_CHANCE, contract(
            "Held-tool chance to duplicate gathering outputs.",
            "Chance clamped to [0%, 60%].",
            "if rand < double_drop_chance then duplicate gathering output",
            "GatheringUtilityDropService.shouldDoubleDrop; GatheringXpSystem; FishingInteraction; FarmingHarvestPickupSystem"
        ));
    }

    private ItemizedStatRuntimeContracts() {
    }

    public static Contract get(ItemizedStat stat) {
        return stat == null ? null : CONTRACTS.get(stat);
    }

    public static Map<ItemizedStat, Contract> all() {
        return Collections.unmodifiableMap(CONTRACTS);
    }

    private static Contract contract(String meaning, String cap, String formula, String hookLocation) {
        return new Contract(meaning, cap, formula, hookLocation);
    }
}
