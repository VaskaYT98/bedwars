package org.vaska.bedwars.npc;

import org.bukkit.Location;
import org.vaska.bedwars.citizens.CitizensManager;

import java.util.Map;

public class CitizensProvider implements NPCProvider {
    private final CitizensManager cm;

    public CitizensProvider(CitizensManager cm) { this.cm = cm; }

    @Override
    public void initialize() {
        // CitizensManager already initialized by caller
    }

    @Override
    public boolean isAvailable() { return cm != null && cm.isEnabled(); }

    @Override
    public int createNPC(String templateName, String npcName, Location loc) {
        return cm.createNPC(templateName, npcName, loc);
    }

    @Override
    public boolean removeNPC(int id) { return cm.removeNPC(id); }

    @Override
    public Map<Integer, String> listNPCs() { return cm.listNPCs(); }
}
