package org.vaska.bedwars.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.vaska.bedwars.Bedwars;

import java.io.File;
import java.sql.*;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Asynchronous player data manager using SQLite (HikariCP). Caches player stats in memory.
 */
public class PlayerDataManager {
    private final Bedwars plugin;
    private HikariDataSource ds;
    private final ExecutorService dbPool = Executors.newFixedThreadPool(2);
    private final ConcurrentMap<UUID, PlayerStats> cache = new ConcurrentHashMap<>();

    public PlayerDataManager(Bedwars plugin) { this.plugin = plugin; }

    public void initialize() {
        try {
            File dbFile = new File(plugin.getDataFolder(), "bedwars.db");
            dbFile.getParentFile().mkdirs();
            HikariConfig cfg = new HikariConfig();
            cfg.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            cfg.setMaximumPoolSize(3);
            ds = new HikariDataSource(cfg);
            createTables();
        } catch (Exception ex) {
            plugin.getLogger().severe("Failed to initialize database: " + ex.getMessage());
        }
    }

    private void createTables() throws SQLException {
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS stats(uuid TEXT PRIMARY KEY, kills INTEGER, deaths INTEGER, wins INTEGER, losses INTEGER, beds_broken INTEGER, winstreak INTEGER)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS player_upgrades(uuid TEXT, upgrade_id TEXT, level INTEGER, PRIMARY KEY(uuid, upgrade_id))");
        }
    }

    /** Get player's upgrade level asynchronously (returns 0 when not present). */
    public CompletableFuture<Integer> getUpgradeLevelAsync(UUID uuid, String upgradeId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT level FROM player_upgrades WHERE uuid=? AND upgrade_id=?")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, upgradeId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt(1);
                }
            } catch (Throwable t) { plugin.getLogger().warning("Failed to get upgrade level: " + t.getMessage()); }
            return 0;
        }, dbPool);
    }

    /** Set player's upgrade level asynchronously. */
    public CompletableFuture<Void> setUpgradeLevelAsync(UUID uuid, String upgradeId, int level) {
        return CompletableFuture.runAsync(() -> {
            try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement("INSERT INTO player_upgrades(uuid,upgrade_id,level) VALUES(?,?,?) ON CONFLICT(uuid,upgrade_id) DO UPDATE SET level=excluded.level")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, upgradeId);
                ps.setInt(3, level);
                ps.executeUpdate();
            } catch (Throwable t) { plugin.getLogger().warning("Failed to set upgrade level: " + t.getMessage()); }
        }, dbPool);
    }

    public CompletableFuture<PlayerStats> loadStatsAsync(UUID uuid) {
        if (cache.containsKey(uuid)) return CompletableFuture.completedFuture(cache.get(uuid));
        return CompletableFuture.supplyAsync(() -> {
            try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT kills,deaths,wins,losses,beds_broken,winstreak FROM stats WHERE uuid=?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        PlayerStats s = new PlayerStats(uuid, rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getInt(4), rs.getInt(5), rs.getInt(6));
                        cache.put(uuid, s);
                        return s;
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("DB load error: " + e.getMessage());
            }
            PlayerStats s = new PlayerStats(uuid,0,0,0,0,0,0);
            cache.put(uuid,s);
            return s;
        }, dbPool);
    }

    /**
     * Get top players by a numeric column (e.g., wins, kills). Returns list of (uuid, value) ordered desc.
     */
    public java.util.List<java.util.Map.Entry<java.util.UUID, Integer>> getTopBy(String column, int limit) {
        java.util.List<java.util.Map.Entry<java.util.UUID, Integer>> out = new java.util.ArrayList<>();
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT uuid," + column + " FROM stats ORDER BY " + column + " DESC LIMIT ?")) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    java.util.UUID u = java.util.UUID.fromString(rs.getString(1));
                    int v = rs.getInt(2);
                    out.add(new java.util.AbstractMap.SimpleEntry<>(u, v));
                }
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("Failed to get top by " + column + ": " + t.getMessage());
        }
        return out;
    }

    /** Get rank of player by wins (1 = top). Returns -1 if unknown. */
    public int getRankByWins(UUID uuid) {
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT wins FROM stats WHERE uuid=?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return -1;
                int wins = rs.getInt(1);
                try (PreparedStatement ps2 = c.prepareStatement("SELECT COUNT(*)+1 FROM stats WHERE wins > ?")) {
                    ps2.setInt(1, wins);
                    try (ResultSet rs2 = ps2.executeQuery()) {
                        if (rs2.next()) return rs2.getInt(1);
                    }
                }
            }
        } catch (Throwable t) { plugin.getLogger().warning("Failed to compute rank: " + t.getMessage()); }
        return -1;
    }

    /** Get a numeric stat value from cache if present, otherwise -1 to indicate unknown. */
    public int getCachedValue(UUID uuid, String key) {
        PlayerStats s = cache.get(uuid);
        if (s == null) return -1;
        switch (key) {
            case "kills": return s.getKills();
            case "deaths": return s.getDeaths();
            case "wins": return s.getWins();
            case "losses": return s.getLosses();
            case "beds_broken": return s.getBedsBroken();
            case "winstreak": return s.getWinstreak();
            default: return -1;
        }
    }

    public CompletableFuture<Void> saveStatsAsync(PlayerStats stats) {
        cache.put(stats.getUuid(), stats);
        return CompletableFuture.runAsync(() -> {
            try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement("INSERT INTO stats(uuid,kills,deaths,wins,losses,beds_broken,winstreak) VALUES(?,?,?,?,?,?,?) ON CONFLICT(uuid) DO UPDATE SET kills=excluded.kills,deaths=excluded.deaths,wins=excluded.wins,losses=excluded.losses,beds_broken=excluded.beds_broken,winstreak=excluded.winstreak")) {
                ps.setString(1, stats.getUuid().toString());
                ps.setInt(2, stats.getKills());
                ps.setInt(3, stats.getDeaths());
                ps.setInt(4, stats.getWins());
                ps.setInt(5, stats.getLosses());
                ps.setInt(6, stats.getBedsBroken());
                ps.setInt(7, stats.getWinstreak());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("DB save error: " + e.getMessage());
            }
        }, dbPool);
    }

    public PlayerStats getCached(UUID uuid) { return cache.get(uuid); }

    public void shutdown() {
        try { dbPool.shutdown(); dbPool.awaitTermination(3, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
        if (ds != null) ds.close();
    }

    public static final class PlayerStats {
        private final UUID uuid;
        private int kills, deaths, wins, losses, bedsBroken, winstreak;

        public PlayerStats(UUID uuid, int kills, int deaths, int wins, int losses, int bedsBroken, int winstreak) {
            this.uuid = uuid; this.kills=kills; this.deaths=deaths; this.wins=wins; this.losses=losses; this.bedsBroken=bedsBroken; this.winstreak=winstreak;
        }

        public UUID getUuid() { return uuid; }
        public int getKills() { return kills; }
        public void setKills(int kills) { this.kills = kills; }
        public int getDeaths() { return deaths; }
        public void setDeaths(int deaths) { this.deaths = deaths; }
        public int getWins() { return wins; }
        public void setWins(int wins) { this.wins = wins; }
        public int getLosses() { return losses; }
        public void setLosses(int losses) { this.losses = losses; }
        public int getBedsBroken() { return bedsBroken; }
        public void setBedsBroken(int bedsBroken) { this.bedsBroken = bedsBroken; }
        public int getWinstreak() { return winstreak; }
        public void setWinstreak(int winstreak) { this.winstreak = winstreak; }
    }
}
