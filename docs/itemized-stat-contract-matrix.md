# Hyrune Itemized Stat Contract Matrix

This matrix is the runtime contract for every `ItemizedStat`: meaning, cap, formula, and hook location.

| Stat | Meaning | Cap | Runtime Formula | Hook Location |
|---|---|---|---|---|
| `PHYSICAL_DAMAGE` | Held physical damage scaling | `0.20x..4.00x` multiplier | `damage *= (1 + physical_damage * 0.02)` | `SkillCombatBonusSystem.applyItemDamageBonus` |
| `MAGICAL_DAMAGE` | Held magical damage scaling | `0.20x..4.00x` multiplier | `damage *= (1 + magical_damage * 0.02)` | `SkillCombatBonusSystem.applyItemDamageBonus` |
| `PHYSICAL_CRIT_CHANCE` | Physical crit chance contribution | final chance `0..95%` | `effective = max(0, chance - defender_crit_reduction)` | `SkillCombatBonusSystem.applyItemCritBonus` |
| `MAGICAL_CRIT_CHANCE` | Magical crit chance contribution | final chance `0..95%` | `effective = max(0, chance - defender_crit_reduction)` | `SkillCombatBonusSystem.applyItemCritBonus` |
| `CRIT_BONUS` | Crit damage bonus | total crit multiplier `1.00x..4.00x` | `crit_multiplier = clamp(1 + crit_bonus, 1, 4)` | `SkillCombatBonusSystem.applyItemCritBonus` |
| `PHYSICAL_PENETRATION` | Physical armour bypass | floored at `0` | `effective_armor = max(0, armor - penetration)` | `SkillCombatBonusSystem.applyDefenceBonus` |
| `MAGICAL_PENETRATION` | Magical armour bypass | floored at `0` | `effective_armor = max(0, armor - penetration)` | `SkillCombatBonusSystem.applyDefenceBonus` |
| `PHYSICAL_DEFENCE` | Physical mitigation armour value | reduction `0..70%` | `reduction = clamp(physical_defence * 0.004, 0, 0.70)` | `SkillCombatBonusSystem.applyDefenceBonus` |
| `MAGICAL_DEFENCE` | Magical mitigation armour value | reduction `0..70%` | `reduction = clamp(magical_defence * 0.004, 0, 0.70)` | `SkillCombatBonusSystem.applyDefenceBonus` |
| `BLOCK_EFFICIENCY` | Lower stamina drain when block succeeds | drain multiplier `0.05..1.00` | `stamina_drain_multiplier = clamp(1 - block_efficiency, 0.05, 1)` | `SkillCombatBonusSystem.applyDefenceBonus` -> `Damage.STAMINA_DRAIN_MULTIPLIER` |
| `REFLECT_DAMAGE` | Reflect mitigated incoming damage | `0..75%` | `reflect = mitigated_damage * reflect_damage` | `SkillCombatBonusSystem.applyReflectDamage` |
| `CRIT_REDUCTION` | Enemy crit suppression | suppression floored at `0` | `effective_enemy_crit = max(0, enemy_crit - crit_reduction)` | `SkillCombatBonusSystem.applyItemCritBonus`, `NpcCombatScalingSystem.applyOutgoing` |
| `MAX_HP` | Max health bonus | non-negative only | `max_hp += item_max_hp` | `SkillStatBonusApplier.apply` |
| `HP_REGEN` | Flat hp regen per second | non-negative only | `hp_regen += hp_regen_stat` | `SkillRegenSystem.tickPlayerRegen` |
| `HEALING_POWER` | Active-heal amount scaler (abilities/items) | multiplier `1.00x..3.00x` | `healing *= clamp(1 + healing_power, 1, 3)` | `Pending runtime hook (healing item/weapon effects; not passive HP regen)` |
| `HEALING_CRIT_CHANCE` | Active-heal crit chance (abilities/items) | `0..75%` | `if rand < healing_crit_chance` | `Pending runtime hook (healing item/weapon effects; not passive HP regen)` |
| `HEALING_CRIT_BONUS` | Active-heal crit multiplier bonus | crit multiplier `1.50x..4.00x` | `heal_crit_mult = clamp(1.5 + healing_crit_bonus, 1.5, 4)` | `Pending runtime hook (healing item/weapon effects; not passive HP regen)` |
| `MANA_COST_REDUCTION` | Mana spent per magic hit | reduction `0..75%` | `mana_cost = base_magic_cost * (1 - reduction)` | `SkillCombatBonusSystem.consumeMagicCost` |
| `MANA_REGEN` | Flat mana regen per second | non-negative only | `mana_regen += mana_regen_stat` | `SkillRegenSystem.applyManaRegen` |
| `MOVEMENT_SPEED` | Gear move speed bonus | soft `20%`, hard `30%` | `soft_cap(raw_gear_move_speed)` | `SkillStatBonusApplier.applyMovementSpeed` |
| `ATTACK_SPEED` | Physical/ranged cadence | speed bonus `-20%..80%`; interval `120..2500ms` | `interval = base_attack_ms / (1 + attack_speed)` | `SkillCombatBonusSystem.passesCadence` |
| `CAST_SPEED` | Magic cadence | speed bonus `-20%..80%`; interval `120..2500ms` | `interval = base_cast_ms / (1 + cast_speed)` | `SkillCombatBonusSystem.passesCadence` |
| `BLOCK_BREAK_SPEED` | Held-tool gathering speed multiplier | `0..150%` | `gather_damage *= (1 + block_break_speed)` | `MiningSpeedSystem`, `WoodcuttingSpeedSystem` |
| `RARE_DROP_CHANCE` | Held-tool gathering rare-table chance bonus | stat contribution `0..75%`, final roll up to `95%` | `rare_roll_chance = clamp(skill_base_chance + rare_drop_chance, 0, 95%)` | `GatheringUtilityDropService`, `GatheringXpSystem`, `FishingInteraction`, `FarmingHarvestPickupSystem` |
| `DOUBLE_DROP_CHANCE` | Held-tool chance to duplicate gathering outputs | `0..60%` | `if rand < chance then duplicate gathering output` | `GatheringUtilityDropService`, `GatheringXpSystem`, `FishingInteraction`, `FarmingHarvestPickupSystem` |
