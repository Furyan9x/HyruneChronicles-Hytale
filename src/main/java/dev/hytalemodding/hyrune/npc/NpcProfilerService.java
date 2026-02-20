package dev.hytalemodding.hyrune.npc;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import dev.hytalemodding.Hyrune;
import dev.hytalemodding.hyrune.config.HyruneConfig;
import dev.hytalemodding.hyrune.config.HyruneConfigManager;
import dev.hytalemodding.hyrune.ui.NpcProfilerPage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Builds and opens the NPC profiler UI from either pick targeting or item interactions.
 */
public final class NpcProfilerService {
    private static final String HEALTH_STAT_ID = "Health";
    private static final String HEALTH_MODIFIER_ID = "hyrune:npc_health_bonus";
    private static final float DEFAULT_NPC_HEALTH_REGEN_PER_LEVEL = 20.0f / 99.0f;
    private static final float DEFAULT_NPC_HEALTH_REGEN_CAP_PER_SEC = 20.0f;
    private static final float DEFAULT_BOSS_HEALTH_REGEN_PER_LEVEL = 1000.0f / 99.0f;
    private static final float DEFAULT_BOSS_HEALTH_REGEN_CAP_PER_SEC = 1000.0f;

    private NpcProfilerService() {
    }

    public static boolean tryOpenNpcProfiler(@Nonnull PlayerRef sender,
                                             @Nonnull Ref<EntityStore> senderRef,
                                             @Nonnull Store<EntityStore> senderStore,
                                             @Nonnull Player senderPlayer,
                                             boolean notifyOnFailure) {
        Ref<EntityStore> targetRef = TargetUtil.getTargetEntity(senderRef, senderStore);
        if (targetRef == null || !targetRef.isValid()) {
            if (notifyOnFailure) {
                sender.sendMessage(Message.raw("No target found for NPC profiling."));
            }
            return false;
        }

        NpcProfilerSnapshot snapshot = buildSnapshot(targetRef);
        if (snapshot == null) {
            if (notifyOnFailure) {
                sender.sendMessage(Message.raw("Target is not a profiled NPC."));
            }
            return false;
        }

        senderPlayer.getPageManager().openCustomPage(senderRef, senderStore, new NpcProfilerPage(sender, targetRef, snapshot));
        return true;
    }

    @Nullable
    public static NpcProfilerSnapshot buildSnapshot(@Nonnull Ref<EntityStore> targetRef) {
        if (!targetRef.isValid()) {
            return null;
        }

        Store<EntityStore> targetStore = targetRef.getStore();
        NPCEntity npc = targetStore.getComponent(targetRef, NPCEntity.getComponentType());
        if (npc == null) {
            return null;
        }

        NpcLevelService levelService = Hyrune.getNpcLevelService();
        NpcLevelComponent level = targetStore.getComponent(targetRef, Hyrune.getNpcLevelComponentType());
        EntityStatMap statMap = targetStore.getComponent(targetRef, EntityStatMap.getComponentType());

        List<ProfilerSection> sections = new ArrayList<>();
        sections.add(buildIdentitySection(targetStore, targetRef, npc, level, levelService));
        sections.add(buildCombatSection(level, levelService));
        sections.add(buildRuntimeSection(level, statMap));
        sections.add(buildStatMapDumpSection(statMap));

        String title = resolveDisplayName(targetStore, targetRef, npc, level);
        String subtitle = "NPC Profiler";
        return new NpcProfilerSnapshot(title, subtitle, sections);
    }

    private static ProfilerSection buildIdentitySection(Store<EntityStore> store,
                                                        Ref<EntityStore> targetRef,
                                                        NPCEntity npc,
                                                        NpcLevelComponent level,
                                                        NpcLevelService levelService) {
        List<ProfilerRow> rows = new ArrayList<>();
        String npcTypeId = npc.getNPCTypeId() == null ? "unknown" : npc.getNPCTypeId();
        rows.add(new ProfilerRow("Name", resolveDisplayName(store, targetRef, npc, level)));
        rows.add(new ProfilerRow("Type", npcTypeId));
        rows.add(new ProfilerRow("Family", level == null ? "none" : fallback(level.getGroupId(), "none")));
        rows.add(new ProfilerRow("Level", level == null ? "n/a" : String.valueOf(level.getLevel())));
        rows.add(new ProfilerRow("Rank", level == null ? "NORMAL" : fallback(level.getRankId(), "NORMAL")));
        rows.add(new ProfilerRow("Archetype", level == null ? "DPS" : fallback(level.getArchetypeId(), "DPS")));
        rows.add(new ProfilerRow("Elite", level != null && level.isElite() ? "Yes" : "No"));
        rows.add(new ProfilerRow("Weakness", resolveWeakness(level, levelService)));
        if (levelService != null) {
            rows.add(new ProfilerRow("Weakness Multiplier", "x" + fmt(levelService.getWeaknessMultiplier(), 3)));
            rows.add(new ProfilerRow("Resistance Multiplier", "x" + fmt(levelService.getResistanceMultiplier(), 3)));
            NpcFamiliesConfig.NpcFamilyDefinition family = level == null ? null : levelService.getFamilyDefinition(level.getGroupId());
            if (family != null) {
                rows.add(new ProfilerRow("Family Tags", csv(family.tags)));
                rows.add(new ProfilerRow("Family Type IDs", csv(family.typeIds)));
                rows.add(new ProfilerRow("Family Role Paths", csv(family.rolePaths)));
            }
        }
        return new ProfilerSection("Identity", rows);
    }

    private static ProfilerSection buildCombatSection(NpcLevelComponent level, NpcLevelService levelService) {
        List<ProfilerRow> rows = new ArrayList<>();
        if (levelService == null) {
            rows.add(new ProfilerRow("Status", "NpcLevelService unavailable"));
            return new ProfilerSection("Combat", rows);
        }

        NpcLevelService.NpcCombatStats melee = levelService.resolveCombatStats(level, CombatStyle.MELEE);
        NpcLevelService.NpcCombatStats ranged = levelService.resolveCombatStats(level, CombatStyle.RANGED);
        NpcLevelService.NpcCombatStats magic = levelService.resolveCombatStats(level, CombatStyle.MAGIC);

        rows.add(new ProfilerRow("Base Damage Multiplier (Melee)", "x" + fmt(melee.damageMultiplier(), 3)));
        rows.add(new ProfilerRow("Base Damage Multiplier (Ranged)", "x" + fmt(ranged.damageMultiplier(), 3)));
        rows.add(new ProfilerRow("Base Damage Multiplier (Magic)", "x" + fmt(magic.damageMultiplier(), 3)));
        rows.add(new ProfilerRow("Defence Mitigation", pct(melee.defenceReduction())));
        rows.add(new ProfilerRow("Crit Chance", pct(melee.critChance())));
        rows.add(new ProfilerRow("Crit Multiplier", "x" + fmt(melee.critMultiplier(), 3)));
        rows.add(new ProfilerRow("Health Multiplier", "x" + fmt(melee.healthMultiplier(), 3)));

        NpcFamiliesConfig.NpcArchetypeProfile archetypeProfile = levelService.getArchetypeProfile(melee.archetypeId());
        if (archetypeProfile != null) {
            rows.add(new ProfilerRow("Melee Bias", "x" + fmt(archetypeProfile.meleeBias, 3)));
            rows.add(new ProfilerRow("Ranged Bias", "x" + fmt(archetypeProfile.rangedBias, 3)));
            rows.add(new ProfilerRow("Magic Bias", "x" + fmt(archetypeProfile.magicBias, 3)));
            rows.add(new ProfilerRow("Damage Growth", "x" + fmt(archetypeProfile.damageGrowthRate, 3) + " / level"));
            rows.add(new ProfilerRow("Defence Growth", "x" + fmt(archetypeProfile.defenceGrowthRate, 3) + " / level"));
            rows.add(new ProfilerRow("Crit Growth", "x" + fmt(archetypeProfile.critGrowthRate, 3) + " / level"));
        }

        NpcFamiliesConfig.NpcRankProfile rankProfile = levelService.getRankProfile(melee.rankId());
        if (rankProfile != null) {
            rows.add(new ProfilerRow("Rank Level Offset", String.valueOf(rankProfile.levelOffset)));
            rows.add(new ProfilerRow("Rank Stat Multiplier", "x" + fmt(rankProfile.statMultiplier, 3)));
        }

        return new ProfilerSection("Combat", rows);
    }

    private static ProfilerSection buildRuntimeSection(NpcLevelComponent level, EntityStatMap statMap) {
        List<ProfilerRow> rows = new ArrayList<>();
        if (statMap == null) {
            rows.add(new ProfilerRow("Status", "EntityStatMap unavailable"));
            return new ProfilerSection("Runtime", rows);
        }

        EntityStatValue health = statMap.get(HEALTH_STAT_ID);
        if (health != null) {
            rows.add(new ProfilerRow("Current HP", fmt(health.get(), 1)));
            rows.add(new ProfilerRow("Max HP", fmt(health.getMax(), 1)));
            rows.add(new ProfilerRow("HP %", pct(health.asPercentage())));
            Modifier healthModifier = statMap.getModifier(health.getIndex(), HEALTH_MODIFIER_ID);
            if (healthModifier instanceof StaticModifier staticModifier) {
                rows.add(new ProfilerRow("Applied HP Max Bonus", fmt(staticModifier.getAmount(), 1)));
            }
        }

        RegenStats regen = resolveNpcRegen(level);
        rows.add(new ProfilerRow("Configured HP Regen/s", fmt(regen.configuredPerSecond(), 3)));
        rows.add(new ProfilerRow("Regen Cap/s", fmt(regen.capPerSecond(), 3)));
        rows.add(new ProfilerRow("Effective HP Regen/s", fmt(regen.effectivePerSecond(), 3)));

        return new ProfilerSection("Runtime", rows);
    }

    private static ProfilerSection buildStatMapDumpSection(EntityStatMap statMap) {
        List<ProfilerRow> rows = new ArrayList<>();
        if (statMap == null) {
            rows.add(new ProfilerRow("Status", "EntityStatMap unavailable"));
            return new ProfilerSection("Runtime Stat Map", rows);
        }

        for (int i = 0; i < statMap.size(); i++) {
            EntityStatValue value = statMap.get(i);
            if (value == null) {
                continue;
            }
            String label = value.getId() == null ? ("Stat[" + i + "]") : value.getId();
            rows.add(new ProfilerRow(label, fmt(value.get(), 2) + " / " + fmt(value.getMax(), 2)));
        }
        if (rows.isEmpty()) {
            rows.add(new ProfilerRow("Status", "No runtime stats available"));
        }
        return new ProfilerSection("Runtime Stat Map", rows);
    }

    private static RegenStats resolveNpcRegen(NpcLevelComponent level) {
        HyruneConfig config = HyruneConfigManager.getConfig();
        HyruneConfig.RegenConfig regen = config == null ? null : config.regen;
        boolean boss = isBoss(level);
        int levelValue = level == null ? 1 : Math.max(1, Math.min(99, level.getLevel()));

        double npcPerLevel = positiveOrDefault(regen == null ? 0.0 : regen.npcHealthRegenPerLevel, DEFAULT_NPC_HEALTH_REGEN_PER_LEVEL);
        double npcCap = positiveOrDefault(regen == null ? 0.0 : regen.npcHealthRegenCapPerSecond, DEFAULT_NPC_HEALTH_REGEN_CAP_PER_SEC);
        double bossPerLevel = positiveOrDefault(regen == null ? 0.0 : regen.bossHealthRegenPerLevel, DEFAULT_BOSS_HEALTH_REGEN_PER_LEVEL);
        double bossCap = positiveOrDefault(regen == null ? 0.0 : regen.bossHealthRegenCapPerSecond, DEFAULT_BOSS_HEALTH_REGEN_CAP_PER_SEC);

        double configured = boss ? levelValue * bossPerLevel : levelValue * npcPerLevel;
        double cap = boss ? bossCap : npcCap;
        double effective = Math.max(0.0, Math.min(configured, cap));
        return new RegenStats(configured, cap, effective);
    }

    private static boolean isBoss(NpcLevelComponent level) {
        if (level == null || level.getArchetypeId() == null) {
            return false;
        }
        return "BOSS".equalsIgnoreCase(level.getArchetypeId().trim());
    }

    private static double positiveOrDefault(double value, double fallback) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value <= 0.0) {
            return fallback;
        }
        return value;
    }

    private static String resolveDisplayName(Store<EntityStore> store, Ref<EntityStore> ref, NPCEntity npc, NpcLevelComponent level) {
        if (level != null && level.getBaseName() != null && !level.getBaseName().isBlank()) {
            return level.getBaseName();
        }
        DisplayNameComponent displayName = store.getComponent(ref, DisplayNameComponent.getComponentType());
        if (displayName != null && displayName.getDisplayName() != null && displayName.getDisplayName().getRawText() != null
            && !displayName.getDisplayName().getRawText().isBlank()) {
            return displayName.getDisplayName().getRawText();
        }
        if (npc.getNPCTypeId() != null && !npc.getNPCTypeId().isBlank()) {
            return npc.getNPCTypeId();
        }
        return "NPC";
    }

    private static String resolveWeakness(NpcLevelComponent level, NpcLevelService levelService) {
        if (level != null && level.getWeakness() != null) {
            return level.getWeakness().name();
        }
        if (levelService != null) {
            return levelService.resolveWeakness(levelService.getDefaultWeakness()).name();
        }
        return CombatStyle.MELEE.name();
    }

    private static String pct(double value) {
        return fmt(value * 100.0, 2) + "%";
    }

    private static String fmt(double value, int decimals) {
        return String.format(Locale.ROOT, "%." + decimals + "f", value);
    }

    private static String csv(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "-";
        }
        return String.join(", ", values);
    }

    private static String fallback(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }

    public record NpcProfilerSnapshot(String title, String subtitle, List<ProfilerSection> sections) {
    }

    public record ProfilerSection(String title, List<ProfilerRow> rows) {
    }

    public record ProfilerRow(String label, String value) {
    }

    private record RegenStats(double configuredPerSecond, double capPerSecond, double effectivePerSecond) {
    }
}
