package dev.hytalemodding;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.DrainPlayerFromWorldEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import dev.hytalemodding.origins.commands.CharacterCommand;
import dev.hytalemodding.origins.commands.CheckTagsCommand;
import dev.hytalemodding.origins.commands.SetSkillCommand;
import dev.hytalemodding.origins.database.JsonLevelRepository;
import dev.hytalemodding.origins.bonus.SkillStatBonusListener;
import dev.hytalemodding.origins.events.LevelingVisualsListener;
import dev.hytalemodding.origins.system.FarmingHarvestListener;
import dev.hytalemodding.origins.system.FarmingHarvestPickupSystem;
import dev.hytalemodding.origins.system.FishingBobberComponent;
import dev.hytalemodding.origins.system.FishingBobberSystem;
import dev.hytalemodding.origins.system.FishingInteraction;
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
import dev.hytalemodding.origins.system.SyncTaskSystem;
import dev.hytalemodding.origins.system.TimedCraftingXpSystem;
import dev.hytalemodding.origins.system.WoodcuttingDurabilitySystem;
import dev.hytalemodding.origins.system.WoodcuttingSpeedSystem;
import dev.hytalemodding.origins.util.NameplateManager;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;

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
    private static ComponentType<EntityStore, FishingBobberComponent> fishingBobberComponentType;

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

        // Initialize nameplate manager
        NameplateManager.init(this.service);

        fishingBobberComponentType = this.getEntityStoreRegistry()
            .registerComponent(FishingBobberComponent.class, FishingBobberComponent::new);

        // Register event listeners
        PlayerJoinListener joinListener = new PlayerJoinListener(this.service);
        this.getEventRegistry().registerGlobal(AddPlayerToWorldEvent.class, joinListener::onPlayerJoin);
        this.getEventRegistry().registerGlobal(DrainPlayerFromWorldEvent.class, joinListener::onPlayerLeave);
        this.getEventRegistry().registerGlobal(PlayerInteractEvent.class, new FarmingHarvestListener()::onPlayerInteract);
        this.getCodecRegistry(Interaction.CODEC).register(
            "OriginsFishing",
            FishingInteraction.class,
            FishingInteraction.CODEC
        );
        this.service.registerLevelUpListener(new LevelingVisualsListener());
        this.service.registerLevelUpListener(new SkillStatBonusListener());

        // Register commands
        this.getCommandRegistry().registerCommand(new CheckTagsCommand());
        this.getCommandRegistry().registerCommand(new CharacterCommand());
        this.getCommandRegistry().registerCommand(new SetSkillCommand());

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
        this.getEntityStoreRegistry().registerSystem(new MiningSpeedSystem());
        this.getEntityStoreRegistry().registerSystem(new MiningDurabilitySystem());
        this.getEntityStoreRegistry().registerSystem(new WoodcuttingSpeedSystem());
        this.getEntityStoreRegistry().registerSystem(new WoodcuttingDurabilitySystem());
        this.getEntityStoreRegistry().registerSystem(new SkillRegenSystem());
        this.getEntityStoreRegistry().registerSystem(new SyncTaskSystem());


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

    public static ComponentType<EntityStore, FishingBobberComponent> getFishingBobberComponentType() {
        return fishingBobberComponentType;
    }
}
