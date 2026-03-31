package org.vaska.bedwars.lobby;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;

public class Portal {
    private final String name;
    private final Location location;
    private final String target; // arbitrary target identifier (e.g., "hub", "arena:template")

    public Portal(String name, Location location, String target) {
        this.name = name;
        this.location = location;
        this.target = target;
    }

    public String getName() { return name; }
    public Location getLocation() { return location; }
    public String getTarget() { return target; }

    public Map<String, Object> serialize() {
        Map<String,Object> m = new HashMap<>();
        m.put("world", location.getWorld().getName());
        m.put("x", location.getX());
        m.put("y", location.getY());
        m.put("z", location.getZ());
        m.put("yaw", location.getYaw());
        m.put("pitch", location.getPitch());
        m.put("target", target == null ? "" : target);
        return m;
    }

    public static Portal deserialize(String name, Map<?,?> map) {
        try {
            String world = (String) map.get("world");
            double x = ((Number) map.get("x")).doubleValue();
            double y = ((Number) map.get("y")).doubleValue();
            double z = ((Number) map.get("z")).doubleValue();
            float yaw = ((Number) map.get("yaw")).floatValue();
            float pitch = ((Number) map.get("pitch")).floatValue();
            String target = map.get("target") == null ? "" : String.valueOf(map.get("target"));
            Location loc = new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
            return new Portal(name, loc, target);
        } catch (Throwable t) {
            return null;
        }
    }
}
