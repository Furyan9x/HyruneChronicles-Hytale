# Origins Mod - Iteration Notes (2026-02-13)

## Completed This Iteration
- VSCode run/debug workflow stabilized (build + copy-to-dev + server run path validated).
- Farming direction updated:
  - Removed farming-yield scaling approach.
  - Farming level now gates what seeds can be planted.
  - Seed rules are config-driven in `gameplay_config.json`.
- Animal husbandry gating scaffold added (stubbed for future event hooks).
- Durability debug logging:
  - Toggle moved to runtime config.
  - Verified logs in mining and woodcutting flows.
  - Added woodcutting fallback handling when API reports `durabilityUse <= 0`.
- Tool-type and resource gating:
  - Pickaxes blocked from wood.
  - Hatchets blocked from ore/stone.
  - Ore damage gating active to prevent bypass behavior.
- NPC level/nameplate pass:
  - Excluded NPCs keep native display naming.
  - Dialogue key matching normalized (underscores/spaces/prefix variants).
  - Global NPC name normalization improved.
  - Added config-driven `npcNameOverrides` for edge-case names.

## UI Work Completed (Character/Skills Focus)
- Character Menu:
  - Arcane Engineering icon resolution fixed for XP drop overlay.
  - Fishing bonuses surfaced in Attributes tab (bite speed + rare chance).
  - Quest details panel polished:
    - Group headers by sort mode:
      - A-Z grouping with "The " normalization.
      - Length grouping.
      - Difficulty grouping.
    - Detail title/header visual upgrades with gold underline.
    - Length + Difficulty indicator line in detail panel.
  - Skills tab refactor:
    - Removed embedded skill detail panel.
    - Resized skills area back to grid-first layout.
    - Added textured skill cells with hover/pressed states.
    - Tuned progress bar dimensions and max-level gold bar alignment.
    - Footer typography/emphasis updated (Combat Level highlighted).

- Skill Info system:
  - Added dedicated `SkillInfoPage` (`.ui` + `.java`) with:
    - Left-side skill selector (3-column icon grid).
    - Right-side skill description + unlock list.
    - Back + Close controls.
  - Added unlock dimming based on player skill level.
  - Added debug command `/skillinfo` to open SkillInfo directly.

## Transition Bug + Resolution
- Symptom:
  - Opening SkillInfo from Character Menu caused non-responsive controls or broken follow-up page interactions.
  - Standalone `/skillinfo` flow worked correctly.
- Root cause:
  - Page transition timing during UI event callback caused inconsistent interactive state.
- Resolution:
  - Transition logic switched to world-thread deferred open (`CompletableFuture.runAsync(..., world)`).
  - Applied in both directions:
    - `CharacterMenu -> SkillInfoPage`.
    - `SkillInfoPage Back -> CharacterMenu`.
  - Added `/skillinfo` as isolation tool for future UI transition debugging.

## CustomUI Gotchas Confirmed This Iteration
- `HorizontalAlignment` values are `Start`, `Center`, `End` (not `Left`/`Right`).
- Appended row templates must use a single top-level root container for reliable selectors.
- `TopScrolling` containers can clip/hitbox-truncate edge content; left icon grid currently uses non-scrolling `Top` for stable input.

## Config Workflow Change
- Removed per-second config file polling.
- Config is now loaded on startup and on-demand via command:
  - `/configreload`
- Rationale: avoids constant timestamp checks in hot paths and gives deterministic reload control.

## Current Known Open Items
1) Smelting/Leatherworking processing-bench hooks (furnace/tanner integration path).
2) NPC persistence hardening follow-up (long-session/restart validation pass).
3) UI follow-up:
   - SkillInfoPage visual fidelity pass (closer RS3 style).
   - Optional reintroduction of scroll behavior for left icon grid if hitbox-safe.
   - Repair UI pass.

## Practical Notes
- Runtime config location is CWD-sensitive: `./origins_data/gameplay_config.json`.
- If gameplay config changes appear ignored:
  1) Edit the file in the server runtime working directory.
  2) Run `/configreload`.
- NPC naming resolution order:
  1) Java normalization heuristics.
  2) `npcNameOverrides` lookup.
  3) Default cleaned name.
- Useful debug commands:
  - `/c` or `/character` to open Character Menu.
  - `/skillinfo` to open SkillInfoPage directly (transition isolation).
