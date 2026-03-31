package org.vaska.bedwars.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.vaska.bedwars.matchmaking.MatchmakingService;
import org.vaska.bedwars.party.PartyManager;

public class JoinCommand implements CommandExecutor {
    private final MatchmakingService matchmaking;
    private final PartyManager partyManager;

    public JoinCommand(MatchmakingService matchmaking, PartyManager partyManager) {
        this.matchmaking = matchmaking; this.partyManager = partyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Players only"); return true; }
        Player p = (Player) sender;
        matchmaking.join(p);
        return true;
    }
}
