# Origins Mod

## Overview
Origins is an RPG progression mod for Hytale focused on skill leveling, quests, and Slayer tasks. The codebase is organized around ECS systems, services, and data repositories for persistence.

## Package Layout
- `dev.hytalemodding.origins.database`: JSON repositories and persistence interfaces.
- `dev.hytalemodding.origins.playerdata`: Player data models for leveling, quests, and Slayer progress.
- `dev.hytalemodding.origins.events`: Event listeners for lifecycle and gameplay hooks.
- `dev.hytalemodding.origins.level`: Leveling services and formulas.
- `dev.hytalemodding.origins.quests`: Quest definitions and quest management.
- `dev.hytalemodding.origins.slayer`: Slayer task logic and rewards.
- `dev.hytalemodding.origins.system`: ECS systems for gameplay mechanics.
- `dev.hytalemodding.origins.ui`: Custom UI pages and overlays.
- `dev.hytalemodding.origins.util`: Shared helpers and managers.

## Persistence Pattern
- Repositories implement `PlayerDataRepository<T>` and expose `load(UUID)` and `save(T)`.
- Player data classes implement `PlayerData` and expose `getUuid()`.
- Services cache player data, provide `load(UUID)` and `unload(UUID)`, and persist immediately on mutations.

## Reference
Use `codex/api_map.md` for Hytale API lookup and package guidance.
