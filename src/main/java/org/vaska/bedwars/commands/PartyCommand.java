package org.vaska.bedwars.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.vaska.bedwars.party.PartyManager;
import org.vaska.bedwars.Bedwars;

public class PartyCommand implements CommandExecutor {
    private final PartyManager partyManager;

    public PartyCommand(PartyManager partyManager) { this.partyManager = partyManager; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Players only"); return true; }
        Player p = (Player) sender;
        if (args.length == 0) {
            p.sendMessage("/party invite <player> | accept | leave | disband");
            return true;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "invite":
                if (args.length < 2) { p.sendMessage("Provide player"); return true; }
                Player t = Bukkit.getPlayer(args[1]);
                if (t == null) { p.sendMessage("Player not online"); return true; }
                partyManager.invite(p, t);
                return true;
            case "accept":
                if (args.length < 2) { p.sendMessage("Usage: /party accept <player>"); return true; }
                Player inviter = Bukkit.getPlayer(args[1]);
                if (inviter == null) { p.sendMessage("Player not online"); return true; }
                if (partyManager.accept(p, inviter)) Bedwars.getInstance().getMessages().send(p, "messages.party.joined");
                else Bedwars.getInstance().getMessages().send(p, "messages.party.no_invite");
                return true;
            case "leave":
                partyManager.leave(p);
                Bedwars.getInstance().getMessages().send(p, "messages.party.left");
                return true;
            case "disband":
                partyManager.getParty(p.getUniqueId()).ifPresentOrElse(party -> {
                    if (party.getLeader().equals(p.getUniqueId())) {
                        partyManager.disband(party);
                        Bedwars.getInstance().getMessages().send(p, "messages.party.disbanded");
                    } else Bedwars.getInstance().getMessages().send(p, "messages.party.only_leader");
                }, () -> Bedwars.getInstance().getMessages().send(p, "messages.party.not_in"));
                return true;
            default:
                p.sendMessage("Unknown subcommand");
                return true;
        }
    }
}
