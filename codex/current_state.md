# Origins Mod - Current State (2026-02-13)

Status scale:
- Implemented = live and working in active gameplay.
- Partial = core exists but expansion/polish still pending.
- Planned = not started or placeholder only.

## Skill/Systems Progress
1) Attack: Implemented (damage scaling + weapon level gating).
2) Defence: Implemented (damage reduction + armor level gating).
3) Strength: Implemented (crit chance + crit damage scaling).
4) Ranged: Implemented (damage scaling + weapon level gating).
5) Magic: Implemented (damage scaling + mana bonuses + weapon level gating).
6) Architect: Partial (crafting XP keyword mapping; no deeper progression hooks yet).
7) Cooking: Partial (crafting XP + double-proc; additional bonuses pending).
8) Alchemy: Partial (crafting XP + double-proc; additional bonuses pending).
9) Mining: Implemented (XP, ore gating, tool gating, durability savings logic).
10) Woodcutting: Implemented (XP, tree gating, tool gating, durability savings logic with fallback path).
11) Farming: Implemented for current scope (planting level-gated; yield bonus removed by design).
12) Smelting: Partial (crafting-side XP rules exist; processing bench hook still pending).
13) Leatherworking: Partial (crafting-side XP rules exist; processing bench hook still pending).
14) Arcane Engineering: Partial (crafting XP rules; RNG crafting phase pending).
15) Armorsmithing: Partial (crafting XP rules; RNG crafting phase pending).
16) Weaponsmithing: Partial (crafting XP rules; RNG crafting phase pending).
17) Fishing: Implemented base loop (custom catch/XP hooks + level-based fish gating).
18) Slayer: Partial (tasking/persistence/vendor in progress).
19) Constitution: Implemented (max health bonuses).
20) Agility: Implemented (movement speed + stamina regen bonuses).

## Recently Completed (Bugfix/Polish + UI Batch)
- Farming yield bonus behavior removed intentionally; farming level now gates planting content instead of harvest output.
- Farming seed level requirements are config-driven (`farmingSeedLevelRequirements`) and enforced on planting interaction.
- Animal husbandry gating scaffold exists and is config-driven (`enableAnimalHusbandryGating` + `farmingAnimalLevelRequirements`) for future expansion.
- Tool-type enforcement added:
  - Pickaxes blocked from wood targets.
  - Hatchets blocked from ore/stone targets.
- Ore damage gating is active (prevents damaging higher-tier ore when requirements are not met).
- Durability debug logging toggle is config-driven (`durabilityDebugLogging`) and hot-reload aware.
- Woodcutting durability now includes fallback handling for `durabilityUse <= 0` edge case.
- NPC exclusion behavior fixed so excluded NPCs keep their own role/display naming.
- NPC dialogue key resolution normalized (handles spaced/underscored/prefixed variants more robustly).
- NPC nameplate normalization improved globally (underscore/hyphen cleanup + generic token normalization rules).
- NPC name overrides added via config (`npcNameOverrides`) for edge-case names not handled by generic normalization.
- Arcane Engineering icon resolution fixed in XP drops (`Arcane Engineering` display name now maps to `arcane_engineering.png`).
- Character Menu attributes now show fishing bonus visibility:
  - Fishing bite speed bonus.
  - Fishing rare fish chance.
- Quest tab polish pass completed:
  - Group headers by sort mode (A-Z with "The" normalization, Length, Difficulty).
  - Brighter/gold detail title header with underline.
  - Length + Difficulty indicator line in detail panel.
- Skills tab refactor completed:
  - Removed embedded skill detail panel from Character Menu.
  - Skills grid restored as primary focus layout.
  - Skill cell texture system added (normal/hover/pressed).
  - Progress bar width/track alignment corrected for max-level gold bar parity.
  - Footer emphasis pass applied (Combat Level highlighted).
- New dedicated Skill Info flow implemented:
  - `SkillInfoPage` added as separate GUI.
  - Skill cells in Character Menu open Skill Info page.
  - Back path from Skill Info returns to Character Menu.
  - Unlock rows now dim/gray when player level is below unlock requirement.
  - Debug command `/skillinfo` added to open SkillInfoPage directly.

## Config Surface (gameplay_config.json)
- `durabilityDebugLogging`: enables durability debug logs in mining/woodcutting flows.
- `enableAnimalHusbandryGating`: stub toggle for future animal interaction enforcement.
- `farmingSeedLevelRequirements`: per-seed farming level gates (planting).
- `farmingAnimalLevelRequirements`: future-facing animal interaction level gates.
- `npcNameOverrides`: per-NPC display-name overrides for difficult naming edge cases.

## Current Open Action Items
1) Smelting/Leatherworking bench processing hooks (event integration path still needed).
2) NPC nameplate/dialogue persistence hardening pass (verify long-session/restart edge cases).
3) UI polish follow-up:
   - SkillInfoPage visual pass to more closely match RS3 reference.
   - Decide if left SkillInfo icon area stays non-scroll (current stable behavior) or reintroduce scrolling with safe hitboxes.
   - Repair UI polish pass.
4) Future UX enhancement:
   - Add dedicated "Skills Info" button inside Character Menu Skills tab (currently skill-cell click opens SkillInfoPage directly).

## Maintained Files (UI Focus)
- `src/main/resources/Common/UI/Custom/Pages/SkillEntry.ui`
  - Character Menu layout (tabs, quests, attributes, skills, footer).
- `src/main/resources/Common/UI/Custom/Pages/character_stats.ui`
  - Individual skill cell visuals + hover/pressed texture behavior.
- `src/main/java/dev/hytalemodding/origins/ui/CharacterMenu.java`
  - Character Menu runtime binding/data population, skill cell click routing to SkillInfoPage.
- `src/main/resources/Common/UI/Custom/Pages/SkillInfoPage.ui`
  - Dedicated SkillInfo layout (left icon selector, right detail panel, back/close controls).
- `src/main/resources/Common/UI/Custom/Pages/skill_info_icon_cell.ui`
  - Skill icon selector cell template.
- `src/main/resources/Common/UI/Custom/Pages/skill_info_unlock_row.ui`
  - Unlock row template for right panel list.
- `src/main/java/dev/hytalemodding/origins/ui/SkillInfoPage.java`
  - SkillInfo page event handling, dynamic population, unlock dimming logic.
- `src/main/java/dev/hytalemodding/origins/ui/SkillDetailRegistry.java`
  - Content source for skill descriptions/unlock entries.
- `src/main/java/dev/hytalemodding/origins/ui/XPDropOverlay.java`
  - XP icon filename normalization (multi-word skills).
- `src/main/java/dev/hytalemodding/origins/commands/SkillInfoCommand.java`
  - Debug command to open SkillInfoPage directly.
- `src/main/java/dev/hytalemodding/Origins.java`
  - Command registration includes `/skillinfo`.

## Project Structure Notes
- Registries: `src/main/java/dev/hytalemodding/origins/registry`
- Components: `src/main/java/dev/hytalemodding/origins/component`
- Event listeners: `src/main/java/dev/hytalemodding/origins/events`
- Systems: `src/main/java/dev/hytalemodding/origins/system`
- NPC systems: `src/main/java/dev/hytalemodding/origins/npc`
- Runtime config: `src/main/java/dev/hytalemodding/origins/config`
- Runtime data: `origins_data`
