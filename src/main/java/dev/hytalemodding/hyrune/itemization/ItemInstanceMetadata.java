package dev.hytalemodding.hyrune.itemization;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import dev.hytalemodding.hyrune.repair.ItemRarity;

/**
 * Per-item rolled metadata for rarity and stat variation.
 */
public class ItemInstanceMetadata {
    public static final String KEY = "HyruneItemInstance";
    public static final int CURRENT_SCHEMA_VERSION = 1;
    public static final BuilderCodec<ItemInstanceMetadata> CODEC = BuilderCodec.builder(ItemInstanceMetadata.class, ItemInstanceMetadata::new)
        .append(new KeyedCodec<>("Version", Codec.LONG), ItemInstanceMetadata::setVersion, ItemInstanceMetadata::getVersion).add()
        .append(new KeyedCodec<>("Rarity", Codec.STRING), ItemInstanceMetadata::setRarityRaw, ItemInstanceMetadata::getRarityRaw).add()
        .append(new KeyedCodec<>("Catalyst", Codec.STRING), ItemInstanceMetadata::setCatalystRaw, ItemInstanceMetadata::getCatalystRaw).add()
        .append(new KeyedCodec<>("Source", Codec.STRING), ItemInstanceMetadata::setSourceRaw, ItemInstanceMetadata::getSourceRaw).add()
        .append(new KeyedCodec<>("Seed", Codec.LONG), ItemInstanceMetadata::setSeed, ItemInstanceMetadata::getSeed).add()
        .append(new KeyedCodec<>("DamageRoll", Codec.DOUBLE), ItemInstanceMetadata::setDamageRoll, ItemInstanceMetadata::getDamageRoll).add()
        .append(new KeyedCodec<>("DefenceRoll", Codec.DOUBLE), ItemInstanceMetadata::setDefenceRoll, ItemInstanceMetadata::getDefenceRoll).add()
        .append(new KeyedCodec<>("HealingRoll", Codec.DOUBLE), ItemInstanceMetadata::setHealingRoll, ItemInstanceMetadata::getHealingRoll).add()
        .append(new KeyedCodec<>("UtilityRoll", Codec.DOUBLE), ItemInstanceMetadata::setUtilityRoll, ItemInstanceMetadata::getUtilityRoll).add()
        .append(new KeyedCodec<>("DroppedPenalty", Codec.DOUBLE), ItemInstanceMetadata::setDroppedPenalty, ItemInstanceMetadata::getDroppedPenalty).add()
        .build();
    public static final KeyedCodec<ItemInstanceMetadata> KEYED_CODEC = new KeyedCodec<>(KEY, CODEC);

    private long version;
    private String rarityRaw = ItemRarity.COMMON.name();
    private String catalystRaw = CatalystAffinity.NONE.name();
    private String sourceRaw = ItemRollSource.CRAFTED.name();
    private long seed = 0L;
    private double damageRoll;
    private double defenceRoll;
    private double healingRoll;
    private double utilityRoll;
    private double droppedPenalty;

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

    public double getDamageRoll() {
        return damageRoll;
    }

    public void setDamageRoll(double damageRoll) {
        this.damageRoll = damageRoll;
    }

    public double getDefenceRoll() {
        return defenceRoll;
    }

    public void setDefenceRoll(double defenceRoll) {
        this.defenceRoll = defenceRoll;
    }

    public double getHealingRoll() {
        return healingRoll;
    }

    public void setHealingRoll(double healingRoll) {
        this.healingRoll = healingRoll;
    }

    public double getUtilityRoll() {
        return utilityRoll;
    }

    public void setUtilityRoll(double utilityRoll) {
        this.utilityRoll = utilityRoll;
    }

    public double getDroppedPenalty() {
        return droppedPenalty;
    }

    public void setDroppedPenalty(double droppedPenalty) {
        this.droppedPenalty = droppedPenalty;
    }
}
