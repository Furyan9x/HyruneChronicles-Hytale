package dev.hytalemodding.origins.events;
import java.util.UUID;

public interface LevelUpListener {
    void onLevelUp(UUID uuid, int newLevel, String source);
}