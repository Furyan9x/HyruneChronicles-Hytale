package dev.hytalemodding.origins.bonus;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import dev.hytalemodding.origins.events.LevelUpListener;

import java.util.UUID;

public class SkillStatBonusListener implements LevelUpListener {
    @Override
    public void onLevelUp(UUID uuid, int newLevel, String source) {
        PlayerRef player = Universe.get().getPlayer(uuid);
        if (player == null) {
            return;
        }

        SkillStatBonusApplier.apply(player);
        SkillStatBonusApplier.applyMovementSpeed(player);
    }
}
