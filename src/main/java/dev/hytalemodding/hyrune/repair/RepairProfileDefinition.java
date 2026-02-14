package dev.hytalemodding.hyrune.repair;

/**
 * Serializable repair profile definition.
 */
public class RepairProfileDefinition {
    public String keyword;
    public String matchType = "contains";
    public int priority = 0;
    public String primaryMaterial;
    public String secondaryMaterial;
    public String rareCatalystMaterial;
    public int primaryBaseCost = 6;
    public int secondaryBaseCost = 4;
    public int catalystBaseCost = 1;
}
