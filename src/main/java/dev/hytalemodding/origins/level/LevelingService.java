package dev.hytalemodding.origins.level;

import dev.hytalemodding.origins.database.LevelRepository;
import dev.hytalemodding.origins.events.LevelUpListener;
import dev.hytalemodding.origins.events.XpGainListener;
import dev.hytalemodding.origins.level.formulas.LevelFormula;
import dev.hytalemodding.origins.playerdata.PlayerLvlData;
import dev.hytalemodding.origins.skills.SkillType;
import dev.hytalemodding.origins.util.XPDropManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LevelingService {

    private static LevelingService instance;

    private final LevelFormula formula;
    private final LevelRepository repository;

    private final Map<UUID, PlayerLvlData> cache = new ConcurrentHashMap<>();

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
     * Returns cached data or loads from the repository if missing.
     */
    private PlayerLvlData get(UUID id) {
        return this.cache.computeIfAbsent(id, uuid -> {
            PlayerLvlData stored = this.repository.load(uuid);
            return stored != null ? stored : new PlayerLvlData(uuid);
        });
    }

    /**
     * Gets the current level for a specific skill (1-99).
     */
    public int getSkillLevel(UUID id, SkillType skill) {
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
        int hp = getSkillLevel(id, SkillType.CONSTITUTION);
        int res = getSkillLevel(id, SkillType.RESTORATION); // Prayer equivalent

        int att = getSkillLevel(id, SkillType.ATTACK);
        int str = getSkillLevel(id, SkillType.STRENGTH);
        int range = getSkillLevel(id, SkillType.RANGED);
        int magic = getSkillLevel(id, SkillType.MAGIC);

        // Base = 0.25 * (Def + HP + Prayer)
        double base = 0.25 * (def + hp + res);

        // Calculate max offense contribution
        double melee = 0.325 * (att + str);
        double ranged = 0.325 * (1.5 * range); // Ranged/Magic are weighted heavier
        double mage = 0.325 * (1.5 * magic);

        double maxOffense = Math.max(melee, Math.max(ranged, mage));

        return (int) (base + maxOffense);
    }

    /**
     * Adds XP to a specific skill. Handles leveling up logic.
     */
    public void addSkillXp(UUID id, SkillType skill, long amount) {
        if (amount <= 0) {
            return;
        }

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
            for (int i = currentLevel + 1; i <= newLevel; i++) {
                int finalI = i;
                this.levelUpListeners.forEach(l -> l.onLevelUp(id, finalI, skill.getDisplayName()));
            }
            data.setSkillLevel(skill, newLevel);
        }

        this.repository.save(data);
        this.xpGainListeners.forEach(l -> l.onXpGain(id, amount, skill));
        float progress = calculateSkillProgress(newXp, newLevel);
        XPDropManager.get().handleXpGain(id, skill.getDisplayName(), amount, progress);
    }

    private float calculateSkillProgress(long currentXp, int currentLevel) {
        if (currentLevel >= 99) return 1.0f;

        long xpForCurrentLevel = this.formula.getXpForLevel(currentLevel);
        long xpForNextLevel = this.formula.getXpForLevel(currentLevel + 1);
        long xpIntoLevel = currentXp - xpForCurrentLevel;
        long xpNeededForLevel = xpForNextLevel - xpForCurrentLevel;

        return (float) xpIntoLevel / (float) xpNeededForLevel;
    }

    /**
     * Manually sets a skill's level (Admin commands / Debug).
     */
    public void setSkillLevel(UUID id, SkillType skill, int level) {
        if (level < 1) {
            level = 1;
        }
        if (level > 99) {
            level = 99;
        }

        PlayerLvlData data = this.get(id);
        long targetXp = this.formula.getXpForLevel(level);

        data.setSkillXp(skill, targetXp);
        data.setSkillLevel(skill, level);

        this.repository.save(data);
    }

    public LevelFormula getLevelFormula() {
        return this.formula;
    }

    /**
     * Saves the player's data to disk and removes it from the cache.
     * Call this on player disconnect.
     */
    public void unload(UUID id) {
        PlayerLvlData data = this.cache.remove(id);
        if (data != null) {
            this.repository.save(data);
        }
    }

    public void registerLevelUpListener(LevelUpListener listener) {
        this.levelUpListeners.add(listener);
    }

    public void registerXpGainListener(XpGainListener listener) {
        this.xpGainListeners.add(listener);
    }
}
