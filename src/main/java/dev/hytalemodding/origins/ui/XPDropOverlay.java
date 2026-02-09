package dev.hytalemodding.origins.ui;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.HytaleServer;
import dev.hytalemodding.origins.util.XPDropManager;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * HUD overlay for x p drop.
 */
public class XPDropOverlay extends CustomUIHud {
    private final String skillName;
    private long totalXpGained;
    private float currentProgress;
    private ScheduledFuture<?> closeTask;
    private static final String MAIN_UI = "Pages/xp_drop_overlay.ui"; // Note: Changed to Hud/ folder

    public XPDropOverlay(PlayerRef playerRef, String skill, long initialAmount, float progress) {
        super(playerRef);
        this.skillName = skill;
        this.totalXpGained = initialAmount;
        this.currentProgress = progress;

        startCloseTimer();
    }

    @Override
    protected void build(UICommandBuilder builder) {
        builder.append(MAIN_UI);
        applyVisualUpdates(builder);
    }

    public void updateData(long addedAmount, float newProgress) {
        this.totalXpGained += addedAmount;
        this.currentProgress = newProgress;
        startCloseTimer();

        UICommandBuilder updateBuilder = new UICommandBuilder();
        applyVisualUpdates(updateBuilder);
        this.update(false, updateBuilder); // Note: HUD uses update() not sendUpdate()
    }

    private void applyVisualUpdates(UICommandBuilder builder) {
        // 1. Update progress ring (0.0 to 1.0)
        builder.set("#ProgressBarFill.Value", currentProgress );

        // 2. Update the skill icon background texture
        String iconPath = "Pages/" + skillName.toLowerCase() + ".png";
        builder.set("#SkillIcon.Background",  iconPath);

        // 3. Update XP text
        builder.set("#XPAmount.Text", "+" + totalXpGained + " XP");
    }

    public String getSkillName() {
      return this.skillName;
    }

    private void startCloseTimer() {
        if (closeTask != null && !closeTask.isDone()) {
            closeTask.cancel(false);
        }

        this.closeTask = HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
            XPDropManager.get().onOverlayClosed(this.getPlayerRef());

            // Clear the HUD by sending an empty update with clear=true
            UICommandBuilder emptyBuilder = new UICommandBuilder();
            this.update(true, emptyBuilder);
        }, 4, TimeUnit.SECONDS);
    }
}
