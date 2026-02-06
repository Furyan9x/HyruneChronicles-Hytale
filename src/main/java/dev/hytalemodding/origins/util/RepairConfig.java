package dev.hytalemodding.origins.util;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Utility class for determining repair costs and materials based on item ID patterns.
 * Uses pattern matching to dynamically calculate repair requirements for Hytale items.
 */
public class RepairConfig {

    // Material mappings - ordered for priority (specific before generic)
    private static final Map<String, String> MATERIAL_PATTERNS = new LinkedHashMap<>();

    // Cost multiplier mappings - ordered for priority
    private static final Map<String, Integer> COST_PATTERNS = new LinkedHashMap<>();

    // Default values
    private static final String DEFAULT_MATERIAL = "Ingredient_Bar_Iron";
    private static final int DEFAULT_BASE_COST = 5;

    static {
        initializeMaterialPatterns();
        initializeCostPatterns();
    }

    /**
     * Initialize material patterns with priority ordering.
     * More specific patterns (e.g., "dark_iron") must come before generic ones (e.g., "iron").
     */
    private static void initializeMaterialPatterns() {
        // Leather types
        MATERIAL_PATTERNS.put("leather_scaled", "Ingredient_Leather_Scaled");
        MATERIAL_PATTERNS.put("leather_prismic", "Ingredient_Leather_Prismic");
        MATERIAL_PATTERNS.put("leather_dark", "Ingredient_Leather_Dark");
        MATERIAL_PATTERNS.put("leather_storm", "Ingredient_Leather_Storm");
        MATERIAL_PATTERNS.put("leather_heavy", "Ingredient_Leather_Heavy");
        MATERIAL_PATTERNS.put("leather_medium", "Ingredient_Leather_Medium");
        MATERIAL_PATTERNS.put("leather_light", "Ingredient_Leather_Light");
        MATERIAL_PATTERNS.put("leather_soft", "Ingredient_Leather_Soft");
        MATERIAL_PATTERNS.put("strap_leather", "Ingredient_Strap_Leather");


        // Fabric types
        MATERIAL_PATTERNS.put("shadoweave", "Ingredient_Bolt_Shadoweave");
        MATERIAL_PATTERNS.put("prismaloom", "Ingredient_Bolt_Prismaloom");
        MATERIAL_PATTERNS.put("cindercloth", "Ingredient_Bolt_Cindercloth");
        MATERIAL_PATTERNS.put("stormsilk", "Ingredient_Bolt_Stormsilk");
        MATERIAL_PATTERNS.put("linen", "Ingredient_Bolt_Linen");
        MATERIAL_PATTERNS.put("cotton", "Ingredient_Bolt_Cotton");
        MATERIAL_PATTERNS.put("silk", "Ingredient_Bolt_Silk");

        // Metal types - specific before generic
        MATERIAL_PATTERNS.put("dark_iron", "Ingredient_Bar_DarkIron");
        MATERIAL_PATTERNS.put("cobalt", "Ingredient_Bar_Cobalt");
        MATERIAL_PATTERNS.put("copper", "Ingredient_Bar_Copper");
        MATERIAL_PATTERNS.put("iron", "Ingredient_Bar_Iron");
        MATERIAL_PATTERNS.put("steel", "Ingredient_Bar_Steel");
        MATERIAL_PATTERNS.put("bronze", "Ingredient_Bar_Bronze");
        MATERIAL_PATTERNS.put("silver", "Ingredient_Bar_Silver");
        MATERIAL_PATTERNS.put("gold", "Ingredient_Bar_Gold");
        MATERIAL_PATTERNS.put("mithril", "Ingredient_Bar_Mithril");
        MATERIAL_PATTERNS.put("adamantite", "Ingredient_Bar_Adamantite");
        MATERIAL_PATTERNS.put("thorium", "Ingredient_Bar_Thorium");
        MATERIAL_PATTERNS.put("onyxium", "Ingredient_Bar_Onyxium");
        MATERIAL_PATTERNS.put("prisma", "Ingredient_Bar_Prisma");

        // Wood types
        MATERIAL_PATTERNS.put("oak", "Ingredient_Plank_Oak");
        MATERIAL_PATTERNS.put("birch", "Ingredient_Plank_Birch");
        MATERIAL_PATTERNS.put("pine", "Ingredient_Plank_Pine");
        MATERIAL_PATTERNS.put("wood", "Ingredient_Plank_Oak");

        // Stone types
        MATERIAL_PATTERNS.put("obsidian", "Ingredient_Shard_Obsidian");
        MATERIAL_PATTERNS.put("stone", "Ingredient_Block_Stone");

        // Special materials
        MATERIAL_PATTERNS.put("crystal", "Ingredient_Crystal_Pure");
        MATERIAL_PATTERNS.put("void", "Ingredient_Essence_Void");
        MATERIAL_PATTERNS.put("flame", "Ingredient_Essence_Flame");
    }

    /**
     * Initialize cost patterns based on item type/quality.
     * Higher values mean more expensive repairs.
     */
    private static void initializeCostPatterns() {
        // Weapon types
        COST_PATTERNS.put("dagger", 2);
        COST_PATTERNS.put("sword", 3);
        COST_PATTERNS.put("axe", 3);
        COST_PATTERNS.put("club", 3);
        COST_PATTERNS.put("spear", 4);
        COST_PATTERNS.put("longsword", 4);
        COST_PATTERNS.put("greatsword", 5);
        COST_PATTERNS.put("mace", 5);
        COST_PATTERNS.put("scythe", 5);
        COST_PATTERNS.put("battleaxe", 5);
        COST_PATTERNS.put("warhammer", 5);
        COST_PATTERNS.put("shortbow", 3);
        COST_PATTERNS.put("crossbow", 4);
        COST_PATTERNS.put("staff", 4);
        COST_PATTERNS.put("wand", 2);
        COST_PATTERNS.put("spellbook", 2);

        // Armor types
        COST_PATTERNS.put("head", 3);
        COST_PATTERNS.put("chest", 5);
        COST_PATTERNS.put("legs", 4);
        COST_PATTERNS.put("hands", 2);
        COST_PATTERNS.put("shield", 4);

        // Tool types
        COST_PATTERNS.put("pickaxe", 3);
        COST_PATTERNS.put("shovel", 2);
        COST_PATTERNS.put("hoe", 2);
        COST_PATTERNS.put("fishing_rod", 2);

        // Quality modifiers (these add to base cost)
        COST_PATTERNS.put("common", 1);
        COST_PATTERNS.put("uncommon", 2);
        COST_PATTERNS.put("rare", 4);
        COST_PATTERNS.put("epic", 6);
        COST_PATTERNS.put("legendary", 10);
    }

    /**
     * Register a custom material pattern.
     *
     * @param keyword The substring to search for in item IDs (case-insensitive)
     * @param materialItemId The material item ID to use for repairs
     */
    public static void registerMaterialPattern(String keyword, String materialItemId) {
        MATERIAL_PATTERNS.put(keyword.toLowerCase(Locale.ROOT), materialItemId);
    }

    /**
     * Register a custom cost pattern.
     *
     * @param keyword The substring to search for in item IDs (case-insensitive)
     * @param baseCost The base cost multiplier for this pattern
     */
    public static void registerCostPattern(String keyword, int baseCost) {
        COST_PATTERNS.put(keyword.toLowerCase(Locale.ROOT), baseCost);
    }

    /**
     * Bulk register standard ingot/bar materials.
     *
     * @param materials Array of material names (will be converted to "Ingredient_Bar_X" format)
     */
    public static void registerIngotMaterials(String... materials) {
        for (String material : materials) {
            String keyword = material.toLowerCase(Locale.ROOT);
            String itemId = "Ingredient_Bar_" + capitalizeFirst(material);
            MATERIAL_PATTERNS.put(keyword, itemId);
        }
    }

    /**
     * Bulk register cost groups with the same base cost.
     *
     * @param baseCost The base cost for all keywords in this group
     * @param keywords The keywords to register
     */
    public static void registerCostGroup(int baseCost, String... keywords) {
        for (String keyword : keywords) {
            COST_PATTERNS.put(keyword.toLowerCase(Locale.ROOT), baseCost);
        }
    }

    /**
     * Get repair information for an item based on its ID and durability.
     *
     * @param itemId The item's ID (e.g., "Weapon_Sword_Iron")
     * @param currentDurability Current durability value
     * @param maxDurability Maximum durability value
     * @return RepairMaterial containing material ID and quantity needed
     */
    public static RepairMaterial getRepairInfo(String itemId, double currentDurability, double maxDurability) {
        String materialItemId = getMaterialForItem(itemId);
        int baseCost = getBaseCostForItem(itemId);
        int quantity = calculateQuantity(baseCost, currentDurability, maxDurability);

        return new RepairMaterial(materialItemId, quantity);
    }

    /**
     * Determine the repair material based on item ID patterns.
     *
     * @param itemId The item's ID
     * @return The material item ID to use for repairs
     */
    private static String getMaterialForItem(String itemId) {
        if (itemId == null) {
            return DEFAULT_MATERIAL;
        }

        String normalized = itemId.toLowerCase(Locale.ROOT);

        // Check patterns in order (LinkedHashMap preserves insertion order)
        for (Map.Entry<String, String> entry : MATERIAL_PATTERNS.entrySet()) {
            if (normalized.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return DEFAULT_MATERIAL;
    }

    /**
     * Determine the base cost multiplier based on item ID patterns.
     *
     * @param itemId The item's ID
     * @return The base cost multiplier
     */
    private static int getBaseCostForItem(String itemId) {
        if (itemId == null) {
            return DEFAULT_BASE_COST;
        }

        String normalized = itemId.toLowerCase(Locale.ROOT);
        int totalCost = 0;
        boolean foundAny = false;

        // Accumulate costs from all matching patterns
        for (Map.Entry<String, Integer> entry : COST_PATTERNS.entrySet()) {
            if (normalized.contains(entry.getKey())) {
                totalCost += entry.getValue();
                foundAny = true;
            }
        }

        return foundAny ? Math.max(1, totalCost) : DEFAULT_BASE_COST;
    }

    /**
     * Calculate the quantity of materials needed based on durability and base cost.
     *
     * @param baseCost The base cost multiplier
     * @param currentDurability Current durability
     * @param maxDurability Maximum durability
     * @return The quantity of materials needed
     */
    private static int calculateQuantity(int baseCost, double currentDurability, double maxDurability) {
        if (maxDurability <= 0) {
            return baseCost;
        }

        double missingRatio = (maxDurability - currentDurability) / maxDurability;
        missingRatio = Math.max(0.0, Math.min(1.0, missingRatio));

        // Scale cost based on how damaged the item is
        int quantity = (int) Math.ceil(baseCost * (1.0 + missingRatio));
        return Math.max(1, quantity);
    }

    /**
     * Capitalize the first letter of a string.
     */
    private static String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase(Locale.ROOT) + str.substring(1);
    }

    /**
     * Represents repair material requirements for an item.
     */
    public static class RepairMaterial {
        private final String materialItemId;
        private final int quantity;

        public RepairMaterial(String materialItemId, int quantity) {
            this.materialItemId = materialItemId;
            this.quantity = quantity;
        }

        /**
         * Get the item ID of the material required for repair.
         *
         * @return The material item ID
         */
        public String getMaterialItemId() {
            return materialItemId;
        }

        /**
         * Get the quantity of material required for repair.
         *
         * @return The quantity needed
         */
        public int getQuantity() {
            return quantity;
        }

        @Override
        public String toString() {
            return "RepairMaterial{" +
                    "materialItemId='" + materialItemId + '\'' +
                    ", quantity=" + quantity +
                    '}';
        }
    }
}