package dev.hytalemodding.hyrune.economy;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import dev.hytalemodding.hyrune.economy.trade.PlayerTradeService;
import dev.hytalemodding.hyrune.economy.commands.TradeHubCommand;

import javax.annotation.Nonnull;

/**
 * Economy plugin module entrypoint.
 */
public class HyruneEconomy extends JavaPlugin {
    private PlayerTradeService tradeService;

    public HyruneEconomy(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        this.tradeService = new PlayerTradeService();
        TradeRequestBridge.registerHandler((requester, target) -> this.tradeService.requestTrade(requester, target));
        this.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class,
            event -> this.tradeService.handlePlayerDisconnect(event == null ? null : event.getPlayerRef()));
        this.getCommandRegistry().registerCommand(new TradeHubCommand());
    }

    @Override
    protected void shutdown() {
        TradeRequestBridge.clearHandler();
    }
}
