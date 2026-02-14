package dev.hytalemodding.hyrune.repair;

import java.util.Collections;
import java.util.List;

/**
 * Computed plan for restoring one item to full durability.
 */
public final class RepairPlan {
    private final ItemRarity rarity;
    private final int restoreAmount;
    private final List<RepairMaterialCost> materialCosts;

    public RepairPlan(ItemRarity rarity, int restoreAmount, List<RepairMaterialCost> materialCosts) {
        this.rarity = rarity;
        this.restoreAmount = Math.max(0, restoreAmount);
        this.materialCosts = materialCosts == null ? Collections.emptyList() : List.copyOf(materialCosts);
    }

    public ItemRarity getRarity() {
        return rarity;
    }

    public int getRestoreAmount() {
        return restoreAmount;
    }

    public List<RepairMaterialCost> getMaterialCosts() {
        return materialCosts;
    }

    public boolean isRepairable() {
        return restoreAmount > 0 && !materialCosts.isEmpty();
    }
}
