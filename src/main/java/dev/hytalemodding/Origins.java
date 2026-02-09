package dev.hytalemodding;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.event.events.entity.LivingEntityInventoryChangeEvent;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.DrainPlayerFromWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
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
import dev.hytalemodding.origins.registry.OriginsComponents;
import dev.hytalemodding.origins.registry.OriginsDialogue;
import dev.hytalemodding.origins.registry.OriginsSystems;
import dev.hytalemodding.origins.component.FishingBobberComponent;
import dev.hytalemodding.origins.interaction.FishingInteraction;
import dev.hytalemodding.origins.interaction.RepairBenchInteraction;
import dev.hytalemodding.origins.events.PlayerJoinListener;
import dev.hytalemodding.origins.level.LevelingService;
import dev.hytalemodding.origins.level.formulas.LevelFormula;
import dev.hytalemodding.origins.events.ArmorRequirementListener;
import dev.hytalemodding.origins.events.TradePackInventoryListener;
import dev.hytalemodding.origins.slayer.*;
import dev.hytalemodding.origins.util.NameplateManager;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import dev.hytalemodding.origins.npc.NpcLevelComponent;
import dev.hytalemodding.origins.npc.NpcLevelConfig;
import dev.hytalemodding.origins.npc.NpcLevelConfigRepository;
import dev.hytalemodding.origins.npc.NpcLevelService;
import dev.hytalemodding.origins.quests.JsonQuestRepository;
import dev.hytalemodding.origins.quests.QuestManager;
import dev.hytalemodding.origins.quests.QuestRepository;

import javax.annotation.Nonnull;
import java.util.logging.Level;

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

        // Initialize NPC level services.
        NpcLevelConfigRepository npcLevelConfigRepository = new NpcLevelConfigRepository("./origins_data");
        NpcLevelConfig npcLevelConfig = npcLevelConfigRepository.loadOrCreate();
        this.npcLevelService = new NpcLevelService(npcLevelConfig);

        // Initialize Slayer services.
        SlayerTaskRegistry slayerTaskRegistry = SlayerDataInitializer.buildRegistry();
        for (String issue : slayerTaskRegistry.validate()) {
            LOGGER.at(Level.WARNING).log("Slayer task registry issue: " + issue);
        }
        JsonSlayerRepository slayerRepository = new JsonSlayerRepository("./origins_data");
        this.slayerService = new SlayerService(slayerRepository, slayerTaskRegistry, npcLevelService);

        QuestRepository questRepository = new JsonQuestRepository("./origins_data");
        QuestManager questManager = QuestManager.get();
        questManager.setRepository(questRepository);

        // Initialize nameplate manager
        NameplateManager.init(this.service);
        // Initialize Components
        OriginsComponents.register(this);
        OriginsSystems.register(this, this.slayerService, this.npcLevelService);
        OriginsDialogue.init(this.service, this.slayerService);

        // Register event listeners
        PlayerJoinListener joinListener = new PlayerJoinListener(this.service, this.slayerService, questManager);

        this.getEventRegistry().registerGlobal(AddPlayerToWorldEvent.class, joinListener::onPlayerJoin);
        System.out.println("[DEBUG] Registered AddPlayerToWorldEvent listener");

        this.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, joinListener::onPlayerDisconnect);
        System.out.println("[DEBUG] Registered PlayerDisconnectEvent listener");

// Keep DrainPlayerFromWorldEvent too - it might be useful for world teleports
        this.getEventRegistry().registerGlobal(DrainPlayerFromWorldEvent.class, joinListener::onPlayerLeave);
        System.out.println("[DEBUG] Registered DrainPlayerFromWorldEvent listener");
        this.getEventRegistry().registerGlobal(PlayerInteractEvent.class, new FarmingHarvestListener()::onPlayerInteract);
        this.getEventRegistry().registerGlobal(LivingEntityInventoryChangeEvent.class, new ArmorRequirementListener()::onInventoryChange);
        this.getEventRegistry().registerGlobal(LivingEntityInventoryChangeEvent.class, new TradePackInventoryListener()::onInventoryChange);

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


    public static NpcLevelService getNpcLevelService() {
        return instance != null ? instance.npcLevelService : null;
    }


    public static ComponentType<EntityStore, NpcLevelComponent> getNpcLevelComponentType() {
        return OriginsComponents.NPC_LEVEL;
    }

    public static ComponentType<EntityStore, FishingBobberComponent> getFishingBobberComponentType() {
        return OriginsComponents.FISHING_BOBBER;
    }

    public static ComponentType<EntityStore, GameModeDataComponent> getGameModeDataComponentType() {
        return OriginsComponents.GAMEMODE_DATA;
    }
}
