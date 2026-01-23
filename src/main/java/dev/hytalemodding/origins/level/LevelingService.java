package dev.hytalemodding.origins.level;

import dev.hytalemodding.origins.database.LevelRepository;
import dev.hytalemodding.origins.events.LevelUpListener;
import dev.hytalemodding.origins.events.XpGainListener;
import dev.hytalemodding.origins.level.formulas.LevelFormula;
import dev.hytalemodding.origins.playerdata.PlayerLvlData;
import dev.hytalemodding.origins.skills.SkillType;

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

    public LevelingService(LevelFormula formula, LevelRepository repository) {
        this.formula = formula;
        this.repository = repository;
        instance = this;
    }

    public static LevelingService get() {
        return instance;
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

    // --- Public API: SKILLS ---

    /**
     * Gets the current level for a specific skill (1-99).
     */
    public int getSkillLevel(UUID id, SkillType skill) {
        // You will need to update PlayerLvlData to support this method
        // e.g., return tracks.get(skill).getLevel();
        return this.get(id).getSkillLevel(skill);
    }

    /**
     * Gets the current XP for a specific skill.
     */
    public long getSkillXp(UUID id, SkillType skill) {
        return this.get(id).getSkillXp(skill);
    }

    /**
     * Calculates the "Total Level" (Sum of all skill levels).
     */
    public int getTotalLevel(UUID id) {
        int total = 0;
        for (SkillType s : SkillType.values()) {
            total += getSkillLevel(id, s);
        }
        return total;
    }

    /**
     * Calculates the Combat Level using the RuneScape formula.
     */
    public int getCombatLevel(UUID id) {
        int def = getSkillLevel(id, SkillType.DEFENCE);
        int hp  = getSkillLevel(id, SkillType.CONSTITUTION);
        int div = getSkillLevel(id, SkillType.DIVINITY); // Prayer equivalent

        int att = getSkillLevel(id, SkillType.ATTACK);
        int str = getSkillLevel(id, SkillType.STRENGTH);
        int range = getSkillLevel(id, SkillType.RANGED);
        int magic = getSkillLevel(id, SkillType.MAGIC);

        // Base = 0.25 * (Def + HP + Prayer)
        double base = 0.25 * (def + hp + div);

        // Calculate max offense contribution
        double melee = 0.325 * (att + str);
        double ranged = 0.325 * (1.5 * range); // Ranged/Magic are weighted heavier
        double mage   = 0.325 * (1.5 * magic);

        double maxOffense = Math.max(melee, Math.max(ranged, mage));

        return (int) (base + maxOffense);
    }

    /**
     * Adds XP to a specific skill. Handles leveling up logic.
     */
    public void addSkillXp(UUID id, SkillType skill, long amount) {
        if (amount <= 0) return;

        PlayerLvlData data = this.get(id);

        // 1. Get current state
        long currentXp = data.getSkillXp(skill);
        int currentLevel = data.getSkillLevel(skill);

        // 2. Add XP
        data.addSkillXp(skill, amount);

        // 3. Check for Level Up
        long newXp = currentXp + amount;
        int newLevel = this.formula.getLevelForXp(newXp);

        if (newLevel > currentLevel) {
            // Handle multi-level jumps (e.g. huge XP drop)
            for (int i = currentLevel + 1; i <= newLevel; i++) {
                int finalI = i;
                this.levelUpListeners.forEach(l -> l.onLevelUp(id, finalI, skill.getDisplayName()));
            }
            // Update level in data
            data.setSkillLevel(skill, newLevel);
        }

        // 4. Save & Notify
        this.repository.save(data);
        this.xpGainListeners.forEach(l -> l.onXpGain(id, amount, skill)); // Updated listener signature
    }

    /**
     * Manually sets a skill's level (Admin commands / Debug).
     */
    public void setSkillLevel(UUID id, SkillType skill, int level) {
        if (level < 1) level = 1;
        if (level > 99) level = 99; // Or whatever your max is

        PlayerLvlData data = this.get(id);
        long targetXp = this.formula.getXpForLevel(level);

        data.setSkillXp(skill, targetXp);
        data.setSkillLevel(skill, level);

        this.repository.save(data);
    }

    /**
     * Saves the player's data to disk and removes it from the cache.
     * Call this on player disconnect.
     */
    public void unload(UUID id) {
        if (this.cache.containsKey(id)) {
            PlayerLvlData data = this.cache.get(id);

            // Save to Disk
            this.repository.save(data);

            // Remove from Memory
            this.cache.remove(id);
        }
    }

    // --- Listener Registration ---

    public void registerLevelUpListener(LevelUpListener listener) {
        this.levelUpListeners.add(listener);
    }

    public void registerXpGainListener(XpGainListener listener) {
        this.xpGainListeners.add(listener);
    }
}