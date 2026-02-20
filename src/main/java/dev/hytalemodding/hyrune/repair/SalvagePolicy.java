package dev.hytalemodding.hyrune.repair;

import com.hypixel.hytale.server.core.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Placeholder salvage policy for future Salvager Workbench tiering.
 */
public final class SalvagePolicy {
    private SalvagePolicy() {
    }

    public static double getReturnRatio(int benchTier) {
        switch (benchTier) {
            case 3:
                return 0.80;
            case 2:
                return 0.60;
            case 1:
            default:
                return 0.40;
        }
    }

    public static List<RepairMaterialCost> estimateSalvageReturns(List<RepairMaterialCost> sourceMaterials, int benchTier) {
        if (sourceMaterials == null || sourceMaterials.isEmpty()) {
            return Collections.emptyList();
        }
        double ratio = getReturnRatio(benchTier);
        List<RepairMaterialCost> returns = new ArrayList<>(sourceMaterials.size());
        for (RepairMaterialCost cost : sourceMaterials) {
            int quantity = Math.max(1, (int) Math.floor(cost.getQuantity() * ratio));
            returns.add(new RepairMaterialCost(cost.getItemId(), quantity));
        }
        return returns;
    }

    public static List<RepairMaterialCost> estimateSalvageReturns(ItemStack stack, ItemRarity rarity, int benchTier) {
        if (stack == null || stack.isEmpty() || stack.getItemId() == null) {
            return Collections.emptyList();
        }
        RepairProfile profile = RepairProfileRegistry.resolve(stack.getItemId());
        List<RepairMaterialCost> fullBuildCosts = profile.buildCosts(1.0, rarity);
        return estimateSalvageReturns(fullBuildCosts, benchTier);
    }
}
