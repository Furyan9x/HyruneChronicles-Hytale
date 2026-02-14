package dev.hytalemodding.hyrune.repair;

import java.util.Objects;

/**
 * One material requirement for a repair operation.
 */
public final class RepairMaterialCost {
    private final String itemId;
    private final int quantity;

    public RepairMaterialCost(String itemId, int quantity) {
        this.itemId = Objects.requireNonNull(itemId, "itemId");
        this.quantity = Math.max(1, quantity);
    }

    public String getItemId() {
        return itemId;
    }

    public int getQuantity() {
        return quantity;
    }
}
