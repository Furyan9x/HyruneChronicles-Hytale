package dev.hytalemodding.hyrune.itemization;

/**
 * High-level itemized stat grouping used for pool biasing and summary rollups.
 */
public enum ItemizedStatFamily {
    OFFENSE_PHYSICAL("offense_physical"),
    OFFENSE_MAGICAL("offense_magical"),
    DEFENSE_PHYSICAL("defense_physical"),
    DEFENSE_MAGICAL("defense_magical"),
    DEFENSE_CORE("defense_core"),
    HEALING("healing"),
    UTILITY("utility");

    private final String id;

    ItemizedStatFamily(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
