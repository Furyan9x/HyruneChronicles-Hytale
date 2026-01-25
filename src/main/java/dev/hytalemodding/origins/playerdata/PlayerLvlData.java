package dev.hytalemodding.origins.playerdata;

import dev.hytalemodding.origins.skills.SkillType;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public class PlayerLvlData {
    private final UUID uuid;

    // Unified storage for ALL skills (Combat, Gathering, Artisan)
    // EnumMap is faster and more memory-efficient than HashMap for Enum keys.
    private final Map<SkillType, ExperienceTrack> skillTracks;

    // --- CONSTRUCTORS ---

    public PlayerLvlData(UUID uuid) {
        this.uuid = uuid;
        this.skillTracks = new EnumMap<>(SkillType.class);
    }

    // --- CORE ACCESSORS ---

    public UUID getUuid() {
        return uuid;
    }

    /**
     * Retrieves the specific track for a skill.
     * Automatically creates a new blank track (Lvl 1, 0 XP) if one doesn't exist yet.
     */
    private ExperienceTrack getTrack(SkillType skill) {
        return this.skillTracks.computeIfAbsent(skill, k -> new ExperienceTrack());
    }

    // --- SKILL METHODS (Used by LevelingService) ---

    public int getSkillLevel(SkillType skill) {
        return getTrack(skill).getLevel();
    }

    public long getSkillXp(SkillType skill) {
        return getTrack(skill).getXp();
    }

    public void setSkillLevel(SkillType skill, int level) {
        getTrack(skill).setLevel(level);
    }

    public void setSkillXp(SkillType skill, long xp) {
        getTrack(skill).setXp(xp);
    }

    public void addSkillXp(SkillType skill, long amount) {
        getTrack(skill).addXp(amount);
    }

    // --- PERSISTENCE HELPER ---
    // Useful if your JSON serializer needs direct access to the map
    public Map<SkillType, ExperienceTrack> getAllTracks() {
        return this.skillTracks;
    }

}
