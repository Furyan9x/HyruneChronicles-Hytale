package dev.hytalemodding.origins.slayer;

/**
 * 
 */
public class SlayerTaskAssignment {
    private String taskId;
    private String targetNpcTypeId;
    private int totalKills;
    private int remainingKills;
    private SlayerTaskState state;

    public SlayerTaskAssignment() {
    }

    public SlayerTaskAssignment(String taskId, String targetNpcTypeId, int totalKills) {
        this.taskId = taskId;
        this.targetNpcTypeId = targetNpcTypeId;
        this.totalKills = totalKills;
        this.remainingKills = totalKills;
        this.state = SlayerTaskState.ACCEPTED;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getTargetNpcTypeId() {
        return targetNpcTypeId;
    }

    public int getTotalKills() {
        return totalKills;
    }

    public int getRemainingKills() {
        return remainingKills;
    }

    public SlayerTaskState getState() {
        return state;
    }

    public void setState(SlayerTaskState state) {
        this.state = state;
    }

    public void decrement() {
        if (remainingKills > 0) {
            remainingKills--;
        }
        if (remainingKills <= 0) {
            remainingKills = 0;
            state = SlayerTaskState.COMPLETED;
        } else if (state == SlayerTaskState.ACCEPTED) {
            state = SlayerTaskState.IN_PROGRESS;
        }
    }
}
