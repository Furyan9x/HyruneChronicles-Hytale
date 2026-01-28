package dev.hytalemodding.origins.slayer;

import java.util.UUID;

public interface SlayerRepository {
    SlayerPlayerData load(UUID uuid);

    void save(SlayerPlayerData data);
}
