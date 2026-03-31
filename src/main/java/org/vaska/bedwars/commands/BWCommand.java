package org.vaska.bedwars.commands;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.vaska.bedwars.Bedwars;
import org.bukkit.plugin.java.JavaPlugin;
import org.vaska.bedwars.matchmaking.MatchmakingService;
import org.vaska.bedwars.party.PartyManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Main /bw command dispatcher. Handles subcommands: join, arena create/setregion/save, etc.
 */
public class BWCommand implements CommandExecutor {
    private final org.vaska.bedwars.matchmaking.MatchmakingService matchmaking;
    private final org.vaska.bedwars.party.PartyManager partyManager;
    private final Map<java.util.UUID, Location[]> selections = new HashMap<>();
    private final Bedwars plugin;


    public BWCommand(Bedwars plugin,
                     MatchmakingService matchmaking,
                     PartyManager partyManager) {
        this.plugin = plugin;
        this.matchmaking = matchmaking;
        this.partyManager = partyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player) { ((Player)sender).performCommand("bw join"); return true; }
            sender.sendMessage("Use /bw join or /bw arena ..."); return true;
        }
        String sub = args[0].toLowerCase();
        if (sub.equals("join")) {
            if (!(sender instanceof Player)) { sender.sendMessage("Players only"); return true; }
            matchmaking.join((Player) sender);
            return true;
        }

        if (sub.equals("arena")) {
            if (!(sender instanceof Player)) { sender.sendMessage("Players only"); return true; }
            Player p = (Player) sender;
            if (!p.hasPermission("bedwars.admin")) { p.sendMessage("No permission"); return true; }
            if (args.length < 2) { p.sendMessage("Usage: /bw arena create|setregion|save"); return true; }
            String cmd = args[1].toLowerCase();
            switch (cmd) {
                case "create":
                    if (args.length < 3) { p.sendMessage("Usage: /bw arena create <name>"); return true; }
                    String name = args[2];
                    boolean ok = Bedwars.getInstance().getArenaManager().createTemplate(name);
                    p.sendMessage(ok ? "Template created: " + name : "Failed to create template (exists?)");
                    return true;
                case "setregion":
                    if (args.length < 3) { p.sendMessage("Usage: /bw arena setregion <key> (use pos1/pos2 to set)"); return true; }
                    String key = args[2];
                    // if player is sneaking and uses hand to set pos1/pos2, but for simplicity use selection stored via /bw arena pos1/pos2
                    p.sendMessage("Use /bw arena pos1 or /bw arena pos2 to store positions, then /bw arena setregion " + key + " <templateName>");
                    return true;
                case "pos1":
                    if (Bedwars.getInstance().getArenaEditor().getActiveTemplate(p) != null) {
                        Bedwars.getInstance().getArenaEditor().setPos1(p);
                        p.sendMessage("Position 1 set for editor session");
                    } else {
                        selections.put(p.getUniqueId(), new Location[]{p.getLocation(), null});
                        p.sendMessage("Position 1 stored");
                    }
                    return true;
                case "pos2":
                    if (Bedwars.getInstance().getArenaEditor().getActiveTemplate(p) != null) {
                        Bedwars.getInstance().getArenaEditor().setPos2(p);
                        p.sendMessage("Position 2 set for editor session");
                    } else {
                        selections.computeIfAbsent(p.getUniqueId(), k -> new Location[2])[1] = p.getLocation();
                        p.sendMessage("Position 2 stored");
                    }
                    return true;
                case "save":
                    if (args.length < 3) { p.sendMessage("Usage: /bw arena save <templateName>"); return true; }
                    String tpl = args[2];
                    Location[] sel = selections.get(p.getUniqueId());
                    if (sel == null || sel[0] == null || sel[1] == null) { p.sendMessage("Select pos1 and pos2 first"); return true; }
                    p.sendMessage("Saving region to template " + tpl);
                    boolean res = Bedwars.getInstance().getArenaManager().setTemplateRegion(tpl, "play_area", sel[0], sel[1]);
                    p.sendMessage(res ? "Saved" : "Failed to save");
                    return true;
                case "edit":
                    if (args.length < 3) { p.sendMessage("Usage: /bw arena edit <template>"); return true; }
                    String tname = args[2];
                    boolean started = Bedwars.getInstance().getArenaEditor().startSession(p, tname);
                    p.sendMessage(started ? "Entered edit session for " + tname : "Failed to start edit session (template exists?)");
                    if (started) Bedwars.getInstance().getArenaEditor();
                    return true;
                case "addteam":
                    if (args.length < 3) { p.sendMessage("Usage: /bw arena addteam <name>"); return true; }
                    String team = args[2];
                    Bedwars.getInstance().getArenaEditor().addTeam(p, team);
                    return true;
                case "teamsetspawn":
                    if (args.length < 3) { p.sendMessage("Usage: /bw arena teamsetspawn <team>"); return true; }
                    String tset = args[2];
                    Bedwars.getInstance().getArenaEditor().setTeamSpawn(p, tset);
                    return true;
                case "teamsetbed":
                    if (args.length < 3) { p.sendMessage("Usage: /bw arena teamsetbed <team>"); return true; }
                    String tbed = args[2];
                    Bedwars.getInstance().getArenaEditor().setTeamBed(p, tbed);
                    return true;
                case "editsave":
                    Bedwars.getInstance().getArenaEditor().stopSessionSave(p); return true;
                case "editcancel":
                    Bedwars.getInstance().getArenaEditor().stopSessionCancel(p); return true;
                default:
                    p.sendMessage("Unknown arena subcommand");
                    return true;
            }
        }

        if (sub.equals("purchases")) {
            if (!sender.hasPermission("bedwars.admin")) { sender.sendMessage("No permission"); return true; }
            java.io.File f = new java.io.File(Bedwars.getInstance().getDataFolder(), "purchases.yml");
            if (!f.exists()) { sender.sendMessage("No purchase records found."); return true; }
            org.bukkit.configuration.file.FileConfiguration cfg = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(f);
            if (args.length >= 2) {
                // show for specific player name
                String name = args[1];
                org.bukkit.OfflinePlayer off = org.bukkit.Bukkit.getOfflinePlayer(name);
                if (off == null) { sender.sendMessage("Player not found"); return true; }
                java.util.UUID uid = off.getUniqueId();
                int count = cfg.getInt(uid.toString() + ".count", 0);
                sender.sendMessage("Purchases for " + name + ": " + count);
                return true;
            }
            // list top entries (up to 20)
            java.util.Set<String> keys = cfg.getKeys(false);
            int shown = 0;
            for (String key : keys) {
                if (shown++ > 20) break;
                int c = cfg.getInt(key + ".count", 0);
                sender.sendMessage(key + " -> " + c);
            }
            if (shown == 0) sender.sendMessage("No purchases recorded yet.");
            return true;
        }

        if (sub.equals("npc")) {
            if (!(sender instanceof Player)) { sender.sendMessage("Players only"); return true; }
            Player p = (Player) sender;
            if (!p.hasPermission("bedwars.admin")) { p.sendMessage("No permission"); return true; }
            if (args.length < 2) { p.sendMessage("Usage: /bw npc list|create|remove <args>"); return true; }
            String cmd = args[1].toLowerCase();
            org.vaska.bedwars.npc.NPCManager nm = Bedwars.getInstance().getNpcManager();
            if (nm == null || !nm.isEnabled()) { p.sendMessage("NPC system not available"); return true; }
            switch (cmd) {
                case "pos1":
                    if (Bedwars.getInstance().getArenaEditor().getActiveTemplate(p) != null) {
                        Bedwars.getInstance().getArenaEditor().setPos1(p);
                        p.sendMessage("Position 1 set for editor session");
                    } else {
                        selections.put(p.getUniqueId(), new Location[]{p.getLocation(), null});
                        p.sendMessage("Position 1 stored");
                    }
                    return true;

                case "pos2":
                    if (Bedwars.getInstance().getArenaEditor().getActiveTemplate(p) != null) {
                        Bedwars.getInstance().getArenaEditor().setPos2(p);
                        p.sendMessage("Position 2 set for editor session");
                    } else {
                        selections.computeIfAbsent(p.getUniqueId(), k -> new Location[2])[1] = p.getLocation();
                        p.sendMessage("Position 2 stored");
                    }
                    return true;
                case "list":
                    java.util.Map<Integer, String> list = nm.listNPCs();
                    if (list.isEmpty()) { p.sendMessage("No NPCs registered"); return true; }
                    list.forEach((id, name) -> p.sendMessage(id + " -> " + name));
                    return true;
                case "create":
                    if (args.length < 3) { p.sendMessage("Usage: /bw npc create <templateName> [displayName]"); return true; }
                    String tpl = args[2];
                    String disp = args.length >= 4 ? args[3] : "BW_NPC";
                    int nid = nm.createNPC(tpl, disp, p.getLocation());
                    p.sendMessage(nid > 0 ? "NPC created id=" + nid : "Failed to create NPC");
                    return true;
                case "remove":
                    if (args.length < 3) { p.sendMessage("Usage: /bw npc remove <id>"); return true; }
                    try {
                        int id = Integer.parseInt(args[2]);
                        boolean ok = nm.removeNPC(id);
                        p.sendMessage(ok ? "NPC removed" : "Failed to remove NPC");
                    } catch (NumberFormatException ex) { p.sendMessage("Invalid id"); }
                    return true;
                default:
                    p.sendMessage("Unknown npc subcommand"); return true;
            }
        }

        if (sub.equals("generator")) {
            if (!sender.hasPermission("bedwars.admin")) { sender.sendMessage("No permission"); return true; }
            if (args.length < 2) { sender.sendMessage("Usage: /bw generator setrate <type> <seconds>"); return true; }
            String cmd2 = args[1].toLowerCase();
            if (cmd2.equals("setrate")) {
                if (args.length < 4) { sender.sendMessage("Usage: /bw generator setrate <type> <seconds>"); return true; }
                String type = args[2];
                int sec;
                try { sec = Integer.parseInt(args[3]); } catch (NumberFormatException ex) { sender.sendMessage("Invalid seconds"); return true; }
                plugin.getGeneratorManager().setRate(type, sec);
                sender.sendMessage("Set generator rate for " + type + " to " + sec + "s");
                return true;
            }
        }

        if (sub.equals("npc") && args.length >= 2 && args[1].equalsIgnoreCase("gui")) {
            if (!(sender instanceof Player)) { sender.sendMessage("Players only"); return true; }
            Player p = (Player) sender;
            if (!p.hasPermission("bedwars.admin")) { p.sendMessage("No permission"); return true; }
            org.vaska.bedwars.citizens.CitizensManager cmg = Bedwars.getInstance().getCitizensManager();
            if (cmg == null || !cmg.isEnabled()) { p.sendMessage("Citizens not available"); return true; }
            new org.vaska.bedwars.citizens.NPCAdminGUI(cmg).open(p);
            return true;
        }

        if (sub.equals("lobby")) {
            if (!(sender instanceof Player)) { sender.sendMessage("Players only"); return true; }
            Player p = (Player) sender;
            if (!p.hasPermission("bedwars.admin")) { p.sendMessage("No permission"); return true; }
            if (args.length < 2) { p.sendMessage("Usage: /bw lobby setspawn|tp"); return true; }
            String cmd2 = args[1].toLowerCase();
            org.vaska.bedwars.lobby.LobbyManager lm = Bedwars.getInstance().getLobbyManager();
            if (lm == null) { p.sendMessage("Lobby manager not available"); return true; }
            switch (cmd2) {
                case "setspawn":
                    lm.setLobbySpawn(p.getLocation());
                    lm.save();
                    p.sendMessage("Lobby spawn set.");
                    return true;
                case "tp":
                    if (lm.getLobbySpawn() == null) { p.sendMessage("Lobby spawn not set."); return true; }
                    p.teleport(lm.getLobbySpawn());
                    p.sendMessage("Teleported to lobby spawn.");
                    return true;
                default:
                    p.sendMessage("Unknown lobby subcommand");
                    return true;
            }
        }

        if (sub.equals("portal")) {
            if (!(sender instanceof Player)) { sender.sendMessage("Players only"); return true; }
            Player p = (Player) sender;
            if (!p.hasPermission("bedwars.admin")) { p.sendMessage("No permission"); return true; }
            if (args.length < 2) { p.sendMessage("Usage: /bw portal set <name> [target] | list | remove <name>"); return true; }
            String cmd2 = args[1].toLowerCase();
            org.vaska.bedwars.lobby.LobbyManager lm = Bedwars.getInstance().getLobbyManager();
            if (lm == null) { p.sendMessage("Lobby manager not available"); return true; }
            switch (cmd2) {
                case "set":
                    if (args.length < 3) { p.sendMessage("Usage: /bw portal set <name> [target]"); return true; }
                    String name = args[2];
                    String target = args.length >= 4 ? args[3] : "";
                    lm.addPortal(name, new org.vaska.bedwars.lobby.Portal(name, p.getLocation(), target));
                    lm.save();
                    p.sendMessage("Portal '" + name + "' set.");
                    return true;
                case "list":
                    java.util.Collection<org.vaska.bedwars.lobby.Portal> list = lm.listPortals();
                    if (list.isEmpty()) { p.sendMessage("No portals set."); return true; }
                    for (org.vaska.bedwars.lobby.Portal pr : list) {
                        p.sendMessage(pr.getName() + " -> " + pr.getTarget() + " @ " + pr.getLocation().getWorld().getName() + "(" + pr.getLocation().getBlockX() + "," + pr.getLocation().getBlockY() + "," + pr.getLocation().getBlockZ() + ")");
                    }
                    return true;
                case "remove":
                    if (args.length < 3) { p.sendMessage("Usage: /bw portal remove <name>"); return true; }
                    String rem = args[2];
                    org.vaska.bedwars.lobby.Portal removed = lm.removePortal(rem);
                    if (removed == null) p.sendMessage("No such portal"); else { lm.save(); p.sendMessage("Portal removed"); }
                    return true;
                default:
                    p.sendMessage("Unknown portal subcommand"); return true;
            }
        }

        sender.sendMessage("Unknown subcommand");
        return true;
    }
}
