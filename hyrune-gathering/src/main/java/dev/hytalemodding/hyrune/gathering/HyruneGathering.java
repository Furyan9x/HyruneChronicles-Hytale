package dev.hytalemodding.hyrune.gathering;

import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import dev.hytalemodding.hyrune.events.FarmingHarvestListener;
import dev.hytalemodding.hyrune.events.FarmingRequirementListener;
import dev.hytalemodding.hyrune.interaction.FishingInteraction;
import dev.hytalemodding.hyrune.system.FarmingHarvestPickupSystem;
import dev.hytalemodding.hyrune.system.FarmingPlantingRestrictionSystem;
import dev.hytalemodding.hyrune.system.FishingBobberSystem;
import dev.hytalemodding.hyrune.system.FishingCastSystem;
import dev.hytalemodding.hyrune.system.FishingRodIdleSystem;
import dev.hytalemodding.hyrune.system.GatheringXpSystem;
import dev.hytalemodding.hyrune.system.MiningDurabilitySystem;
import dev.hytalemodding.hyrune.system.MiningSpeedSystem;
import dev.hytalemodding.hyrune.system.ToolTypeEnforcementSystem;
import dev.hytalemodding.hyrune.system.WoodcuttingDurabilitySystem;
import dev.hytalemodding.hyrune.system.WoodcuttingSpeedSystem;

import javax.annotation.Nonnull;

/**
 * Plugin module that owns gathering/fishing system registration.
 */
public class HyruneGathering extends JavaPlugin {

    public HyruneGathering(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        this.getEntityStoreRegistry().registerSystem(new GatheringXpSystem());
        this.getEntityStoreRegistry().registerSystem(new FarmingPlantingRestrictionSystem());
        this.getEntityStoreRegistry().registerSystem(new FarmingHarvestPickupSystem());
        this.getEntityStoreRegistry().registerSystem(new ToolTypeEnforcementSystem());
        this.getEntityStoreRegistry().registerSystem(new MiningSpeedSystem());
        this.getEntityStoreRegistry().registerSystem(new MiningDurabilitySystem());
        this.getEntityStoreRegistry().registerSystem(new WoodcuttingSpeedSystem());
        this.getEntityStoreRegistry().registerSystem(new WoodcuttingDurabilitySystem());
        this.getEntityStoreRegistry().registerSystem(new FishingBobberSystem());
        this.getEntityStoreRegistry().registerSystem(new FishingCastSystem());
        this.getEntityStoreRegistry().registerSystem(new FishingRodIdleSystem());

        this.getEventRegistry().registerGlobal(PlayerInteractEvent.class, new FarmingHarvestListener()::onPlayerInteract);
        this.getEventRegistry().registerGlobal(PlayerInteractEvent.class, new FarmingRequirementListener()::onPlayerInteract);

        this.getCodecRegistry(Interaction.CODEC).register(
            "HyruneFishing",
            FishingInteraction.class,
            FishingInteraction.CODEC
        );
    }
}
