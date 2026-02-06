package dev.hytalemodding.origins.registry;


import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.origins.component.FishingBobberComponent;
import dev.hytalemodding.origins.component.GameModeDataComponent;
import dev.hytalemodding.origins.npc.NpcLevelComponent;

public class OriginsComponents {

    public static ComponentType<EntityStore, FishingBobberComponent> FISHING_BOBBER;
    public static ComponentType<EntityStore, NpcLevelComponent> NPC_LEVEL;
    public static ComponentType<EntityStore, GameModeDataComponent> GAMEMODE_DATA;

    public static void register(JavaPlugin plugin) {
        FISHING_BOBBER = plugin.getEntityStoreRegistry()
                .registerComponent(FishingBobberComponent.class, FishingBobberComponent::new);

        NPC_LEVEL = plugin.getEntityStoreRegistry()
                .registerComponent(NpcLevelComponent.class, NpcLevelComponent::new);

        GAMEMODE_DATA = plugin.getEntityStoreRegistry()
                .registerComponent(GameModeDataComponent.class, "gameMode", GameModeDataComponent.CODEC);
    }
}