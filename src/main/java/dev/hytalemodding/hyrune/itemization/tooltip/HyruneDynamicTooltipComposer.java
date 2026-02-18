package dev.hytalemodding.hyrune.itemization.tooltip;

import com.hypixel.hytale.logger.HytaleLogger;
import dev.hytalemodding.hyrune.config.HyruneConfig;
import dev.hytalemodding.hyrune.config.HyruneConfigManager;
import dev.hytalemodding.hyrune.itemization.ItemPrefixService;
import dev.hytalemodding.hyrune.itemization.ItemArchetype;
import dev.hytalemodding.hyrune.itemization.ItemArchetypeResolver;
import dev.hytalemodding.hyrune.itemization.ItemizationSpecializedStatConfigHelper;
import dev.hytalemodding.hyrune.itemization.ItemStatDisplayFormatter;
import dev.hytalemodding.hyrune.itemization.ItemInstanceMetadata;
import dev.hytalemodding.hyrune.itemization.ItemInstanceMetadataMigration;
import dev.hytalemodding.hyrune.itemization.ItemStatResolution;
import dev.hytalemodding.hyrune.itemization.ItemStatResolver;
import dev.hytalemodding.hyrune.itemization.ItemizedStat;
import dev.hytalemodding.hyrune.itemization.ItemizedStatBlock;
import dev.hytalemodding.hyrune.itemization.GemSocketConfigHelper;
import dev.hytalemodding.hyrune.itemization.ItemRarityRollModel;
import dev.hytalemodding.hyrune.repair.ItemRarity;
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
        ComposedTooltip composed = composedCache.computeIfAbsent(combinedHash, hash -> buildTooltip(itemId, hash, metadata));
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

    private ComposedTooltip buildTooltip(String itemId,
                                         String combinedHash,
                                         ItemInstanceMetadata metadata) {
        ItemRarity rarity = metadata.getRarity();
        ItemRarity resolvedRarity = rarity == null ? ItemRarity.COMMON : rarity;
        String displayNameOverride = ItemPrefixService.resolveDisplayName(itemId, metadata.getPrefixRaw());

        List<String> lines = new ArrayList<>(10);
        for (String baseLine : buildBaseStatLines(itemId, metadata)) {
            lines.add(baseLine);
        }
        lines.add("<color is=\"#505050\">----------------</color>");
        lines.add("<color is=\"#8AD7FF\">Sockets: " + metadata.getSocketedGemCount() + "/" + metadata.getSocketCapacity() + "</color>");
        for (String gemLine : GemSocketConfigHelper.describeSocketedGemLinesForItem(itemId, metadata.getSocketedGems())) {
            lines.add("<color is=\"#8AD7FF\">" + gemLine + "</color>");
        }
        lines.add("<color is=\"#505050\">----------------</color>");
        for (String rollLine : buildRollLines(itemId, metadata)) {
            lines.add("<color is=\"#1EFF00\">" + rollLine + "</color>");
        }
        if (metadata.getDroppedPenalty() > 0d) {
            lines.add("<color is=\"#D08A8A\">Drop Penalty: -" + pct(metadata.getDroppedPenalty()) + "</color>");
        }

        return new ComposedTooltip(lines, resolvedRarity, combinedHash, displayNameOverride);
    }

    private static String buildStableInput(@Nullable UUID playerUuid, String itemId, ItemInstanceMetadata metadata) {
        return itemId
            + "|player=" + (playerUuid == null ? "no-player" : playerUuid)
            + "|r=" + metadata.getRarity().name()
            + "|prefix=" + metadata.getPrefixRaw()
            + "|s=" + metadata.getSource().name()
            + "|sockets=" + metadata.getSocketCapacity()
            + ":" + metadata.getSocketedGemsJson()
            + "|flat=" + metadata.getStatFlatRollsJson()
            + "|pct=" + metadata.getStatPercentRollsJson()
            + "|dp=" + fmt(metadata.getDroppedPenalty());
    }

    private static List<String> buildRollLines(String itemId, ItemInstanceMetadata metadata) {
        List<String> out = new ArrayList<>();
        if (metadata == null) {
            return out;
        }
        ItemArchetype archetype = ItemArchetypeResolver.resolve(itemId);
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
            RollLineData entry = sanitizeRollLine(entries.get(i));
            ItemizedStat stat = ItemizedStat.fromId(entry.statId());
            String label = stat != null ? stat.getDisplayName() : entry.statId();
            out.add(label + ": "
                + ItemStatDisplayFormatter.formatRoll(stat, entry.flatRoll(), entry.percentRoll())
                + formatRangeSuffix(itemId, archetype, metadata.getRarity(), entry));
        }
        if (entries.size() > limit) {
            out.add("... +" + (entries.size() - limit) + " more rolled stats");
        }
        return out;
    }

    private static List<String> buildBaseStatLines(String itemId, ItemInstanceMetadata metadata) {
        List<String> out = new ArrayList<>();
        ItemStatResolution resolution = ItemStatResolver.resolveDetailed(itemId, metadata);
        ItemizedStatBlock base = resolution.getBaseSpecializedStats();

        String weaponDamageLine = buildWeaponDamageLine(resolution);
        if (weaponDamageLine != null) {
            out.add(baseLine("Weapon Damage", weaponDamageLine));
        }

        List<Map.Entry<ItemizedStat, Double>> entries = new ArrayList<>(base.asMap().entrySet());
        entries.sort(Comparator.comparingDouble((Map.Entry<ItemizedStat, Double> e) -> Math.abs(e.getValue())).reversed());
        for (Map.Entry<ItemizedStat, Double> entry : entries) {
            ItemizedStat stat = entry.getKey();
            double value = entry.getValue() == null ? 0.0 : entry.getValue();
            if (stat == null || value <= 0.0 || stat == ItemizedStat.PHYSICAL_DAMAGE || stat == ItemizedStat.MAGICAL_DAMAGE) {
                continue;
            }
            out.add(baseLine(stat.getDisplayName(), ItemStatDisplayFormatter.formatRoll(stat, value, 0.0)));
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
    private static String buildWeaponDamageLine(ItemStatResolution resolution) {
        if (resolution == null) {
            return null;
        }
        ItemArchetype archetype = resolution.getArchetype();
        if (archetype == null || !archetype.getId().startsWith("weapon_")) {
            return null;
        }

        ItemizedStatBlock stats = resolution.getBaseSpecializedStats();
        double physical = Math.max(0.0, stats.get(ItemizedStat.PHYSICAL_DAMAGE));
        double magical = Math.max(0.0, stats.get(ItemizedStat.MAGICAL_DAMAGE));
        double baseDamage = physical + magical;
        if (baseDamage <= 0.0) {
            return null;
        }
        return number(baseDamage);
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

    private static String formatRangeSuffix(String itemId, ItemArchetype archetype, ItemRarity rarity, RollLineData entry) {
        ItemizedStat stat = ItemizedStat.fromId(entry.statId());
        if (stat == null) {
            return "";
        }
        boolean hasFlat = Math.abs(entry.flatRoll()) > 1e-9;
        boolean hasPercent = Math.abs(entry.percentRoll()) > 1e-9;
        if (!hasFlat && !hasPercent) {
            return "";
        }
        if (hasFlat && !hasPercent) {
            return " <color is=\"#7A7A7A\">[" + flatRange(stat, itemId, archetype, rarity) + "]</color>";
        }
        if (hasPercent && !hasFlat) {
            return " <color is=\"#7A7A7A\">[" + percentRange(stat, itemId, rarity) + "]</color>";
        }
        return "";
    }

    private static RollLineData sanitizeRollLine(RollLineData entry) {
        if (entry == null) {
            return null;
        }
        ItemizedStat stat = ItemizedStat.fromId(entry.statId());
        if (stat == null) {
            return entry;
        }
        boolean hasFlat = Math.abs(entry.flatRoll()) > 1e-9;
        boolean hasPercent = Math.abs(entry.percentRoll()) > 1e-9;
        if (!(hasFlat && hasPercent)) {
            return entry;
        }

        ItemizationSpecializedStatConfigHelper.RollConstraint constraint =
            ItemizationSpecializedStatConfigHelper.rollConstraintForStat(stat);
        return switch (constraint) {
            case FLAT_ONLY -> new RollLineData(entry.statId(), entry.flatRoll(), 0.0);
            case PERCENT_ONLY -> new RollLineData(entry.statId(), 0.0, entry.percentRoll());
            case EITHER -> {
                if (Math.abs(entry.percentRoll()) >= Math.abs(entry.flatRoll())) {
                    yield new RollLineData(entry.statId(), 0.0, entry.percentRoll());
                }
                yield new RollLineData(entry.statId(), entry.flatRoll(), 0.0);
            }
        };
    }

    private static String flatRange(ItemizedStat stat, String itemId, ItemArchetype archetype, ItemRarity rarity) {
        double base = ItemizationSpecializedStatConfigHelper.baseValueForArchetypeStat(archetype, stat);
        double tierScalar = ItemizationSpecializedStatConfigHelper.tierScalar(itemId);
        double rarityRollMultiplier = ItemRarityRollModel.rollStatMultiplierFlat(rarity);
        double min = round4(base * ItemizationSpecializedStatConfigHelper.flatRollMinScalar() * tierScalar * rarityRollMultiplier);
        double max = round4(base * ItemizationSpecializedStatConfigHelper.flatRollMaxScalar() * tierScalar * rarityRollMultiplier);
        double minFloor = ItemizationSpecializedStatConfigHelper.scaledFlatRollMinimumFloor(stat, tierScalar) * rarityRollMultiplier;
        min = Math.max(minFloor, min);
        max = Math.max(minFloor, max);
        if (min > max) {
            double t = min;
            min = max;
            max = t;
        }
        return stripPlus(ItemStatDisplayFormatter.formatFlat(stat, min))
            + "-"
            + stripPlus(ItemStatDisplayFormatter.formatFlat(stat, max));
    }

    private static String percentRange(ItemizedStat stat, String itemId, ItemRarity rarity) {
        double tierScalar = ItemizationSpecializedStatConfigHelper.tierScalar(itemId);
        ItemizationSpecializedStatConfigHelper.PercentRollDefinition definition =
            ItemizationSpecializedStatConfigHelper.percentRollDefinition(stat);
        double rarityRollMultiplier = ItemRarityRollModel.rollStatMultiplierPercent(rarity);
        double min = round4(definition.baseMin() * (1.0 + (Math.max(0.0, tierScalar) * definition.scalingWeight())) * rarityRollMultiplier);
        double max = round4(definition.baseMax() * (1.0 + (Math.max(0.0, tierScalar) * definition.scalingWeight())) * rarityRollMultiplier);
        if (min > max) {
            double t = min;
            min = max;
            max = t;
        }
        String minStr = stripPlus(ItemStatDisplayFormatter.formatPercent(min)).replace("%", "");
        String maxStr = stripPlus(ItemStatDisplayFormatter.formatPercent(max)).replace("%", "");
        return minStr + "%-" + maxStr + "%";
    }

    private static String stripPlus(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        return text.startsWith("+") ? text.substring(1) : text;
    }

    private static String baseLine(String label, String value) {
        return "<color is=\"#8A8A8A\">" + label + ":</color> <color is=\"#FFFFFF\">" + value + "</color>";
    }

    private static double round4(double value) {
        return Math.round(value * 10000.0) / 10000.0;
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

}

