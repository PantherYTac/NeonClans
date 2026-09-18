package com.neonclans.manager;

import com.neonclans.NeonClans;
import com.neonclans.model.Clan;
import com.neonclans.model.ClanMember;
import com.neonclans.model.ClanWar;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class WarManager {
    private final NeonClans plugin;
    private final List<ClanWar> activeWars = new ArrayList<>();
    private final Map<String, Set<String>> pendingDeclarations = new HashMap<>(); // Challenger -> Set<TargetClanId>
    private BukkitTask expiryTask;

    public WarManager(NeonClans plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("features.wars.enabled", true);
    }

    public void load() {
        if (!isEnabled()) return;
        activeWars.clear();
        activeWars.addAll(plugin.getDatabaseManager().loadWars());
        startExpiryTask();
    }

    public void stop() {
        if (expiryTask != null) {
            expiryTask.cancel();
        }
    }

    public List<ClanWar> getActiveWars() {
        return activeWars;
    }

    public ClanWar getActiveWar(String clanId) {
        for (ClanWar war : activeWars) {
            if (war.getStatus().equalsIgnoreCase("ACTIVE") && (war.getClan1Id().equals(clanId) || war.getClan2Id().equals(clanId))) {
                return war;
            }
        }
        return null;
    }

    public void declareWar(Clan challenger, Clan target) {
        if (!isEnabled()) return;
        pendingDeclarations.computeIfAbsent(challenger.getId(), k -> new HashSet<>()).add(target.getId());
    }

    public boolean hasDeclaration(Clan challenger, Clan target) {
        Set<String> set = pendingDeclarations.get(challenger.getId());
        return set != null && set.contains(target.getId());
    }

    public ClanWar acceptWar(Clan challenger, Clan target) {
        if (!isEnabled()) return null;

        Set<String> set = pendingDeclarations.get(challenger.getId());
        if (set != null) {
            set.remove(target.getId());
        }

        long hours = plugin.getConfig().getLong("features.wars.duration-hours", 24L);
        long start = System.currentTimeMillis();
        long end = start + (hours * 3600L * 1000L);
        String warId = UUID.randomUUID().toString();

        ClanWar war = new ClanWar(warId, challenger.getId(), target.getId(), 0, 0, start, end, "ACTIVE");
        activeWars.add(war);

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> 
            plugin.getDatabaseManager().saveWar(war)
        );

        String msg = plugin.getPrefix() + ChatColor.RED + "&lCLAN WAR DECLARED! &e" + challenger.getName() + " &cVS &e" + target.getName() + "&c for 24 hours!";
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', msg));

        return war;
    }

    public void handlePvPKill(Player killer, Player victim) {
        if (!isEnabled()) return;

        Clan killerClan = plugin.getClanManager().getClanByPlayer(killer.getUniqueId());
        Clan victimClan = plugin.getClanManager().getClanByPlayer(victim.getUniqueId());

        if (killerClan == null || victimClan == null) return;

        ClanWar war = getActiveWar(killerClan.getId());
        if (war == null || !war.getStatus().equalsIgnoreCase("ACTIVE")) return;

        if (war.getClan1Id().equals(killerClan.getId()) && war.getClan2Id().equals(victimClan.getId())) {
            war.incrementClan1Kills();
        } else if (war.getClan2Id().equals(killerClan.getId()) && war.getClan1Id().equals(victimClan.getId())) {
            war.incrementClan2Kills();
        } else {
            return;
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> 
            plugin.getDatabaseManager().saveWar(war)
        );

        String score = plugin.getPrefix() + ChatColor.YELLOW + "[War Score] " + killerClan.getName() + " " + (war.getClan1Id().equals(killerClan.getId()) ? war.getClan1Kills() : war.getClan2Kills()) + " - " + (war.getClan1Id().equals(victimClan.getId()) ? war.getClan1Kills() : war.getClan2Kills()) + " " + victimClan.getName();
        broadcastToWarringClans(killerClan, victimClan, score);
    }

    public void endWar(ClanWar war, String winnerClanId) {
        war.setStatus(winnerClanId != null ? (winnerClanId.equals(war.getClan1Id()) ? "CLAN1_WON" : "CLAN2_WON") : "DRAW");

        Clan clan1 = plugin.getClanManager().getClan(war.getClan1Id());
        Clan clan2 = plugin.getClanManager().getClan(war.getClan2Id());

        if (winnerClanId != null && clan1 != null && clan2 != null) {
            Clan winner = winnerClanId.equals(clan1.getId()) ? clan1 : clan2;
            Clan loser = winnerClanId.equals(clan1.getId()) ? clan2 : clan1;

            double percent = plugin.getConfig().getDouble("features.wars.winner-bank-take-percent", 15.0);
            double loot = (loser.getBalance() * percent) / 100.0;

            loser.withdraw(loot);
            winner.deposit(loot);

            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                plugin.getDatabaseManager().saveClan(winner);
                plugin.getDatabaseManager().saveClan(loser);
            });

            String announcement = plugin.getPrefix() + ChatColor.GOLD + "&lCLAN WAR ENDED! &e" + winner.getName() + " &aVICTORIOUS &eover " + loser.getName() + "! Looted $" + String.format("%.2f", loot) + " from loser's bank!";
            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', announcement));
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> 
            plugin.getDatabaseManager().saveWar(war)
        );
    }

    private void startExpiryTask() {
        stop();
        expiryTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!isEnabled()) return;
            for (ClanWar war : new ArrayList<>(activeWars)) {
                if (war.isExpired()) {
                    if (war.getClan1Kills() > war.getClan2Kills()) {
                        endWar(war, war.getClan1Id());
                    } else if (war.getClan2Kills() > war.getClan1Kills()) {
                        endWar(war, war.getClan2Id());
                    } else {
                        endWar(war, null); // Draw
                    }
                }
            }
        }, 1200L, 1200L); // check every 60 seconds
    }

    private void broadcastToWarringClans(Clan c1, Clan c2, String message) {
        for (ClanMember m : c1.getMembers().values()) {
            Player p = Bukkit.getPlayer(m.getUuid());
            if (p != null && p.isOnline()) p.sendMessage(message);
        }
        for (ClanMember m : c2.getMembers().values()) {
            Player p = Bukkit.getPlayer(m.getUuid());
            if (p != null && p.isOnline()) p.sendMessage(message);
        }
    }
}
