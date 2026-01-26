# Session Summary (2026-01-26)

## Fishing System Progress
- Implemented a custom fishing interaction that is wired through item Interactions (type `OriginsFishing`). The interaction now works reliably and is no longer tied to `PlayerInteractEvent`.
- Fishing uses a custom bobber entity with a dedicated component/system for bite timing. The bite window notifies the player and plays a splash sound.
- Bait required to cast (currently keyword-based). `minnow` bait is supported in the registry.
- Catching fish now spawns a pickup at the bobber and magnet-pulls it to the player (`ItemUtils.interactivelyPickupItem`). XP is awarded on successful catch.
- Bobber model now shows correctly and is scaled up for visibility. Spawn height adjusted to sit on the water surface.
- Bite sound volume increased to be clearly audible.

## Key Changes
- `OriginsFishing` Interaction is registered in `dev.hytalemodding.Origins` and should be referenced in rod JSON:
  ```json
  "Interactions": {
    "Secondary": {
      "Interactions": [
        { "Type": "OriginsFishing" }
      ]
    }
  }
  ```
- Fishing logic moved into `FishingInteraction` (Interaction-based), with bobber spawning, bait consumption, XP, and catch handling.
- Custom bobber component/system created (`FishingBobberComponent`, `FishingBobberSystem`).
- Fish table updated to use real item IDs (Fish_*_Item). Placeholder IDs removed.

## Files Touched (not exhaustive)
- `src/main/java/dev/hytalemodding/Origins.java`
- `src/main/java/dev/hytalemodding/origins/system/FishingInteraction.java`
- `src/main/java/dev/hytalemodding/origins/system/FishingBobberComponent.java`
- `src/main/java/dev/hytalemodding/origins/system/FishingBobberSystem.java`
- `src/main/java/dev/hytalemodding/origins/system/FishingRegistry.java`
- `src/main/resources/Server/Models/Bobber/OriginsFishingBobber.json`
- `src/main/resources/Server/Audio/SoundEvents/SFX/Tools/Fishing_Rod/SFX_Origins_Fishing_Rod_Cast.json`
- `src/main/resources/Server/Audio/SoundEvents/SFX/Tools/Fishing_Rod/SFX_Origins_Fishing_Rod_Reel.json`

## Open Items / Notes
- Temporary debug messages are still present in `FishingInteraction` (“interaction triggered”, “no water target”, etc.). Remove when ready.
- Pickup animation is fast; may need tuning if API allows.
- If asset packs conflict, ensure the rod JSON is only in the Origins asset pack (avoid server-mods override).

---

# Next Steps Prompt

## Final Fishing Touches
- Remove debug chat messages from `FishingInteraction`.
- Confirm pickup animation behavior and investigate if pickup speed can be tuned.
- Optionally swap bite sound to a custom SFX/VFX when ready.

## Slayer Skill (New System)
Implement a custom NPC task system:
- Create a quest-giver NPC with an interaction/dialogue system.
- NPC offers a task: kill X of a specific NPC type.
- Track task state per player (active task, target type, remaining count).
- On target NPC death, decrement the counter if player has that task.
- When counter reaches 0, mark task as completed.
- Player returns to NPC to turn in for custom rewards.

Focus on a robust, extensible structure so new tasks/rewards can be added easily.
