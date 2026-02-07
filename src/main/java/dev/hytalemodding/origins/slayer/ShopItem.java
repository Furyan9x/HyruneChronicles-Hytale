package dev.hytalemodding.origins.slayer;


public class ShopItem {
    private final String id;
    private final String displayName;
    private final int cost;
    // We can add "icon" here later if we move away from standard resource paths

    public ShopItem(String id, String displayName, int cost) {
        this.id = id;
        this.displayName = displayName;
        this.cost = cost;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getCost() {
        return cost;
    }
}