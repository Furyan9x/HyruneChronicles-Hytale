package dev.hytalemodding.hyrune.itemization.tooltip;

import com.hypixel.hytale.logger.HytaleLogger;
import dev.hytalemodding.hyrune.config.HyruneConfig;
import dev.hytalemodding.hyrune.config.HyruneConfigManager;
import dev.hytalemodding.hyrune.itemization.CatalystAffinity;
import dev.hytalemodding.hyrune.itemization.ItemInstanceMetadata;
import dev.hytalemodding.hyrune.repair.ItemRarity;
import org.bson.BsonDocument;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Composes dynamic tooltip lines from item metadata with per-item-state caching.
 */
public final class HyruneDynamicTooltipComposer {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final int STATE_CACHE_MAX = 4096;
    private static final ComposedTooltip EMPTY_SENTINEL = new ComposedTooltip(List.of(), ItemRarity.COMMON, "");

    private final ConcurrentHashMap<String, ComposedTooltip> itemStateCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ComposedTooltip> composedCache = new ConcurrentHashMap<>();

    @Nullable
    public ComposedTooltip compose(String itemId, String metadataJson) {
        if (itemId == null || itemId.isBlank() || metadataJson == null || metadataJson.isBlank()) {
            return null;
        }

        String stateKey = itemId + "|" + shortHash(metadataJson);
        ComposedTooltip cached = itemStateCache.get(stateKey);
        if (cached != null) {
            logCompose("compose-hit stateKey=" + stateKey);
            return cached == EMPTY_SENTINEL ? null : cached;
        }

        logCompose("compose-miss stateKey=" + stateKey);

        ItemInstanceMetadata metadata = parseMetadata(metadataJson);
        if (metadata == null) {
            cacheState(stateKey, EMPTY_SENTINEL);
            return null;
        }

        String stableInput = buildStableInput(itemId, metadata);
        String combinedHash = shortHash(stableInput);
        ComposedTooltip composed = composedCache.computeIfAbsent(combinedHash, hash -> buildTooltip(hash, metadata));
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
            return ItemInstanceMetadata.KEYED_CODEC.getOrNull(doc);
        } catch (Exception ex) {
            HyruneConfig cfg = HyruneConfigManager.getConfig();
            if (cfg.dynamicTooltipComposeDebug) {
                LOGGER.at(Level.INFO).log("[DynamicTooltip] metadata-parse-failed: " + ex.getMessage());
            }
            return null;
        }
    }

    private ComposedTooltip buildTooltip(String combinedHash, ItemInstanceMetadata metadata) {
        ItemRarity rarity = metadata.getRarity();
        CatalystAffinity catalyst = metadata.getCatalyst();
        ItemRarity resolvedRarity = rarity == null ? ItemRarity.COMMON : rarity;

        List<String> lines = new ArrayList<>(6);
        lines.add("<color is=\"" + rarityColor(rarity) + "\"><i>" + rarityLabel(rarity) + "</i></color>");
        lines.add("<color is=\"#505050\">----------------</color>");
        lines.add("<color is=\"" + catalystColor(catalyst) + "\">Catalyst: " + catalystLabel(catalyst) + "</color>");
        lines.add("<color is=\"#9EA7B3\">Rolls: DMG +" + pct(metadata.getDamageRoll())
            + " | DEF +" + pct(metadata.getDefenceRoll())
            + " | HEAL +" + pct(metadata.getHealingRoll())
            + " | UTIL +" + pct(metadata.getUtilityRoll()) + "</color>");
        if (metadata.getDroppedPenalty() > 0d) {
            lines.add("<color is=\"#D08A8A\">Drop Penalty: -" + pct(metadata.getDroppedPenalty()) + "</color>");
        }

        return new ComposedTooltip(lines, resolvedRarity, combinedHash);
    }

    private static String buildStableInput(String itemId, ItemInstanceMetadata metadata) {
        return itemId
            + "|r=" + metadata.getRarity().name()
            + "|c=" + metadata.getCatalyst().name()
            + "|s=" + metadata.getSource().name()
            + "|d=" + fmt(metadata.getDamageRoll())
            + "|f=" + fmt(metadata.getDefenceRoll())
            + "|h=" + fmt(metadata.getHealingRoll())
            + "|u=" + fmt(metadata.getUtilityRoll())
            + "|p=" + fmt(metadata.getDroppedPenalty());
    }

    private static String fmt(double value) {
        return String.format(Locale.US, "%.6f", value);
    }

    private static String pct(double value) {
        return String.format(Locale.US, "%.2f%%", value * 100.0d);
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

    private static String catalystLabel(CatalystAffinity affinity) {
        if (affinity == null) {
            return "None";
        }
        return switch (affinity) {
            case WATER -> "Water";
            case FIRE -> "Fire";
            case AIR -> "Air";
            case EARTH -> "Earth";
            case NONE -> "None";
        };
    }

    private static String catalystColor(CatalystAffinity affinity) {
        if (affinity == null) {
            return "#A7C7FF";
        }
        return switch (affinity) {
            case FIRE -> "#FF8A70";
            case WATER -> "#70C6FF";
            case AIR -> "#B9F0FF";
            case EARTH -> "#9AD28A";
            case NONE -> "#A7C7FF";
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

    public static final class ComposedTooltip {
        private final List<String> additiveLines;
        private final ItemRarity rarity;
        private final String combinedHash;

        private ComposedTooltip(List<String> additiveLines, ItemRarity rarity, String combinedHash) {
            this.additiveLines = List.copyOf(additiveLines);
            this.rarity = rarity;
            this.combinedHash = combinedHash;
        }

        public ItemRarity getRarity() {
            return rarity;
        }

        public String getCombinedHash() {
            return combinedHash;
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
}
