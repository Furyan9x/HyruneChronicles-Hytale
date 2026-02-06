package dev.hytalemodding.origins.slayer;

import java.util.UUID;

public class SlayerPlayerData {
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

    public UUID getUuid() {
        return uuid;
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
        this.slayerPoints += points;
    }

    public void removeSlayerPoints(int points) {this.slayerPoints -= points;}

    public int getCompletedTasks() {
        return completedTasks;
    }

    public void incrementCompletedTasks() {
        this.completedTasks++;
    }
}
