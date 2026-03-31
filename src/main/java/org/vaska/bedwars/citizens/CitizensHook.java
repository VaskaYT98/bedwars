package org.vaska.bedwars.citizens;

import org.vaska.bedwars.Bedwars;

/**
 * Optional Citizens integration. This class attempts to detect Citizens API and log availability.
 * Full NPC creation/traits should be implemented when Citizens is present on the server.
 */
public class CitizensHook {
    private final Bedwars plugin;

    public CitizensHook(Bedwars plugin) { this.plugin = plugin; }

    public void initialize() {
        try {
            Class<?> citizens = Class.forName("net.citizensnpcs.api.CitizensAPI");
            plugin.getLogger().info("Citizens detected — attempting to create join NPC.");
            // Obtain NPCRegistry: CitizensAPI.getNPCRegistry()
            java.lang.reflect.Method getRegistry = citizens.getMethod("getNPCRegistry");
            Object registry = getRegistry.invoke(null);

            // Create an NPC: registry.createNPC(EntityType.PLAYER, "BedWars Join")
            Class<?> registryClass = registry.getClass();
            java.lang.reflect.Method createNPC = registryClass.getMethod("createNPC", org.bukkit.entity.EntityType.class, String.class);
            Object npc = createNPC.invoke(registry, org.bukkit.entity.EntityType.PLAYER, "BedWars Join");

            // Spawn NPC in lobby if lobby world defined
            String lobby = plugin.getConfig().getString("lobby-world", plugin.getConfig().getString("lobby-world", "lobby"));
            org.bukkit.World w = plugin.getServer().getWorld(lobby);
            if (w != null) {
                org.bukkit.Location spawn = new org.bukkit.Location(w, 0.5, 65, 0.5);
                // npc.spawn(Location) using reflection
                java.lang.reflect.Method spawnMethod = npc.getClass().getMethod("spawn", org.bukkit.Location.class);
                spawnMethod.invoke(npc, spawn);
            }

            // Register a listener to translate player clicks on NPCs into /bw join
            plugin.getServer().getPluginManager().registerEvents(new org.bukkit.event.Listener() {
                @org.bukkit.event.EventHandler
                public void onInteract(org.bukkit.event.player.PlayerInteractAtEntityEvent e) {
                    try {
                        org.bukkit.entity.Entity clicked = e.getRightClicked();
                        java.lang.reflect.Method getNPC = registryClass.getMethod("getNPC", org.bukkit.entity.Entity.class);
                        Object got = getNPC.invoke(registry, clicked);
                        if (got != null) {
                            // player clicked an NPC — run /bw join for them
                            e.getPlayer().performCommand("bw join");
                        }
                    } catch (Exception ignored) {}
                }
            }, plugin);

            plugin.getLogger().info("Citizens NPC created and click handler registered.");
        } catch (ClassNotFoundException ex) {
            plugin.getLogger().info("Citizens not present — skipping NPC features.");
        } catch (Throwable t) {
            plugin.getLogger().severe("Failed to initialize Citizens hook: " + t.getMessage());
        }
    }
}
