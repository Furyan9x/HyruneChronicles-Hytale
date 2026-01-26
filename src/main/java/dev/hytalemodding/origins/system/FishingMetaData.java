package dev.hytalemodding.origins.system;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import java.util.UUID;

public class FishingMetaData {
    public static final String KEY = "OriginsBoundBobber";
    public static final BuilderCodec<FishingMetaData> CODEC;
    public static final KeyedCodec<FishingMetaData> KEYED_CODEC;

    private UUID boundBobber;

    public FishingMetaData() {
    }

    public UUID getBoundBobber() {
        return boundBobber;
    }

    public void setBoundBobber(UUID boundBobber) {
        this.boundBobber = boundBobber;
    }

    static {
        CODEC = BuilderCodec.builder(FishingMetaData.class, FishingMetaData::new)
            .append(
                new KeyedCodec<>("BoundBobber", Codec.UUID_BINARY),
                FishingMetaData::setBoundBobber,
                FishingMetaData::getBoundBobber
            )
            .documentation("The bobber that is bound to the fishing rod.")
            .add()
            .build();
        KEYED_CODEC = new KeyedCodec<>(KEY, CODEC);
    }
}
