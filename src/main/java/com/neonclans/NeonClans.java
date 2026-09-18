package com.neonclans;

import com.neonclans.command.ClanCommand;
import com.neonclans.database.DatabaseManager;
import com.neonclans.hook.PlaceholderAPIHook;
import com.neonclans.hook.VaultHook;
import com.neonclans.listener.ChatListener;
import com.neonclans.listener.ClanListener;
import com.neonclans.manager.ClanManager;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class NeonClans extends JavaPlugin {
    private static NeonClans instance;
    private DatabaseManager databaseManager;
    private ClanManager clanManager;
    private ChatListener chatListener;
    private VaultHook vaultHook;
    private BukkitTask autoSaveTask;

    private com.neonclans.manager.LogManager logManager;
    private com.neonclans.manager.NexusManager nexusManager;
    private com.neonclans.manager.QuestManager questManager;
    private com.neonclans.manager.WarManager warManager;
    private com.neonclans.manager.PerkManager perkManager;
    private com.neonclans.manager.DiscordWebhookManager discordWebhookManager;

    @Override
    public void onEnable() {
        instance = this;

        // 1. Save and Load config.yml
        saveDefaultConfig();

        // 2. Setup Vault Hook
        vaultHook = new VaultHook(this);
        if (vaultHook.setupEconomy()) {
            getLogger().info("Successfully hooked into Vault Economy.");
        } else {
            getLogger().warning("Vault not found or no economy plugin available. Clan economy features will be disabled (costs will be free).");
        }

        // 3. Setup SQLite Database
        databaseManager = new DatabaseManager(this);
        databaseManager.initialize();

        // 4. Setup Clan Manager & Load Cache
        clanManager = new ClanManager(this);
        clanManager.loadClans();

        // 4.1 Setup Feature Managers
        logManager = new com.neonclans.manager.LogManager(this);
        nexusManager = new com.neonclans.manager.NexusManager(this);
        nexusManager.load();
        questManager = new com.neonclans.manager.QuestManager(this);
        questManager.load();
        warManager = new com.neonclans.manager.WarManager(this);
        warManager.load();
        perkManager = new com.neonclans.manager.PerkManager(this);
        perkManager.load();
        discordWebhookManager = new com.neonclans.manager.DiscordWebhookManager(this);

        // 5. Setup PlaceholderAPI expansion
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderAPIHook(this).register();
            getLogger().info("Successfully registered placeholders with PlaceholderAPI.");
        } else {
            getLogger().info("PlaceholderAPI not found. Custom placeholders won't be resolved external to this plugin.");
        }

        // 6. Register Commands
        ClanCommand clanCommand = new ClanCommand(this);
        getCommand("clan").setExecutor(clanCommand);
        getCommand("clan").setTabCompleter(clanCommand);

        // 7. Register Listeners
        chatListener = new ChatListener(this);
        getServer().getPluginManager().registerEvents(new ClanListener(this), this);
        getServer().getPluginManager().registerEvents(chatListener, this);

        // 8. Start Auto-Save & Synergy tasks
        startAutoSaveTask();
        startSynergyTask();

        getLogger().info("NeonClans has been enabled!");
    }

    @Override
    public void onDisable() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
        }

        if (clanManager != null) {
            getLogger().info("Saving clans data...");
            clanManager.saveAll();
        }

        if (nexusManager != null) nexusManager.stop();
        if (warManager != null) warManager.stop();

        if (databaseManager != null) {
            databaseManager.close();
        }

        getLogger().info("NeonClans has been disabled.");
    }

    public static NeonClans getInstance() {
        return instance;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public ClanManager getClanManager() {
        return clanManager;
    }

    public ChatListener getChatListener() {
        return chatListener;
    }

    public VaultHook getVaultHook() {
        return vaultHook;
    }

    public com.neonclans.manager.LogManager getLogManager() {
        return logManager;
    }

    public com.neonclans.manager.NexusManager getNexusManager() {
        return nexusManager;
    }

    public com.neonclans.manager.QuestManager getQuestManager() {
        return questManager;
    }

    public com.neonclans.manager.WarManager getWarManager() {
        return warManager;
    }

    public com.neonclans.manager.PerkManager getPerkManager() {
        return perkManager;
    }

    public com.neonclans.manager.DiscordWebhookManager getDiscordWebhookManager() {
        return discordWebhookManager;
    }

    public void reloadPlugin() {
        reloadConfig();
        clanManager.saveAll();
        clanManager.loadClans();
        
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
        }
        startAutoSaveTask();
    }

    private void startAutoSaveTask() {
        int intervalMinutes = getConfig().getInt("save-interval-minutes", 5);
        long ticks = intervalMinutes * 60L * 20L;
        autoSaveTask = getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            getLogger().info("Auto-saving clans data...");
            clanManager.saveAll();
        }, ticks, ticks);
    }

    private void startSynergyTask() {
        getServer().getScheduler().runTaskTimer(this, () -> {
            if (!getConfig().getBoolean("features.effects.enabled", true)) return;
            double radius = getConfig().getDouble("features.effects.synergy-radius", 5.0);

            for (org.bukkit.entity.Player p1 : getServer().getOnlinePlayers()) {
                com.neonclans.model.Clan c1 = clanManager.getClanByPlayer(p1.getUniqueId());
                if (c1 == null) continue;

                for (org.bukkit.entity.Player p2 : getServer().getOnlinePlayers()) {
                    if (p1.equals(p2)) continue;
                    com.neonclans.model.Clan c2 = clanManager.getClanByPlayer(p2.getUniqueId());
                    if (c2 != null && c1.getId().equals(c2.getId())) {
                        if (p1.getWorld().equals(p2.getWorld()) && p1.getLocation().distance(p2.getLocation()) <= radius) {
                            p1.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, p1.getLocation().add(0, 1, 0), 1, 0.1, 0.1, 0.1, 0.02);
                            break;
                        }
                    }
                }
            }
        }, 40L, 40L);
    }

    public String getPrefix() {
        return ChatColor.translateAlternateColorCodes('&', getConfig().getString("chat.prefix", "&8[&dNeonClans&8] &r"));
    }
}
