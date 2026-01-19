package dev.hytalemodding;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import dev.hytalemodding.origins.commands.*;
import dev.hytalemodding.origins.database.JsonLevelRepository;
import dev.hytalemodding.origins.events.LevelingVisualsListener;


import dev.hytalemodding.origins.events.PlayerJoinListener;
import dev.hytalemodding.origins.level.LevelingService;
import dev.hytalemodding.origins.level.formulas.DefaultFormula;
import dev.hytalemodding.origins.system.CombatXpSystem;
import dev.hytalemodding.origins.system.MiningXpSystem;

import javax.annotation.Nonnull;
import java.util.logging.Level;

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
        var eventBus = HytaleServer.get().getEventBus();
        // 1. Setup the Database (JSON file path)
        // We use the server's running directory + "/origins_data"
        JsonLevelRepository repository = new JsonLevelRepository("./origins_data");
        // 2. Setup the Math
        DefaultFormula formula = new DefaultFormula();
        // 3. Initialize the Service
        this.service = new LevelingService(formula, repository);
        PlayerJoinListener joinListener = new PlayerJoinListener(this.service);
        service.registerLevelUpListener(new LevelingVisualsListener(service));

        this.getEventRegistry().registerGlobal(AddPlayerToWorldEvent.class, joinListener::onPlayerJoin);
        // 4. Register Commands
        this.getCommandRegistry().registerCommand(new AddXpCommand());
        this.getCommandRegistry().registerCommand(new CheckLevelCommand());
        this.getCommandRegistry().registerCommand(new SetCombatLevelCommand());
        this.getCommandRegistry().registerCommand(new SetClassCommand());
        this.getCommandRegistry().registerCommand(new CheckTagsCommand());
        LOGGER.at(Level.INFO).log("Origins Service & Commands Loaded!");

        this.getEntityStoreRegistry().registerSystem(new CombatXpSystem());
        //this.getEntityStoreRegistry().registerSystem(new MiningXpSystem());
    }

    public static LevelingService getService() {
        return instance.service;
    }
}