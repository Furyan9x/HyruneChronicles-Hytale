package dev.hytalemodding.hyrune.slayer;


import java.util.List;

/**
 * Builds default slayer data.
 */
public class SlayerDataInitializer {

    public static SlayerTaskRegistry buildRegistry() {
        SlayerTaskRegistry registry = new SlayerTaskRegistry();

        // TIER 1: Vermin & Insects (Groups from NpcLevelConfig)
        registry.registerTier(new SlayerTaskTier(1, 19, List.of(
                new SlayerTaskDefinition("t1_vermin", "vermin", 8, 14),
                new SlayerTaskDefinition("t1_beasts", "beasts", 6, 12)
        )));

        // TIER 2: Wildlife & Outlaws
        registry.registerTier(new SlayerTaskTier(20, 39, List.of(
                new SlayerTaskDefinition("t2_beasts", "beasts", 22, 30),
                new SlayerTaskDefinition("t2_preds", "predators", 18, 26)
        )));

        // TIER 3: Undead & Beasts
        registry.registerTier(new SlayerTaskTier(40, 59, List.of(
                new SlayerTaskDefinition("t3_preds", "predators", 35, 50),
                new SlayerTaskDefinition("t3_outlaws", "outlaws", 21, 33),
                new SlayerTaskDefinition("t3_brutes", "brutes", 16, 54)
        )));

        // TIER 4: Elites
        registry.registerTier(new SlayerTaskTier(60, 79, List.of(
                new SlayerTaskDefinition("t4_outlaws", "outlaws", 53, 80),
                new SlayerTaskDefinition("t4_undead", "undead", 44, 62)
        )));

        return registry;
    }
}
