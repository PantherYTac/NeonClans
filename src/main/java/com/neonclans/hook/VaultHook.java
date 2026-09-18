package com.neonclans.hook;

import com.neonclans.NeonClans;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultHook {
    private final NeonClans plugin;
    private Economy econ = null;

    public VaultHook(NeonClans plugin) {
        this.plugin = plugin;
    }

    public boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public boolean hasEconomy() {
        return econ != null;
    }

    public Economy getEconomy() {
        return econ;
    }

    public double getBalance(OfflinePlayer player) {
        if (!hasEconomy()) return 0;
        return econ.getBalance(player);
    }

    public boolean has(OfflinePlayer player, double amount) {
        if (!hasEconomy()) return true; // Free if no economy hook
        return econ.has(player, amount);
    }

    public boolean withdraw(OfflinePlayer player, double amount) {
        if (!hasEconomy()) return true;
        return econ.withdrawPlayer(player, amount).transactionSuccess();
    }

    public boolean deposit(OfflinePlayer player, double amount) {
        if (!hasEconomy()) return true;
        return econ.depositPlayer(player, amount).transactionSuccess();
    }
}
