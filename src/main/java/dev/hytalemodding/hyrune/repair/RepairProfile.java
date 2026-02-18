package dev.hytalemodding.hyrune.repair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Declarative repair profile for an item family.
 */
public final class RepairProfile {
    private final String primaryMaterial;
    private final String secondaryMaterial;
    private final String rareGemMaterial;
    private final int primaryBaseCost;
    private final int secondaryBaseCost;
    private final int gemBaseCost;

    private RepairProfile(Builder builder) {
        this.primaryMaterial = builder.primaryMaterial;
        this.secondaryMaterial = builder.secondaryMaterial;
        this.rareGemMaterial = builder.rareGemMaterial;
        this.primaryBaseCost = builder.primaryBaseCost;
        this.secondaryBaseCost = builder.secondaryBaseCost;
        this.gemBaseCost = builder.gemBaseCost;
    }

    public List<RepairMaterialCost> buildCosts(double missingRatio, ItemRarity rarity) {
        if (missingRatio <= 0d) {
            return Collections.emptyList();
        }
        double clampedMissing = Math.max(0d, Math.min(1d, missingRatio));
        double rarityMultiplier = rarity != null ? rarity.getRepairCostMultiplier() : 1.0;

        List<RepairMaterialCost> costs = new ArrayList<>(3);
        costs.add(new RepairMaterialCost(primaryMaterial, scale(primaryBaseCost, clampedMissing, rarityMultiplier)));
        costs.add(new RepairMaterialCost(secondaryMaterial, scale(secondaryBaseCost, clampedMissing, rarityMultiplier)));

        if (rareGemMaterial != null && rarity != null && rarity.ordinal() >= ItemRarity.RARE.ordinal()) {
            costs.add(new RepairMaterialCost(rareGemMaterial, scale(gemBaseCost, clampedMissing, rarityMultiplier)));
        }
        return costs;
    }

    private int scale(int baseCost, double missingRatio, double rarityMultiplier) {
        double raw = baseCost * missingRatio * rarityMultiplier;
        return Math.max(1, (int) Math.ceil(raw));
    }

    public static Builder builder(String primaryMaterial, String secondaryMaterial) {
        return new Builder(primaryMaterial, secondaryMaterial);
    }

    public static final class Builder {
        private final String primaryMaterial;
        private final String secondaryMaterial;
        private String rareGemMaterial;
        private int primaryBaseCost = 6;
        private int secondaryBaseCost = 4;
        private int gemBaseCost = 1;

        private Builder(String primaryMaterial, String secondaryMaterial) {
            this.primaryMaterial = primaryMaterial;
            this.secondaryMaterial = secondaryMaterial;
        }

        public Builder baseCosts(int primaryBaseCost, int secondaryBaseCost) {
            this.primaryBaseCost = Math.max(1, primaryBaseCost);
            this.secondaryBaseCost = Math.max(1, secondaryBaseCost);
            return this;
        }

        public Builder rareGem(String materialItemId, int baseCost) {
            this.rareGemMaterial = materialItemId;
            this.gemBaseCost = Math.max(1, baseCost);
            return this;
        }

        public RepairProfile build() {
            return new RepairProfile(this);
        }
    }
}

