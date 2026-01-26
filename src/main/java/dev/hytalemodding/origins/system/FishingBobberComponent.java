package dev.hytalemodding.origins.system;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class FishingBobberComponent implements Component<EntityStore> {
    private UUID ownerId;
    private FishingRegistry.BaitDefinition bait;
    private int fishingLevel;
    private boolean canCatch;
    private int timeUntilCatch;
    private int catchTimer;
    private int bobberAge;

    public FishingBobberComponent() {
        this.timeUntilCatch = -1;
    }

    public void init(UUID ownerId, FishingRegistry.BaitDefinition bait, int fishingLevel) {
        this.ownerId = ownerId;
        this.bait = bait;
        this.fishingLevel = Math.max(1, fishingLevel);
        this.canCatch = false;
        this.catchTimer = 0;
        this.bobberAge = 0;
        setRandomTimeUntilCatch();
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public FishingRegistry.BaitDefinition getBait() {
        return bait;
    }

    public int getFishingLevel() {
        return fishingLevel;
    }

    public int getBobberAge() {
        return bobberAge;
    }

    public void setBobberAge(int bobberAge) {
        this.bobberAge = bobberAge;
    }

    public boolean canCatchFish() {
        return canCatch && catchTimer > 0;
    }

    public int getCatchTimer() {
        return catchTimer;
    }

    public void setCatchTimer(int catchTimer) {
        this.catchTimer = catchTimer;
    }

    public int getTimeUntilCatch() {
        return timeUntilCatch;
    }

    public void setTimeUntilCatch(int timeUntilCatch) {
        this.timeUntilCatch = timeUntilCatch;
    }

    public void startCatchWindow() {
        this.canCatch = true;
        this.catchTimer = ThreadLocalRandom.current().nextInt(
            FishingBobberSystem.MIN_BITE_WINDOW_TICKS,
            FishingBobberSystem.MAX_BITE_WINDOW_TICKS + 1
        );
    }

    public void clearCatchWindow() {
        this.canCatch = false;
        this.catchTimer = 0;
    }

    public void setRandomTimeUntilCatch() {
        int baseWait = ThreadLocalRandom.current().nextInt(
            FishingBobberSystem.MIN_BITE_TICKS,
            FishingBobberSystem.MAX_BITE_TICKS + 1
        );
        double levelBonus = Math.min(1.0, fishingLevel / 99.0) * FishingRegistry.BITE_SPEED_MAX_BONUS;
        double waitTicks = baseWait * bait.speedMultiplier * (1.0 - levelBonus);
        this.timeUntilCatch = Math.max(20, (int) Math.round(waitTicks));
    }

    @Override
    public Component<EntityStore> clone() {
        FishingBobberComponent copy = new FishingBobberComponent();
        copy.ownerId = this.ownerId;
        copy.bait = this.bait;
        copy.fishingLevel = this.fishingLevel;
        copy.canCatch = this.canCatch;
        copy.timeUntilCatch = this.timeUntilCatch;
        copy.catchTimer = this.catchTimer;
        copy.bobberAge = this.bobberAge;
        return copy;
    }
}
