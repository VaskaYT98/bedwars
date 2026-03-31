package org.vaska.bedwars.matchmaking;

import org.bukkit.entity.Player;
import org.vaska.bedwars.Bedwars;
import org.vaska.bedwars.arena.Arena;
import org.vaska.bedwars.arena.ArenaManager;
import org.vaska.bedwars.party.Party;
import org.vaska.bedwars.party.PartyManager;

import java.util.Optional;
import java.util.UUID;

/**
 * Lightweight matchmaking: finds available arena or creates new instance.
 */
public class MatchmakingService {
    private final Bedwars plugin;
    private final ArenaManager arenaManager;
    private final PartyManager partyManager;

    public MatchmakingService(Bedwars plugin, ArenaManager arenaManager, PartyManager partyManager) {
        this.plugin = plugin; this.arenaManager = arenaManager; this.partyManager = partyManager;
    }

    public void join(Player player) {
        join(player, null);
    }

    public void join(Player player, String mode) {
        // determine party size and try to keep party together
        Optional<Party> maybeParty = partyManager.getParty(player.getUniqueId());
        if (maybeParty.isPresent()) {
            Party party = maybeParty.get();
            int partySize = party.size();
            // find arena with enough space on single team
            Optional<Arena> opt = arenaManager.findAvailableArena(partySize);
            if (opt.isPresent()) {
                Arena a = opt.get();
                if (a.canAssignParty(partySize)) {
                    UUID[] members = party.getMembers().toArray(new UUID[0]);
                    int team = a.assignParty(members);
                    // teleport/notify members
                    for (UUID uid : members) {
                        org.bukkit.entity.Player p = org.bukkit.Bukkit.getPlayer(uid);
                        if (p != null && p.isOnline()) {
                            java.util.Map<String, String> vars = new java.util.HashMap<>();
                            vars.put("arena", a.getId());
                            vars.put("team", String.valueOf(team));
                            org.vaska.bedwars.Bedwars.getInstance().getMessages().send(p, "messages.matchmaking.party_joined_arena", vars);
                        }
                    }
                    return;
                }
            }
            // otherwise create new instance sized for mode/party
            String template = "example_arena";
            arenaManager.createInstanceAsync(template).thenAccept(a -> {
                UUID[] members = party.getMembers().toArray(new UUID[0]);
                int team = a.assignParty(members);
                for (UUID uid : members) {
                    org.bukkit.entity.Player p = org.bukkit.Bukkit.getPlayer(uid);
                    if (p != null && p.isOnline()) {
                        java.util.Map<String, String> vars = new java.util.HashMap<>();
                        vars.put("arena", a.getId());
                        vars.put("team", String.valueOf(team));
                        org.vaska.bedwars.Bedwars.getInstance().getMessages().send(p, "messages.matchmaking.created_and_joined_arena", vars);
                    }
                }
            });
            return;
        }

        // single player flow
        int needed = 1;
        Optional<Arena> opt = arenaManager.findAvailableArena(needed);
        if (opt.isPresent()) {
            Arena a = opt.get();
            int team = org.vaska.bedwars.matchmaking.TeamBalancer.assignTeam(a);
            a.join(player);
            java.util.Map<String, String> vars = new java.util.HashMap<>();
            vars.put("arena", a.getId());
            vars.put("team", String.valueOf(team));
            org.vaska.bedwars.Bedwars.getInstance().getMessages().send(player, "messages.matchmaking.joined_arena", vars);
            return;
        }

        arenaManager.createInstanceAsync("example_arena").thenAccept(a -> {
            int team = org.vaska.bedwars.matchmaking.TeamBalancer.assignTeam(a);
            a.join(player);
            java.util.Map<String, String> vars = new java.util.HashMap<>();
            vars.put("arena", a.getId());
            vars.put("team", String.valueOf(team));
            org.vaska.bedwars.Bedwars.getInstance().getMessages().send(player, "messages.matchmaking.created_and_joined_arena", vars);
        });
    }

    public void joinParty(UUID leader) {
        // placeholder for party join orchestration
    }
}
