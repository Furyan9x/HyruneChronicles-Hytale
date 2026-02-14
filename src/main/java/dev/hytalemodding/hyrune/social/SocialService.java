package dev.hytalemodding.hyrune.social;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import dev.hytalemodding.hyrune.database.SocialRepository;
import dev.hytalemodding.hyrune.playerdata.SocialPlayerData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Handles social relationships and friend request workflows.
 */
public class SocialService {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String[] DEBUG_KEYS = {
        "friend_a",
        "friend_b",
        "friend_c",
        "incoming_a",
        "incoming_b",
        "outgoing_a",
        "ignored_a",
        "cycle"
    };

    private final SocialRepository repository;
    private final Map<UUID, SocialPlayerData> cache = new ConcurrentHashMap<>();
    private final Map<UUID, String> debugAliases = new ConcurrentHashMap<>();

    public SocialService(SocialRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public void load(UUID uuid) {
        if (uuid == null) {
            return;
        }
        cache.computeIfAbsent(uuid, this::loadOrCreate);
    }

    public void unload(UUID uuid) {
        if (uuid == null) {
            return;
        }
        SocialPlayerData data = cache.remove(uuid);
        if (data != null) {
            persist(data);
        }
    }

    public SocialPlayerData getPlayerData(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        return cache.computeIfAbsent(uuid, this::loadOrCreate);
    }

    public SocialActionResult sendFriendRequest(UUID sender, UUID target) {
        if (sender == null || target == null) {
            return SocialActionResult.failure("Invalid player.");
        }
        if (sender.equals(target)) {
            return SocialActionResult.failure("You cannot add yourself.");
        }

        SocialPlayerData senderData = getPlayerData(sender);
        SocialPlayerData targetData = getPlayerData(target);

        if (senderData.getFriends().contains(target)) {
            return SocialActionResult.failure("You are already friends.");
        }
        if (senderData.getIgnored().contains(target)) {
            return SocialActionResult.failure("Unignore that player before sending a friend request.");
        }
        if (targetData.getIgnored().contains(sender)) {
            return SocialActionResult.failure("That player is not accepting requests from you.");
        }
        if (targetData.getIncomingFriendRequests().contains(sender)) {
            return SocialActionResult.failure("Friend request already sent.");
        }
        if (senderData.getIncomingFriendRequests().contains(target)) {
            return acceptFriendRequest(sender, target);
        }

        senderData.getOutgoingFriendRequests().add(target);
        targetData.getIncomingFriendRequests().add(sender);
        persistAll(senderData, targetData);

        notifyPlayer(target, "Friend request from " + resolveDisplayName(sender) + ". Open Friends tab to accept or deny.");
        return SocialActionResult.success("Friend request sent to " + resolveDisplayName(target) + ".");
    }

    public SocialActionResult acceptFriendRequest(UUID receiver, UUID requester) {
        if (receiver == null || requester == null || receiver.equals(requester)) {
            return SocialActionResult.failure("Invalid friend request.");
        }

        SocialPlayerData receiverData = getPlayerData(receiver);
        SocialPlayerData requesterData = getPlayerData(requester);

        if (!receiverData.getIncomingFriendRequests().contains(requester)) {
            return SocialActionResult.failure("No pending request from that player.");
        }

        receiverData.getIncomingFriendRequests().remove(requester);
        requesterData.getOutgoingFriendRequests().remove(receiver);
        receiverData.getFriends().add(requester);
        requesterData.getFriends().add(receiver);
        persistAll(receiverData, requesterData);

        notifyPlayer(requester, resolveDisplayName(receiver) + " accepted your friend request.");
        return SocialActionResult.success("You are now friends with " + resolveDisplayName(requester) + ".");
    }

    public SocialActionResult denyFriendRequest(UUID receiver, UUID requester) {
        if (receiver == null || requester == null || receiver.equals(requester)) {
            return SocialActionResult.failure("Invalid friend request.");
        }

        SocialPlayerData receiverData = getPlayerData(receiver);
        SocialPlayerData requesterData = getPlayerData(requester);

        if (!receiverData.getIncomingFriendRequests().remove(requester)) {
            return SocialActionResult.failure("No pending request from that player.");
        }
        requesterData.getOutgoingFriendRequests().remove(receiver);
        persistAll(receiverData, requesterData);

        notifyPlayer(requester, resolveDisplayName(receiver) + " denied your friend request.");
        return SocialActionResult.success("Friend request denied.");
    }

    public SocialActionResult removeFriend(UUID player, UUID other) {
        if (player == null || other == null || player.equals(other)) {
            return SocialActionResult.failure("Invalid friend target.");
        }

        SocialPlayerData playerData = getPlayerData(player);
        SocialPlayerData otherData = getPlayerData(other);

        boolean removedA = playerData.getFriends().remove(other);
        boolean removedB = otherData.getFriends().remove(player);

        if (!removedA && !removedB) {
            return SocialActionResult.failure("That player is not on your friends list.");
        }

        persistAll(playerData, otherData);
        notifyPlayer(other, resolveDisplayName(player) + " removed you from friends.");
        return SocialActionResult.success("Removed " + resolveDisplayName(other) + " from friends.");
    }

    public SocialActionResult ignore(UUID player, UUID target) {
        if (player == null || target == null || player.equals(target)) {
            return SocialActionResult.failure("Invalid ignore target.");
        }

        SocialPlayerData playerData = getPlayerData(player);
        SocialPlayerData targetData = getPlayerData(target);

        if (playerData.getIgnored().contains(target)) {
            return SocialActionResult.failure("That player is already ignored.");
        }

        playerData.getIgnored().add(target);

        playerData.getFriends().remove(target);
        targetData.getFriends().remove(player);

        playerData.getIncomingFriendRequests().remove(target);
        playerData.getOutgoingFriendRequests().remove(target);
        targetData.getIncomingFriendRequests().remove(player);
        targetData.getOutgoingFriendRequests().remove(player);

        persistAll(playerData, targetData);
        return SocialActionResult.success("Ignored " + resolveDisplayName(target) + ".");
    }

    public SocialActionResult unignore(UUID player, UUID target) {
        if (player == null || target == null || player.equals(target)) {
            return SocialActionResult.failure("Invalid ignore target.");
        }

        SocialPlayerData playerData = getPlayerData(player);
        if (!playerData.getIgnored().remove(target)) {
            return SocialActionResult.failure("That player is not ignored.");
        }

        persist(playerData);
        return SocialActionResult.success("Unignored " + resolveDisplayName(target) + ".");
    }

    public List<UUID> getFriends(UUID uuid) {
        return toSortedList(getPlayerData(uuid).getFriends());
    }

    public List<UUID> getIgnored(UUID uuid) {
        return toSortedList(getPlayerData(uuid).getIgnored());
    }

    public List<UUID> getIncomingRequests(UUID uuid) {
        return toSortedList(getPlayerData(uuid).getIncomingFriendRequests());
    }

    public List<UUID> getOutgoingRequests(UUID uuid) {
        return toSortedList(getPlayerData(uuid).getOutgoingFriendRequests());
    }

    public boolean isFriend(UUID player, UUID other) {
        return player != null && other != null && getPlayerData(player).getFriends().contains(other);
    }

    public boolean isIgnored(UUID player, UUID other) {
        return player != null && other != null && getPlayerData(player).getIgnored().contains(other);
    }

    public boolean hasIncomingRequest(UUID receiver, UUID requester) {
        return receiver != null && requester != null && getPlayerData(receiver).getIncomingFriendRequests().contains(requester);
    }

    public boolean hasOutgoingRequest(UUID sender, UUID target) {
        return sender != null && target != null && getPlayerData(sender).getOutgoingFriendRequests().contains(target);
    }

    public boolean isOnline(UUID uuid) {
        Universe universe = Universe.get();
        return universe != null && universe.getPlayer(uuid) != null;
    }

    public String resolveDisplayName(UUID uuid) {
        if (uuid == null) {
            return "Unknown";
        }
        Universe universe = Universe.get();
        if (universe != null) {
            PlayerRef player = universe.getPlayer(uuid);
            if (player != null && player.getUsername() != null && !player.getUsername().isBlank()) {
                return player.getUsername();
            }
        }
        String debugAlias = debugAliases.get(uuid);
        if (debugAlias != null && !debugAlias.isBlank()) {
            return debugAlias;
        }
        String raw = uuid.toString();
        return raw.substring(0, Math.min(raw.length(), 8));
    }

    /**
     * Seeds predictable solo-test social data for a player.
     */
    public String debugSeedSolo(UUID playerUuid) {
        if (playerUuid == null) {
            return "Social debug seed failed: missing player UUID.";
        }

        debugClearSolo(playerUuid);

        UUID friendA = debugUuid(playerUuid, "friend_a");
        UUID friendB = debugUuid(playerUuid, "friend_b");
        UUID friendC = debugUuid(playerUuid, "friend_c");
        UUID incomingA = debugUuid(playerUuid, "incoming_a");
        UUID incomingB = debugUuid(playerUuid, "incoming_b");
        UUID outgoingA = debugUuid(playerUuid, "outgoing_a");
        UUID ignoredA = debugUuid(playerUuid, "ignored_a");

        setDebugAlias(friendA, "Ayla");
        setDebugAlias(friendB, "Borin");
        setDebugAlias(friendC, "Cinder");
        setDebugAlias(incomingA, "Dahlia");
        setDebugAlias(incomingB, "Eryndor");
        setDebugAlias(outgoingA, "Fenris");
        setDebugAlias(ignoredA, "Grim");

        SocialPlayerData playerData = getPlayerData(playerUuid);
        SocialPlayerData friendAData = getPlayerData(friendA);
        SocialPlayerData friendBData = getPlayerData(friendB);
        SocialPlayerData friendCData = getPlayerData(friendC);
        SocialPlayerData incomingAData = getPlayerData(incomingA);
        SocialPlayerData incomingBData = getPlayerData(incomingB);
        SocialPlayerData outgoingAData = getPlayerData(outgoingA);
        SocialPlayerData ignoredAData = getPlayerData(ignoredA);

        linkFriends(playerData, friendAData);
        linkFriends(playerData, friendBData);
        linkFriends(playerData, friendCData);

        playerData.getIncomingFriendRequests().add(incomingA);
        playerData.getIncomingFriendRequests().add(incomingB);
        incomingAData.getOutgoingFriendRequests().add(playerUuid);
        incomingBData.getOutgoingFriendRequests().add(playerUuid);

        playerData.getOutgoingFriendRequests().add(outgoingA);
        outgoingAData.getIncomingFriendRequests().add(playerUuid);

        playerData.getIgnored().add(ignoredA);
        ignoredAData.getFriends().remove(playerUuid);
        ignoredAData.getIncomingFriendRequests().remove(playerUuid);
        ignoredAData.getOutgoingFriendRequests().remove(playerUuid);

        persistAll(playerData, friendAData, friendBData, friendCData, incomingAData, incomingBData, outgoingAData, ignoredAData);

        return "Social debug seed complete: friends=3, incoming=2, outgoing=1, ignored=1.";
    }

    /**
     * Clears social data for the player and removes seeded debug aliases.
     */
    public String debugClearSolo(UUID playerUuid) {
        if (playerUuid == null) {
            return "Social debug clear failed: missing player UUID.";
        }

        SocialPlayerData playerData = getPlayerData(playerUuid);
        Set<UUID> touched = new HashSet<>();
        touched.addAll(playerData.getFriends());
        touched.addAll(playerData.getIgnored());
        touched.addAll(playerData.getIncomingFriendRequests());
        touched.addAll(playerData.getOutgoingFriendRequests());

        for (UUID other : touched) {
            SocialPlayerData otherData = getPlayerData(other);
            otherData.getFriends().remove(playerUuid);
            otherData.getIncomingFriendRequests().remove(playerUuid);
            otherData.getOutgoingFriendRequests().remove(playerUuid);
            persist(otherData);
        }

        playerData.getFriends().clear();
        playerData.getIgnored().clear();
        playerData.getIncomingFriendRequests().clear();
        playerData.getOutgoingFriendRequests().clear();
        persist(playerData);

        for (String key : DEBUG_KEYS) {
            debugAliases.remove(debugUuid(playerUuid, key));
        }

        return "Social debug data cleared.";
    }

    /**
     * Cycles one deterministic request path for repeatable solo testing:
     * pending request -> accepted friend -> removed friend -> pending request...
     */
    public String debugCycleSolo(UUID playerUuid) {
        if (playerUuid == null) {
            return "Social debug cycle failed: missing player UUID.";
        }

        UUID cycleUuid = debugUuid(playerUuid, "cycle");
        setDebugAlias(cycleUuid, "CycleTester");
        SocialPlayerData playerData = getPlayerData(playerUuid);
        SocialPlayerData cycleData = getPlayerData(cycleUuid);

        if (playerData.getIgnored().contains(cycleUuid)) {
            playerData.getIgnored().remove(cycleUuid);
        }

        if (playerData.getIncomingFriendRequests().contains(cycleUuid)) {
            SocialActionResult accepted = acceptFriendRequest(playerUuid, cycleUuid);
            return "Cycle step: request accepted. " + accepted.message();
        }
        if (playerData.getFriends().contains(cycleUuid)) {
            SocialActionResult removed = removeFriend(playerUuid, cycleUuid);
            return "Cycle step: friend removed. " + removed.message();
        }

        playerData.getIncomingFriendRequests().add(cycleUuid);
        cycleData.getOutgoingFriendRequests().add(playerUuid);
        persistAll(playerData, cycleData);
        return "Cycle step: incoming request created from CycleTester.";
    }

    private void setDebugAlias(UUID uuid, String alias) {
        if (uuid != null && alias != null && !alias.isBlank()) {
            debugAliases.put(uuid, alias);
        }
    }

    private UUID debugUuid(UUID ownerUuid, String key) {
        String raw = "hyrune-social-debug:" + ownerUuid + ":" + key;
        return UUID.nameUUIDFromBytes(raw.getBytes(StandardCharsets.UTF_8));
    }

    private void linkFriends(SocialPlayerData a, SocialPlayerData b) {
        if (a == null || b == null || a.getUuid() == null || b.getUuid() == null) {
            return;
        }
        a.getFriends().add(b.getUuid());
        b.getFriends().add(a.getUuid());
        a.getIncomingFriendRequests().remove(b.getUuid());
        a.getOutgoingFriendRequests().remove(b.getUuid());
        b.getIncomingFriendRequests().remove(a.getUuid());
        b.getOutgoingFriendRequests().remove(a.getUuid());
    }

    private List<UUID> toSortedList(Collection<UUID> uuids) {
        List<UUID> sorted = new ArrayList<>(uuids);
        sorted.sort(Comparator.comparing(this::resolveDisplayName, String.CASE_INSENSITIVE_ORDER));
        return sorted;
    }

    private SocialPlayerData loadOrCreate(UUID uuid) {
        SocialPlayerData stored = repository.load(uuid);
        if (stored == null) {
            return new SocialPlayerData(uuid);
        }
        normalizeData(stored, uuid);
        return stored;
    }

    private void normalizeData(SocialPlayerData data, UUID fallbackUuid) {
        if (data.getUuid() == null) {
            data.setUuid(fallbackUuid);
        }
        data.setFriends(ensureSet(data.getFriends()));
        data.setIgnored(ensureSet(data.getIgnored()));
        data.setIncomingFriendRequests(ensureSet(data.getIncomingFriendRequests()));
        data.setOutgoingFriendRequests(ensureSet(data.getOutgoingFriendRequests()));
    }

    private Set<UUID> ensureSet(Set<UUID> values) {
        return values != null ? values : new HashSet<>();
    }

    private void notifyPlayer(UUID uuid, String message) {
        Universe universe = Universe.get();
        if (universe == null) {
            return;
        }
        PlayerRef player = universe.getPlayer(uuid);
        if (player != null) {
            player.sendMessage(Message.raw(message));
        }
    }

    private void persistAll(SocialPlayerData... entries) {
        if (entries == null) {
            return;
        }
        for (SocialPlayerData entry : entries) {
            persist(entry);
        }
    }

    private void persist(SocialPlayerData data) {
        if (data == null || data.getUuid() == null) {
            return;
        }
        try {
            repository.save(data);
        } catch (RuntimeException e) {
            LOGGER.at(Level.WARNING).log("Failed to save social data for " + data.getUuid() + ": " + e.getMessage());
        }
    }
}
