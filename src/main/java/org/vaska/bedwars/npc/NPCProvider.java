package org.vaska.bedwars.npc;

import org.bukkit.Location;

public interface NPCProvider {
    void initialize();
    boolean isAvailable();
    int createNPC(String templateName, String npcName, Location loc);
    boolean removeNPC(int id);
    java.util.Map<Integer,String> listNPCs();
}
