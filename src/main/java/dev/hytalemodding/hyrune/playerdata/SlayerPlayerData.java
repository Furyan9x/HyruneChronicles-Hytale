package dev.hytalemodding.hyrune.playerdata;

import dev.hytalemodding.hyrune.slayer.SlayerTaskAssignment;

import java.util.UUID;

/**
 * Stores persistent Slayer-related player progression.
 */
public class SlayerPlayerData implements PlayerData {
    private UUID uuid;
    private SlayerTaskAssignment assignment;
    private int slayerPoints;
    private int completedTasks;

    public SlayerPlayerData() {
    }

    public SlayerPlayerData(UUID uuid) {
        this.uuid = uuid;
        this.slayerPoints = 0;
        this.assignment = null;
    }

    @Override
    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public SlayerTaskAssignment getAssignment() {
        return assignment;
    }

    public void setAssignment(SlayerTaskAssignment assignment) {
        this.assignment = assignment;
    }

    public int getSlayerPoints() {
        return slayerPoints;
    }

    public void addSlayerPoints(int points) {
        if (points > 0) {
            this.slayerPoints += points;
        }
    }

    public void removeSlayerPoints(int points) {
        if (points > 0) {
            this.slayerPoints = Math.max(0, this.slayerPoints - points);
        }
    }

    public int getCompletedTasks() {
        return completedTasks;
    }

    public void incrementCompletedTasks() {
        this.completedTasks++;
    }
}
