package org.vaska.bedwars;

import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.vaska.bedwars.anti.AntiCheatListener;
import org.vaska.bedwars.anti.DupeProtectionListener;
import org.vaska.bedwars.citizens.CitizensManager;
import org.vaska.bedwars.arena.ArenaManager;
import org.vaska.bedwars.commands.JoinCommand;
import org.vaska.bedwars.commands.PartyCommand;
import org.vaska.bedwars.db.PlayerDataManager;
import org.vaska.bedwars.generator.GeneratorManager;
import org.vaska.bedwars.matchmaking.MatchmakingService;
import org.vaska.bedwars.party.PartyManager;
import org.vaska.bedwars.spectator.SpectatorManager;
import org.vaska.bedwars.util.WorldResetService;
import org.vaska.bedwars.commands.BWCommand;

public final class Bedwars extends JavaPlugin {

    private static Bedwars instance;

    private ArenaManager arenaManager;
    private PlayerDataManager playerDataManager;
    private PartyManager partyManager;
    private MatchmakingService matchmakingService;
    private WorldResetService worldResetService;
    private org.vaska.bedwars.generator.GeneratorManager generatorManager;
    private SpectatorManager spectatorManager;
    private org.vaska.bedwars.ui.ScoreboardManager scoreboardManager;
    private org.vaska.bedwars.ui.TabManager tabManager;
    private org.vaska.bedwars.ui.TabAdapter tabAdapter;
    private org.vaska.bedwars.lobby.LobbyManager lobbyManager;
    private org.vaska.bedwars.arena.ArenaEditor arenaEditor;

    private MatchmakingService matchmaking;
    private org.vaska.bedwars.shop.ShopManager shopManager;
    private CitizensManager citizensManager;
    private org.vaska.bedwars.npc.NPCManager npcManager;
    private org.vaska.bedwars.util.WorldCloneOptimizer worldCloneOptimizer;
    private org.vaska.bedwars.util.Messages messages;
    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        // messages loader (config-driven admin messages)
        messages = new org.vaska.bedwars.util.Messages(this);
        // managers
        worldResetService = new WorldResetService(this);
        arenaManager = new ArenaManager(this, worldResetService);
        playerDataManager = new PlayerDataManager(this);
        partyManager = new PartyManager(this);
        spectatorManager = new SpectatorManager(this);
        generatorManager = new org.vaska.bedwars.generator.GeneratorManager(this);

        partyManager = new PartyManager(this);
        matchmaking = new MatchmakingService(this, arenaManager, partyManager);

        // register commands
        getCommand("party").setExecutor(new PartyCommand(partyManager));
        getCommand("bw").setExecutor(new BWCommand(this, matchmaking, partyManager));

        // register listeners
        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new AntiCheatListener(), this);
        pm.registerEvents(new org.vaska.bedwars.anti.TradeProtectionListener(this), this);
        pm.registerEvents(new org.vaska.bedwars.listeners.PlayerListener(), this);
        pm.registerEvents(new org.vaska.bedwars.citizens.NPCAdminClickListener(), this);
        pm.registerEvents(new org.vaska.bedwars.shop.ShopListener(), this);
        worldCloneOptimizer = new org.vaska.bedwars.util.WorldCloneOptimizer(this);
        worldCloneOptimizer.initialize();
        pm.registerEvents(new org.vaska.bedwars.protect.UnloadProtectionListener(this), this);
        pm.registerEvents(new org.vaska.bedwars.listeners.RegionProtectionListener(), this);

        // load arenas and DB async
        arenaManager.loadArenas();
        playerDataManager.initialize();
        generatorManager.start();
        shopManager = new org.vaska.bedwars.shop.ShopManager(this);
        shopManager.load();

        // AFK manager (5 minutes default)
        new org.vaska.bedwars.util.AFKManager(this, 300);

        // Citizens hook (optional)
        new org.vaska.bedwars.citizens.CitizensHook(this).initialize();
        // initialize NPC manager (uses config to select provider)
        npcManager = new org.vaska.bedwars.npc.NPCManager(this);
        npcManager.initialize();

        // lobby manager (loads portals and spawn from config)
        lobbyManager = new org.vaska.bedwars.lobby.LobbyManager(this);
        // arena editor
        arenaEditor = new org.vaska.bedwars.arena.ArenaEditor(this, arenaManager);

        // optional PlaceholderAPI hook
        try {
            Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            new org.vaska.bedwars.placeholder.PlaceholderHook(this).register();
        } catch (ClassNotFoundException ignored) {
            getLogger().info("PlaceholderAPI not found, skipping placeholder registration.");
        }

        // UI managers
        scoreboardManager = new org.vaska.bedwars.ui.ScoreboardManager(this);
        scoreboardManager.start();
        // prefer TAB plugin if present
        if (pm.getPlugin("TAB") != null) {
            tabAdapter = new org.vaska.bedwars.ui.TabAdapter(this);
            tabAdapter.start();
        } else {
            tabManager = new org.vaska.bedwars.ui.TabManager(this);
            tabManager.start();
        }

        // register anti-dupe protections
        pm.registerEvents(new DupeProtectionListener(this), this);

        getLogger().info("Bedwars enabled");
    }

    @Override
    public void onDisable() {
        // purge purchased dropped items to avoid leftover dupes
        try {
            org.bukkit.NamespacedKey boughtKey = new org.bukkit.NamespacedKey(this, "bw_purchased");
            for (org.bukkit.World w : org.bukkit.Bukkit.getWorlds()) {
                for (org.bukkit.entity.Entity ent : w.getEntities()) {
                    if (ent instanceof org.bukkit.entity.Item) {
                        org.bukkit.entity.Item it = (org.bukkit.entity.Item) ent;
                        if (it.getItemStack() != null && it.getItemStack().getItemMeta() != null) {
                            try {
                                if (it.getItemStack().getItemMeta().getPersistentDataContainer().has(boughtKey, org.bukkit.persistence.PersistentDataType.STRING) ||
                                        it.getItemStack().getItemMeta().getPersistentDataContainer().has(boughtKey, org.bukkit.persistence.PersistentDataType.BYTE)) {
                                    it.remove();
                                }
                            } catch (Throwable ignored) {}
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
        if (playerDataManager != null) playerDataManager.shutdown();
        if (arenaManager != null) arenaManager.shutdown();
        if (scoreboardManager != null) scoreboardManager.stop();
        if (tabManager != null) tabManager.stop();
        if (tabAdapter != null) tabAdapter.stop();
        if (lobbyManager != null) lobbyManager.save();
        getLogger().info("Bedwars disabled");
    }

    public static Bedwars getInstance() {
        return instance;
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public PartyManager getPartyManager() {
        return partyManager;
    }

    public MatchmakingService getMatchmakingService() {
        return matchmakingService;
    }

    public org.vaska.bedwars.util.Messages getMessages() { return messages; }

    public SpectatorManager getSpectatorManager() {
        return spectatorManager;
    }

    public CitizensManager getCitizensManager() {
        return citizensManager;
    }

    public org.vaska.bedwars.npc.NPCManager getNpcManager() { return npcManager; }

    public org.vaska.bedwars.util.WorldCloneOptimizer getWorldCloneOptimizer() { return worldCloneOptimizer; }

    public GeneratorManager getGeneratorManager() { return generatorManager;}

    public org.vaska.bedwars.lobby.LobbyManager getLobbyManager() { return lobbyManager; }
    public org.vaska.bedwars.arena.ArenaEditor getArenaEditor() { return arenaEditor; }
}
