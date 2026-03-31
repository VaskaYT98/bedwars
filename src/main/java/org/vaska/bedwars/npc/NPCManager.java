package org.vaska.bedwars.npc;

import org.vaska.bedwars.Bedwars;
import org.vaska.bedwars.citizens.CitizensManager;

/**
 * High-level NPC manager that selects a provider based on config.
 */
public class NPCManager {
    private final Bedwars plugin;
    private NPCProvider provider;

    public NPCManager(Bedwars plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        String choice = plugin.getConfig().getString("npcs.provider", "auto").toLowerCase();
        // auto detection prefers Citizens
        if (choice.equals("auto") || choice.equals("citizens")) {
            CitizensManager cm = new CitizensManager(plugin);
            cm.initialize();
            if (cm.isEnabled()) {
                provider = new CitizensProvider(cm);
            } else if (!choice.equals("citizens")) {
                provider = new SimpleNPCProvider(plugin);
            }
        } else if (choice.equals("builtin") || choice.equals("simple")) {
            provider = new SimpleNPCProvider(plugin);
        } else {
            // attempt to detect plugin presence
            if (plugin.getServer().getPluginManager().getPlugin(choice) != null) {
                // not implemented adapters for other plugins yet — fallback
                plugin.getLogger().info("Detected NPC plugin '" + choice + "' but no adapter implemented; using builtin fallback.");
                provider = new SimpleNPCProvider(plugin);
            } else {
                plugin.getLogger().info("NPC provider '" + choice + "' not found; using builtin fallback.");
                provider = new SimpleNPCProvider(plugin);
            }
        }
        if (provider != null) provider.initialize();
    }

    public boolean isEnabled() { return provider != null && provider.isAvailable(); }

    public int createNPC(String template, String name, org.bukkit.Location loc) { return provider == null ? -1 : provider.createNPC(template, name, loc); }

    public boolean removeNPC(int id) { return provider != null && provider.removeNPC(id); }

    public java.util.Map<Integer,String> listNPCs() { return provider == null ? java.util.Collections.emptyMap() : provider.listNPCs(); }
}
