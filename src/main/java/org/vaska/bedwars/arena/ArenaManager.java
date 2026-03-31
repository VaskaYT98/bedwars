package org.vaska.bedwars.arena;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.vaska.bedwars.Bedwars;
import org.vaska.bedwars.util.WorldResetService;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages arena templates and live instances. Loads arena configs from arenas/ folder.
 */
public class ArenaManager {
    private final Bedwars plugin;
    private final WorldResetService worldResetService;

    private final Map<String, FileConfiguration> templates = new ConcurrentHashMap<>();
    private final Map<String, File> templateFiles = new ConcurrentHashMap<>();
    private final Map<String, Arena> instances = new ConcurrentHashMap<>();
    private final Map<String, java.util.List<Region>> templateRegions = new ConcurrentHashMap<>();
    private final Map<String, java.util.List<Region>> instanceRegions = new ConcurrentHashMap<>();
    private final java.util.Set<org.bukkit.Material> protectedWhitelist = ConcurrentHashMap.newKeySet();

    public ArenaManager(Bedwars plugin, WorldResetService worldResetService) {
        this.plugin = plugin;
        this.worldResetService = worldResetService;
        File arenasDir = new File(plugin.getDataFolder(), "arenas");
        if (!arenasDir.exists()) arenasDir.mkdirs();
    }

    public void loadArenas() {
        File dir = new File(plugin.getDataFolder(), "arenas");
        if (!dir.exists()) dir.mkdirs();
        File[] files = dir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null || files.length == 0) {
            // create an example arena
            try {
                File example = new File(dir, "example_arena.yml");
                if (!example.exists()) plugin.saveResource("arenas/example_arena.yml", false);
            } catch (Exception ignored) {}
            files = dir.listFiles((d, n) -> n.endsWith(".yml"));
        }
        for (File f : Optional.ofNullable(files).orElse(new File[0])) {
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(f);
            String name = f.getName().replaceFirst("\\.yml$", "");
            templates.put(name, cfg);
            templateFiles.put(name, f);
            // load regions if defined
            java.util.List<Region> regions = new java.util.ArrayList<>();
            if (cfg.isConfigurationSection("regions")) {
                for (String key : cfg.getConfigurationSection("regions").getKeys(false)) {
                    org.bukkit.configuration.ConfigurationSection sec = cfg.getConfigurationSection("regions." + key);
                    if (sec == null) continue;
                    String world = sec.getString("world");
                    java.util.List<Integer> min = sec.getIntegerList("min");
                    java.util.List<Integer> max = sec.getIntegerList("max");
                    if (min.size() >= 3 && max.size() >=3) {
                        org.bukkit.World w = plugin.getServer().getWorld(world);
                        if (w == null) continue;
                        org.bukkit.Location a = new org.bukkit.Location(w, min.get(0), min.get(1), min.get(2));
                        org.bukkit.Location b = new org.bukkit.Location(w, max.get(0), max.get(1), max.get(2));
                        regions.add(new Region(a,b));
                    }
                }
            }
            templateRegions.put(name, regions);
            // default whitelist (can be overridden in config)
            protectedWhitelist.add(org.bukkit.Material.BEDROCK);
            for (org.bukkit.Material mat : org.bukkit.Material.values()) {
                if (mat.name().endsWith("_BED")) {
                    protectedWhitelist.add(mat);
                }
            }
            protectedWhitelist.add(org.bukkit.Material.ENDER_CHEST);
            plugin.getLogger().info("Loaded arena template: " + name);
        }
    }

    public boolean createTemplate(String name) {
        try {
            File dir = new File(plugin.getDataFolder(), "arenas");
            if (!dir.exists()) dir.mkdirs();
            File f = new File(dir, name + ".yml");
            if (f.exists()) return false;
            FileConfiguration cfg = new YamlConfiguration();
            cfg.set("name", name);
            cfg.set("world_folder", name);
            cfg.set("max_players", 16);
            cfg.set("teams", 4);
            cfg.save(f);
            templates.put(name, cfg);
            templateFiles.put(name, f);
            return true;
        } catch (Exception ex) {
            plugin.getLogger().severe("Failed to create template: " + ex.getMessage());
            return false;
        }
    }

    /**
     * Return the raw FileConfiguration for a template so editors can modify it.
     */
    public FileConfiguration getTemplateConfig(String name) {
        return templates.get(name);
    }

    public boolean hasTemplate(String name) {
        return templates.containsKey(name);
    }

    public boolean setTemplateRegion(String templateName, String regionKey, org.bukkit.Location a, org.bukkit.Location b) {
        FileConfiguration cfg = templates.get(templateName);
        if (cfg == null) return false;
        String base = "regions." + regionKey;
        cfg.set(base + ".world", a.getWorld().getName());
        cfg.set(base + ".min", java.util.Arrays.asList((int)a.getBlockX(), (int)a.getBlockY(), (int)a.getBlockZ()));
        cfg.set(base + ".max", java.util.Arrays.asList((int)b.getBlockX(), (int)b.getBlockY(), (int)b.getBlockZ()));
        // persist
        File f = templateFiles.get(templateName);
        if (f == null) f = new File(plugin.getDataFolder(), "arenas/" + templateName + ".yml");
        try { cfg.save(f); templateFiles.put(templateName, f); return true; } catch (Exception ex) { plugin.getLogger().severe("Failed to save template region: " + ex.getMessage()); return false; }
    }

    public boolean saveTemplate(String templateName) {
        FileConfiguration cfg = templates.get(templateName);
        if (cfg == null) return false;
        File f = templateFiles.get(templateName);
        if (f == null) f = new File(plugin.getDataFolder(), "arenas/" + templateName + ".yml");
        try { cfg.save(f); templateFiles.put(templateName,f); return true; } catch (Exception ex) { plugin.getLogger().severe("Failed to save template: " + ex.getMessage()); return false; }
    }

    public boolean isLocationProtected(org.bukkit.Location loc) {
        if (loc == null) return false;
        // check all template regions
        for (java.util.List<Region> list : templateRegions.values()) {
            for (Region r : list) if (r.contains(loc)) return true;
        }
        // check instance worlds
        for (Map.Entry<String,Arena> entry : instances.entrySet()) {
            Arena a = entry.getValue();
            java.util.List<Region> regs = instanceRegions.get(entry.getKey());
            if (regs != null) {
                for (Region r : regs) if (r.contains(loc)) return true;
            }
            if (a.getWorld() != null && a.getWorld().equals(loc.getWorld())) return true;
        }
        return false;
    }

    public boolean isWhitelisted(org.bukkit.Material mat) { return protectedWhitelist.contains(mat); }

    public void cloneTemplateRegionsToInstance(String templateName, String instanceId, org.bukkit.World world) {
        java.util.List<Region> regs = templateRegions.get(templateName);
        if (regs == null) return;
        java.util.List<Region> copy = new java.util.ArrayList<>();
        for (Region r : regs) copy.add(r.withWorld(world));
        instanceRegions.put(instanceId, copy);
    }

    public Optional<Arena> findAvailableArena(int neededPlayers) {
        return instances.values().stream()
                .filter(Arena::isAvailable)
                .filter(a -> a.getMaxPlayers() - a.getPlayerCount() >= neededPlayers)
                .min(Comparator.comparingInt(Arena::getPlayerCount));
    }

    public java.util.concurrent.CompletableFuture<Arena> createInstanceAsync(String templateName) {
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            FileConfiguration cfg = templates.get(templateName);
            if (cfg == null) cfg = templates.values().stream().findFirst().orElse(null);
            if (cfg == null) throw new IllegalStateException("No arena templates loaded");

            String id = templateName + "-" + UUID.randomUUID().toString().substring(0, 8);
            int teams = cfg.getInt("teams", 4);
            int playersPerTeam = cfg.getInt("players_per_team", cfg.getInt("max_players", 16) / Math.max(1, teams));
            Arena arena = new Arena(id, templateName, playersPerTeam, teams);
            instances.put(id, arena);

            String templateFolder = cfg.getString("world_folder", templateName);
            try {
                // if a snapshot exists, extract it to templates dir to speed cloning
                try {
                    org.vaska.bedwars.util.WorldCloneOptimizer opt = Bedwars.getInstance().getWorldCloneOptimizer();
                    if (opt != null) {
                        java.io.File templatesDir = new java.io.File(plugin.getDataFolder(), "templates");
                        java.io.File target = new java.io.File(templatesDir, templateFolder);
                        if (!target.exists()) {
                            boolean ok = opt.extractSnapshot(templateFolder, target);
                            if (ok) plugin.getLogger().info("Extracted snapshot for template: " + templateFolder);
                        }
                    }
                } catch (Throwable ignored) {}

                worldResetService.cloneWorldAsync(templateFolder, id).thenAccept(w -> {
                    arena.setWorld(w);
                    // clone regions into this instance bound to new world
                    cloneTemplateRegionsToInstance(templateName, id, w);
                }).exceptionally(ex -> {
                    plugin.getLogger().severe("Failed to clone arena world: " + ex.getMessage());
                    return null;
                });
            } catch (Exception ex) {
                plugin.getLogger().severe("Failed to schedule world clone: " + ex.getMessage());
            }

            return arena;
        });
    }

    public Optional<Arena> getArenaById(String id) { return Optional.ofNullable(instances.get(id)); }

    public Optional<Arena> getArenaForPlayer(UUID player) {
        return instances.values().stream().filter(a -> a.getPlayers().contains(player)).findFirst();
    }

    public void shutdown() {
        // unload worlds and cleanup instances
        instances.values().forEach(a -> {
            World w = a.getWorld();
            if (w != null) Bukkit.unloadWorld(w, false);
        });
        instances.clear();
    }
}
