package dev.hytalemodding.hyrune.itemization;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import dev.hytalemodding.hyrune.repair.ItemRarity;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Per-item rolled metadata for rarity and specialized stat variation.
 */
public class ItemInstanceMetadata {
    public static final String KEY = "HyruneItemInstance";
    public static final int CURRENT_SCHEMA_VERSION = 3;
    public static final BuilderCodec<ItemInstanceMetadata> CODEC = BuilderCodec.builder(ItemInstanceMetadata.class, ItemInstanceMetadata::new)
        .append(new KeyedCodec<>("Version", Codec.LONG), ItemInstanceMetadata::setVersion, ItemInstanceMetadata::getVersion).add()
        .append(new KeyedCodec<>("Rarity", Codec.STRING), ItemInstanceMetadata::setRarityRaw, ItemInstanceMetadata::getRarityRaw).add()
        .append(new KeyedCodec<>("Catalyst", Codec.STRING), ItemInstanceMetadata::setCatalystRaw, ItemInstanceMetadata::getCatalystRaw).add()
        .append(new KeyedCodec<>("Source", Codec.STRING), ItemInstanceMetadata::setSourceRaw, ItemInstanceMetadata::getSourceRaw).add()
        .append(new KeyedCodec<>("Seed", Codec.LONG), ItemInstanceMetadata::setSeed, ItemInstanceMetadata::getSeed).add()
        .append(new KeyedCodec<>("StatFlatRolls", Codec.STRING), ItemInstanceMetadata::setStatFlatRollsJson, ItemInstanceMetadata::getStatFlatRollsJson).add()
        .append(new KeyedCodec<>("StatPercentRolls", Codec.STRING), ItemInstanceMetadata::setStatPercentRollsJson, ItemInstanceMetadata::getStatPercentRollsJson).add()
        .append(new KeyedCodec<>("DroppedPenalty", Codec.DOUBLE), ItemInstanceMetadata::setDroppedPenalty, ItemInstanceMetadata::getDroppedPenalty).add()
        .build();
    public static final KeyedCodec<ItemInstanceMetadata> KEYED_CODEC = new KeyedCodec<>(KEY, CODEC);

    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Double>>() {
    }.getType();

    private long version;
    private String rarityRaw = ItemRarity.COMMON.name();
    private String catalystRaw = CatalystAffinity.NONE.name();
    private String sourceRaw = ItemRollSource.CRAFTED.name();
    private long seed = 0L;
    private String statFlatRollsJson = "{}";
    private String statPercentRollsJson = "{}";
    private double droppedPenalty;

    private transient Map<String, Double> statFlatRollCache;
    private transient Map<String, Double> statPercentRollCache;

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public ItemRarity getRarity() {
        try {
            return ItemRarity.valueOf(rarityRaw);
        } catch (IllegalArgumentException ignored) {
            return ItemRarity.COMMON;
        }
    }

    public void setRarity(ItemRarity rarity) {
        this.rarityRaw = rarity == null ? ItemRarity.COMMON.name() : rarity.name();
    }

    public CatalystAffinity getCatalyst() {
        return CatalystAffinity.fromString(catalystRaw);
    }

    public void setCatalyst(CatalystAffinity catalyst) {
        this.catalystRaw = catalyst == null ? CatalystAffinity.NONE.name() : catalyst.name();
    }

    public ItemRollSource getSource() {
        try {
            return ItemRollSource.valueOf(sourceRaw);
        } catch (IllegalArgumentException ignored) {
            return ItemRollSource.CRAFTED;
        }
    }

    public void setSource(ItemRollSource source) {
        this.sourceRaw = source == null ? ItemRollSource.CRAFTED.name() : source.name();
    }

    public String getRarityRaw() {
        return rarityRaw;
    }

    public void setRarityRaw(String rarityRaw) {
        this.rarityRaw = rarityRaw;
    }

    public String getCatalystRaw() {
        return catalystRaw;
    }

    public void setCatalystRaw(String catalystRaw) {
        this.catalystRaw = catalystRaw;
    }

    public String getSourceRaw() {
        return sourceRaw;
    }

    public void setSourceRaw(String sourceRaw) {
        this.sourceRaw = sourceRaw;
    }

    public long getSeed() {
        return seed;
    }

    public void setSeed(long seed) {
        this.seed = seed;
    }

    public String getStatFlatRollsJson() {
        return statFlatRollsJson;
    }

    public void setStatFlatRollsJson(String statFlatRollsJson) {
        this.statFlatRollsJson = (statFlatRollsJson == null || statFlatRollsJson.isBlank()) ? "{}" : statFlatRollsJson;
        this.statFlatRollCache = null;
    }

    public String getStatPercentRollsJson() {
        return statPercentRollsJson;
    }

    public void setStatPercentRollsJson(String statPercentRollsJson) {
        this.statPercentRollsJson = (statPercentRollsJson == null || statPercentRollsJson.isBlank()) ? "{}" : statPercentRollsJson;
        this.statPercentRollCache = null;
    }

    public Map<String, Double> getStatFlatRollsRaw() {
        return new LinkedHashMap<>(loadFlatRollCache());
    }

    public Map<String, Double> getStatPercentRollsRaw() {
        return new LinkedHashMap<>(loadPercentRollCache());
    }

    public double getFlatStatRoll(ItemizedStat stat) {
        if (stat == null) {
            return 0.0;
        }
        return loadFlatRollCache().getOrDefault(stat.getId(), 0.0);
    }

    public double getPercentStatRoll(ItemizedStat stat) {
        if (stat == null) {
            return 0.0;
        }
        return loadPercentRollCache().getOrDefault(stat.getId(), 0.0);
    }

    public void setStatFlatRollsRaw(Map<String, Double> rolls) {
        Map<String, Double> sanitized = sanitizeRolls(rolls);
        this.statFlatRollCache = sanitized;
        this.statFlatRollsJson = GSON.toJson(sanitized, MAP_TYPE);
    }

    public void setStatPercentRollsRaw(Map<String, Double> rolls) {
        Map<String, Double> sanitized = sanitizeRolls(rolls);
        this.statPercentRollCache = sanitized;
        this.statPercentRollsJson = GSON.toJson(sanitized, MAP_TYPE);
    }

    public void setFlatStatRoll(ItemizedStat stat, double value) {
        if (stat == null) {
            return;
        }
        Map<String, Double> mutable = new LinkedHashMap<>(loadFlatRollCache());
        updateMapRoll(mutable, stat, value);
        setStatFlatRollsRaw(mutable);
    }

    public void setPercentStatRoll(ItemizedStat stat, double value) {
        if (stat == null) {
            return;
        }
        Map<String, Double> mutable = new LinkedHashMap<>(loadPercentRollCache());
        updateMapRoll(mutable, stat, value);
        setStatPercentRollsRaw(mutable);
    }

    public double getDroppedPenalty() {
        return droppedPenalty;
    }

    public void setDroppedPenalty(double droppedPenalty) {
        this.droppedPenalty = droppedPenalty;
    }

    private Map<String, Double> loadFlatRollCache() {
        if (statFlatRollCache != null) {
            return statFlatRollCache;
        }
        Map<String, Double> parsed = parseRollJson(statFlatRollsJson);
        statFlatRollCache = sanitizeRolls(parsed);
        return statFlatRollCache;
    }

    private Map<String, Double> loadPercentRollCache() {
        if (statPercentRollCache != null) {
            return statPercentRollCache;
        }
        Map<String, Double> parsed = parseRollJson(statPercentRollsJson);
        statPercentRollCache = sanitizeRolls(parsed);
        return statPercentRollCache;
    }

    private static void updateMapRoll(Map<String, Double> map, ItemizedStat stat, double value) {
        double safe = sanitizeRoll(value);
        if (Math.abs(safe) <= 1e-9) {
            map.remove(stat.getId());
        } else {
            map.put(stat.getId(), safe);
        }
    }

    private static Map<String, Double> parseRollJson(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, Double> parsed = GSON.fromJson(json, MAP_TYPE);
            return parsed == null ? new LinkedHashMap<>() : parsed;
        } catch (RuntimeException ignored) {
            return new LinkedHashMap<>();
        }
    }

    private static Map<String, Double> sanitizeRolls(Map<String, Double> input) {
        Map<String, Double> out = new LinkedHashMap<>();
        if (input == null) {
            return out;
        }
        for (Map.Entry<String, Double> entry : input.entrySet()) {
            String key = normalizeId(entry.getKey());
            if (key == null) {
                continue;
            }
            double value = sanitizeRoll(entry.getValue());
            if (Math.abs(value) <= 1e-9) {
                continue;
            }
            out.put(key, value);
        }
        return out;
    }

    private static String normalizeId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    private static double sanitizeRoll(Double value) {
        if (value == null || Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }
        return value;
    }
}
