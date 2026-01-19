package dev.hytalemodding.origins.events;
import java.util.UUID;

public interface XpGainListener {
    void onXpGain(UUID uuid, long amount);
}