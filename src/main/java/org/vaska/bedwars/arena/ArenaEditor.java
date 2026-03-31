package org.vaska.bedwars.arena;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.vaska.bedwars.Bedwars;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Interactive arena editor for admins. Stores temporary edit sessions per-player.
 */
public class ArenaEditor {
    private final Bedwars plugin;
    private final ArenaManager arenaManager;
    private final Map<UUID, EditSession> sessions = new HashMap<>();
    private ArenaEditorGUI gui;

    public ArenaEditor(Bedwars plugin, ArenaManager arenaManager) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
    }

    public boolean startSession(Player admin, String template) {
        if (!admin.hasPermission("bedwars.admin")) return false;
        if (!arenaManager.hasTemplate(template)) return false;
        EditSession s = new EditSession(template);
        // populate default teams based on template teams count (if specified)
        org.bukkit.configuration.file.FileConfiguration cfg = arenaManager.getTemplateConfig(template);
        int teams = 4;
        if (cfg != null) teams = cfg.getInt("teams", teams);
        // friendly color names
        String[] defaults = new String[]{"Red","Blue","Green","Yellow","Aqua","Purple","Orange","White","Black","Brown","Pink","Lime"};
        for (int i = 0; i < teams; i++) {
            String nameKey = i < defaults.length ? defaults[i] : "team" + (i+1);
            s.teams.put(nameKey, new TeamData());
        }
        sessions.put(admin.getUniqueId(), s);
        admin.sendMessage("[BW] Editing template: " + template + ". Opening GUI editor...");
        // open GUI (single instance)
        if (gui == null) gui = new ArenaEditorGUI(plugin, this);
        gui.open(admin);
        return true;
    }

    public String getActiveTemplate(org.bukkit.entity.Player p) {
        EditSession s = sessions.get(p.getUniqueId());
        return s == null ? null : s.templateName;
    }

    public java.util.Map<String, Boolean[]> getTeamsStatus(org.bukkit.entity.Player p) {
        EditSession s = sessions.get(p.getUniqueId());
        java.util.Map<String, Boolean[]> out = new java.util.LinkedHashMap<>();
        if (s == null) return out;
        for (java.util.Map.Entry<String, TeamData> e : s.teams.entrySet()) {
            Boolean[] b = new Boolean[2]; b[0] = e.getValue().spawn != null; b[1] = e.getValue().bed != null;
            out.put(e.getKey(), b);
        }
        return out;
    }

    public java.util.List<String> validateSessionFor(org.bukkit.entity.Player p) {
        EditSession s = sessions.get(p.getUniqueId());
        java.util.List<String> missing = new java.util.ArrayList<>();
        if (s == null) { missing.add("No active session"); return missing; }
        for (java.util.Map.Entry<String, TeamData> e : s.teams.entrySet()) {
            String team = e.getKey();
            TeamData td = e.getValue();
            if (td.spawn == null) missing.add(team + " missing spawn");
            if (td.bed == null) missing.add(team + " missing bed");
        }
        if (s.pos1 == null || s.pos2 == null) missing.add("play_area region (pos1/pos2) not set");
        return missing;
    }

    public boolean stopSessionSave(Player admin) {
        EditSession s = sessions.remove(admin.getUniqueId());
        if (s == null) return false;
        // apply changes to config
        org.bukkit.configuration.file.FileConfiguration cfg = arenaManager.getTemplateConfig(s.templateName);
        if (cfg == null) return false;
        // teams
        for (Map.Entry<String, TeamData> te : s.teams.entrySet()) {
            String base = "teamsData." + te.getKey();
            TeamData td = te.getValue();
            if (td.spawn != null) {
                cfg.set(base + ".spawn.world", td.spawn.getWorld().getName());
                cfg.set(base + ".spawn.x", td.spawn.getX());
                cfg.set(base + ".spawn.y", td.spawn.getY());
                cfg.set(base + ".spawn.z", td.spawn.getZ());
                cfg.set(base + ".spawn.yaw", td.spawn.getYaw());
                cfg.set(base + ".spawn.pitch", td.spawn.getPitch());
            }
            if (td.bed != null) {
                cfg.set(base + ".bed.world", td.bed.getWorld().getName());
                cfg.set(base + ".bed.x", td.bed.getX());
                cfg.set(base + ".bed.y", td.bed.getY());
                cfg.set(base + ".bed.z", td.bed.getZ());
                cfg.set(base + ".bed.yaw", td.bed.getYaw());
                cfg.set(base + ".bed.pitch", td.bed.getPitch());
            }
        }
        // region: if selection set
        if (s.pos1 != null && s.pos2 != null) {
            arenaManager.setTemplateRegion(s.templateName, "play_area", s.pos1, s.pos2);
        }
        boolean ok = arenaManager.saveTemplate(s.templateName);
        admin.sendMessage(ok ? "[BW] Template saved." : "[BW] Failed to save template.");
        return ok;
    }

    public boolean stopSessionCancel(Player admin) {
        EditSession s = sessions.remove(admin.getUniqueId());
        if (s == null) return false;
        admin.sendMessage("[BW] Edit cancelled for template: " + s.templateName);
        return true;
    }

    public boolean addTeam(Player admin, String teamName) {
        EditSession s = sessions.get(admin.getUniqueId());
        if (s == null) { admin.sendMessage("No active edit session. Use /bw arena edit <template>"); return false; }
        s.teams.put(teamName, new TeamData());
        admin.sendMessage("Team '" + teamName + "' added to session.");
        return true;
    }

    public boolean setTeamSpawn(Player admin, String teamName) {
        EditSession s = sessions.get(admin.getUniqueId());
        if (s == null) { admin.sendMessage("No active edit session."); return false; }
        TeamData td = s.teams.get(teamName);
        if (td == null) { admin.sendMessage("Team not found in session. Use /bw arena addteam <name>"); return false; }
        td.spawn = admin.getLocation();
        admin.sendMessage("Team spawn set for '" + teamName + "'.");
        return true;
    }

    public boolean setTeamBed(Player admin, String teamName) {
        EditSession s = sessions.get(admin.getUniqueId());
        if (s == null) { admin.sendMessage("No active edit session."); return false; }
        TeamData td = s.teams.get(teamName);
        if (td == null) { admin.sendMessage("Team not found in session. Use /bw arena addteam <name>"); return false; }
        td.bed = admin.getLocation();
        admin.sendMessage("Team bed set for '" + teamName + "'.");
        return true;
    }

    public boolean setPos1(Player admin) {
        EditSession s = sessions.get(admin.getUniqueId());
        if (s == null) { admin.sendMessage("No active edit session."); return false; }
        s.pos1 = admin.getLocation();
        admin.sendMessage("Position 1 set.");
        return true;
    }

    public boolean setPos2(Player admin) {
        EditSession s = sessions.get(admin.getUniqueId());
        if (s == null) { admin.sendMessage("No active edit session."); return false; }
        s.pos2 = admin.getLocation();
        admin.sendMessage("Position 2 set.");
        return true;
    }

    // inner classes
    private static class EditSession {
        final String templateName;
        Location pos1;
        Location pos2;
        final Map<String, TeamData> teams = new HashMap<>();
        EditSession(String templateName) { this.templateName = templateName; }
    }

    private static class TeamData {
        Location spawn;
        Location bed;
    }
}
