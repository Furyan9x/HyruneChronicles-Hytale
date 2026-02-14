package dev.hytalemodding.hyrune.registry;

import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Registry for fishing.
 */
public final class FishingRegistry {
    public static final double BITE_SPEED_MAX_BONUS = 0.30;
    public static final double BASE_RARE_CHANCE = 0.02;
    public static final double RARE_CHANCE_BONUS = 0.10;

    public static final List<String> FISHING_ROD_KEYWORDS = List.of(
        "fishing_rod",
        "rod_fishing",
        "tool_rod_crude",
        "rod_crude"
    );

    private static final List<BaitDefinition> BAITS = List.of(
        new BaitDefinition("minnow", 1.0, 1.0, "default")
    );

    private static final List<FishDefinition> FISH = List.of(
            //common
        new FishDefinition("Fish_Minnow_Item", 1, 10, false, 10, "default"),
        new FishDefinition("Fish_Bluegill_Item", 5, 15, false, 10, "default"),
        new FishDefinition("Fish_Catfish_Item", 10, 20, false, 10, "default"),
        //uncommon
        new FishDefinition("Fish_Tang_Blue_Item", 15, 25, false, 9, "default"),
        new FishDefinition("Fish_Tang_Chevron_Item", 15, 27, false, 9, "default"),
        new FishDefinition("Fish_Tang_Lemon_Peel_Item", 15, 29, false, 9, "default"),
        new FishDefinition("Fish_Tang_Sailfin_Item", 15, 31, false, 9, "default"),
        //rare
        new FishDefinition("Fish_Trout_Rainbow_Item", 20, 35, false, 8, "default"),
        new FishDefinition("Fish_Salmon_Item", 25, 45, false, 8, "default"),
        new FishDefinition("Fish_Clownfish_Item", 30, 55, false, 8, "default"),
        new FishDefinition("Fish_Pufferfish_Item", 35, 65, true, 8, "default"),
        //Epic
        new FishDefinition("Fish_Jellyfish_Blue_Item", 50, 90, true, 7, "default"),
        new FishDefinition("Fish_Jellyfish_Cyan_Item", 50, 92, true, 7, "default"),
        new FishDefinition("Fish_Jellyfish_Green_Item", 50, 94, true, 7, "default"),
        new FishDefinition("Fish_Jellyfish_Red_Item", 50, 96, true, 7, "default"),
        new FishDefinition("Fish_Jellyfish_Yellow_Item", 50, 98, true, 7, "default"),
        //legendary
        new FishDefinition("Fish_Pike_Item", 55, 110, false, 6, "default"),
        new FishDefinition("Fish_Crab_Item", 58, 135, false, 6, "default"),
        new FishDefinition("Fish_Lobster_Item", 62, 160, false, 6, "default"),
        new FishDefinition("Fish_Eel_Moray_Item", 65, 190, false, 5, "default"),
        new FishDefinition("Fish_Piranha_Item", 68, 210, false, 5, "default"),
        new FishDefinition("Fish_Trilobite_Item", 70, 250, false, 4, "default"),
        new FishDefinition("Fish_Piranha_Black_Item", 72, 295, false, 4, "default"),
        new FishDefinition("Fish_Trilobite_Black_Item", 75, 350, false, 4, "default"),
        new FishDefinition("Fish_Frostgill_Item", 80, 480, true, 3, "default"),
        new FishDefinition("Fish_Jellyfish_Man_Of_War_Item", 85, 620, true, 1, "default"),
        new FishDefinition("Fish_Snapjaw_Item", 95, 795, true, 1, "default"),
        new FishDefinition("Fish_Shellfish_Lava_Item", 99, 990, true, 1, "default")
    );

    private FishingRegistry() {
    }

    public static boolean isFishingRod(ItemStack stack) {
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

    public static Optional<BaitStack> findBait(Inventory inventory) {
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

    public static List<FishDefinition> getFishForBait(String poolId, int level, boolean rare) {
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

    public static FishDefinition rollFish(int level, BaitDefinition bait) {
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

    public static final class BaitDefinition {
        public final String keyword;
        public final double speedMultiplier;
        public final double rareMultiplier;
        public final String poolId;

        BaitDefinition(String keyword, double speedMultiplier, double rareMultiplier, String poolId) {
            this.keyword = keyword;
            this.speedMultiplier = speedMultiplier;
            this.rareMultiplier = rareMultiplier;
            this.poolId = poolId;
        }
    }

    public static final class FishDefinition {
        public final String itemId;
        public final int minLevel;
        public final int xp;
        public final boolean isRare;
        public final int weight;
        public final String poolId;

        FishDefinition(String itemId, int minLevel, int xp, boolean isRare, int weight, String poolId) {
            this.itemId = itemId;
            this.minLevel = minLevel;
            this.xp = xp;
            this.isRare = isRare;
            this.weight = weight;
            this.poolId = poolId;
        }
    }

    public static final class BaitStack {
        public final BaitDefinition definition;
        public final String itemId;

        BaitStack(BaitDefinition definition, String itemId) {
            this.definition = definition;
            this.itemId = itemId;
        }
    }
}
