package dev.hytalemodding.origins.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.Origins;

import javax.annotation.Nonnull;

public class FishingBobberSystem extends EntityTickingSystem<EntityStore> {
    static final int MIN_BITE_TICKS = 120;
    static final int MAX_BITE_TICKS = 240;
    static final int MIN_BITE_WINDOW_TICKS = 20;
    static final int MAX_BITE_WINDOW_TICKS = 40;
    private static final String BITE_SOUND_ID = "SFX_Player_Swim";

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
        Ref<EntityStore> bobberRef = chunk.getReferenceTo(index);
        FishingBobberComponent bobber = chunk.getComponent(index, Origins.getFishingBobberComponentType());
        if (bobber == null) {
            return;
        }

        bobber.setBobberAge(bobber.getBobberAge() + 1);

        if (bobber.canCatchFish()) {
            int remaining = bobber.getCatchTimer() - 1;
            if (remaining <= 0) {
                bobber.clearCatchWindow();
                bobber.setRandomTimeUntilCatch();
            } else {
                bobber.setCatchTimer(remaining);
            }
            return;
        }

        int until = bobber.getTimeUntilCatch();
        if (until <= 0) {
            bobber.startCatchWindow();
            notifyBite(bobber, bobberRef, store);
            return;
        }

        bobber.setTimeUntilCatch(until - 1);
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
}
