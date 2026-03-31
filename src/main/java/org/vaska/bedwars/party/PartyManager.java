package org.vaska.bedwars.party;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.vaska.bedwars.Bedwars;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple party manager: handles invites, accept, leave, disband. Kept lightweight and thread-safe.
 */
public class PartyManager {
    private final Bedwars plugin;
    private final Map<UUID, Party> leaderParties = new ConcurrentHashMap<>();
    private final Map<UUID, Party> memberIndex = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> invites = new ConcurrentHashMap<>();

    public PartyManager(Bedwars plugin) { this.plugin = plugin; }

    public boolean invite(Player from, Player to) {
        invites.computeIfAbsent(to.getUniqueId(), k -> ConcurrentHashMap.newKeySet()).add(from.getUniqueId());
        to.sendMessage("&aYou have been invited to a party by &f" + from.getName());
        from.sendMessage("&aInvite sent to &f" + to.getName());
        return true;
    }

    public boolean accept(Player who, Player inviter) {
        Set<UUID> set = invites.getOrDefault(who.getUniqueId(), Collections.emptySet());
        if (!set.contains(inviter.getUniqueId())) return false;
        set.remove(inviter.getUniqueId());
        Party party = leaderParties.get(inviter.getUniqueId());
        if (party == null) {
            party = new Party(inviter.getUniqueId());
            leaderParties.put(inviter.getUniqueId(), party);
            memberIndex.put(inviter.getUniqueId(), party);
        }
        party.add(who.getUniqueId());
        memberIndex.put(who.getUniqueId(), party);
        who.sendMessage("&aYou joined &f" + inviter.getName() + "'s party");
        inviter.sendMessage("&a" + who.getName() + " joined your party");
        return true;
    }

    public void leave(Player who) {
        Party p = memberIndex.remove(who.getUniqueId());
        if (p != null) {
            p.remove(who.getUniqueId());
            if (p.size() == 0) leaderParties.values().removeIf(pp -> pp == p);
        }
    }

    public void disband(Party party) {
        leaderParties.values().removeIf(p -> p == party);
        party.getMembers().forEach(memberIndex::remove);
    }

    public Optional<Party> getParty(UUID player) {
        return Optional.ofNullable(memberIndex.get(player));
    }
}
