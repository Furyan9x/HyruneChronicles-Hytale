# Origins Mod - Feature Status Report

## Summary

| Status | Count | Description |
|--------|-------|-------------|
| ✅ Functional | 35 | Working as intended, may need minor tuning |
| ⚠️ Needs Debug | 4 | Logic exists but requires testing/verification |
| ❌ Not Functional | 5 | Broken or not implemented |
| 🔄 Needs Iteration | 11 | Working but requires improvement/expansion |

**Total Items Tracked:** 55

---

## ✅ Functional

### Combat Skills
- **Attack XP gain** - XP granted based on mob max HP
- **Attack damage scaling** - Working (tuning applied: 0.01f → 0.02f per level)
- **Weapon level gating** - Weapons do no damage if player doesn't meet level requirement
- **Defence formula** - Damage reduction calculation working
- **Armor level gating** - Cannot equip armor without required level
- **Strength** - Increases crit chance and crit damage
- **Strength formula** - Crit scaling working correctly
- **Ranged XP** - XP awarded on ranged attacks
- **Ranged formula** - Working (tuning applied: 0.01 → 0.02 per level)
- **Ranged level gating** - Level requirements enforced
- **Magic XP** - XP awarded on magic attacks
- **Magic gating** - Level requirements enforced
- **Magic formula** - Working (tuning applied: 0.01 → 0.03 per level)
- **Magic bonuses** - Mana bonus and mana regen working

### Gathering Skills
- **Mining XP** - XP awarded on mining
- **Woodcutting XP** - XP awarded on tree chopping
- **Mining speed** - Higher mining level = more damage to blocks (tuning applied: 0.01 → 0.02 per level)
- **Woodcutting speed** - Higher woodcutting level = more damage to blocks (tuning applied: 0.01 → 0.02 per level)
- **Mining/Woodcutting tool level gating** - Level requirements enforced
- **Farming XP** - Pick up/break/harvest with sickle all award XP
- **Farming sickle bonus** - Sickle grants +25% bonus XP

### Crafting Skills
- **Architect XP** - XP awarded on relevant crafts
- **Alchemy XP** - XP awarded on alchemy crafts
- **Cooking XP** - XP awarded on cooking crafts
- **Alchemy double proc** - Random chance to double output based on level
- **Cooking double proc** - Random chance to double output based on level
- **Leatherworking XP** - XP granted for crafting items with leather material
- **Recipe exclusions** - Excluded items are not craftable

### Special Skills
- **Fishing XP** - XP awarded on catches
- **Fishing level gating** - Fish gated by fishing level
- **Constitution** - Max HP increases with level
- **Agility regen** - Stamina regen increases with level
- **Agility movement speed** - Movement speed increases with level

### UI & Systems
- **Data persistence** - All skills and systems persist correctly across sessions
- **Repair bench** - Fully functional (UI needs polish)
- **Hans game mode select** - Working, starter kit provided
- **Slayer vendor** - Functional (needs improvement and iteration)

---

## ⚠️ Needs Debug

### Gathering Skills
- **@INVESTIGATE Mining/Woodcutting durability reduction** - Logic exists but needs debug testing to verify it's working
    - *Note: Should reduce durability consumption based on level*

- **@INVESTIGATE Farming yield bonus** - Set to 200%, then 300%, but no increase in drops observed
    - *Note: Should increase items per harvest based on farming level*

### Mining Edge Case
- **@BUG Mining ore above level with valid tool** - When mining ore player can't mine due to level, but using a tool they CAN use, player can damage block to last hit but not break it
    - *Action Required: Prevent damage entirely if ore level requirement not met*

### NPC Nameplate System
- **@INVESTIGATE NPC exclusion from nameplate system** - Spawned NPCs show correct display name without level, but on server restart name changes to underscored version
    - *Example: "Slayer Master" becomes "Slayer_Master" after restart*
    - *Dialogue also breaks after restart*
    - *Goal: NPCs should maintain Display Name from JSON and not use player nameplate system at all*

---

## ❌ Not Functional

### Combat Skills
- **@TODO Defence XP** - No logic implemented for Defence XP gain
    - *Action Required: Implement XP gain on taking damage*

### Crafting Skills
- **@TODO Smelting XP** - Cannot figure out how to tap into processing bench
    - *Action Required: Hook into furnace/smelting bench events*

- **@TODO Leatherworking processing** - Tanner processing bench not functional (same issue as smelting)
    - *Action Required: Hook into tanner bench processing events*

### UI & Systems
- **@TODO Trade Pack visual logic** - Either remove visual logic or fix naked character bug
    - *Issue: Applying trade pack visual makes character appear naked*
    - *Note: Wasted significant AI credits trying to fix, may need to scrap feature*

### Special Skills
- **@TODO Slayer task ID matching** - Tasks cannot be completed due to ID mismatch
    - *Example: Moose task expects ID "Moose" but only "Moose_Bull" and "Moose_Cow" exist*
    - *Action Required: Implement fuzzy matching or parent ID system*

---

## 🔄 Needs Iteration

### Combat Skills
- **@ITERATE Attack damage scaling tuning** - 1-99 attack is almost unnoticeable with crude sword on rats
    - *Status: Increased from 0.01f to 0.02f, needs further testing*

- **@ITERATE XP gain system** - Switch from weapon-type-based to selectable "combat style"
    - *Current: Weapon type determines XP*
    - *Desired: Melee weapons → Attack/Strength/Defence/Shared; Ranged weapons → Ranged/Defence/Shared*

- **@BUG Signature energy gain** - Needs to be cancelled/nullified when attacking with weapon player doesn't have level to use
    - *Current: Signature energy still gained even when weapon deals 0 damage*

### Gathering Skills
- **@BUG Tool-block type enforcement** - Need to prevent hatchets from breaking stone and pickaxes from breaking wood
    - *Action Required: Add tool-type validation to block break events*

### Crafting Skills
- **@ITERATE Recipe normalization** - Recipes generally normalized but need another pass
    - *Action Required: Audit all recipes for consistency*

### Special Skills
- **@ITERATE Fishing bonuses** - Needs more rod types and visible bonuses
    - *Missing: Faster bite rate, higher rare catch chance at higher levels*
    - *Note: Bonuses may exist but not populated in Attributes page*

### UI & Systems
- **@ITERATE Character Menu - Skills tab** - Very nice but needs improvement
    - *TODO: Go through each skill, fix unlocks*
    - *TODO: Make Skill Details page neater*
    - *TODO: Add "Current XP / XP to next level" on skill cell*
    - *TODO: Add bottom borders to total level, combat level, quest points when skill selected*

- **@ITERATE Server info page** - Figure out what to do with this page
    - *Status: Page exists but purpose unclear*

- **@ITERATE Quest page** - Coming along nicely but has persistence issues
    - *BUG: Filter and checkboxes don't save through GUI open/close or logout/login*

- **@ITERATE Trade Pack crafting** - Multiple improvements needed
    - *TODO: Add crafting timer*
    - *TODO: Create more trade pack types*
    - *See "Not Functional" section for visual logic issue*

- **@ITERATE Hans game mode systems** - Need to expand game mode logic
    - *TODO: Implement Iron Man mode restrictions*
    - *TODO: Implement Hardcore mode (permadeath?)*

- **@ITERATE Repair kits** - Functional but needs max durability reduction implementation
    - *TODO: Crude repair kit should reduce max durability by 10%*
    - *TODO: Iron repair kit should reduce max durability by 5%*

### UI Visual Issues
- **@BUG Arcane Engineering icon** - Icon not showing in XpDropOverlay
    - *Note: All other skill icons display correctly*

---

## Notes for Agent

### High Priority Action Items
1. **@TODO** Implement Defence XP logic (combat loop incomplete without this)
2. **@BUG** Fix Signature energy gain when using invalid weapons (exploitable)
3. **@TODO** Fix Slayer task ID matching (breaks core Slayer gameplay)
4. **@INVESTIGATE** Debug farming yield bonus (confirmed not working despite high multiplier)
5. **@ITERATE** Implement combat style selection system (fundamental XP distribution change)

### Medium Priority Action Items
1. **@TODO** Hook into processing benches (Smelting, Leatherworking)
2. **@BUG** Fix Quest page filter/checkbox persistence
3. **@ITERATE** Add repair kit max durability reduction
4. **@BUG** Fix NPC nameplate/dialogue issue after server restart
5. **@BUG** Prevent wrong tool types from breaking blocks (hatchet/pickaxe)

### Low Priority Polish Items
1. **@ITERATE** Character Menu UI improvements
2. **@ITERATE** Add fishing bonuses to Attributes page
3. **@ITERATE** Recipe normalization pass
4. **@ITERATE** Repair bench UI polish
5. **@BUG** Fix Arcane Engineering icon in XP overlay

### Consider Scrapping
- Trade Pack visual system (naked character bug unsolvable after extensive debugging)
