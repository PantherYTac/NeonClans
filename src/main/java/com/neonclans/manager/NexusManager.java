package com.neonclans.manager;

import com.neonclans.NeonClans;
import com.neonclans.model.Clan;
import com.neonclans.model.ClanMember;
import com.neonclans.model.ClanNexus;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class NexusManager {
    private final NeonClans plugin;
    private final Map<String, ClanNexus> nexusMap = new HashMap<>(); // clanId -> ClanNexus
    private final Map<UUID, Long> intruderAlertCooldown = new HashMap<>();
    private BukkitTask task;

    public NexusManager(NeonClans plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("features.nexus.enabled", true);
    }

    public void load() {
        if (!isEnabled()) return;
        nexusMap.clear();
        nexusMap.putAll(plugin.getDatabaseManager().loadNexusMap());
        startNexusTask();
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
    }

    public ClanNexus getNexus(String clanId) {
        return nexusMap.get(clanId);
    }

    public boolean setNexus(Clan clan, Location loc) {
        if (!isEnabled()) return false;
        ClanNexus nexus = new ClanNexus(clan.getId(), loc);
        nexusMap.put(clan.getId(), nexus);
        
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> 
            plugin.getDatabaseManager().saveNexus(nexus)
        );
        return true;
    }

    public boolean removeNexus(String clanId) {
        if (!isEnabled()) return false;
        nexusMap.remove(clanId);
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> 
            plugin.getDatabaseManager().deleteNexus(clanId)
        );
        return true;
    }

    public double getNexusRadius(Clan clan) {
        double base = plugin.getConfig().getDouble("features.nexus.base-radius", 30.0);
        double perLevel = plugin.getConfig().getDouble("features.nexus.radius-per-level", 10.0);
        return base + (clan.getLevel() * perLevel);
    }

    private void startNexusTask() {
        stop();
        long intervalSeconds = plugin.getConfig().getLong("features.nexus.buff-interval-seconds", 5L);
        long ticks = intervalSeconds * 20L;

        task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!isEnabled()) return;

            for (ClanNexus nexus : nexusMap.values()) {
                Clan clan = plugin.getClanManager().getClan(nexus.getClanId());
                if (clan == null || nexus.getLocation() == null || nexus.getLocation().getWorld() == null) continue;

                double radius = getNexusRadius(clan);
                Location loc = nexus.getLocation();

                for (Player player : loc.getWorld().getPlayers()) {
                    if (player.getLocation().distance(loc) <= radius) {
                        Clan playerClan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());

                        if (playerClan != null && playerClan.getId().equals(clan.getId())) {
                            // Clan member gets passive buffs
                            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 160, 0, true, false));
                            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 160, 0, true, false));
                            player.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, 160, 0, true, false));
                        } else if (playerClan == null || (!playerClan.getId().equals(clan.getId()) && !clan.isAlly(playerClan.getId()))) {
                            // Intruder detected! Send alert to online clan members (cooldown 30s)
                            long lastAlert = intruderAlertCooldown.getOrDefault(player.getUniqueId(), 0L);
                            if (System.currentTimeMillis() - lastAlert > 30000L) {
                                intruderAlertCooldown.put(player.getUniqueId(), System.currentTimeMillis());
                                String alert = plugin.getPrefix() + ChatColor.RED + "WARNING: Intruder " + ChatColor.YELLOW + player.getName() + ChatColor.RED + " entered your Clan Nexus territory!";
                                for (ClanMember m : clan.getMembers().values()) {
                                    Player online = Bukkit.getPlayer(m.getUuid());
                                    if (online != null && online.isOnline()) {
                                        online.sendMessage(alert);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }, ticks, ticks);
    }
}
