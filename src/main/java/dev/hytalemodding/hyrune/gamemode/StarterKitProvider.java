package dev.hytalemodding.hyrune.gamemode;

import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides starter kits for different game modes.
 * Define your starter items for each game mode here.
 */
public class StarterKitProvider {

    // Game mode constants
    public static final String MODE_NORMAL = "normal";
    public static final String MODE_IRONMAN = "ironman";
    public static final String MODE_HARDCORE_IRONMAN = "hardcore_ironman";

    /**
     * Get the starter kit for a specific game mode.
     *
     * @param gameMode The game mode
     * @return List of ItemStacks to give to the player
     */
    @Nonnull
    public static List<ItemStack> getStarterKit(@Nonnull String gameMode) {
        switch (gameMode.toLowerCase()) {
            case MODE_NORMAL:
                return getNormalModeKit();
            case MODE_IRONMAN:
                return getIronmanModeKit();
            case MODE_HARDCORE_IRONMAN:
                return getHardcoreIronmanKit();
            default:
                return getNormalModeKit(); // Default fallback
        }
    }

    /**
     * Normal Mode Starter Kit - Generous starting items
     */
    @Nonnull
    private static List<ItemStack> getNormalModeKit() {
        List<ItemStack> items = new ArrayList<>();

        // Weapons
        items.add(new ItemStack("Weapon_Sword_Iron", 1));
        items.add(new ItemStack("Weapon_Bow_Wood", 1));

        // Armor - Full iron set
        items.add(new ItemStack("Armor_Helmet_Iron", 1));
        items.add(new ItemStack("Armor_Chestplate_Iron", 1));
        items.add(new ItemStack("Armor_Leggings_Iron", 1));
        items.add(new ItemStack("Armor_Boots_Iron", 1));

        // Tools
        items.add(new ItemStack("Tool_Pickaxe_Iron", 1));
        items.add(new ItemStack("Tool_Axe_Iron", 1));
        items.add(new ItemStack("Tool_Shovel_Iron", 1));

        // Consumables
        items.add(new ItemStack("Food_Bread", 16));
        items.add(new ItemStack("Potion_Health_Minor", 5));

        // Resources
        items.add(new ItemStack("Ingredient_Bar_Iron", 32));
        items.add(new ItemStack("Item_Torch", 64));
        items.add(new ItemStack("Item_Arrow", 64));

        return items;
    }

    /**
     * Ironman Mode Starter Kit - Moderate starting items
     * Ironman rules: Cannot trade with other players, self-sufficient gameplay
     */
    @Nonnull
    private static List<ItemStack> getIronmanModeKit() {
        List<ItemStack> items = new ArrayList<>();

        // Weapons - Basic tier
        items.add(new ItemStack("Weapon_Sword_Copper", 1));
        items.add(new ItemStack("Weapon_Bow_Wood", 1));

        // Armor - Leather set
        items.add(new ItemStack("Armor_Helmet_Leather", 1));
        items.add(new ItemStack("Armor_Chestplate_Leather", 1));
        items.add(new ItemStack("Armor_Leggings_Leather", 1));
        items.add(new ItemStack("Armor_Boots_Leather", 1));

        // Tools - Copper tier
        items.add(new ItemStack("Tool_Pickaxe_Copper", 1));
        items.add(new ItemStack("Tool_Axe_Copper", 1));
        items.add(new ItemStack("Tool_Shovel_Wood", 1));

        // Consumables - Less than normal
        items.add(new ItemStack("Food_Bread", 8));
        items.add(new ItemStack("Potion_Health_Minor", 3));

        // Resources - Minimal
        items.add(new ItemStack("Ingredient_Bar_Copper", 16));
        items.add(new ItemStack("Item_Torch", 32));
        items.add(new ItemStack("Item_Arrow", 32));

        return items;
    }

    /**
     * Hardcore Ironman Mode Starter Kit - Minimal starting items
     * Hardcore rules: One life + Ironman rules
     */
    @Nonnull
    private static List<ItemStack> getHardcoreIronmanKit() {
        List<ItemStack> items = new ArrayList<>();

        // Weapons - Very basic
        items.add(new ItemStack("Weapon_Sword_Wood", 1));

        // Armor - None! They must craft it

        // Tools - Bare minimum
        items.add(new ItemStack("Tool_Pickaxe_Wood", 1));
        items.add(new ItemStack("Tool_Axe_Wood", 1));

        // Consumables - Survival basics
        items.add(new ItemStack("Food_Bread", 4));

        // Resources - Very minimal
        items.add(new ItemStack("Item_Torch", 16));

        return items;
    }

    /**
     * Give a starter kit to a player's inventory.
     *
     * @param inventory The player's inventory
     * @param gameMode The game mode to determine which kit to give
     * @return true if all items were successfully added
     */
    public static boolean giveStarterKit(@Nonnull Inventory inventory, @Nonnull String gameMode) {
        List<ItemStack> starterKit = getStarterKit(gameMode);

        ItemContainer container = inventory.getCombinedEverything();
        if (container == null) {
            return false;
        }

        boolean success = true;
        for (ItemStack item : starterKit) {
            var transaction = container.addItemStack(item);
            if (transaction == null || !transaction.succeeded()) {
                success = false;
                // Continue trying to add other items even if one fails
            }
        }

        return success;
    }

    /**
     * Get a human-readable description of a game mode.
     *
     * @param gameMode The game mode
     * @return Description string
     */
    @Nonnull
    public static String getGameModeDescription(@Nonnull String gameMode) {
        switch (gameMode.toLowerCase()) {
            case MODE_NORMAL:
                return "Normal Mode - Standard gameplay with full trading and cooperation. " +
                        "Respawn on death. Best for beginners!";
            case MODE_IRONMAN:
                return "Ironman Mode - Self-sufficient gameplay. Cannot trade with other players. " +
                        "Respawn on death. For experienced players seeking a challenge!";
            case MODE_HARDCORE_IRONMAN:
                return "Hardcore Ironman Mode - Ultimate challenge! Cannot trade AND one life only. " +
                        "Death is permanent. Only for the bravest adventurers!";
            default:
                return "Unknown game mode";
        }
    }

    /**
     * Get display name for a game mode.
     *
     * @param gameMode The game mode
     * @return Display name
     */
    @Nonnull
    public static String getGameModeDisplayName(@Nonnull String gameMode) {
        switch (gameMode.toLowerCase()) {
            case MODE_NORMAL:
                return "Normal Mode";
            case MODE_IRONMAN:
                return "Ironman Mode";
            case MODE_HARDCORE_IRONMAN:
                return "Hardcore Ironman";
            default:
                return "Unknown Mode";
        }
    }
}