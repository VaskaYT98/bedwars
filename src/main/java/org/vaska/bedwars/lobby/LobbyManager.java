package org.vaska.bedwars.lobby;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.vaska.bedwars.Bedwars;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manages lobby spawn and named portals persisted in the plugin config.
 */
public class LobbyManager {
    private final Bedwars plugin;
    private Location lobbySpawn;
    private final Map<String, Portal> portals = new LinkedHashMap<>();

    public LobbyManager(Bedwars plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        FileConfiguration cfg = plugin.getConfig();
        if (cfg.isSet("lobby.spawn")) {
            ConfigurationSection s = cfg.getConfigurationSection("lobby.spawn");
            if (s != null) {
                String world = s.getString("world");
                double x = s.getDouble("x");
                double y = s.getDouble("y");
                double z = s.getDouble("z");
                float yaw = (float) s.getDouble("yaw");
                float pitch = (float) s.getDouble("pitch");
                if (plugin.getServer().getWorld(world) != null) {
                    lobbySpawn = new Location(plugin.getServer().getWorld(world), x, y, z, yaw, pitch);
                }
            }
        }
        if (cfg.isConfigurationSection("lobby.portals")) {
            ConfigurationSection ps = cfg.getConfigurationSection("lobby.portals");
            for (String key : ps.getKeys(false)) {
                ConfigurationSection p = ps.getConfigurationSection(key);
                if (p == null) continue;
                java.util.Map<String,Object> map = new java.util.HashMap<>();
                for (String k : p.getKeys(false)) map.put(k, p.get(k));
                Portal portal = Portal.deserialize(key, map);
                if (portal != null) portals.put(key, portal);
            }
        }
    }

    public void save() {
        FileConfiguration cfg = plugin.getConfig();
        if (lobbySpawn != null) {
            cfg.set("lobby.spawn.world", lobbySpawn.getWorld().getName());
            cfg.set("lobby.spawn.x", lobbySpawn.getX());
            cfg.set("lobby.spawn.y", lobbySpawn.getY());
            cfg.set("lobby.spawn.z", lobbySpawn.getZ());
            cfg.set("lobby.spawn.yaw", lobbySpawn.getYaw());
            cfg.set("lobby.spawn.pitch", lobbySpawn.getPitch());
        }
        ConfigurationSection ps = cfg.createSection("lobby.portals");
        for (Map.Entry<String, Portal> e : portals.entrySet()) {
            ps.createSection(e.getKey(), e.getValue().serialize());
        }
        plugin.saveConfig();
    }

    public void setLobbySpawn(Location loc) { this.lobbySpawn = loc; }
    public Location getLobbySpawn() { return lobbySpawn; }

    public void addPortal(String name, Portal portal) { portals.put(name, portal); }
    public Portal removePortal(String name) { return portals.remove(name); }
    public Portal getPortal(String name) { return portals.get(name); }
    public Collection<Portal> listPortals() { return Collections.unmodifiableCollection(portals.values()); }
}
