# Slayer System Configuration Notes

## Task pools (tiers and tasks)
- Edit `src/main/java/dev/hytalemodding/Origins.java` in `buildSlayerTaskRegistry()`.
- Each tier is `new SlayerTaskTier(minLevel, maxLevel, List.of(...tasks...))`.
- Each task is `new SlayerTaskDefinition(id, targetNpcId, minCount, maxCount)`.
- Use unique `id` strings and the NPC asset id for `targetNpcId` (ex: `"Rat"`).
- Kill counts roll between `minCount` and `maxCount` (inclusive).
- Startup validation logs warnings for empty tiers, invalid ranges, or overlapping tiers.

## Reward logic
- Rewards are granted in `SlayerService.turnInTask(...)`.
- Current rewards:
  - Slayer points (used by vendor UI).
  - Slayer XP (integrates with existing leveling service).
  - Random item reward is stubbed; replace TODO with real item grant logic.
- To customize rewards per task/tier, extend `SlayerTaskDefinition` with reward fields
  and update `SlayerService` to use them instead of the current flat reward.

## Vendor UI
- UI layout: `src/main/resources/Common/UI/Custom/Pages/SlayerVendor.ui`.
- Placeholder items live in the scroll area; swap these with real entries later.
- Values bound in code:
  - `#VendorPoints` and `#VendorTasks` are set in
    `src/main/java/dev/hytalemodding/origins/ui/SlayerVendorPage.java`.
- To support real purchases, add item entries + click handlers in `SlayerVendorPage`.

## Dialogue UI + interaction
- Dialogue UI: `src/main/resources/Common/UI/Custom/Pages/SlayerDialogue.ui`.
- Dialogue logic: `src/main/java/dev/hytalemodding/origins/ui/SlayerDialoguePage.java`.
- NPC action wiring:
  - `BuilderActionOpenSlayerDialogue` + `ActionOpenSlayerDialogue`.
  - Registered in `src/main/java/dev/hytalemodding/Origins.java`.
- NPC role file uses the action name `OpenSlayerDialogue`.

## Persistence
- JSON storage is in `./origins_data` via `JsonSlayerRepository`.
- Player data includes:
  - Current task assignment
  - Task state
  - Slayer points
  - Completed task count

