package dev.hytalemodding;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.event.events.entity.LivingEntityInventoryChangeEvent;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.DrainPlayerFromWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import dev.hytalemodding.hyrune.commands.CharacterCommand;
import dev.hytalemodding.hyrune.commands.CheckTagsCommand;
import dev.hytalemodding.hyrune.commands.ClearNpcHologramsCommand;
import dev.hytalemodding.hyrune.commands.ReloadConfigCommand;
import dev.hytalemodding.hyrune.commands.SetSkillCommand;
import dev.hytalemodding.hyrune.commands.SkillInfoCommand;
import dev.hytalemodding.hyrune.commands.SocialDebugCommand;
import dev.hytalemodding.hyrune.commands.ItemMetaCommand;
import dev.hytalemodding.hyrune.commands.ItemRollsCommand;
import dev.hytalemodding.hyrune.commands.ItemDiagCommand;
import dev.hytalemodding.hyrune.commands.ItemStatsCommand;
import dev.hytalemodding.hyrune.commands.GemUiCommand;
import dev.hytalemodding.hyrune.component.GameModeDataComponent;
import dev.hytalemodding.hyrune.database.JsonLevelRepository;
import dev.hytalemodding.hyrune.database.JsonQuestRepository;
import dev.hytalemodding.hyrune.database.JsonSocialRepository;
import dev.hytalemodding.hyrune.database.JsonSlayerRepository;
import dev.hytalemodding.hyrune.database.QuestRepository;
import dev.hytalemodding.hyrune.bonus.SkillStatBonusListener;
import dev.hytalemodding.hyrune.config.HyruneConfigManager;
import dev.hytalemodding.hyrune.events.LevelingVisualsListener;
import dev.hytalemodding.hyrune.events.FarmingHarvestListener;
import dev.hytalemodding.hyrune.events.FarmingRequirementListener;
import dev.hytalemodding.hyrune.events.ItemizationInventoryListener;
import dev.hytalemodding.hyrune.itemization.tooltip.HyruneDynamicTooltipService;
import dev.hytalemodding.hyrune.registry.HyruneComponents;
import dev.hytalemodding.hyrune.registry.HyruneDialogue;
import dev.hytalemodding.hyrune.registry.HyruneSystems;
import dev.hytalemodding.hyrune.component.FishingBobberComponent;
import dev.hytalemodding.hyrune.interaction.FishingInteraction;
import dev.hytalemodding.hyrune.interaction.GemSocketInteraction;
import dev.hytalemodding.hyrune.interaction.RepairBenchInteraction;
import dev.hytalemodding.hyrune.events.PlayerJoinListener;
import dev.hytalemodding.hyrune.level.LevelingService;
import dev.hytalemodding.hyrune.level.formulas.LevelFormula;
import dev.hytalemodding.hyrune.events.ArmorRequirementListener;
import dev.hytalemodding.hyrune.events.TradePackInventoryListener;
import dev.hytalemodding.hyrune.slayer.SlayerDataInitializer;
import dev.hytalemodding.hyrune.slayer.SlayerService;
import dev.hytalemodding.hyrune.slayer.SlayerTaskRegistry;
import dev.hytalemodding.hyrune.repair.RepairProfileConfig;
import dev.hytalemodding.hyrune.repair.RepairProfileConfigRepository;
import dev.hytalemodding.hyrune.repair.RepairProfileRegistry;
import dev.hytalemodding.hyrune.social.SocialInteractionWatcher;
import dev.hytalemodding.hyrune.social.SocialService;
import dev.hytalemodding.hyrune.util.NameplateManager;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import dev.hytalemodding.hyrune.npc.NpcLevelComponent;
import dev.hytalemodding.hyrune.npc.NpcLevelConfig;
import dev.hytalemodding.hyrune.npc.NpcLevelConfigRepository;
import dev.hytalemodding.hyrune.npc.NpcLevelService;
import dev.hytalemodding.hyrune.quests.QuestManager;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Hyrune Chronicles - RPG Leveling System for Hytale
 * <p>
 * Main plugin class that initializes and manages:
 * - Adventurer (global) leveling system
 * - Class progression with tier requirements
 * - Character attributes (Strength, Constitution, Intellect, Agility, Wisdom)
 * - MMO-style nameplates showing level and class
 * - Combat XP distribution
 * - Persistent player data via JSON repositories
 */
public class Hyrune extends JavaPlugin {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static Hyrune instance;
    private LevelingService service;
    private SlayerService slayerService;
    private NpcLevelService npcLevelService;
    private SocialService socialService;
    private PacketFilter socialInteractionWatcherFilter;
    private HyruneDynamicTooltipService dynamicTooltipService;


    /**
     * Constructs the Hyrune plugin.
     *
     * @param init Plugin initialization context
     */
    public Hyrune(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    @Override
    protected void setup() {
        // Load config and bootstrap persistence-backed services first.
        JsonLevelRepository levelRepository = new JsonLevelRepository("./hyrune_data");
        HyruneConfigManager.reload();
        LevelFormula formula = new LevelFormula();
        this.service = new LevelingService(formula, levelRepository);

        // Initialize progression/AI services that depend on local data repositories.
        NpcLevelConfigRepository npcLevelConfigRepository = new NpcLevelConfigRepository("./hyrune_data");
        NpcLevelConfig npcLevelConfig = npcLevelConfigRepository.loadOrCreate();
        this.npcLevelService = new NpcLevelService(npcLevelConfig);

        SlayerTaskRegistry slayerTaskRegistry = SlayerDataInitializer.buildRegistry();
        for (String issue : slayerTaskRegistry.validate()) {
            LOGGER.at(Level.WARNING).log("Slayer task registry issue: " + issue);
        }
        JsonSlayerRepository slayerRepository = new JsonSlayerRepository("./hyrune_data");
        this.slayerService = new SlayerService(slayerRepository, slayerTaskRegistry, npcLevelService);
        this.socialService = new SocialService(new JsonSocialRepository("./hyrune_data"));
        RepairProfileConfigRepository repairProfileRepository = new RepairProfileConfigRepository("./hyrune_data");
        RepairProfileConfig repairConfig = repairProfileRepository.loadOrCreate(RepairProfileRegistry.getDefaultDefinitions());
        RepairProfileRegistry.reloadFromConfig(repairConfig);

        QuestRepository questRepository = new JsonQuestRepository("./hyrune_data");
        QuestManager questManager = QuestManager.get();
        questManager.setRepository(questRepository);

        // Register shared component/system/dialogue registries before event listeners.
        NameplateManager.init(this.service);
        HyruneComponents.register(this);
        HyruneSystems.register(this, this.slayerService, this.npcLevelService);
        HyruneDialogue.init(this.service, this.slayerService);

        PlayerJoinListener joinListener = new PlayerJoinListener(this.service, this.slayerService, questManager, this.socialService);
        registerCoreEvents(joinListener);
        registerDynamicTooltipHooks();
        registerInteractions();

        this.service.registerLevelUpListener(new LevelingVisualsListener());
        this.service.registerLevelUpListener(new SkillStatBonusListener());
        this.service.registerLevelUpListener((uuid, newLevel, source) -> {
            if (uuid != null) {
                Hyrune.refreshDynamicTooltipsForPlayer(uuid);
            }
        });

        registerCommands();


        LOGGER.at(Level.INFO).log("Hyrune Chronicles leveling system initialized successfully!");
    }

    private void registerCoreEvents(PlayerJoinListener joinListener) {
        // Event and packet hooks are attached after registries are available.
        this.getEventRegistry().registerGlobal(AddPlayerToWorldEvent.class, joinListener::onPlayerJoin);
        this.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, joinListener::onPlayerDisconnect);
        // Keep DrainPlayerFromWorldEvent too - it might be useful for world teleports.
        this.getEventRegistry().registerGlobal(DrainPlayerFromWorldEvent.class, joinListener::onPlayerLeave);
        this.getEventRegistry().registerGlobal(PlayerInteractEvent.class, new FarmingHarvestListener()::onPlayerInteract);
        this.getEventRegistry().registerGlobal(PlayerInteractEvent.class, new FarmingRequirementListener()::onPlayerInteract);
        this.getEventRegistry().registerGlobal(LivingEntityInventoryChangeEvent.class, new ArmorRequirementListener()::onInventoryChange);
        this.getEventRegistry().registerGlobal(LivingEntityInventoryChangeEvent.class, new TradePackInventoryListener()::onInventoryChange);
        this.getEventRegistry().registerGlobal(LivingEntityInventoryChangeEvent.class, new ItemizationInventoryListener()::onInventoryChange);
        this.socialInteractionWatcherFilter = PacketAdapters.registerInbound(new SocialInteractionWatcher());
    }

    private void registerDynamicTooltipHooks() {
        if (HyruneConfigManager.getConfig().enableDynamicItemTooltips) {
            this.dynamicTooltipService = new HyruneDynamicTooltipService();
            this.dynamicTooltipService.register();
            LOGGER.at(Level.INFO).log("Dynamic metadata tooltip pipeline registered.");
        }
        this.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, event -> {
            if (this.dynamicTooltipService != null && event != null && event.getPlayerRef() != null) {
                this.dynamicTooltipService.onPlayerLeave(event.getPlayerRef().getUuid());
            }
        });
    }

    private void registerInteractions() {
        this.getCodecRegistry(Interaction.CODEC).register(
            "HyruneFishing",
            FishingInteraction.class,
            FishingInteraction.CODEC
        );
        this.getCodecRegistry(Interaction.CODEC).register(
            "HyruneRepairBench",
            RepairBenchInteraction.class,
            RepairBenchInteraction.CODEC
        );
        this.getCodecRegistry(Interaction.CODEC).register(
            "HyruneGemSocket",
            GemSocketInteraction.class,
            GemSocketInteraction.CODEC
        );
    }

    private void registerCommands() {
        this.getCommandRegistry().registerCommand(new CheckTagsCommand());
        this.getCommandRegistry().registerCommand(new CharacterCommand());
        this.getCommandRegistry().registerCommand(new SetSkillCommand());
        this.getCommandRegistry().registerCommand(new SkillInfoCommand());
        this.getCommandRegistry().registerCommand(new ClearNpcHologramsCommand());
        this.getCommandRegistry().registerCommand(new ReloadConfigCommand());
        this.getCommandRegistry().registerCommand(new SocialDebugCommand());
        this.getCommandRegistry().registerCommand(new ItemMetaCommand());
        this.getCommandRegistry().registerCommand(new ItemRollsCommand());
        this.getCommandRegistry().registerCommand(new ItemStatsCommand());
        this.getCommandRegistry().registerCommand(new ItemDiagCommand());
        this.getCommandRegistry().registerCommand(new GemUiCommand());
    }

    @Override
    protected void shutdown() {
        if (this.dynamicTooltipService != null) {
            this.dynamicTooltipService.shutdown();
            this.dynamicTooltipService = null;
        }
        if (this.socialInteractionWatcherFilter != null) {
            PacketAdapters.deregisterInbound(this.socialInteractionWatcherFilter);
            this.socialInteractionWatcherFilter = null;
        }
    }

    /**
     * Gets the LevelingService instance for managing player levels and XP.
     *
     * @return The singleton LevelingService instance
     */
    public static LevelingService getService() {
        return instance.service;
    }

    /**
     * Gets the SlayerService instance.
     *
     * @return the SlayerService singleton
     */
    public static SlayerService getSlayerService() {
        return instance.slayerService;
    }

    /**
     * Gets the SocialService instance.
     *
     * @return social service singleton
     */
    public static SocialService getSocialService() {
        return instance != null ? instance.socialService : null;
    }

    /**
     * Gets the dynamic tooltip service instance.
     *
     * @return dynamic tooltip service, if enabled
     */
    public static HyruneDynamicTooltipService getDynamicTooltipService() {
        return instance != null ? instance.dynamicTooltipService : null;
    }

    /**
     * Invalidates and refreshes dynamic tooltips for one player.
     * Useful for level-ups, potion buffs/debuffs, and other temporary stat changes.
     */
    public static boolean refreshDynamicTooltipsForPlayer(UUID playerUuid) {
        HyruneDynamicTooltipService service = getDynamicTooltipService();
        return service != null && service.invalidateAndRefreshPlayer(playerUuid);
    }


    /**
     * Gets the NPC level service instance.
     *
     * @return the NPC level service, if initialized
     */
    public static NpcLevelService getNpcLevelService() {
        return instance != null ? instance.npcLevelService : null;
    }


    /**
     * Gets the component type for NPC level data.
     *
     * @return NPC level component type
     */
    public static ComponentType<EntityStore, NpcLevelComponent> getNpcLevelComponentType() {
        return HyruneComponents.NPC_LEVEL;
    }

    /**
     * Gets the component type for fishing bobber data.
     *
     * @return fishing bobber component type
     */
    public static ComponentType<EntityStore, FishingBobberComponent> getFishingBobberComponentType() {
        return HyruneComponents.FISHING_BOBBER;
    }

    /**
     * Gets the component type for game mode data.
     *
     * @return game mode data component type
     */
    public static ComponentType<EntityStore, GameModeDataComponent> getGameModeDataComponentType() {
        return HyruneComponents.GAMEMODE_DATA;
    }
}

