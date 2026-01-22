package dev.hytalemodding.origins.level;

import dev.hytalemodding.origins.classes.Classes;
import dev.hytalemodding.origins.database.LevelRepository;
import dev.hytalemodding.origins.events.LevelUpListener;
import dev.hytalemodding.origins.events.XpGainListener;
import dev.hytalemodding.origins.level.formulas.LevelFormula;
import dev.hytalemodding.origins.playerdata.ExperienceTrack;
import dev.hytalemodding.origins.playerdata.PlayerLvlData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LevelingService {
    private static LevelingService instance;
    // Core Dependencies
    private final LevelFormula formula;
    private final LevelRepository repository;

    // 1. The Cache
    private final Map<UUID, PlayerLvlData> cache = new ConcurrentHashMap<>();

    // 2. The Listeners
    private final List<LevelUpListener> levelUpListeners = new ArrayList<>();
    private final List<XpGainListener> xpGainListeners = new ArrayList<>();

    public enum ClassChangeResult {
        SUCCESS,
        INVALID_CLASS,
        WRONG_PARENT,
        LEVEL_TOO_LOW,
        ALREADY_THIS_CLASS
    }
    public LevelingService(LevelFormula formula, LevelRepository repository) {
        this.formula = formula;
        this.repository = repository;
        instance = this;
    }

    /**
     * Internal helper: Get data from Cache first, then Disk.
     */
    private PlayerLvlData get(UUID id) {
        return this.cache.computeIfAbsent(id, (uuid) -> {
            PlayerLvlData stored = this.repository.load(uuid);
            return stored != null ? stored : new PlayerLvlData(uuid);
        });
    }
    public static LevelingService get() {
        return instance;
    }
    // --- Public API ---

    public int getAdventurerLevel(UUID id) {
        return this.get(id).getAdventurerLevel();
    }

    public int getClassLevel(UUID id, String classId) {
        PlayerLvlData data = this.get(id);
        // FIX: Grab the specific track for the requested class ID
        ExperienceTrack track = data.getClassTrack(classId);
        return (track != null) ? track.getLevel() : 1;
    }

    public String getActiveClassId(UUID id) {
        return this.get(id).getActiveClassId();
    }

    /**
     * Helper: Returns XP of the "Main Focus" (Class if active, otherwise Global)
     */
    public long getCombatXp(UUID id) {
        PlayerLvlData data = this.get(id);
        ExperienceTrack track = data.getActiveTrack();
        return (track != null) ? track.getXp() : data.getAdventurerXp();
    }

    /**
     * Helper: Returns Level of the "Main Focus" (Class if active, otherwise Global)
     */
    public int getCombatLevel(UUID id) {
        PlayerLvlData data = this.get(id);
        ExperienceTrack track = data.getActiveTrack();
        return (track != null) ? track.getLevel() : data.getAdventurerLevel();
    }

    /**
     * Adds XP to Global (always) and Class (if active).
     */
    public void addCombatXp(UUID id, long amount) {
        if (amount <= 0) return;

        PlayerLvlData data = this.get(id);

        // 1. ALWAYS Update Adventurer (Global) Level
        data.addAdventurerXp(amount);
        int oldAdvLevel = data.getAdventurerLevel();

        int newAdvLevel = this.formula.getLevelForXp(data.getAdventurerXp());
        if (newAdvLevel > oldAdvLevel) {
            data.setAdventurerLevel(newAdvLevel);
            this.levelUpListeners.forEach(l -> l.onLevelUp(id, newAdvLevel, "Global"));
        }

        // 2. CONDITIONALLY Update Class Level (if one is active)
        if (data.getActiveClassId() != null) {
            ExperienceTrack classTrack = data.getActiveTrack();
            int oldClassLevel = classTrack.getLevel();

            classTrack.addXp(amount);

            int newClassLevel = this.formula.getLevelForXp(classTrack.getXp());
            if (newClassLevel > oldClassLevel) {
                for (int lvl = oldClassLevel + 1; lvl <= newClassLevel; lvl++) {

                    // Fire the listener for EACH level
                    // This ensures the AttributeListener runs 3 times if you gain 3 levels
                    int currentStep = lvl;
                    String sourceName = data.getActiveClassId();
                    this.levelUpListeners.forEach(l -> l.onLevelUp(id, currentStep, sourceName));
                }

                // Finally, save the new level state
                classTrack.setLevel(newClassLevel);
            }
        }

        this.repository.save(data);
        this.xpGainListeners.forEach(l -> l.onXpGain(id, amount));
    }

    /**
     * Manually set a level.
     * Behavior: If Class is Active -> Sets Class Level.
     * If No Class Active -> Sets Global Adventurer Level.
     */
    public void setCombatLevel(UUID id, int level) {
        if (level < 1) level = 1;

        PlayerLvlData data = this.get(id);
        long targetXp = this.formula.getXpForLevel(level);

        int oldLevel;

        // Mode: Class Leveling
        if (data.getActiveClassId() != null) {
            ExperienceTrack activeTrack = data.getActiveTrack();
            oldLevel = activeTrack.getLevel();

            activeTrack.addXp(targetXp - activeTrack.getXp()); // Adjust XP
            activeTrack.setLevel(level);
        }
        // Mode: Global Leveling
        else {
            oldLevel = data.getAdventurerLevel();
            data.setAdventurerXp(targetXp);
            data.setAdventurerLevel(level);
        }

        this.repository.save(data);

        final int finalLevel = level;
        if (finalLevel > oldLevel) {
            String source = (data.getActiveClassId() != null) ? data.getActiveClassId() : "Global";
            this.levelUpListeners.forEach(l -> l.onLevelUp(id, finalLevel, source));
        }
    }

    /**
     * Attempts to change the active class for a player.
     * Includes logic for Tier 1 (Adventurer level) and Tier 2 (Parent class level) checks.
     * 
     * @param id The UUID of the player.
     * @param classId The ID of the class to switch to.
     * @return The result of the class change attempt.
     */
    public ClassChangeResult changeClass(UUID id, String classId) {
        PlayerLvlData data = this.get(id);

        // Handle "Reset" or "Adventurer" request
        if (classId.equalsIgnoreCase("adventurer") || classId.equalsIgnoreCase("none")) {
            data.setActiveClassId(null);
            this.repository.save(data);
            return ClassChangeResult.SUCCESS;
        }

        Classes targetClass = Classes.fromId(classId);
        if (targetClass == null) return ClassChangeResult.INVALID_CLASS;

        // Tier 1 Check: Requires a minimum global Adventurer level
        if (targetClass.getParent() == null) {
            if (data.getAdventurerLevel() < targetClass.getRequiredLevel()) {
                return ClassChangeResult.LEVEL_TOO_LOW;
            }
        }
        // Tier 2 Check: Requires a minimum level in a specific parent class
        else {
            String currentClassId = data.getActiveClassId();

            if (currentClassId == null || !currentClassId.equalsIgnoreCase(targetClass.getParent().getId())) {
                return ClassChangeResult.WRONG_PARENT;
            }

            ExperienceTrack activeTrack = data.getActiveTrack();
            if (activeTrack.getLevel() < targetClass.getRequiredLevel()) {
                return ClassChangeResult.LEVEL_TOO_LOW;
            }
        }

        // Apply Change
        data.setActiveClassId(targetClass.getId());
        data.getClassTrack(targetClass.getId()); // Initialize track if new
        this.repository.save(data);

        return ClassChangeResult.SUCCESS;
    }

    // --- Listener Registration ---

    public void registerLevelUpListener(LevelUpListener listener) {
        this.levelUpListeners.add(listener);
    }

    public void registerXpGainListener(XpGainListener listener) {
        this.xpGainListeners.add(listener);
    }

}