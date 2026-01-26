package dev.hytalemodding.origins.system;

import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

final class FishingRegistry {
    static final double BITE_SPEED_MAX_BONUS = 0.30;
    static final double BASE_RARE_CHANCE = 0.02;
    static final double RARE_CHANCE_BONUS = 0.10;

    static final List<String> FISHING_ROD_KEYWORDS = List.of(
        "fishing_rod",
        "rod_fishing",
        "tool_rod_crude",
        "rod_crude"
    );

    private static final List<BaitDefinition> BAITS = List.of(
        new BaitDefinition("minnow", 1.0, 1.0, "default")
    );

    private static final List<FishDefinition> FISH = List.of(
        new FishDefinition("Fish_Bluegill_Item", 1, 10, false, 10, "default"),
        new FishDefinition("Fish_Minnow_Item", 1, 10, false, 10, "default"),
        new FishDefinition("Fish_Crab_Item", 5, 11, false, 10, "default"),
        new FishDefinition("Fish_Clownfish_Item", 10, 12, false, 9, "default"),
        new FishDefinition("Fish_Pike_Item", 15, 13, false, 9, "default"),
        new FishDefinition("Fish_Catfish_Item", 20, 14, false, 8, "default"),
        new FishDefinition("Fish_Trout_Rainbow_Item", 25, 14, false, 8, "default"),
        new FishDefinition("Fish_Salmon_Item", 30, 16, false, 7, "default"),
        new FishDefinition("Fish_Piranha_Item", 35, 17, false, 7, "default"),
        new FishDefinition("Fish_Eel_Moray_Item", 40, 18, false, 6, "default"),
        new FishDefinition("Fish_Tang_Blue_Item", 45, 18, false, 6, "default"),
        new FishDefinition("Fish_Tang_Chevron_Item", 50, 19, false, 6, "default"),
        new FishDefinition("Fish_Tang_Lemon_Peel_Item", 55, 20, false, 6, "default"),
        new FishDefinition("Fish_Tang_Sailfin_Item", 60, 20, false, 5, "default"),
        new FishDefinition("Fish_Lobster_Item", 65, 22, false, 5, "default"),
        new FishDefinition("Fish_Trilobite_Item", 70, 24, false, 5, "default"),
        new FishDefinition("Fish_Trilobite_Black_Item", 75, 26, false, 4, "default"),
        new FishDefinition("Fish_Frostgill_Item", 80, 28, true, 3, "default"),
        new FishDefinition("Fish_Pufferfish_Item", 85, 30, true, 3, "default"),
        new FishDefinition("Fish_Jellyfish_Blue_Item", 88, 32, true, 2, "default"),
        new FishDefinition("Fish_Jellyfish_Cyan_Item", 88, 32, true, 2, "default"),
        new FishDefinition("Fish_Jellyfish_Green_Item", 88, 32, true, 2, "default"),
        new FishDefinition("Fish_Jellyfish_Red_Item", 90, 34, true, 2, "default"),
        new FishDefinition("Fish_Jellyfish_Yellow_Item", 90, 34, true, 2, "default"),
        new FishDefinition("Fish_Jellyfish_Man_Of_War_Item", 95, 40, true, 1, "default"),
        new FishDefinition("Fish_Shellfish_Lava_Item", 99, 45, true, 1, "default")
    );

    private FishingRegistry() {
    }

    static boolean isFishingRod(ItemStack stack) {
        if (stack == null || stack.getItemId() == null) {
            return false;
        }
        String id = stack.getItemId().toLowerCase(Locale.ROOT);
        for (String keyword : FISHING_ROD_KEYWORDS) {
            if (id.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    static Optional<BaitStack> findBait(Inventory inventory) {
        if (inventory == null) {
            return Optional.empty();
        }
        ItemContainer container = inventory.getCombinedEverything();
        if (container == null) {
            return Optional.empty();
        }

        BaitStack[] result = new BaitStack[1];
        container.forEach((slot, stack) -> {
            if (result[0] != null || stack == null || stack.getItemId() == null) {
                return;
            }
            String id = stack.getItemId().toLowerCase(Locale.ROOT);
            for (BaitDefinition bait : BAITS) {
                if (id.contains(bait.keyword)) {
                    result[0] = new BaitStack(bait, stack.getItemId());
                    return;
                }
            }
        });

        return Optional.ofNullable(result[0]);
    }

    static List<FishDefinition> getFishForBait(String poolId, int level, boolean rare) {
        List<FishDefinition> result = new ArrayList<>();
        for (FishDefinition fish : FISH) {
            if (fish.minLevel > level) {
                continue;
            }
            if (fish.isRare != rare) {
                continue;
            }
            if (!fish.poolId.equals(poolId)) {
                continue;
            }
            result.add(fish);
        }
        return result;
    }

    static FishDefinition rollFish(int level, BaitDefinition bait) {
        if (bait == null) {
            return null;
        }
        double rareChance = BASE_RARE_CHANCE + (Math.min(level, 99) / 99.0) * RARE_CHANCE_BONUS;
        rareChance *= bait.rareMultiplier;

        boolean rare = ThreadLocalRandom.current().nextDouble() < rareChance;
        List<FishDefinition> candidates = getFishForBait(bait.poolId, level, rare);
        if (candidates.isEmpty() && rare) {
            candidates = getFishForBait(bait.poolId, level, false);
        }
        if (candidates.isEmpty()) {
            return null;
        }
        return weightedPick(candidates);
    }

    private static FishDefinition weightedPick(List<FishDefinition> candidates) {
        int total = 0;
        for (FishDefinition fish : candidates) {
            total += Math.max(1, fish.weight);
        }
        int roll = ThreadLocalRandom.current().nextInt(total);
        int cursor = 0;
        for (FishDefinition fish : candidates) {
            cursor += Math.max(1, fish.weight);
            if (roll < cursor) {
                return fish;
            }
        }
        return candidates.get(0);
    }

    static final class BaitDefinition {
        final String keyword;
        final double speedMultiplier;
        final double rareMultiplier;
        final String poolId;

        BaitDefinition(String keyword, double speedMultiplier, double rareMultiplier, String poolId) {
            this.keyword = keyword;
            this.speedMultiplier = speedMultiplier;
            this.rareMultiplier = rareMultiplier;
            this.poolId = poolId;
        }
    }

    static final class FishDefinition {
        final String itemId;
        final int minLevel;
        final int xp;
        final boolean isRare;
        final int weight;
        final String poolId;

        FishDefinition(String itemId, int minLevel, int xp, boolean isRare, int weight, String poolId) {
            this.itemId = itemId;
            this.minLevel = minLevel;
            this.xp = xp;
            this.isRare = isRare;
            this.weight = weight;
            this.poolId = poolId;
        }
    }

    static final class BaitStack {
        final BaitDefinition definition;
        final String itemId;

        BaitStack(BaitDefinition definition, String itemId) {
            this.definition = definition;
            this.itemId = itemId;
        }
    }
}
