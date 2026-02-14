package dev.hytalemodding.hyrune.playerdata;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Persistent social state for one player.
 */
public class SocialPlayerData implements PlayerData {
    private UUID uuid;
    private Set<UUID> friends;
    private Set<UUID> ignored;
    private Set<UUID> incomingFriendRequests;
    private Set<UUID> outgoingFriendRequests;

    public SocialPlayerData() {
        this.friends = new HashSet<>();
        this.ignored = new HashSet<>();
        this.incomingFriendRequests = new HashSet<>();
        this.outgoingFriendRequests = new HashSet<>();
    }

    public SocialPlayerData(UUID uuid) {
        this();
        this.uuid = uuid;
    }

    @Override
    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public Set<UUID> getFriends() {
        return friends;
    }

    public void setFriends(Set<UUID> friends) {
        this.friends = friends != null ? friends : new HashSet<>();
    }

    public Set<UUID> getIgnored() {
        return ignored;
    }

    public void setIgnored(Set<UUID> ignored) {
        this.ignored = ignored != null ? ignored : new HashSet<>();
    }

    public Set<UUID> getIncomingFriendRequests() {
        return incomingFriendRequests;
    }

    public void setIncomingFriendRequests(Set<UUID> incomingFriendRequests) {
        this.incomingFriendRequests = incomingFriendRequests != null ? incomingFriendRequests : new HashSet<>();
    }

    public Set<UUID> getOutgoingFriendRequests() {
        return outgoingFriendRequests;
    }

    public void setOutgoingFriendRequests(Set<UUID> outgoingFriendRequests) {
        this.outgoingFriendRequests = outgoingFriendRequests != null ? outgoingFriendRequests : new HashSet<>();
    }
}

