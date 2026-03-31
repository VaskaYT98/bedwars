package org.vaska.bedwars.arena;

import org.bukkit.Location;

public class Region {
    private final Location min;
    private final Location max;

    public Region(Location a, Location b) {
        double minX = Math.min(a.getX(), b.getX());
        double minY = Math.min(a.getY(), b.getY());
        double minZ = Math.min(a.getZ(), b.getZ());
        double maxX = Math.max(a.getX(), b.getX());
        double maxY = Math.max(a.getY(), b.getY());
        double maxZ = Math.max(a.getZ(), b.getZ());
        this.min = new Location(a.getWorld(), minX, minY, minZ);
        this.max = new Location(a.getWorld(), maxX, maxY, maxZ);
    }

    public boolean contains(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        if (!loc.getWorld().equals(min.getWorld())) return false;
        return loc.getX() >= min.getX() && loc.getX() <= max.getX()
                && loc.getY() >= min.getY() && loc.getY() <= max.getY()
                && loc.getZ() >= min.getZ() && loc.getZ() <= max.getZ();
    }

    public Location getMin() { return min; }
    public Location getMax() { return max; }

    /** Create a copy of this region but bound to a different world (keeps coordinates). */
    public Region withWorld(org.bukkit.World world) {
        Location a = new Location(world, min.getX(), min.getY(), min.getZ());
        Location b = new Location(world, max.getX(), max.getY(), max.getZ());
        return new Region(a, b);
    }
}
