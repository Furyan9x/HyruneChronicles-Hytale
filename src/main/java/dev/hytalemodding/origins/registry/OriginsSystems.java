package dev.hytalemodding.origins.registry;


import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.origins.npc.NpcCombatScalingSystem;
import dev.hytalemodding.origins.npc.NpcLevelAssignmentSystem;
import dev.hytalemodding.origins.npc.NpcLevelDisplaySystem;
import dev.hytalemodding.origins.npc.NpcLevelService;
import dev.hytalemodding.origins.slayer.SlayerService;
import dev.hytalemodding.origins.system.*;


/**
 * 
 */
public class OriginsSystems {

    public static void register(JavaPlugin plugin, SlayerService slayerService, NpcLevelService npcLevelService) {
        ComponentRegistryProxy<EntityStore> registry = plugin.getEntityStoreRegistry();

        // Core Combat & XP
        registry.registerSystem(new CombatStateSystem());
        registry.registerSystem(new SkillCombatBonusSystem());
        registry.registerSystem(new CombatXpSystem(slayerService));
        registry.registerSystem(new SkillRegenSystem());

        // Profession XP & Logic
        registry.registerSystem(new GatheringXpSystem());
        registry.registerSystem(new CraftingRestrictionSystem());
        registry.registerSystem(new CraftingXpSystem());
        registry.registerSystem(new TimedCraftingXpSystem());
        registry.registerSystem(new FarmingPlantingRestrictionSystem());
        registry.registerSystem(new FarmingHarvestPickupSystem());
        registry.registerSystem(new ToolTypeEnforcementSystem());
        registry.registerSystem(new MiningSpeedSystem());
        registry.registerSystem(new MiningDurabilitySystem());
        registry.registerSystem(new WoodcuttingSpeedSystem());
        registry.registerSystem(new WoodcuttingDurabilitySystem());

        // Fishing
        registry.registerSystem(new FishingBobberSystem());
        registry.registerSystem(new FishingCastSystem());
        registry.registerSystem(new FishingRodIdleSystem());

        // Syncing
        registry.registerSystem(new SyncTaskSystem());

        // Service-Dependent Systems
        registry.registerSystem(new NpcLevelAssignmentSystem(npcLevelService));
        registry.registerSystem(new NpcCombatScalingSystem(npcLevelService));
        registry.registerSystem(new NpcLevelDisplaySystem());
    }
}
