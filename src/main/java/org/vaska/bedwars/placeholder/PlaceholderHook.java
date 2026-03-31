package org.vaska.bedwars.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.vaska.bedwars.Bedwars;
import org.vaska.bedwars.db.PlayerDataManager;

import java.util.concurrent.CompletableFuture;

/**
 * PlaceholderAPI expansion exposing a few BedWars placeholders.
 *
 * This implementation avoids blocking the main thread by:
 * - returning cached values when available for fast stats
 * - scheduling heavy DB queries asynchronously when called from the main thread
 */
public class PlaceholderHook extends PlaceholderExpansion {
    private final Bedwars plugin;

    public PlaceholderHook(Bedwars plugin) { this.plugin = plugin; }

    @Override
    public boolean canRegister() { return true; }

    @Override
    public String getIdentifier() { return "bw"; }

    @Override
    public String getAuthor() { return "vaska"; }

    @Override
    public String getVersion() { return "1.0.0"; }

    @Override
    public String onRequest(OfflinePlayer player, String identifier) {
        if (player == null) return "";
        PlayerDataManager pdm = plugin.getPlayerDataManager();
        try {
            boolean sync = Bukkit.isPrimaryThread();

            // direct stats (fast, cached when possible)
            switch (identifier) {
                case "winstreak":
                    if (pdm != null) {
                        int v = pdm.getCachedValue(player.getUniqueId(), "winstreak");
                        if (v >= 0) return String.valueOf(v);
                        // warm cache asynchronously if we are on the main thread
                        if (sync) pdm.loadStatsAsync(player.getUniqueId());
                        return "0";
                    }
                    return "0";
                case "kills":
                case "deaths":
                case "wins":
                case "losses":
                case "beds_broken":
                    if (pdm != null) {
                        int v = pdm.getCachedValue(player.getUniqueId(), identifier);
                        if (v >= 0) return String.valueOf(v);
                        if (sync) pdm.loadStatsAsync(player.getUniqueId());
                        return "0";
                    }
                    return "0";
                case "rank":
                    if (pdm != null) {
                        if (sync) {
                            // schedule async rank computation to avoid blocking main thread
                            CompletableFuture.runAsync(() -> {
                                try { pdm.getRankByWins(player.getUniqueId()); } catch (Throwable ignored) {}
                            });
                            return "Unranked";
                        } else {
                            int r = pdm.getRankByWins(player.getUniqueId());
                            return r <= 0 ? "Unranked" : String.valueOf(r);
                        }
                    }
                    return "Unranked";
                case "level":
                    if (pdm != null) {
                        int wins = pdm.getCachedValue(player.getUniqueId(), "wins");
                        int kills = pdm.getCachedValue(player.getUniqueId(), "kills");
                        if (wins < 0 || kills < 0) {
                            if (sync) pdm.loadStatsAsync(player.getUniqueId());
                            wins = Math.max(0, wins);
                            kills = Math.max(0, kills);
                        }
                        int lvl = (int)Math.floor(Math.sqrt(wins + kills));
                        return String.valueOf(lvl);
                    }
                    return "0";
            }

            // tops: format top_wins_1, top_kills_2
            if (identifier.startsWith("top_")) {
                String[] parts = identifier.split("_");
                if (parts.length >= 3) {
                    String metric = parts[1];
                    int idx = 1;
                    try { idx = Integer.parseInt(parts[2]); } catch (NumberFormatException ignored) {}
                    if (pdm != null) {
                        if (sync) {
                            // compute asynchronously to avoid large DB query on main thread
                            int finalIdx = idx;
                            CompletableFuture.runAsync(() -> {
                                try { pdm.getTopBy(metric.equals("kills")?"kills":"wins", Math.max(10, finalIdx)); } catch (Throwable ignored) {}
                            });
                            return "-";
                        } else {
                            java.util.List<java.util.Map.Entry<java.util.UUID,Integer>> tops = pdm.getTopBy(metric.equals("kills")?"kills":"wins", Math.max(10, idx));
                            if (tops.size() >= idx) {
                                java.util.UUID uid = tops.get(idx-1).getKey();
                                int val = tops.get(idx-1).getValue();
                                String name = org.bukkit.Bukkit.getOfflinePlayer(uid).getName();
                                return (name == null ? uid.toString() : name) + " - " + val;
                            }
                        }
                    }
                    return "-";
                }
            }

            // map name: current arena template for player
            if (identifier.equals("map")) {
                org.vaska.bedwars.arena.ArenaManager am = plugin.getArenaManager();
                if (am != null) {
                    java.util.Optional<org.vaska.bedwars.arena.Arena> a = am.getArenaForPlayer(player.getUniqueId());
                    if (a.isPresent()) return a.get().getTemplateName();
                }
                return "lobby";
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("PlaceholderHook error for " + identifier + ": " + t.getMessage());
        }
        return "";
    }
}
