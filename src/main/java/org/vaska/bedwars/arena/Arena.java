package org.vaska.bedwars.arena;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a single arena instance. Thread-safe collections used for concurrency.
 */
public class Arena {
    private final String id; // unique instance id
    private final String templateName; // name of arena template
    private volatile ArenaState state = ArenaState.WAITING;
    private final int maxPlayers;
    private final int teams;
    private final int playersPerTeam;
    private volatile World world;

    private final Set<UUID> players = ConcurrentHashMap.newKeySet();
    private final Map<String, Location> spawnLocations = new ConcurrentHashMap<>();
    private final Map<String, Location> bedLocations = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> teamAssignments = new ConcurrentHashMap<>();
    private final Map<Integer, Set<UUID>> teamPlayers = new ConcurrentHashMap<>();

    public Arena(String id, String templateName, int playersPerTeam, int teams) {
        this.id = id;
        this.templateName = templateName;
        this.playersPerTeam = playersPerTeam;
        this.teams = teams;
        this.maxPlayers = playersPerTeam * teams;
        for (int i = 1; i <= teams; i++) teamPlayers.put(i, ConcurrentHashMap.newKeySet());
    }

    public String getId() { return id; }

    public String getTemplateName() { return templateName; }

    public synchronized void setWorld(World world) { this.world = world; }

    public World getWorld() { return world; }

    public ArenaState getState() { return state; }

    public synchronized void setState(ArenaState state) { this.state = state; }

    public boolean isAvailable() {
        return state == ArenaState.WAITING && players.size() < maxPlayers;
    }

    public int getPlayerCount() { return players.size(); }

    public int getMaxPlayers() { return maxPlayers; }

    public int getTeams() { return teams; }

    public int getPlayersPerTeam() { return playersPerTeam; }

    public boolean join(Player player) {
        if (!isAvailable()) return false;
        players.add(player.getUniqueId());
        int assigned = org.vaska.bedwars.matchmaking.TeamBalancer.assignTeam(this);
        teamAssignments.put(player.getUniqueId(), assigned);
        teamPlayers.computeIfAbsent(assigned, k -> ConcurrentHashMap.newKeySet()).add(player.getUniqueId());
        // teleport to team spawn if available
        Location spawn = spawnLocations.getOrDefault("team" + assigned, null);
        if (spawn != null && player.isOnline()) player.teleport(spawn);
        return true;
    }

    public void leave(Player player) {
        players.remove(player.getUniqueId());
        Integer t = teamAssignments.remove(player.getUniqueId());
        if (t != null) {
            Set<UUID> set = teamPlayers.get(t);
            if (set != null) set.remove(player.getUniqueId());
        }
    }

    public Integer getTeam(UUID uuid) { return teamAssignments.get(uuid); }

    public Map<UUID, Integer> getTeamAssignments() { return teamAssignments; }

    public boolean canAssignParty(int partySize) {
        // find any team with enough free slots
        for (int i = 1; i <= teams; i++) {
            Set<UUID> set = teamPlayers.get(i);
            int free = playersPerTeam - (set == null ? 0 : set.size());
            if (free >= partySize) return true;
        }
        return false;
    }

    public int assignParty(UUID[] members) {
        int size = members.length;
        for (int i = 1; i <= teams; i++) {
            Set<UUID> set = teamPlayers.get(i);
            int free = playersPerTeam - (set == null ? 0 : set.size());
            if (free >= size) {
                for (UUID u : members) {
                    teamAssignments.put(u, i);
                    set.add(u);
                }
                return i;
            }
        }
        // fallback: assign individually
        int assigned = org.vaska.bedwars.matchmaking.TeamBalancer.assignTeam(this);
        for (UUID u : members) {
            teamAssignments.put(u, assigned);
            teamPlayers.computeIfAbsent(assigned, k -> ConcurrentHashMap.newKeySet()).add(u);
        }
        return assigned;
    }

    public Set<UUID> getPlayers() { return Collections.unmodifiableSet(players); }

    public void addSpawn(String teamKey, Location loc) { spawnLocations.put(teamKey, loc); }

    public void addBed(String teamKey, Location loc) { bedLocations.put(teamKey, loc); }

    public void start() {
        if (state != ArenaState.WAITING) return;
        setState(ArenaState.STARTING);
        // set state to INGAME after preparations
        Bukkit.getScheduler().runTaskLaterAsynchronously(Bukkit.getPluginManager().getPlugin("bedwars"), () -> setState(ArenaState.INGAME), 60L);
    }

    public void end() {
        setState(ArenaState.ENDING);
        // perform cleanup later
        Bukkit.getScheduler().runTaskLaterAsynchronously(Bukkit.getPluginManager().getPlugin("bedwars"), () -> setState(ArenaState.RESETTING), 40L);
    }
}
