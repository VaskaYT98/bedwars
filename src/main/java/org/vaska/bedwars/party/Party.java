package org.vaska.bedwars.party;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Party {
    private final UUID leader;
    private final Set<UUID> members = new HashSet<>();

    public Party(UUID leader) {
        this.leader = leader;
        this.members.add(leader);
    }

    public UUID getLeader() { return leader; }

    public Set<UUID> getMembers() { return Collections.unmodifiableSet(members); }

    public boolean add(UUID player) { return members.add(player); }

    public boolean remove(UUID player) { return members.remove(player); }

    public int size() { return members.size(); }
}
