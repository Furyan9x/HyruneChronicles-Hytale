package dev.hytalemodding.hyrune.itemization.tooltip;

import com.hypixel.hytale.logger.HytaleLogger;
import dev.hytalemodding.Hyrune;
import dev.hytalemodding.hyrune.config.HyruneConfig;
import dev.hytalemodding.hyrune.config.HyruneConfigManager;
import dev.hytalemodding.hyrune.itemization.ItemPrefixService;
import dev.hytalemodding.hyrune.itemization.ItemArchetype;
import dev.hytalemodding.hyrune.itemization.ItemStatDisplayFormatter;
import dev.hytalemodding.hyrune.itemization.ItemInstanceMetadata;
import dev.hytalemodding.hyrune.itemization.ItemInstanceMetadataMigration;
import dev.hytalemodding.hyrune.itemization.ItemStatResolution;
import dev.hytalemodding.hyrune.itemization.ItemStatResolver;
import dev.hytalemodding.hyrune.itemization.ItemizedStat;
import dev.hytalemodding.hyrune.itemization.ItemizedStatBlock;
import dev.hytalemodding.hyrune.itemization.GemSocketConfigHelper;
import dev.hytalemodding.hyrune.level.LevelingService;
import dev.hytalemodding.hyrune.repair.ItemRarity;
import dev.hytalemodding.hyrune.skills.SkillType;
import dev.hytalemodding.hyrune.system.SkillCombatBonusSystem;
import org.bson.BsonDocument;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.logging.Level;

/**
 * Composes dynamic tooltip lines from item metadata with per-item-state caching.
 */
public final class HyruneDynamicTooltipComposer {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final int STATE_CACHE_MAX = 4096;
    private static final long COMPOSE_SUMMARY_INTERVAL_MS = 5000L;
    private static final ComposedTooltip EMPTY_SENTINEL = new ComposedTooltip(List.of(), ItemRarity.COMMON, "", null);

    private final ConcurrentHashMap<String, ComposedTooltip> itemStateCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ComposedTooltip> composedCache = new ConcurrentHashMap<>();
    private final LongAdder composeHits = new LongAdder();
    private final LongAdder composeMisses = new LongAdder();
    private final LongAdder parseFailures = new LongAdder();
    private volatile long lastComposeSummaryMs = 0L;

    @Nullable
    public ComposedTooltip compose(@Nullable UUID playerUuid, String itemId, String metadataJson) {
        if (itemId == null || itemId.isBlank() || metadataJson == null || metadataJson.isBlank()) {
            return null;
        }

        String playerKey = playerUuid == null ? "no-player" : playerUuid.toString();
        String stateKey = playerKey + "|" + itemId + "|" + shortHash(metadataJson);
        ComposedTooltip cached = itemStateCache.get(stateKey);
        if (cached != null) {
            noteCompose(true);
            return cached == EMPTY_SENTINEL ? null : cached;
        }

        noteCompose(false);

        ItemInstanceMetadata metadata = parseMetadata(metadataJson);
        if (metadata == null) {
            cacheState(stateKey, EMPTY_SENTINEL);
            return null;
        }

        String stableInput = buildStableInput(playerUuid, itemId, metadata);
        String combinedHash = shortHash(stableInput);
        ComposedTooltip composed = composedCache.computeIfAbsent(combinedHash, hash -> buildTooltip(playerUuid, itemId, hash, metadata));
        cacheState(stateKey, composed);
        return composed;
    }

    public void clearCache() {
        itemStateCache.clear();
        composedCache.clear();
    }

    private void cacheState(String stateKey, ComposedTooltip value) {
        if (itemStateCache.size() >= STATE_CACHE_MAX) {
            itemStateCache.clear();
            logCache("state-cache-cleared");
        }
        itemStateCache.put(stateKey, value);
    }

    @Nullable
    private ItemInstanceMetadata parseMetadata(String metadataJson) {
        try {
            BsonDocument doc = BsonDocument.parse(metadataJson);
            ItemInstanceMetadata metadata = ItemInstanceMetadata.KEYED_CODEC.getOrNull(doc);
            return ItemInstanceMetadataMigration.migrateToCurrent(metadata);
        } catch (Exception ex) {
            parseFailures.increment();
            HyruneConfig cfg = HyruneConfigManager.getConfig();
            if (cfg.dynamicTooltipComposeDebug) {
                LOGGER.at(Level.INFO).log("[DynamicTooltip] metadata-parse-failed: " + ex.getMessage());
            }
            return null;
        }
    }

    private ComposedTooltip buildTooltip(@Nullable UUID playerUuid,
                                         String itemId,
                                         String combinedHash,
                                         ItemInstanceMetadata metadata) {
        ItemRarity rarity = metadata.getRarity();
        ItemRarity resolvedRarity = rarity == null ? ItemRarity.COMMON : rarity;
        String displayNameOverride = ItemPrefixService.resolveDisplayName(itemId, metadata.getPrefixRaw());

        List<String> lines = new ArrayList<>(10);
        lines.add("<color is=\"" + rarityColor(rarity) + "\"><i>" + rarityLabel(rarity) + "</i></color>");
        lines.add("<color is=\"#8AD7FF\">Sockets: " + metadata.getSocketedGemCount() + "/" + metadata.getSocketCapacity() + "</color>");
        for (String gemLine : GemSocketConfigHelper.describeSocketedGemLinesForItem(itemId, metadata.getSocketedGems())) {
            lines.add("<color is=\"#8AD7FF\">" + gemLine + "</color>");
        }
        String weaponDamageLine = buildWeaponDamageLine(playerUuid, itemId, metadata);
        if (weaponDamageLine != null) {
            lines.add(weaponDamageLine);
        }
        lines.add("<color is=\"#505050\">----------------</color>");
        for (String rollLine : buildRollLines(metadata)) {
            lines.add("<color is=\"#1EFF00\">" + rollLine + "</color>");
        }
        if (metadata.getDroppedPenalty() > 0d) {
            lines.add("<color is=\"#D08A8A\">Drop Penalty: -" + pct(metadata.getDroppedPenalty()) + "</color>");
        }

        return new ComposedTooltip(lines, resolvedRarity, combinedHash, displayNameOverride);
    }

    private static String buildStableInput(@Nullable UUID playerUuid, String itemId, ItemInstanceMetadata metadata) {
        SkillContext skill = resolveSkillContext(playerUuid, itemId);
        return itemId
            + "|player=" + (playerUuid == null ? "no-player" : playerUuid)
            + "|r=" + metadata.getRarity().name()
            + "|prefix=" + metadata.getPrefixRaw()
            + "|s=" + metadata.getSource().name()
            + "|sockets=" + metadata.getSocketCapacity()
            + ":" + metadata.getSocketedGemsJson()
            + "|flat=" + metadata.getStatFlatRollsJson()
            + "|pct=" + metadata.getStatPercentRollsJson()
            + "|skill=" + skill.skillType().name()
            + ":" + skill.skillLevel()
            + ":" + fmt(skill.multiplier())
            + "|dp=" + fmt(metadata.getDroppedPenalty());
    }

    private static List<String> buildRollLines(ItemInstanceMetadata metadata) {
        List<String> out = new ArrayList<>();
        if (metadata == null) {
            return out;
        }
        Map<String, Double> flatRolls = metadata.getStatFlatRollsRaw();
        Map<String, Double> percentRolls = metadata.getStatPercentRollsRaw();
        if (flatRolls.isEmpty() && percentRolls.isEmpty()) {
            out.add("Rolls: none");
            return out;
        }

        Map<String, RollLineData> merged = new LinkedHashMap<>();
        addRollEntries(merged, flatRolls.entrySet(), true);
        addRollEntries(merged, percentRolls.entrySet(), false);

        List<RollLineData> entries = new ArrayList<>(merged.values());
        entries.sort(Comparator.comparingDouble((RollLineData e) -> rollSortKey(e.flatRoll(), e.percentRoll())).reversed());

        int limit = Math.min(6, entries.size());
        for (int i = 0; i < limit; i++) {
            RollLineData entry = entries.get(i);
            ItemizedStat stat = ItemizedStat.fromId(entry.statId());
            String label = stat != null ? stat.getDisplayName() : entry.statId();
            out.add(label + ": " + ItemStatDisplayFormatter.formatRoll(stat, entry.flatRoll(), entry.percentRoll()));
        }
        if (entries.size() > limit) {
            out.add("... +" + (entries.size() - limit) + " more rolled stats");
        }
        return out;
    }

    private static void addRollEntries(Map<String, RollLineData> merged,
                                       Collection<Map.Entry<String, Double>> entries,
                                       boolean flat) {
        for (Map.Entry<String, Double> entry : entries) {
            String statId = entry.getKey();
            if (statId == null || statId.isBlank()) {
                continue;
            }
            RollLineData current = merged.get(statId);
            double flatValue = current == null ? 0.0 : current.flatRoll();
            double percentValue = current == null ? 0.0 : current.percentRoll();
            if (flat) {
                flatValue = valueOrZero(entry.getValue());
            } else {
                percentValue = valueOrZero(entry.getValue());
            }
            merged.put(statId, new RollLineData(statId, flatValue, percentValue));
        }
    }

    private static double rollSortKey(double flat, double percent) {
        return Math.abs(flat) + (Math.abs(percent) * 100.0);
    }

    private static double valueOrZero(Double value) {
        if (value == null || Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }
        return value;
    }

    private static String fmt(double value) {
        return String.format(Locale.US, "%.6f", value);
    }

    @Nullable
    private static String buildWeaponDamageLine(@Nullable UUID playerUuid, String itemId, ItemInstanceMetadata metadata) {
        if (itemId == null || itemId.isBlank() || metadata == null) {
            return null;
        }
        ItemStatResolution resolution = ItemStatResolver.resolveDetailed(itemId, metadata);
        ItemArchetype archetype = resolution.getArchetype();
        if (archetype == null || !archetype.getId().startsWith("weapon_")) {
            return null;
        }

        ItemizedStatBlock stats = resolution.getResolvedSpecializedStats();
        double physical = Math.max(0.0, stats.get(ItemizedStat.PHYSICAL_DAMAGE));
        double magical = Math.max(0.0, stats.get(ItemizedStat.MAGICAL_DAMAGE));
        double baseDamage = physical + magical;
        if (baseDamage <= 0.0) {
            return null;
        }

        double critChance = clamp(
            stats.get(ItemizedStat.PHYSICAL_CRIT_CHANCE) + stats.get(ItemizedStat.MAGICAL_CRIT_CHANCE),
            0.0,
            0.80
        );
        double critMultiplier = Math.max(1.0, 1.5 + stats.get(ItemizedStat.CRIT_BONUS));
        double penetration = clamp(
            stats.get(ItemizedStat.PHYSICAL_PENETRATION) + stats.get(ItemizedStat.MAGICAL_PENETRATION),
            0.0,
            2.0
        );
        double speed = Math.max(0.0, stats.get(ItemizedStat.ATTACK_SPEED) + stats.get(ItemizedStat.CAST_SPEED));

        double expectedDamage = baseDamage;
        expectedDamage *= 1.0 + (critChance * (critMultiplier - 1.0));
        expectedDamage *= 1.0 + (penetration * 0.35);
        expectedDamage *= 1.0 + (speed * 0.60);
        SkillContext skill = resolveSkillContext(playerUuid, itemId);
        double finalDamage = expectedDamage * skill.multiplier();

        return "<color is=\"#B8860B\">Weapon Damage:</color> <color is=\"#FFD700\">"
            + number(baseDamage) + " -> " + number(finalDamage) + "</color>";
    }

    private static SkillContext resolveSkillContext(@Nullable UUID playerUuid, String itemId) {
        SkillType skill = SkillType.STRENGTH;
        float perLevel = SkillCombatBonusSystem.STRENGTH_DAMAGE_PER_LEVEL;
        String normalized = itemId == null ? "" : itemId.toLowerCase(Locale.ROOT);
        if (SkillCombatBonusSystem.isRangedWeapon(normalized)) {
            skill = SkillType.RANGED;
            perLevel = SkillCombatBonusSystem.RANGED_DAMAGE_PER_LEVEL;
        } else if (SkillCombatBonusSystem.isMagicWeapon(normalized)) {
            skill = SkillType.MAGIC;
            perLevel = SkillCombatBonusSystem.MAGIC_DAMAGE_PER_LEVEL;
        }

        int level = 1;
        if (playerUuid != null) {
            LevelingService service = Hyrune.getService();
            if (service != null) {
                level = Math.max(1, service.getSkillLevel(playerUuid, skill));
            }
        }

        double multiplier = 1.0 + (level * perLevel);
        return new SkillContext(skill, level, multiplier);
    }

    private static String pct(double value) {
        return String.format(Locale.US, "%.2f%%", value * 100.0d);
    }

    private static String number(double value) {
        double abs = Math.abs(value);
        if (abs >= 100.0) {
            return String.format(Locale.US, "%.0f", value);
        }
        if (abs >= 10.0) {
            return String.format(Locale.US, "%.1f", value);
        }
        if (abs >= 1.0) {
            return String.format(Locale.US, "%.2f", value);
        }
        return String.format(Locale.US, "%.3f", value);
    }

    private static double clamp(double value, double min, double max) {
        if (Double.isNaN(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    private static String rarityLabel(ItemRarity rarity) {
        if (rarity == null) {
            return "Common";
        }
        return switch (rarity) {
            case UNCOMMON -> "Uncommon";
            case RARE -> "Rare";
            case EPIC -> "Epic";
            case VOCATIONAL -> "Vocational";
            case LEGENDARY -> "Legendary";
            case MYTHIC -> "Mythic";
            case COMMON -> "Common";
        };
    }

    private static String rarityColor(ItemRarity rarity) {
        if (rarity == null) {
            return "#C8C8C8";
        }
        return switch (rarity) {
            case UNCOMMON -> "#7CFF9E";
            case RARE -> "#70D8FF";
            case EPIC -> "#CFA8FF";
            case VOCATIONAL -> "#8AF0C8";
            case LEGENDARY -> "#FFD46B";
            case MYTHIC -> "#E41E3D";
            case COMMON -> "#C8C8C8";
        };
    }

    private static String shortHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(8);
            for (int i = 0; i < 4; i++) {
                out.append(String.format(Locale.US, "%02x", bytes[i]));
            }
            return out.toString();
        } catch (Exception ex) {
            return String.format(Locale.US, "%08x", input.hashCode());
        }
    }

    private static void logCache(String message) {
        HyruneConfig cfg = HyruneConfigManager.getConfig();
        if (cfg.dynamicTooltipCacheDebug) {
            LOGGER.at(Level.INFO).log("[DynamicTooltip] " + message);
        }
    }

    private static void logCompose(String message) {
        HyruneConfig cfg = HyruneConfigManager.getConfig();
        if (cfg.dynamicTooltipComposeDebug) {
            LOGGER.at(Level.INFO).log("[DynamicTooltip] " + message);
        }
    }

    private void noteCompose(boolean hit) {
        if (hit) {
            composeHits.increment();
        } else {
            composeMisses.increment();
        }

        HyruneConfig cfg = HyruneConfigManager.getConfig();
        if (cfg == null || !cfg.dynamicTooltipComposeDebug) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastComposeSummaryMs < COMPOSE_SUMMARY_INTERVAL_MS) {
            return;
        }
        synchronized (this) {
            now = System.currentTimeMillis();
            if (now - lastComposeSummaryMs < COMPOSE_SUMMARY_INTERVAL_MS) {
                return;
            }
            long hits = composeHits.sumThenReset();
            long misses = composeMisses.sumThenReset();
            long parseFails = parseFailures.sumThenReset();
            lastComposeSummaryMs = now;
            logCompose("compose-summary hits=" + hits
                + ", misses=" + misses
                + ", parseFails=" + parseFails
                + ", stateCache=" + itemStateCache.size()
                + ", composedCache=" + composedCache.size());
        }
    }

    public static final class ComposedTooltip {
        private final List<String> additiveLines;
        private final ItemRarity rarity;
        private final String combinedHash;
        private final String displayNameOverride;

        private ComposedTooltip(List<String> additiveLines, ItemRarity rarity, String combinedHash, @Nullable String displayNameOverride) {
            this.additiveLines = List.copyOf(additiveLines);
            this.rarity = rarity;
            this.combinedHash = combinedHash;
            this.displayNameOverride = (displayNameOverride == null || displayNameOverride.isBlank()) ? null : displayNameOverride;
        }

        public ItemRarity getRarity() {
            return rarity;
        }

        public String getCombinedHash() {
            return combinedHash;
        }

        @Nullable
        public String getDisplayNameOverride() {
            return displayNameOverride;
        }

        public String buildDescription(String baseDescription) {
            StringBuilder description = new StringBuilder();
            if (baseDescription != null && !baseDescription.isBlank()) {
                description.append(baseDescription);
            }

            if (!additiveLines.isEmpty()) {
                if (description.length() > 0) {
                    description.append("\n\n");
                }
                for (int i = 0; i < additiveLines.size(); i++) {
                    if (i > 0) {
                        description.append('\n');
                    }
                    description.append(additiveLines.get(i));
                }
            }
            return description.toString();
        }
    }

    private record RollLineData(String statId, double flatRoll, double percentRoll) {
    }

    private record SkillContext(SkillType skillType, int skillLevel, double multiplier) {
    }
}

