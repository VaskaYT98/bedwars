package org.vaska.bedwars.generator;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;
import org.vaska.bedwars.Bedwars;

import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight generator manager. In production this would use more robust scheduling and region handling.
 */
public class GeneratorManager {
    private final Bedwars plugin;
    private final List<Block> generators = new ArrayList<>();
    private final java.util.Map<Block, String> typeMap = new java.util.WeakHashMap<>();
    private final java.util.Map<String, Integer> rates = new java.util.HashMap<>(); // ticks

    public GeneratorManager(Bedwars plugin) { this.plugin = plugin; }

    public void registerGenerator(Block block, String type) {
        // store block and schedule if needed
        generators.add(block);
        typeMap.put(block, type);
    }

    public void start() {
        // schedule per-type tasks using configured rates
        // load default rates from config if present
        try {
            java.io.File f = new java.io.File(plugin.getDataFolder(), "generators.yml");
            org.bukkit.configuration.file.FileConfiguration cfg = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(f);
            if (cfg.isConfigurationSection("rates")) {
                for (String key : cfg.getConfigurationSection("rates").getKeys(false)) {
                    int sec = cfg.getInt("rates." + key, 1);
                    rates.put(key, Math.max(1, sec * 20));
                }
            }
        } catch (Throwable ignored) {}

        // for each distinct type schedule a task
        java.util.Set<String> types = new java.util.HashSet<>(typeMap.values());
        if (types.isEmpty()) types.add("default");
        for (String t : types) {
            int interval = rates.getOrDefault(t, 20); // default 1s
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (Block b : new ArrayList<>(generators)) {
                        if (!t.equals(typeMap.get(b))) continue;
                        if (b == null || b.getLocation().getWorld() == null) continue;
                        org.bukkit.inventory.ItemStack drop;
                        if (t.equalsIgnoreCase("iron")) drop = new org.bukkit.inventory.ItemStack(Material.IRON_INGOT);
                        else drop = new org.bukkit.inventory.ItemStack(Material.GOLD_INGOT);
                        b.getWorld().dropItemNaturally(b.getLocation().add(0.5,1,0.5), drop);
                    }
                }
            }.runTaskTimer(plugin, interval, interval);
        }
    }

    public void setRate(String type, int seconds) {
        rates.put(type, Math.max(1, seconds * 20));
        // persist
        try {
            java.io.File f = new java.io.File(plugin.getDataFolder(), "generators.yml");
            org.bukkit.configuration.file.FileConfiguration cfg = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(f);
            cfg.set("rates." + type, seconds);
            cfg.save(f);
        } catch (Throwable ignored) {}
    }
}
