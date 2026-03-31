package org.vaska.bedwars.citizens;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.vaska.bedwars.Bedwars;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages Citizens NPCs via reflection so plugin has no compile-time dependency.
 */
public class CitizensManager {
    private final Bedwars plugin;
    private boolean enabled = false;
    private Object registry;
    private Method getNPCById;
    private Method createNPC;
    private Method getNPC;
    private final File storageFile;
    private final FileConfiguration storage;

    public CitizensManager(Bedwars plugin) {
        this.plugin = plugin;
        this.storageFile = new File(plugin.getDataFolder(), "npcs.yml");
        if (!storageFile.exists()) {
            try { plugin.saveResource("npcs.yml", false); } catch (Exception ignored) {}
        }
        this.storage = YamlConfiguration.loadConfiguration(storageFile);
    }

    public void initialize() {
        try {
            Class<?> citizens = Class.forName("net.citizensnpcs.api.CitizensAPI");
            Method getRegistry = citizens.getMethod("getNPCRegistry");
            registry = getRegistry.invoke(null);
            Class<?> registryClass = registry.getClass();
            // methods
            createNPC = registryClass.getMethod("createNPC", org.bukkit.entity.EntityType.class, String.class);
            getNPCById = registryClass.getMethod("getById", int.class);
            getNPC = registryClass.getMethod("getNPC", org.bukkit.entity.Entity.class);
            enabled = true;
            plugin.getLogger().info("Citizens detected: CitizensManager enabled.");

            // load saved npcs
            if (storage.isConfigurationSection("npcs")) {
                for (String key : storage.getConfigurationSection("npcs").getKeys(false)) {
                    int id = storage.getInt("npcs." + key + ".id", -1);
                    String name = storage.getString("npcs." + key + ".name", key);
                    String world = storage.getString("npcs." + key + ".world", plugin.getServer().getWorlds().get(0).getName());
                    double x = storage.getDouble("npcs." + key + ".x");
                    double y = storage.getDouble("npcs." + key + ".y");
                    double z = storage.getDouble("npcs." + key + ".z");
                    try {
                        Object npc = null;
                        if (id >= 0) {
                            npc = getNPCById.invoke(registry, id);
                        }
                        if (npc != null) {
                            // spawn if possible
                            Method spawn = npc.getClass().getMethod("spawn", org.bukkit.Location.class);
                            org.bukkit.World w = plugin.getServer().getWorld(world);
                            if (w != null) spawn.invoke(npc, new Location(w, x, y, z));
                        }
                    } catch (Exception ex) {
                        plugin.getLogger().warning("Failed to respawn NPC " + name + ": " + ex.getMessage());
                    }
                }
            }

            // register click listener (handled in registry when interacting with entity)
            plugin.getServer().getPluginManager().registerEvents(new org.bukkit.event.Listener() {
                @org.bukkit.event.EventHandler
                public void onInteract(org.bukkit.event.player.PlayerInteractAtEntityEvent e) {
                    try {
                        Object got = getNPC.invoke(registry, e.getRightClicked());
                        if (got != null) {
                            // find npc id
                            Method getId = got.getClass().getMethod("getId");
                            int nid = (Integer) getId.invoke(got);
                            // look up in storage
                            if (storage.isConfigurationSection("npcs")) {
                                for (String key : storage.getConfigurationSection("npcs").getKeys(false)) {
                                    int sid = storage.getInt("npcs." + key + ".id", -1);
                                    if (sid == nid) {
                                        String template = storage.getString("npcs." + key + ".template", "example_arena");
                                        // perform join for player (could open UI instead)
                                        e.getPlayer().performCommand("bw join");
                                        return;
                                    }
                                }
                            }
                        }
                    } catch (Throwable ignored) {}
                }
            }, plugin);

        } catch (ClassNotFoundException ex) {
            plugin.getLogger().info("Citizens not present — skipping NPC features.");
        } catch (Throwable t) {
            plugin.getLogger().severe("Failed to initialize CitizensManager: " + t.getMessage());
        }
    }

    public boolean isEnabled() { return enabled; }

    public int createNPC(String templateName, String npcName, Location loc) {
        if (!enabled) return -1;
        try {
            Object npc = createNPC.invoke(registry, org.bukkit.entity.EntityType.PLAYER, npcName);
            Method spawn = npc.getClass().getMethod("spawn", org.bukkit.Location.class);
            spawn.invoke(npc, loc);
            Method getId = npc.getClass().getMethod("getId");
            int id = (Integer) getId.invoke(npc);
            // persist
            String key = "npc_" + id;
            storage.set("npcs." + key + ".id", id);
            storage.set("npcs." + key + ".name", npcName);
            storage.set("npcs." + key + ".template", templateName);
            storage.set("npcs." + key + ".world", loc.getWorld().getName());
            storage.set("npcs." + key + ".x", loc.getX());
            storage.set("npcs." + key + ".y", loc.getY());
            storage.set("npcs." + key + ".z", loc.getZ());
            storage.save(storageFile);
            return id;
        } catch (Throwable t) {
            plugin.getLogger().severe("Failed to create NPC: " + t.getMessage());
            return -1;
        }
    }

    public boolean removeNPC(int id) {
        if (!enabled) return false;
        try {
            Object npc = getNPCById.invoke(registry, id);
            if (npc != null) {
                Method destroy = npc.getClass().getMethod("destroy");
                destroy.invoke(npc);
            }
        } catch (Throwable ignored) {}
        // remove from storage
        if (storage.isConfigurationSection("npcs")) {
            for (String key : storage.getConfigurationSection("npcs").getKeys(false)) {
                int sid = storage.getInt("npcs." + key + ".id", -1);
                if (sid == id) {
                    storage.set("npcs." + key, null);
                    try { storage.save(storageFile); } catch (Exception ignored) {}
                    return true;
                }
            }
        }
        return false;
    }

    public Map<Integer, String> listNPCs() {
        Map<Integer, String> out = new HashMap<>();
        if (storage.isConfigurationSection("npcs")) {
            for (String key : storage.getConfigurationSection("npcs").getKeys(false)) {
                int sid = storage.getInt("npcs." + key + ".id", -1);
                String name = storage.getString("npcs." + key + ".name", key);
                out.put(sid, name);
            }
        }
        return out;
    }

}
