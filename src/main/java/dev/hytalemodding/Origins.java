package dev.hytalemodding;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.DrainPlayerFromWorldEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import dev.hytalemodding.origins.commands.*;
import dev.hytalemodding.origins.component.GameModeDataComponent;
import dev.hytalemodding.origins.database.JsonLevelRepository;
import dev.hytalemodding.origins.bonus.SkillStatBonusListener;
import dev.hytalemodding.origins.events.LevelingVisualsListener;
import dev.hytalemodding.origins.events.FarmingHarvestListener;
import dev.hytalemodding.origins.gamemode.BuilderActionOpenGameModeDialogue;
import dev.hytalemodding.origins.system.FarmingHarvestPickupSystem;
import dev.hytalemodding.origins.component.FishingBobberComponent;
import dev.hytalemodding.origins.system.FishingBobberSystem;
import dev.hytalemodding.origins.system.FishingCastSystem;
import dev.hytalemodding.origins.interaction.FishingInteraction;
import dev.hytalemodding.origins.interaction.RepairBenchInteraction;
import dev.hytalemodding.origins.system.FishingRodIdleSystem;
import dev.hytalemodding.origins.events.PlayerJoinListener;
import dev.hytalemodding.origins.level.LevelingService;
import dev.hytalemodding.origins.level.formulas.LevelFormula;
import dev.hytalemodding.origins.system.CombatStateSystem;
import dev.hytalemodding.origins.system.CombatXpSystem;
import dev.hytalemodding.origins.system.CraftingRestrictionSystem;
import dev.hytalemodding.origins.system.CraftingXpSystem;
import dev.hytalemodding.origins.system.GatheringXpSystem;
import dev.hytalemodding.origins.system.MiningDurabilitySystem;
import dev.hytalemodding.origins.system.MiningSpeedSystem;
import dev.hytalemodding.origins.system.SkillCombatBonusSystem;
import dev.hytalemodding.origins.system.SkillRegenSystem;
import dev.hytalemodding.origins.events.ArmorRequirementListener;
import dev.hytalemodding.origins.events.TradePackInventoryListener;
import dev.hytalemodding.origins.system.SyncTaskSystem;
import dev.hytalemodding.origins.system.TimedCraftingXpSystem;
import dev.hytalemodding.origins.system.WoodcuttingDurabilitySystem;
import dev.hytalemodding.origins.system.WoodcuttingSpeedSystem;
import dev.hytalemodding.origins.util.NameplateManager;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import dev.hytalemodding.origins.slayer.JsonSlayerRepository;
import dev.hytalemodding.origins.slayer.SlayerService;
import dev.hytalemodding.origins.slayer.SlayerTaskDefinition;
import dev.hytalemodding.origins.slayer.SlayerTaskRegistry;
import dev.hytalemodding.origins.slayer.SlayerTaskTier;
import dev.hytalemodding.origins.slayer.SlayerTaskKillSystem;
import dev.hytalemodding.origins.slayer.BuilderActionOpenSlayerDialogue;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.asset.builder.BuilderFactory;
import com.hypixel.hytale.server.npc.instructions.Action;
import dev.hytalemodding.origins.npc.NpcLevelComponent;
import dev.hytalemodding.origins.npc.NpcLevelConfig;
import dev.hytalemodding.origins.npc.NpcLevelConfigRepository;
import dev.hytalemodding.origins.npc.NpcLevelService;
import dev.hytalemodding.origins.npc.NpcLevelAssignmentSystem;
import dev.hytalemodding.origins.npc.NpcCombatScalingSystem;
import dev.hytalemodding.origins.npc.NpcLevelDisplaySystem;

import javax.annotation.Nonnull;
import javax.swing.text.html.parser.Entity;
import java.util.logging.Level;
import java.util.List;

/**
 * Origins - RPG Leveling System for Hytale
 * <p>
 * Main plugin class that initializes and manages:
 * - Adventurer (global) leveling system
 * - Class progression with tier requirements
 * - Character attributes (Strength, Constitution, Intellect, Agility, Wisdom)
 * - MMO-style nameplates showing level and class
 * - Combat XP distribution
 * - Persistent player data via JSON repositories
 */
public class Origins extends JavaPlugin {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static Origins instance;
    private LevelingService service;
    private SlayerService slayerService;
    private NpcLevelService npcLevelService;
    private static ComponentType<EntityStore, FishingBobberComponent> fishingBobberComponentType;
    private static ComponentType<EntityStore, NpcLevelComponent> npcLevelComponentType;
    private static ComponentType<EntityStore, GameModeDataComponent> gameModeDataComponentType;

    /**
     * Constructs the Origins plugin.
     *
     * @param init Plugin initialization context
     */
    public Origins(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    @Override
    protected void setup() {
        // Initialize database repositories
        JsonLevelRepository levelRepository = new JsonLevelRepository("./origins_data");

        // Initialize leveling formula
        LevelFormula formula = new LevelFormula();

        // Initialize core services.
        this.service = new LevelingService(formula, levelRepository);

        // Initialize Slayer services.
        SlayerTaskRegistry slayerTaskRegistry = buildSlayerTaskRegistry();
        for (String issue : slayerTaskRegistry.validate()) {
            LOGGER.at(Level.WARNING).log("Slayer task registry issue: " + issue);
        }
        JsonSlayerRepository slayerRepository = new JsonSlayerRepository("./origins_data");
        this.slayerService = new SlayerService(slayerRepository, slayerTaskRegistry);

        // Initialize NPC level services.
        NpcLevelConfigRepository npcLevelConfigRepository = new NpcLevelConfigRepository("./origins_data");
        NpcLevelConfig npcLevelConfig = npcLevelConfigRepository.loadOrCreate();
        this.npcLevelService = new NpcLevelService(npcLevelConfig);

        registerSlayerNpcActions();
        registerGameModeNPCActions();

        // Initialize nameplate manager
        NameplateManager.init(this.service);

        fishingBobberComponentType = this.getEntityStoreRegistry()
            .registerComponent(FishingBobberComponent.class, FishingBobberComponent::new);
        npcLevelComponentType = this.getEntityStoreRegistry()
            .registerComponent(NpcLevelComponent.class, NpcLevelComponent::new);
        gameModeDataComponentType = this.getEntityStoreRegistry()
            .registerComponent(GameModeDataComponent.class, GameModeDataComponent::new);

        // Register event listeners
        PlayerJoinListener joinListener = new PlayerJoinListener(this.service, this.slayerService);
        this.getEventRegistry().registerGlobal(AddPlayerToWorldEvent.class, joinListener::onPlayerJoin);
        this.getEventRegistry().registerGlobal(DrainPlayerFromWorldEvent.class, joinListener::onPlayerLeave);
        this.getEventRegistry().registerGlobal(PlayerInteractEvent.class, new FarmingHarvestListener()::onPlayerInteract);
        this.getEventRegistry().registerGlobal(
            com.hypixel.hytale.server.core.event.events.entity.LivingEntityInventoryChangeEvent.class,
            new ArmorRequirementListener()::onInventoryChange
        );
        this.getEventRegistry().registerGlobal(
            com.hypixel.hytale.server.core.event.events.entity.LivingEntityInventoryChangeEvent.class,
            new TradePackInventoryListener()::onInventoryChange
        );
        this.getCodecRegistry(Interaction.CODEC).register(
            "OriginsFishing",
            FishingInteraction.class,
            FishingInteraction.CODEC
        );
        this.getCodecRegistry(Interaction.CODEC).register(
            "OriginsRepairBench",
            RepairBenchInteraction.class,
            RepairBenchInteraction.CODEC
        );
        this.service.registerLevelUpListener(new LevelingVisualsListener());
        this.service.registerLevelUpListener(new SkillStatBonusListener());

        // Register commands
        this.getCommandRegistry().registerCommand(new CheckTagsCommand());
        this.getCommandRegistry().registerCommand(new CharacterCommand());
        this.getCommandRegistry().registerCommand(new SetSkillCommand());
        this.getCommandRegistry().registerCommand(new ClearNpcHologramsCommand());

        // Register ECS systems
        this.getEntityStoreRegistry().registerSystem(new CombatStateSystem());
        this.getEntityStoreRegistry().registerSystem(new SkillCombatBonusSystem());
        this.getEntityStoreRegistry().registerSystem(new CombatXpSystem());
        this.getEntityStoreRegistry().registerSystem(new GatheringXpSystem());
        this.getEntityStoreRegistry().registerSystem(new CraftingRestrictionSystem());
        this.getEntityStoreRegistry().registerSystem(new CraftingXpSystem());
        this.getEntityStoreRegistry().registerSystem(new TimedCraftingXpSystem());
        this.getEntityStoreRegistry().registerSystem(new FarmingHarvestPickupSystem());
        this.getEntityStoreRegistry().registerSystem(new FishingBobberSystem());
        this.getEntityStoreRegistry().registerSystem(new FishingCastSystem());
        this.getEntityStoreRegistry().registerSystem(new FishingRodIdleSystem());
        this.getEntityStoreRegistry().registerSystem(new MiningSpeedSystem());
        this.getEntityStoreRegistry().registerSystem(new MiningDurabilitySystem());
        this.getEntityStoreRegistry().registerSystem(new WoodcuttingSpeedSystem());
        this.getEntityStoreRegistry().registerSystem(new WoodcuttingDurabilitySystem());
        this.getEntityStoreRegistry().registerSystem(new SkillRegenSystem());
        this.getEntityStoreRegistry().registerSystem(new SyncTaskSystem());
        this.getEntityStoreRegistry().registerSystem(new SlayerTaskKillSystem(this.slayerService));
        this.getEntityStoreRegistry().registerSystem(new NpcLevelAssignmentSystem(this.npcLevelService));
        this.getEntityStoreRegistry().registerSystem(new NpcCombatScalingSystem(this.npcLevelService));
        this.getEntityStoreRegistry().registerSystem(new NpcLevelDisplaySystem());


        LOGGER.at(Level.INFO).log("Origins leveling system initialized successfully!");
    }

    /**
     * Gets the LevelingService instance for managing player levels and XP.
     *
     * @return The singleton LevelingService instance
     */
    public static LevelingService getService() {
        return instance.service;
    }

    public static SlayerService getSlayerService() {
        return instance.slayerService;
    }

    public static ComponentType<EntityStore, NpcLevelComponent> getNpcLevelComponentType() {
        return npcLevelComponentType;
    }

    public static NpcLevelService getNpcLevelService() {
        return instance != null ? instance.npcLevelService : null;
    }


    private void registerSlayerNpcActions() {
        NPCPlugin npcPlugin = NPCPlugin.get();
        if (npcPlugin == null) {
            return;
        }

        BuilderFactory<Action> actionFactory = npcPlugin.getBuilderManager().getFactory(Action.class);
        if (actionFactory == null) {
            actionFactory = new BuilderFactory<>(Action.class, NPCPlugin.FACTORY_CLASS_ACTION);
            npcPlugin.getBuilderManager().registerFactory(actionFactory);
        }

        actionFactory.add("OpenSlayerDialogue", BuilderActionOpenSlayerDialogue::new);
    }
    private void registerGameModeNPCActions() {
        NPCPlugin npcPlugin = NPCPlugin.get();
        if (npcPlugin == null) {
            return;
        }

        BuilderFactory<Action> actionFactory = npcPlugin.getBuilderManager().getFactory(Action.class);
        if (actionFactory == null) {
            actionFactory = new BuilderFactory<>(Action.class, NPCPlugin.FACTORY_CLASS_ACTION);
            npcPlugin.getBuilderManager().registerFactory(actionFactory);
        }

        actionFactory.add("OpenGameModeDialogue", BuilderActionOpenGameModeDialogue::new);
    }

    private SlayerTaskRegistry buildSlayerTaskRegistry() {
        SlayerTaskRegistry registry = new SlayerTaskRegistry();

        registry.registerTier(new SlayerTaskTier(1, 19, List.of(
                new SlayerTaskDefinition("tier1_rats_a", "Rat", 8, 14),
                new SlayerTaskDefinition("tier1_rats_b", "Rat", 10, 16)
        )));

        registry.registerTier(new SlayerTaskTier(20, 39, List.of(
                new SlayerTaskDefinition("tier20_rats_a", "Rat", 12, 18),
                new SlayerTaskDefinition("tier20_rats_b", "Rat", 14, 20)
        )));

        registry.registerTier(new SlayerTaskTier(40, 59, List.of(
                new SlayerTaskDefinition("tier40_rats_a", "Rat", 18, 26),
                new SlayerTaskDefinition("tier40_rats_b", "Rat", 20, 28)
        )));

        registry.registerTier(new SlayerTaskTier(60, 79, List.of(
                new SlayerTaskDefinition("tier60_rats_a", "Rat", 24, 34),
                new SlayerTaskDefinition("tier60_rats_b", "Rat", 26, 38)
        )));

        registry.registerTier(new SlayerTaskTier(80, 99, List.of(
                new SlayerTaskDefinition("tier80_rats_a", "Rat", 32, 46),
                new SlayerTaskDefinition("tier80_rats_b", "Rat", 36, 52)
        )));

        return registry;
    }

    public static ComponentType<EntityStore, FishingBobberComponent> getFishingBobberComponentType() {
        return fishingBobberComponentType;
    }
    public static ComponentType<EntityStore, GameModeDataComponent> getGameModeDataComponentType() {
        return gameModeDataComponentType;
    }
}
