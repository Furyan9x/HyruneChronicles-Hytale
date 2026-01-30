# Origins (Hytale Mod) – Overview

Origins is an RPG-style progression mod that adds a persistent skill system, MMO-inspired UI, and a full Slayer task loop with a custom master NPC. It is built around long-term character growth through combat, gathering, crafting, and specialized activities.

## What It Adds
- **Multi-skill progression** with combat, gathering, and artisan skills (Attack, Strength, Defence, Ranged, Magic, Restoration, Constitution, Slayer, Mining, Woodcutting, Fishing, Farming, Smelting, and more).
- **Persistent player data** stored as JSON in `./origins_data`.
- **MMO-style nameplates** that display player level.
- **Custom UI** including a character/skills menu and RuneScape-inspired dialogue panels.
- **Custom fishing system** (rod + bobber behavior, casting/idle systems, XP hooks).
- **Slayer system** with tiered tasks, kill tracking, rewards, and a vendor UI.

## How It Works
### Leveling + Skills
- XP is awarded through combat, gathering, crafting, farming, mining, woodcutting, fishing, and timed crafting systems.
- Levels and skill progression are handled by the `LevelingService` and persisted in JSON.
- Bonuses (combat stats, regen, movement) are applied on level-up.

### Slayer System
- A custom **Kweebec Rootling** Slayer Master (`Tier1_Slayer_Master`) offers tasks via dialogue.
- Tasks are **tier-gated** by Slayer level and assign a target NPC type with a random kill count.
- State flow: **Accepted → In Progress → Completed**.
- Only **killing blows** by the assigned player count toward the task.
- Turn-ins grant **Slayer points** and **Slayer XP** (item rewards are stubbed for future expansion).
- A **Slayer vendor UI** displays points and completed tasks (shop items are placeholders for now).

### UI/Interaction
- Press **F** to interact with the Slayer Master and open custom dialogue.
- The character menu is accessible via a command and shows skills/levels.

## Commands (Debug/Utility)
- `/character` – opens the Character Menu (skills/levels UI).
- `/setskill <skill> <level>` – debug command to set a skill level.
- `/checktags` – debug command to print current nameplate text.

## Notes / Extensibility
- Task tiers and NPC targets are defined in code and can be expanded easily.
- The Slayer reward system is designed for future item/shop integration.
- UI layouts are in `src/main/resources/Common/UI/Custom/Pages`.

