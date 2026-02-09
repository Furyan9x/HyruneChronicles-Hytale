# Origins Mod - Current Skill Progress

Status scale:
- Implemented = core gameplay loop and bonuses are live.
- Partial = XP/progression exists, but bonuses or extra mechanics are still missing.
- Planned = placeholder only or not started yet.

## Skill Order Progress (per codex/skill-order.md)
1) Attack: Implemented (damage bonus per level). Weapon level requirements enforced (invalid weapons deal 0 damage with warning).
2) Defence: Implemented (damage reduction per level with cap). Armor level requirements are enforced (invalid armor equip is denied).
3) Strength: Implemented (crit chance + crit damage scaling).
4) Ranged: Implemented (damage + crit scaling for ranged weapons). Weapon level requirements enforced (invalid weapons deal 0 damage with warning).
5) Magic: Implemented (damage + crit scaling for magic weapons; mana bonuses/regeneration). Weapon level requirements enforced (invalid weapons deal 0 damage with warning).
6) Architect: Partial (crafting XP rules via keyword matching; no extra output/refund bonuses yet). No level requirements for architect tools or benches yet. Keyword matching now includes tools/benches/traps for XP.
7) Cooking: Partial (crafting XP + double-proc chance; no buff duration scaling yet). No level requirements for cooking recipes yet.
8) Alchemy: Partial (crafting XP + double-proc chance; no buff duration scaling yet). No level requirements for alchemy recipes yet. Keyword matching now includes bandages for XP.
9) Mining: Implemented (XP, tool level gating, speed bonus, durability savings). 
10) Woodcutting: Implemented (XP, tool level gating, speed bonus, durability savings).
11) Farming: Partial (XP on mature harvest + item pickup, sickle bonus, yield bonus; planting/growth gating not yet).
12) Smelting: Partial (crafting XP rules; no advanced processing pipeline yet).
13) Leatherworking: Partial (crafting XP rules; no advanced processing pipeline yet).
14) Arcane Engineering: Partial (crafting XP rules; RNG crafting system not yet).
15) Armorsmithing: Partial (crafting XP rules; RNG crafting system not yet).
16) Weaponsmithing: Partial (crafting XP rules; RNG crafting system not yet).
17) Fishing: Implemented (custom bobber/cast/idle systems + XP hooks, fish gated by level). No level requirements for rods yet, no higher tier rods. 
18) Slayer: Partial (tiered tasks, kill tracking and XP awarded per kill based on maxHP, persistence, turn-in rewards, vendor UI second pass done, buy tab working (placeholder items) learn and task tabs empty for now).
19) Constitution: Implemented (max health bonuses).
20) Agility: Implemented (stamina regen + movement speed bonuses).


## Extra Systems
- Character UI: Implemented (skills menu with tabs + attributes tab with collapsible categories + skill detail panel).
- Nameplates: Implemented (level/class display). *Need to investigate how to completely exclude NPCs from this, so that they retain their "Display Name" property from json. 
- Data persistence: Implemented (JSON repositories in ./origins_data).
- Crafting overrides: base Hytale assets audited with a generator to add/fix recipes, normalize recipe arrays, and enforce bench categories while excluding non-craftable items.
- Basic trade pack crafting implemented. Craft Trade packs at a trade pack bench, they slow you down and display a model on the players back when in inventory (unintended functionality: makes the character naked. Seek fix.)
- Repair bench added for safer repairing but not easy on the go. Repair kits for quick repair - lose max durability. Repair bench for repairing with base materials - maintain max durability.
- Hans implemented - game mode selection page in and working, just needs icons for each game mode. Sound played when game mode is selected; starter kit needs to be improved. 
- Universal Dialogue system(DialogueRegistry/OriginsDialogue, SimpleDialoguePage for every dialogue we want to create going forward.)
- Custom XP notifications with our custom XpDropOverlay, Runescape Style circular XP notifications and tracker. 



## Crafting Overrides
- Generator: `tools/generate_crafting_recipes.ps1` (audits base assets, applies exclusion rules, normalizes recipe arrays, and writes overrides).
- Exclusions include debug/test/materials/blocks and specified keywords (bench exclusions list lives in the generator script).
- first pass on fixing most recipes completed. Need to further refine in future. 
- re-ordered craftingskillregistry rules so that armor crafted with "leather" or "cloth" in the name gives XP to its respective skill, leatherworking or arcane engineering. 

## Gear Requirement Maintenance
- Central registry: `src/main/java/dev/hytalemodding/origins/registry/CombatRequirementRegistry.java`
- To add new gear requirements:
- Add a keyword or exact match in `CombatRequirementRegistry` (material/ID tokens).
- Map it to the required level.
- For armor, requirements are checked against Defence level on equip.
- For weapons, requirements are checked against Attack/Ranged/Magic when dealing damage.

## Project Structure Notes
- Registries now live in `src/main/java/dev/hytalemodding/origins/registry`.
- Components live in `src/main/java/dev/hytalemodding/origins/component`.
- Event listeners live in `src/main/java/dev/hytalemodding/origins/events`.
- Utility helpers live in `src/main/java/dev/hytalemodding/origins/util`.
- Interaction handlers live in `src/main/java/dev/hytalemodding/origins/interaction`.


## Current Focus
-Implementing Quest GUI within CharacterMenu. Runescape style quest journal/tracker with support for requirements, rewards, persistant states of quests (not started, in-progress, completed)



