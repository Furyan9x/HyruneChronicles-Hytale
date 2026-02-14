package dev.hytalemodding.hyrune.repair;

import com.hypixel.hytale.server.core.inventory.ItemStack;

/**
 * Computes bench repair plans using profile + rarity + missing durability.
 */
public final class RepairPlanner {
    private RepairPlanner() {
    }

    public static RepairPlan planFullRepair(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return new RepairPlan(ItemRarity.COMMON, 0, null);
        }
        double maxDurability = stack.getMaxDurability();
        double currentDurability = stack.getDurability();
        if (maxDurability <= 0d || currentDurability >= maxDurability) {
            return new RepairPlan(ItemRarity.COMMON, 0, null);
        }

        String itemId = stack.getItemId();
        ItemRarity rarity = ItemRarity.fromItemId(itemId);
        RepairProfile profile = RepairProfileRegistry.resolve(itemId);

        double missingRatio = (maxDurability - currentDurability) / maxDurability;
        int restoreAmount = (int) Math.ceil(maxDurability - currentDurability);
        return new RepairPlan(rarity, restoreAmount, profile.buildCosts(missingRatio, rarity));
    }
}
