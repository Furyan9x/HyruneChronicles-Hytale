# Slayer System Task Plan

## Scope
- Build a custom Slayer skill system with a Kweebec Rootling Slayer Master NPC.
- Dialogue choices: explain Slayer, request task, nevermind (if player has completed task, turn in)
- Custom UI: dialogue panel + (future) slayer vendor.
- Per-player task state: accepted -> in-progress -> completed.
- Track kill counts on matching target and killing blow only.
- Persist data per player in JSON.
- Reward system: points + XP + random item stub/TODO.
- Level-gated task pools: 1, 20, 40, 60, 80 tiers.

## Phases
- [x] Task 1: Audit current mod structure and identify NPC/interaction hooks.
- [x] Task 2: Define data models for Slayer tasks, player state, and persistence.
- [x] Task 3: Implement JSON storage service and load/save lifecycle wiring.
- [x] Task 4: Build task registry with tiered pools and placeholder Rats.
- [x] Task 5: Create Slayer Master NPC interaction and dialogue options.
- [x] Task 6: Build custom UI for Slayer dialogue (initial implementation).
- [x] Task 7: Implement task assignment and state transitions.
- [x] Task 8: Implement kill tracking and validation (target match + killing blow).
- [x] Task 9: Implement task completion and turn-in flow.
- [x] Task 10: Stub rewards (points/XP/random item) with TODOs.
- [x] Task 11: Draft Slayer vendor UI placeholder (optional wiring).
- [x] Task 12: Add tests or lightweight validation hooks if feasible.
- [x] Task 13: Document configuration points for adding new tasks/rewards.
## Notes
- Placeholder target NPC: Rats for all tiers until real NPCs are provided.
- Random item reward can be a stubbed hook or TODO.
