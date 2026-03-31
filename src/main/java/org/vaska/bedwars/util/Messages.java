package org.vaska.bedwars.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

/**
 * Simple messages loader that reads templates from config.yml and applies replacements.
 */
public class Messages {
    private final JavaPlugin plugin;

    public Messages(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public String getRaw(String path) {
        String v = plugin.getConfig().getString(path);
        return v == null ? "" : v;
    }

    public String get(String path) {
        return translate(getRaw(path));
    }

    public String format(String path, Map<String, String> vars) {
        String s = getRaw(path);
        if (s == null) s = "";
        if (vars != null) {
            for (Map.Entry<String, String> e : vars.entrySet()) {
                s = s.replace("{" + e.getKey() + "}", e.getValue() == null ? "" : e.getValue());
            }
        }
        return translate(s);
    }

    public void send(CommandSender to, String path) {
        to.sendMessage(get(path));
    }

    public void send(CommandSender to, String path, Map<String, String> vars) {
        to.sendMessage(format(path, vars));
    }

    private String translate(String in) {
        return ChatColor.translateAlternateColorCodes('&', in == null ? "" : in);
    }
}
