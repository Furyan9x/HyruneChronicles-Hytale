package dev.hytalemodding.origins.commands;


import dev.hytalemodding.origins.ui.CharacterMenu;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.concurrent.CompletableFuture;

public class CharacterCommand extends AbstractAsyncCommand {

    public CharacterCommand() {
        super("character", "Opens the Character Menu displaying skills and levels");
        // Add permission requirements here if needed
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext commandContext) {
        CommandSender sender = commandContext.sender();

        if (sender instanceof Player player) {
            Ref<EntityStore> ref = player.getReference();

            if (ref != null && ref.isValid()) {
                Store<EntityStore> store = ref.getStore();
                World world = store.getExternalData().getWorld();

                // Crucial: Dispatch to the World Thread to prevent crashes
                return CompletableFuture.runAsync(() -> {
                    PlayerRef playerRefComponent = store.getComponent(ref, PlayerRef.getComponentType());

                    if (playerRefComponent != null) {
                        player.getPageManager().openCustomPage(ref, store, new CharacterMenu(playerRefComponent));
                    }
                }, world);
            }
        }

        return CompletableFuture.completedFuture(null);
    }
}