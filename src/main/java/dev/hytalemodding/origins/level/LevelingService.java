package dev.hytalemodding.origins.level;

import com.hypixel.hytale.logger.HytaleLogger;
import dev.hytalemodding.origins.database.LevelRepository;
import dev.hytalemodding.origins.events.LevelUpListener;
import dev.hytalemodding.origins.events.XpGainListener;
import dev.hytalemodding.origins.level.formulas.LevelFormula;
import dev.hytalemodding.origins.playerdata.PlayerLvlData;
import dev.hytalemodding.origins.skills.SkillType;
import dev.hytalemodding.origins.util.XPDropManager;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Service for managing player skill experience, levels, and persistence.
 */
public class LevelingService {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final int MIN_LEVEL = 1;
    private static final int MAX_LEVEL = 99;

    private static final EnumSet<CombatXpStyle> MELEE_XP_STYLES = EnumSet.of(
        CombatXpStyle.ATTACK, CombatXpStyle.STRENGTH, CombatXpStyle.DEFENCE, CombatXpStyle.SHARED
    );
    private static final EnumSet<CombatXpStyle> RANGED_XP_STYLES = EnumSet.of(
        CombatXpStyle.RANGED, CombatXpStyle.DEFENCE, CombatXpStyle.SHARED
    );
    private static final EnumSet<CombatXpStyle> MAGIC_XP_STYLES = EnumSet.of(
        CombatXpStyle.MAGIC, CombatXpStyle.DEFENCE, CombatXpStyle.SHARED
    );

    private static LevelingService instance;

    private final LevelFormula formula;
    private final LevelRepository repository;

    private final Map<UUID, PlayerLvlData> cache = new ConcurrentHashMap<>();

    private final List<LevelUpListener> levelUpListeners = new ArrayList<>();
    private final List<XpGainListener> xpGainListeners = new ArrayList<>();

    public LevelingService(LevelFormula formula, LevelRepository repository) {
        this.formula = Objects.requireNonNull(formula, "formula");
        this.repository = Objects.requireNonNull(repository, "repository");
        instance = this;
    }

    public static LevelingService get() {
        return instance;
    }

    /**
     * Preloads and caches player data if available.
     */
    public void load(UUID id) {
        if (id == null) {
            return;
        }
        cache.computeIfAbsent(id, this::loadOrCreate);
    }

    /**
     * Returns cached data or loads from the repository if missing.
     */
    private PlayerLvlData getOrCreate(UUID id) {
        Objects.requireNonNull(id, "id");
        return this.cache.computeIfAbsent(id, this::loadOrCreate);
    }

    /**
     * Gets the current level for a specific skill.
     */
    public int getSkillLevel(UUID id, SkillType skill) {
        if (skill == null) {
            return MIN_LEVEL;
        }
        return this.getOrCreate(id).getSkillLevel(skill);
    }

    /**
     * Gets the current XP for a specific skill.
     */
    public long getSkillXp(UUID id, SkillType skill) {
        if (skill == null) {
            return 0L;
        }
        return this.getOrCreate(id).getSkillXp(skill);
    }

    public CombatXpStyle getMeleeXpStyle(UUID id) {
        PlayerLvlData data = this.getOrCreate(id);
        CombatXpStyle style = data.getMeleeXpStyle();
        if (!MELEE_XP_STYLES.contains(style)) {
            style = CombatXpStyle.ATTACK;
            data.setMeleeXpStyle(style);
            persist(data);
        }
        return style;
    }

    public CombatXpStyle getRangedXpStyle(UUID id) {
        PlayerLvlData data = this.getOrCreate(id);
        CombatXpStyle style = data.getRangedXpStyle();
        if (!RANGED_XP_STYLES.contains(style)) {
            style = CombatXpStyle.RANGED;
            data.setRangedXpStyle(style);
            persist(data);
        }
        return style;
    }

    public CombatXpStyle getMagicXpStyle(UUID id) {
        PlayerLvlData data = this.getOrCreate(id);
        CombatXpStyle style = data.getMagicXpStyle();
        if (!MAGIC_XP_STYLES.contains(style)) {
            style = CombatXpStyle.MAGIC;
            data.setMagicXpStyle(style);
            persist(data);
        }
        return style;
    }

    public void setMeleeXpStyle(UUID id, CombatXpStyle style) {
        if (id == null) {
            return;
        }
        CombatXpStyle resolved = MELEE_XP_STYLES.contains(style) ? style : CombatXpStyle.ATTACK;
        PlayerLvlData data = this.getOrCreate(id);
        if (data.getMeleeXpStyle() != resolved) {
            data.setMeleeXpStyle(resolved);
            persist(data);
        }
    }

    public void setRangedXpStyle(UUID id, CombatXpStyle style) {
        if (id == null) {
            return;
        }
        CombatXpStyle resolved = RANGED_XP_STYLES.contains(style) ? style : CombatXpStyle.RANGED;
        PlayerLvlData data = this.getOrCreate(id);
        if (data.getRangedXpStyle() != resolved) {
            data.setRangedXpStyle(resolved);
            persist(data);
        }
    }

    public void setMagicXpStyle(UUID id, CombatXpStyle style) {
        if (id == null) {
            return;
        }
        CombatXpStyle resolved = MAGIC_XP_STYLES.contains(style) ? style : CombatXpStyle.MAGIC;
        PlayerLvlData data = this.getOrCreate(id);
        if (data.getMagicXpStyle() != resolved) {
            data.setMagicXpStyle(resolved);
            persist(data);
        }
    }

    /**
     * Calculates the total level across all skills.
     */
    public int getTotalLevel(UUID id) {
        int total = 0;
        for (SkillType s : SkillType.values()) {
            total += getSkillLevel(id, s);
        }
        return total;
    }

    /**
     * Calculates the combat level using the RuneScape formula.
     */
    public int getCombatLevel(UUID id) {
        int def = getSkillLevel(id, SkillType.DEFENCE);
        int hp = getSkillLevel(id, SkillType.CONSTITUTION);
        int res = getSkillLevel(id, SkillType.RESTORATION);

        int att = getSkillLevel(id, SkillType.ATTACK);
        int str = getSkillLevel(id, SkillType.STRENGTH);
        int range = getSkillLevel(id, SkillType.RANGED);
        int magic = getSkillLevel(id, SkillType.MAGIC);

        double base = 0.25 * (def + hp + res);
        double melee = 0.325 * (att + str);
        double ranged = 0.325 * (1.5 * range);
        double mage = 0.325 * (1.5 * magic);

        double maxOffense = Math.max(melee, Math.max(ranged, mage));
        return (int) (base + maxOffense);
    }

    /**
     * Adds XP to a specific skill and handles leveling up.
     */
    public void addSkillXp(UUID id, SkillType skill, long amount) {
        if (id == null || skill == null || amount <= 0) {
            return;
        }

        PlayerLvlData data = this.getOrCreate(id);
        long currentXp = data.getSkillXp(skill);
        int currentLevel = data.getSkillLevel(skill);

        data.addSkillXp(skill, amount);

        long newXp = currentXp + amount;
        int newLevel = this.formula.getLevelForXp(newXp);

        if (newLevel > currentLevel) {
            for (int i = currentLevel + 1; i <= newLevel; i++) {
                int level = i;
                this.levelUpListeners.forEach(l -> l.onLevelUp(id, level, skill.getDisplayName()));
            }
            data.setSkillLevel(skill, newLevel);
        }

        persist(data);
        this.xpGainListeners.forEach(l -> l.onXpGain(id, amount, skill));
        float progress = calculateSkillProgress(newXp, newLevel);
        XPDropManager.get().handleXpGain(id, skill.getDisplayName(), amount, progress);
    }

    /**
     * Manually sets a skill's level (admin commands / debug).
     */
    public void setSkillLevel(UUID id, SkillType skill, int level) {
        if (id == null || skill == null) {
            return;
        }
        int boundedLevel = Math.min(MAX_LEVEL, Math.max(MIN_LEVEL, level));

        PlayerLvlData data = this.getOrCreate(id);
        long targetXp = this.formula.getXpForLevel(boundedLevel);

        data.setSkillXp(skill, targetXp);
        data.setSkillLevel(skill, boundedLevel);

        persist(data);
    }

    public LevelFormula getLevelFormula() {
        return this.formula;
    }

    /**
     * Saves the player's data to disk and removes it from the cache.
     * Call this on player disconnect.
     */
    public void unload(UUID id) {
        if (id == null) {
            return;
        }
        PlayerLvlData data = this.cache.remove(id);
        if (data != null) {
            persist(data);
        }
    }

    public void registerLevelUpListener(LevelUpListener listener) {
        if (listener != null) {
            this.levelUpListeners.add(listener);
        }
    }

    public void registerXpGainListener(XpGainListener listener) {
        if (listener != null) {
            this.xpGainListeners.add(listener);
        }
    }

    private PlayerLvlData loadOrCreate(UUID uuid) {
        PlayerLvlData stored = this.repository.load(uuid);
        return stored != null ? stored : new PlayerLvlData(uuid);
    }

    private float calculateSkillProgress(long currentXp, int currentLevel) {
        if (currentLevel >= MAX_LEVEL) {
            return 1.0f;
        }

        long xpForCurrentLevel = this.formula.getXpForLevel(currentLevel);
        long xpForNextLevel = this.formula.getXpForLevel(currentLevel + 1);
        long xpIntoLevel = currentXp - xpForCurrentLevel;
        long xpNeededForLevel = xpForNextLevel - xpForCurrentLevel;

        if (xpNeededForLevel <= 0) {
            return 1.0f;
        }

        return (float) xpIntoLevel / (float) xpNeededForLevel;
    }

    private void persist(PlayerLvlData data) {
        if (data == null) {
            return;
        }
        try {
            repository.save(data);
        } catch (RuntimeException e) {
            LOGGER.at(Level.WARNING)
                .log("Failed to save level data for " + (data != null ? data.getUuid() : "unknown") + ": " + e.getMessage());
        }
    }
}
