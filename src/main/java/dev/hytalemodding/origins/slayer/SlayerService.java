package dev.hytalemodding.origins.slayer;

import com.hypixel.hytale.server.core.entity.entities.Player;
import dev.hytalemodding.origins.level.LevelingService;
import dev.hytalemodding.origins.skills.SkillType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SlayerService {

    private final SlayerRepository repository;
    private final SlayerTaskRegistry taskRegistry;
    private final Map<UUID, SlayerPlayerData> cache = new ConcurrentHashMap<>();

    public SlayerService(SlayerRepository repository, SlayerTaskRegistry taskRegistry) {
        this.repository = repository;
        this.taskRegistry = taskRegistry;
    }

    public SlayerPlayerData getPlayerData(UUID uuid) {
        return cache.computeIfAbsent(uuid, id -> {
            SlayerPlayerData stored = repository.load(id);
            return stored != null ? stored : new SlayerPlayerData(id);
        });
    }

    public SlayerTaskAssignment getAssignment(UUID uuid) {
        SlayerTaskAssignment assignment = getPlayerData(uuid).getAssignment();
        return normalizeAssignment(assignment);
    }

    public SlayerTaskAssignment assignTask(UUID uuid, int slayerLevel) {
        SlayerPlayerData data = getPlayerData(uuid);
        SlayerTaskAssignment current = data.getAssignment();
        if (current != null && current.getState() != SlayerTaskState.COMPLETED) {
            return current;
        }

        SlayerTaskDefinition task = taskRegistry.getRandomTaskForLevel(slayerLevel);
        if (task == null) {
            return null;
        }

        int count = taskRegistry.rollKillCount(task);
        SlayerTaskAssignment assignment = new SlayerTaskAssignment(task.getId(), task.getTargetNpcTypeId(), count);
        data.setAssignment(assignment);
        repository.save(data);
        return assignment;
    }

    public boolean onKill(UUID killerUuid, String npcTypeId) {
        SlayerPlayerData data = getPlayerData(killerUuid);
        SlayerTaskAssignment assignment = normalizeAssignment(data.getAssignment());
        if (assignment == null) {
            return false;
        }
        if (assignment.getState() == SlayerTaskState.COMPLETED) {
            return false;
        }
        if (!assignment.getTargetNpcTypeId().equalsIgnoreCase(npcTypeId)) {
            return false;
        }

        assignment.decrement();
        repository.save(data);
        return true;
    }

    public SlayerTurnInResult turnInTask(Player player) {
        if (player == null) {
            return null;
        }
        UUID uuid = player.getUuid();
        SlayerPlayerData data = getPlayerData(uuid);
        SlayerTaskAssignment assignment = normalizeAssignment(data.getAssignment());
        if (assignment == null || assignment.getState() != SlayerTaskState.COMPLETED) {
            return null;
        }

        int pointsAwarded = 5 + Math.max(1, assignment.getTotalKills() / 5);
        long slayerXpAwarded = 25L * assignment.getTotalKills();
        boolean itemRewarded = Math.random() < 0.1;

        data.addSlayerPoints(pointsAwarded);
        data.incrementCompletedTasks();
        data.setAssignment(null);
        repository.save(data);

        LevelingService levelingService = LevelingService.get();
        if (levelingService != null) {
            levelingService.addSkillXp(uuid, SkillType.SLAYER, slayerXpAwarded);
        }

        // TODO: Hook into item reward system when available.
        return new SlayerTurnInResult(pointsAwarded, slayerXpAwarded, itemRewarded);
    }

    private SlayerTaskAssignment normalizeAssignment(SlayerTaskAssignment assignment) {
        if (assignment == null) {
            return null;
        }
        if (assignment.getState() == null) {
            assignment.setState(SlayerTaskState.ACCEPTED);
        }
        if (assignment.getRemainingKills() <= 0 && assignment.getTotalKills() > 0) {
            assignment.setState(SlayerTaskState.COMPLETED);
        }
        return assignment;
    }

    public void unload(UUID uuid) {
        SlayerPlayerData data = cache.remove(uuid);
        if (data != null) {
            repository.save(data);
        }
    }
}
