### Phase 1: Core Systems Architecture (Foundation)
**1.1 Custom Stat System Design**

Data Structure: Create a CustomItemStats class that wraps Hytale's StatModifiers

Properties: itemId, rarity, statPool[], rolledValues{}
This allows you to layer your system on top of base stats without fighting the engine


Stat Pool Database: JSON configuration files defining stat pools per item category


EX: {
"item_category": "sword",
"stat_pool": [
{"stat": "attack_damage", "common_range": [10,15], "legendary_range": [25,40]},
{"stat": "crit_chance", "common_range": [2,5], "legendary_range": [15,25]}
]
}

- **Consideration**: Pre-calculate stat ranges for each rarity tier (5 tiers × stat ranges) to avoid runtime math

**1.2 Rarity System**
- **Rarity Enum**: Common (1 stat), Uncommon (2), Rare (3), Epic (4), Legendary (5)
- **Stat Magnitude Formula**: `base_value * (1 + (rarity_tier * 0.15))`
    - Common (tier 0) = base
    - Uncommon (tier 1) = base × 1.15
    - Rare (tier 2) = base × 1.30
    - Epic (tier 3) = base × 1.45
    - Legendary (tier 4) = base × 1.60
- **Rarity Roll Weights**: Define base probabilities (e.g., 60% Common, 20% Uncommon, 18% Rare, 1.5% Epic, 0.5% Legendary)

**1.3 Influence System (Quality Modifiers)**
- **Modifier Types**:
    - Crafting Bench Tier influences quality outcome, or requires less materials
    - Player Profession Level (Arcane Engineer bonus for magic items, weaponsmithing bonus for melee weapons, armorsmithing bonus to metal armors, etc)
    - Consumable Buffs (food/potions)
- **Implementation**: Additive probability shifts
    - Example: Tier 3 bench adds +10% to Rare/Epic/Legendary rolls, redistributes from Common
    - Profession mastery adds +5% per 10 levels to relevant item types
- **Technical Note**: Create a `CraftingContext` object passed to the craft function containing all active modifiers

### Phase 2: Item Override System

**2.1 Base Game Item Interception**
- **Investigation Needed**:
    - Does Hytale allow recipe override/replacement via modding API?
    - Can you intercept crafting events before item creation?
    - Fallback: Replace all base recipes with custom versions
- **Item Registry Override**:
    - Create modified versions of all base items with `rarity: common` and baseline stats
    - Store in "resources/server/item/items/"

**2.2 Dynamic Stat Application**
- **Crafting Event Hook**: Intercept `onItemCrafted` event
    1. Determine base item type
    2. Roll rarity (modified by CraftingContext)
    3. Select N stats from stat pool (where N = rarity tier stat count)
    4. Roll stat values within rarity-appropriate ranges
    5. Apply stats to item instance using Hytale's StatModifiers system
    6. Update item tooltip/display name with rarity color coding
- **Performance Consideration**: Cache stat pool lookups per item category

**2.3 Item Identification System**
- **Unique Item IDs**: Each crafted item needs persistent unique identifier
    - Format: `{base_item_id}_{rarity}_{stat_hash}_{timestamp}`
    - Required for salvaging/trading systems to track individual items
- **Tooltip Generation**: Dynamic tooltip builder showing rarity + rolled stats

---

### Phase 3: Salvaging & Disposal Systems

**3.1 Enhanced Salvage System**
- **Salvage Return Formula**:
```
  base_materials * salvage_bench_multiplier * invention_skill_multiplier
```
- NOT affected by item rarity (prevents exploit loops)
- Higher tier benches return more of materials(capped maybe?)

- Invention skill needs to be designed and implemented at a later date.


**3.2 High Alchemy System** ***CAN NOT BE IMPLEMENTED UNTIL WE HAVE CURRENCY/ECONOMY***
- **Gold Conversion Formula**:

  base_item_value * rarity_multiplier * alchemy_level_bonus(Low vs High Alchemy)

Rarity multipliers: Common 1x, Uncommon 1.5x, Rare 2.5x, Epic 4x, Legendary 7x
Encourages alching/salvaging high-rarity duds instead of vendoring
Magic XP Reward: Grants magic skill XP, creates gold sink/faucet balance

**3.3 Mass Processing QoL**
Investigate:
Batch Salvage/Alchemy: Allow processing multiple items at once
Filters: "Salvage all Common quality" or "Alch all items below Rare"


### Phase 4: Stat Modification System (Gem System)
**4.1 Sockets and Socketing Gems**
Investigate possibility of adding "sockets" to items. 
Gem Types: Offensive/Defensive/utility(non-combat) gems corresponding to stat categories
plan gem types and bonuses associated with them. 


Application Mechanic:

Consumes gem, applies to a socket on an item. 
Items stats are updated to include the gems additions. 
Maintains rarity tier stat magnitude rules


Limitation: 1 gem socket on common items, 1 on uncommon, 2 on rare, 3 on epic, 4 on legendary

**4.2 Gem Crafting/Acquisition**

Gems as rare drops from specific content (bosses, dungeons, events)
Possible gem crafting from rare essences
Creates endgame material chase beyond basic resources


### Phase 5: Profession Integration
**5.1 Profession-Specific Bonuses**

Arcane Engineer Example:

+5% Rare+ rarity chance on magic items (wands/staves/robes)
+10% stat magnitude on magic items
Unlocks unique catalyst/gem recipes


System Design: Profession bonus lookup table applied in CraftingContext

5.2 Crafting Bench Tiers

Progression Path:

Tier 1 Bench (no bonuses) → Tier 2 → Tier 3 → Tier 4 → Tier 5
Each tier: +8% chance to shift rarity upward, +5% stat roll improvement


Bench Crafting Requirements: Exponential resource costs, profession unlocks




Technical Considerations & Risks
⚠️ Critical Investigations:

Stat Modifier Limits: Does Hytale have a cap on StatModifiers per item? Test with 5+ modifiers
Save Data Bloat: Unique item instances with custom stats = large save files

Mitigation: Compress stat data, implement periodic cleanup of vendored items


Network Sync: In multiplayer, how do custom item stats sync between clients?

May need server-authoritative stat generation with client-side caching


Item Comparison UI: Base game item tooltips may not support stat comparison

May need custom UI overlay for comparing equipped vs crafted items



🔧 Performance Optimizations:

Stat Pool Caching: Load all stat pools into memory at game start (small footprint)
Lazy Stat Generation: Only roll stats when item is actually crafted, not on recipe preview
Batched Salvage: Process multiple salvages in single operation to reduce UI lag

🎮 Gameplay Balancing Hooks:

Configurability: All formulas/weights should be in external JSON configs for easy tuning
Analytics Tracking: Log rarity distribution, stat rolls, salvage rates for balancing
Emergency Nerf Valve: Admin command to reroll all existing items if balance breaks


Recommended Development Order

Week 1-2: Core stat system + rarity rolling (can test in isolation)
Week 3-4: Item override system + crafting event hooks
Week 5: Crafting influence modifiers (benches/professions/buffs)
Week 6: Salvage system enhancement
Week 7: High alchemy implementation
Week 8: Gem stat modification system
Week 9-10: Balance testing, UI polish, multiplayer testing