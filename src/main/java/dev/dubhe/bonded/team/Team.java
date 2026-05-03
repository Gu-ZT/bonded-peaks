package dev.dubhe.bonded.team;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Team {
    private final String name;
    private UUID owner;
    private final List<UUID> members;
    private final long createTime;

    public Team(String name, UUID owner, List<UUID> members, long createTime) {
        this.name = Objects.requireNonNull(name, "name");
        this.owner = Objects.requireNonNull(owner, "owner");
        this.members = new ArrayList<>(Objects.requireNonNull(members, "members"));
        if (!this.members.contains(owner)) {
            this.members.addFirst(owner);
        }
        this.createTime = createTime;
    }

    public static Team create(String name, UUID owner, long createTime) {
        return new Team(name, owner, List.of(owner), createTime);
    }

    public String getName() {
        return this.name;
    }

    public UUID getOwner() {
        return this.owner;
    }

    public List<UUID> getMembers() {
        return Collections.unmodifiableList(this.members);
    }

    public long getCreateTime() {
        return this.createTime;
    }

    public boolean isOwner(UUID playerId) {
        return this.owner.equals(playerId);
    }

    public boolean hasMember(UUID playerId) {
        return this.members.contains(playerId);
    }

    public int getMemberCount() {
        return this.members.size();
    }

    public void addMember(UUID playerId) {
        if (!this.members.contains(playerId)) {
            this.members.add(playerId);
        }
    }

    public void removeMember(UUID playerId) {
        this.members.remove(playerId);
    }

    public void transferOwnership(UUID newOwner) {
        if (!this.members.contains(newOwner)) {
            throw new IllegalArgumentException("New owner must be a member of the team.");
        }
        this.owner = newOwner;
        this.members.remove(newOwner);
        this.members.addFirst(newOwner);
    }
}

