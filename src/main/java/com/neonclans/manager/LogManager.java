package com.neonclans.manager;

import com.neonclans.NeonClans;
import com.neonclans.model.ClanLog;

import java.util.List;

public class LogManager {
    private final NeonClans plugin;

    public LogManager(NeonClans plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("features.logs.enabled", true);
    }

    public void log(String clanId, String playerName, String actionType, String details) {
        if (!isEnabled()) return;
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> 
            plugin.getDatabaseManager().addLog(clanId, playerName, actionType, details)
        );
    }

    public List<ClanLog> getLogs(String clanId) {
        int limit = plugin.getConfig().getInt("features.logs.max-entries-stored", 100);
        return plugin.getDatabaseManager().getLogs(clanId, limit);
    }
}
