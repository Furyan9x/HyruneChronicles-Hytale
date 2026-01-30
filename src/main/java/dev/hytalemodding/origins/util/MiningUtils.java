package dev.hytalemodding.origins.util;

import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.inventory.ItemStack;

public final class MiningUtils {
    private MiningUtils() {
    }

    public static boolean isPickaxe(ItemStack stack) {
        if (stack == null || stack.getItemId() == null) {
            return false;
        }
        String id = stack.getItemId().toLowerCase();
        return id.contains("pickaxe");
    }

    public static boolean isWoodBlock(BlockType blockType) {
        if (blockType == null || blockType.getId() == null) {
            return false;
        }
        String lowered = blockType.getId().toLowerCase();
        return lowered.contains("wood")
            || lowered.contains("log")
            || lowered.contains("plank")
            || lowered.contains("leaf")
            || lowered.contains("leaves")
            || lowered.contains("sapling")
            || lowered.contains("bark")
            || lowered.contains("branch");
    }

    public static boolean isStoneOrOreBlock(BlockType blockType) {
        if (blockType == null || blockType.getId() == null) {
            return false;
        }
        String id = blockType.getId().toLowerCase();
        return id.contains("stone")
            || id.contains("ore")
            || id.contains("rock")
            || id.contains("metal")
            || id.contains("cobblestone");
    }

    public static boolean isAxe(ItemStack stack) {
        if (stack == null || stack.getItemId() == null) {
            return false;
        }
        String id = stack.getItemId().toLowerCase();
        boolean looksLikeAxe = id.contains("axe") || id.contains("hatchet");
        boolean isPick = id.contains("pickaxe");
        return looksLikeAxe && !isPick;
    }
}
