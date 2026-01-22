package dev.hytalemodding;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import dev.hytalemodding.origins.commands.*;
import dev.hytalemodding.origins.database.JsonLevelRepository;
import dev.hytalemodding.origins.events.AttributeGrowthListener;
import dev.hytalemodding.origins.events.LevelingVisualsListener;
import dev.hytalemodding.origins.events.PlayerJoinListener;
import dev.hytalemodding.origins.level.LevelingService;
import dev.hytalemodding.origins.level.formulas.DefaultFormula;
import dev.hytalemodding.origins.system.CombatXpSystem;
import dev.hytalemodding.origins.util.NameplateManager;
import dev.hytalemodding.origins.system.SyncTaskSystem;

import javax.annotation.Nonnull;
import java.util.logging.Level;

/**
 * Origins - RPG Leveling System for Hytale
 * Adds adventurer levels, class progression, and MMO-style nameplates.
 */
public class Origins extends JavaPlugin {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static Origins instance;
    private LevelingService service;

    public Origins(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    @Override
    protected void setup() {
        // Initialize database repository
        JsonLevelRepository repository = new JsonLevelRepository("./origins_data");

        // Initialize leveling formula
        DefaultFormula formula = new DefaultFormula();

        // Initialize leveling service
        this.service = new LevelingService(formula, repository);

        // Initialize nameplate manager
        NameplateManager.init(this.service);

        // Register event listeners
        PlayerJoinListener joinListener = new PlayerJoinListener(this.service);
        this.getEventRegistry().registerGlobal(AddPlayerToWorldEvent.class, joinListener::onPlayerJoin);

        service.registerLevelUpListener(new LevelingVisualsListener(service));
        service.registerLevelUpListener(new AttributeGrowthListener());


        // Register commands
        this.getCommandRegistry().registerCommand(new AddXpCommand());
        this.getCommandRegistry().registerCommand(new CheckLevelCommand());
        this.getCommandRegistry().registerCommand(new SetCombatLevelCommand());
        this.getCommandRegistry().registerCommand(new SetClassCommand());
        this.getCommandRegistry().registerCommand(new CheckTagsCommand());
        this.getCommandRegistry().registerCommand(new CharacterCommand());

        // Register ECS systems
        this.getEntityStoreRegistry().registerSystem(new CombatXpSystem());
        this.getEntityStoreRegistry().registerSystem(new SyncTaskSystem());

        LOGGER.at(Level.INFO).log("Origins leveling system initialized successfully!");
    }

    public static LevelingService getService() {
        return instance.service;
    }
}