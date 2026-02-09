package dev.hytalemodding.origins.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.origins.ui.CharacterMenu;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.concurrent.CompletableFuture;

/**
 * Command handler for character.
 */
public class CharacterCommand extends AbstractAsyncCommand {

    public CharacterCommand() {
        super("character", "Opens the Character Menu displaying skills and levels");
        this.addAliases("c", "char");
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext commandContext) {
        CommandSender sender = commandContext.sender();

        if (!(sender instanceof Player player)) {
            return CompletableFuture.completedFuture(null);
        }

        Ref<EntityStore> ref = player.getReference();
        if (ref == null || !ref.isValid()) {
            return CompletableFuture.completedFuture(null);
        }

        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();

        // Dispatch to the world thread to avoid UI access on async threads.
        return CompletableFuture.runAsync(() -> {
            PlayerRef playerRefComponent = store.getComponent(ref, PlayerRef.getComponentType());
            if (playerRefComponent != null) {
                player.getPageManager().openCustomPage(ref, store, new CharacterMenu(playerRefComponent));
            }
        }, world);
    }
}
