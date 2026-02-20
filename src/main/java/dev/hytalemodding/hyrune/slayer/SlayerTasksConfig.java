package dev.hytalemodding.hyrune.slayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Config file model for Slayer task definitions.
 */
public class SlayerTasksConfig {
    public int schemaVersion = 1;
    public List<Task> tasks = new ArrayList<>();

    public static class Task {
        public String id;
        public String targetFamilyId;
        public int requiredSlayerLevel = 1;
        public int requiredCombatLevel = 1;
    }
}

