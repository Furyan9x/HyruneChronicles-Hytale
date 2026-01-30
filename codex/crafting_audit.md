# Crafting Audit (Hytale Base Assets)

Assets root: C:\\Users\\devin\\Desktop\\HytaleServer\\Assets

## Summary
- Items scanned: 2958
- Items with no effective recipe: 385
- Items with bench issues: 104
- Overrides written: 0
- Overrides skipped (already existed in mod): 489
- Recipes added: 0
- Bench requirements fixed: 0

## Assumptions
- If an item lacks a Recipe, copy a template recipe from the most common recipe in the same directory group; fallback to parent recipe if present.
- If no group or parent recipe is available, choose the most common bench requirement for the item's Tags.Type and use a basic input fallback (Stick + Rubble).
- If a recipe has a BenchRequirement with Id "TODO" or no benches, replace it using the group or type-derived bench.
- Material substitution only adjusts Ingredient_Bar_* inputs when the item id or Tags.Family contains a matching material token.
- Existing mod overrides are not overwritten.

## Notes
- Crafting XP and gating remain driven by item ids and existing CraftingSkillRegistry rules.
