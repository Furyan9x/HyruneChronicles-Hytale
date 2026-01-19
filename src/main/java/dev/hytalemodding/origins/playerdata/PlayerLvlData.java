package dev.hytalemodding.origins.playerdata;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerLvlData {
    private final UUID uuid;

    // --- GLOBAL / ADVENTURER STATS ---
    // These are your base stats that never reset.
    private long adventurerXp;
    private int adventurerLevel;

    // --- SPECIALIZATION STATS ---
    // Currently active class (e.g., "warrior"). Null if no specialization chosen yet.
    private String activeClassId;

    // Stores progress for specialized classes (Warrior, Mage, etc.)
    private Map<String, ExperienceTrack> classProgress;

    // Stores progress for professions (Mining, Smithing, etc.) - Added for future-proofing
    private Map<String, ExperienceTrack> professionProgress;

    // --- CONSTRUCTORS ---

    // 1. New Player Constructor
    public PlayerLvlData(UUID uuid) {
        this.uuid = uuid;
        this.adventurerXp = 0;
        this.adventurerLevel = 1;
        this.activeClassId = null; // null means "Just an Adventurer"
        this.classProgress = new HashMap<>();
        this.professionProgress = new HashMap<>();
    }

    // --- ACCESSORS ---

    public UUID getUuid() { return uuid; }

    // Core Adventurer Methods
    public void addAdventurerXp(long amount) { this.adventurerXp += amount; }
    public long getAdventurerXp() { return adventurerXp; }
    public void setAdventurerXp(long xp) { this.adventurerXp = xp; }

    public int getAdventurerLevel() { return adventurerLevel; }
    public void setAdventurerLevel(int level) { this.adventurerLevel = level; }

    // Specialization Methods
    public String getActiveClassId() { return activeClassId; }

    public void setActiveClassId(String id) {
        // Normalize to lowercase to prevent "Warrior" vs "warrior" bugs
        this.activeClassId = (id != null) ? id.toLowerCase() : null;
    }

    // Safer "Get Track" - Ensures map is never null
    public ExperienceTrack getClassTrack(String classId) {
        if (this.classProgress == null) this.classProgress = new HashMap<>();
        return this.classProgress.computeIfAbsent(classId.toLowerCase(), k -> new ExperienceTrack());
    }

    // Helper: Get the track for the currently active class (returns null if just Adventurer)
    public ExperienceTrack getActiveTrack() {
        if (activeClassId == null) return null;
        return getClassTrack(activeClassId);
    }

    // Profession Accessor (Future Proofing)
    public ExperienceTrack getProfessionTrack(String professionId) {
        if (this.professionProgress == null) this.professionProgress = new HashMap<>();
        return this.professionProgress.computeIfAbsent(professionId.toLowerCase(), k -> new ExperienceTrack());
    }
}