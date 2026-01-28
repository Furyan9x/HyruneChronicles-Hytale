package dev.hytalemodding.origins.slayer;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.corecomponents.ActionBase;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import dev.hytalemodding.Origins;
import dev.hytalemodding.origins.ui.SlayerDialoguePage;

import javax.annotation.Nonnull;

public class ActionOpenSlayerDialogue extends ActionBase {

    public ActionOpenSlayerDialogue(@Nonnull BuilderActionOpenSlayerDialogue builder) {
        super(builder);
    }

    @Override
    public boolean canExecute(@Nonnull Ref<EntityStore> ref,
                              @Nonnull Role role,
                              InfoProvider sensorInfo,
                              double dt,
                              @Nonnull Store<EntityStore> store) {
        return super.canExecute(ref, role, sensorInfo, dt, store)
                && role.getStateSupport().getInteractionIterationTarget() != null;
    }

    @Override
    public boolean execute(@Nonnull Ref<EntityStore> ref,
                           @Nonnull Role role,
                           InfoProvider sensorInfo,
                           double dt,
                           @Nonnull Store<EntityStore> store) {
        super.execute(ref, role, sensorInfo, dt, store);

        Ref<EntityStore> playerReference = role.getStateSupport().getInteractionIterationTarget();
        if (playerReference == null) {
            return false;
        }

        PlayerRef playerRefComponent = store.getComponent(playerReference, PlayerRef.getComponentType());
        Player playerComponent = store.getComponent(playerReference, Player.getComponentType());

        if (playerRefComponent == null || playerComponent == null) {
            return false;
        }

        SlayerService slayerService = Origins.getSlayerService();
        if (slayerService == null) {
            return false;
        }

        playerComponent.getPageManager().openCustomPage(
                ref,
                store,
                new SlayerDialoguePage(playerRefComponent, slayerService)
        );
        return true;
    }
}
