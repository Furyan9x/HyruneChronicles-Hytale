package dev.hytalemodding.origins.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.AnimationUtils;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.Origins;
import dev.hytalemodding.origins.component.FishingBobberComponent;
import dev.hytalemodding.origins.interaction.FishingInteraction;
import dev.hytalemodding.origins.registry.FishingRegistry;

import javax.annotation.Nonnull;

public class FishingBobberSystem extends EntityTickingSystem<EntityStore> {
    public static final int MIN_BITE_TICKS = 120;
    public static final int MAX_BITE_TICKS = 240;
    public static final int MIN_BITE_WINDOW_TICKS = 40;
    public static final int MAX_BITE_WINDOW_TICKS = 80;
    private static final String BITE_SOUND_ID = "SFX_Player_Swim";
    private static final int MAX_TETHER_DISTANCE = 12;
    private static final String ANIM_IDLE = "Idle";
    private static final String ANIM_CAST = "Cast";
    private static final String ANIM_CATCH = "Bite";

    @Override
    public @Nonnull Query<EntityStore> getQuery() {
        return Origins.getFishingBobberComponentType();
    }

    @Override
    public void tick(float deltaTime,
                     int index,
                     @Nonnull ArchetypeChunk<EntityStore> chunk,
                     @Nonnull Store<EntityStore> store,
                     @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        // Advance bite timers and drive bobber animations/SFX.
        Ref<EntityStore> bobberRef = chunk.getReferenceTo(index);
        FishingBobberComponent bobber = chunk.getComponent(index, Origins.getFishingBobberComponentType());
        if (bobber == null) {
            return;
        }

        if (shouldCancelBobber(bobberRef, bobber, store)) {
            cancelBobber(bobberRef, bobber, store, commandBuffer);
            return;
        }

        bobber.setBobberAge(bobber.getBobberAge() + 1);
        if (bobber.getBobberAge() <= 5) {
            playStatusAnimationIfChanged(bobber, bobberRef, commandBuffer, ANIM_CAST);
        }

        if (bobber.canCatchFish()) {
            int remaining = bobber.getCatchTimer() - 1;
            if (remaining <= 0) {
                bobber.clearCatchWindow();
                bobber.setRandomTimeUntilCatch();
                playStatusAnimationIfChanged(bobber, bobberRef, commandBuffer, ANIM_IDLE);
            } else {
                bobber.setCatchTimer(remaining);
            }
            return;
        }

        int until = bobber.getTimeUntilCatch();
        if (until <= 0) {
            bobber.startCatchWindow();
            playStatusAnimationIfChanged(bobber, bobberRef, commandBuffer, ANIM_CATCH);
            notifyBite(bobber, bobberRef, store);
            return;
        }

        bobber.setTimeUntilCatch(until - 1);
        playStatusAnimationIfChanged(bobber, bobberRef, commandBuffer, ANIM_IDLE);
    }

    private void notifyBite(FishingBobberComponent bobber, Ref<EntityStore> bobberRef, Store<EntityStore> store) {
        PlayerRef playerRef = Universe.get().getPlayer(bobber.getOwnerId());
        if (playerRef != null) {
            playerRef.sendMessage(Message.raw("A fish is biting!"));
        }

        TransformComponent transform = store.getComponent(bobberRef, TransformComponent.getComponentType());
        if (transform == null) {
            return;
        }

        int splashSound = SoundEvent.getAssetMap().getIndex(BITE_SOUND_ID);
        Vector3d pos = transform.getPosition();
        SoundUtil.playSoundEvent3d(splashSound, SoundCategory.SFX, pos.x, pos.y, pos.z, 3.0f, 2.0f, store);
        if (playerRef != null) {
            SoundUtil.playSoundEvent2dToPlayer(playerRef, splashSound, SoundCategory.SFX, 3.0f, 2.0f);
        }
    }

    private void playStatusAnimationIfChanged(FishingBobberComponent bobber,
                                              Ref<EntityStore> bobberRef,
                                              CommandBuffer<EntityStore> commandBuffer,
                                              String animation) {
        if (animation == null || animation.equals(bobber.getCurrentAnimation())) {
            return;
        }
        bobber.setCurrentAnimation(animation);
        AnimationUtils.playAnimation(
            bobberRef,
            AnimationSlot.Status,
            animation,
            true,
            commandBuffer
        );
    }

    private boolean shouldCancelBobber(Ref<EntityStore> bobberRef,
                                       FishingBobberComponent bobber,
                                       Store<EntityStore> store) {
        PlayerRef playerRef = Universe.get().getPlayer(bobber.getOwnerId());
        if (playerRef == null || playerRef.getReference() == null) {
            return true;
        }

        Player player = store.getComponent(playerRef.getReference(), Player.getComponentType());
        Inventory inventory = player != null ? player.getInventory() : null;
        ItemStack active = inventory != null ? inventory.getActiveHotbarItem() : null;
        if (!FishingRegistry.isFishingRod(active)) {
            return true;
        }

        TransformComponent bobberTransform = store.getComponent(bobberRef, TransformComponent.getComponentType());
        TransformComponent playerTransform = store.getComponent(playerRef.getReference(), TransformComponent.getComponentType());
        if (bobberTransform == null || playerTransform == null) {
            return true;
        }

        Vector3d bobberPos = bobberTransform.getPosition();
        Vector3d playerPos = playerTransform.getPosition();
        double dx = bobberPos.x - playerPos.x;
        double dy = bobberPos.y - playerPos.y;
        double dz = bobberPos.z - playerPos.z;
        return (dx * dx + dy * dy + dz * dz) > (MAX_TETHER_DISTANCE * MAX_TETHER_DISTANCE);
    }

    private void cancelBobber(Ref<EntityStore> bobberRef,
                              FishingBobberComponent bobber,
                              Store<EntityStore> store,
                              CommandBuffer<EntityStore> commandBuffer) {
        PlayerRef playerRef = Universe.get().getPlayer(bobber.getOwnerId());
        if (playerRef != null && playerRef.getReference() != null) {
            Player player = store.getComponent(playerRef.getReference(), Player.getComponentType());
            Inventory inventory = player != null ? player.getInventory() : null;
            FishingInteraction.updateRodMetadata(inventory, null);
            FishingSystem.cancelSession(playerRef.getUuid());
        }
        commandBuffer.removeEntity(bobberRef, com.hypixel.hytale.component.RemoveReason.REMOVE);
    }
}
