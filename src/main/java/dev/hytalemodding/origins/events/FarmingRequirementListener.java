package dev.hytalemodding.origins.events;

import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import dev.hytalemodding.Origins;
import dev.hytalemodding.origins.level.LevelingService;
import dev.hytalemodding.origins.registry.FarmingRequirementRegistry;
import dev.hytalemodding.origins.skills.SkillType;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Foundation for future husbandry interaction gates.
 * Planting gates are enforced in FarmingPlantingRestrictionSystem via PlaceBlockEvent.
 */
public class FarmingRequirementListener {
    private static final long WARNING_COOLDOWN_MS = 2000L;
    private static final Map<UUID, Long> LAST_WARNING_MS = new ConcurrentHashMap<>();

    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event == null || event.isCancelled()) {
            return;
        }

        Player player = event.getPlayer();
        if (player == null) {
            return;
        }

        // Foundation for future husbandry rules. Disabled by config by default.
        tryBlockAnimalHusbandry(event, player);
    }

    private void tryBlockAnimalHusbandry(PlayerInteractEvent event, Player player) {
        Entity target = event.getTargetEntity();
        if (target == null) {
            return;
        }

        String targetId = resolveTargetId(target);
        Integer requiredLevel = FarmingRequirementRegistry.getAnimalRequiredLevel(targetId);
        if (requiredLevel == null) {
            return;
        }

        if (!FarmingRequirementRegistry.isAnimalHusbandryGatingEnabled()) {
            return;
        }

        int farmingLevel = getFarmingLevel(player);
        if (farmingLevel >= requiredLevel) {
            return;
        }

        event.setCancelled(true);
        sendRateLimitedMessage(
            player,
            "You need Farming level " + requiredLevel + " to interact with this animal."
        );
    }

    private int getFarmingLevel(Player player) {
        if (player == null || player.getPlayerRef() == null) {
            return 1;
        }

        LevelingService service = Origins.getService();
        if (service == null) {
            return 1;
        }

        return service.getSkillLevel(player.getPlayerRef().getUuid(), SkillType.FARMING);
    }

    @Nullable
    private String resolveTargetId(Entity target) {
        if (target instanceof NPCEntity npcEntity) {
            String npcTypeId = npcEntity.getNPCTypeId();
            if (npcTypeId != null && !npcTypeId.isBlank()) {
                return npcTypeId;
            }
            String roleName = npcEntity.getRoleName();
            if (roleName != null && !roleName.isBlank()) {
                return roleName;
            }
        }
        return target.getLegacyDisplayName();
    }

    private void sendRateLimitedMessage(Player player, String message) {
        UUID uuid = player.getUuid();
        long now = System.currentTimeMillis();
        Long last = LAST_WARNING_MS.get(uuid);
        if (last != null && now - last < WARNING_COOLDOWN_MS) {
            return;
        }
        LAST_WARNING_MS.put(uuid, now);
        player.sendMessage(Message.raw(message));
    }
}
