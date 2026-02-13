package dev.hytalemodding.origins.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Runtime config for Origins gameplay switches and requirement maps.
 */
public class OriginsConfig {
    public boolean durabilityDebugLogging = true;
    public boolean enableAnimalHusbandryGating = false;
    public Map<String, Integer> farmingSeedLevelRequirements = defaultSeedRequirements();
    public Map<String, Integer> farmingAnimalLevelRequirements = defaultAnimalRequirements();
    public Map<String, String> npcNameOverrides = new LinkedHashMap<>();

    private static Map<String, Integer> defaultSeedRequirements() {
        Map<String, Integer> defaults = new LinkedHashMap<>();
        defaults.put("Plant_Seeds_Wheat", 1);
        defaults.put("Plant_Seeds_Lettuce", 10);
        defaults.put("Plant_Seeds_Carrots", 20);
        return defaults;
    }

    private static Map<String, Integer> defaultAnimalRequirements() {
        Map<String, Integer> defaults = new LinkedHashMap<>();
        defaults.put("chicken", 1);
        defaults.put("pig", 10);
        defaults.put("cow", 20);
        return defaults;
    }
}
