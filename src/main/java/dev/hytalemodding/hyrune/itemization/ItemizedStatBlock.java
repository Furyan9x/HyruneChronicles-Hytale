package dev.hytalemodding.hyrune.itemization;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Immutable-ish specialized stat container used by resolver and player cache.
 */
public final class ItemizedStatBlock {
    private final EnumMap<ItemizedStat, Double> values = new EnumMap<>(ItemizedStat.class);

    public static ItemizedStatBlock empty() {
        return new ItemizedStatBlock();
    }

    public double get(ItemizedStat stat) {
        if (stat == null) {
            return 0.0;
        }
        return values.getOrDefault(stat, 0.0);
    }

    public void set(ItemizedStat stat, double value) {
        if (stat == null) {
            return;
        }
        if (Math.abs(value) <= 1e-9) {
            values.remove(stat);
            return;
        }
        values.put(stat, value);
    }

    public void add(ItemizedStat stat, double value) {
        if (stat == null || Math.abs(value) <= 1e-9) {
            return;
        }
        set(stat, get(stat) + value);
    }

    public void addAll(ItemizedStatBlock other) {
        if (other == null) {
            return;
        }
        for (Map.Entry<ItemizedStat, Double> entry : other.values.entrySet()) {
            add(entry.getKey(), entry.getValue());
        }
    }

    public ItemizedStatBlock copy() {
        ItemizedStatBlock out = new ItemizedStatBlock();
        out.values.putAll(this.values);
        return out;
    }

    public Map<ItemizedStat, Double> asMap() {
        return Collections.unmodifiableMap(values);
    }
}
