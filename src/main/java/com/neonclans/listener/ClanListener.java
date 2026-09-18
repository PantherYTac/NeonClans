package com.neonclans.listener;

import com.neonclans.NeonClans;
import com.neonclans.gui.ClanMenuHolder;
import com.neonclans.gui.ClanVaultHolder;
import com.neonclans.model.Clan;
import com.neonclans.util.InventorySerializer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.Particle;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class ClanListener implements Listener {
    private final NeonClans plugin;

    public ClanListener(NeonClans plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player victim = (Player) event.getEntity();

        Player damager = null;
        Entity damagerEntity = event.getDamager();
        
        if (damagerEntity instanceof Player) {
            damager = (Player) damagerEntity;
        } else if (damagerEntity instanceof Projectile) {
            Projectile projectile = (Projectile) damagerEntity;
            if (projectile.getShooter() instanceof Player) {
                damager = (Player) projectile.getShooter();
            }
        }

        if (damager == null || damager.equals(victim)) return;

        Clan victimClan = plugin.getClanManager().getClanByPlayer(victim.getUniqueId());
        Clan damagerClan = plugin.getClanManager().getClanByPlayer(damager.getUniqueId());

        if (victimClan == null || damagerClan == null) return;

        // Same Clan Damage
        if (victimClan.getId().equals(damagerClan.getId())) {
            if (!victimClan.isFriendlyFireEnabled()) {
                event.setCancelled(true);
                damager.sendMessage(plugin.getPrefix() + ChatColor.RED + "Friendly fire is disabled for your clan!");
            }
            return;
        }

        // Allied Clan Damage
        if (victimClan.isAlly(damagerClan.getId())) {
            // If friendly fire is disabled for either clan, we block it to protect alliances
            if (!victimClan.isFriendlyFireEnabled() || !damagerClan.isFriendlyFireEnabled()) {
                event.setCancelled(true);
                damager.sendMessage(plugin.getPrefix() + ChatColor.RED + "Friendly fire is disabled between allied clans!");
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof ClanMenuHolder) {
            event.setCancelled(true); // Prevent taking item out

            if (event.getClickedInventory() != null && event.getClickedInventory().getHolder() instanceof ClanMenuHolder) {
                ClanMenuHolder holder = (ClanMenuHolder) event.getClickedInventory().getHolder();
                if (holder.getClickHandler() != null) {
                    holder.getClickHandler().onClick(event);
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof ClanVaultHolder) {
            ClanVaultHolder holder = (ClanVaultHolder) event.getInventory().getHolder();
            Clan clan = holder.getClan();
            
            // Serialize vault items
            String base64 = InventorySerializer.itemStackArrayToBase64(event.getInventory().getContents());
            clan.setVaultData(base64);
            
            // Save async to database
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> 
                plugin.getDatabaseManager().saveVault(clan.getId(), base64)
            );
            
            event.getPlayer().sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Clan vault contents saved successfully.");
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan != null) {
            plugin.getQuestManager().incrementQuestProgress(clan, "MINE_BLOCKS", 1);
        }

        // Nexus Break check
        if (plugin.getNexusManager().isEnabled()) {
            com.neonclans.model.ClanNexus nexus = plugin.getNexusManager().getNexus(clan != null ? clan.getId() : "");
            if (nexus != null && nexus.getLocation() != null) {
                if (event.getBlock().getLocation().equals(nexus.getLocation())) {
                    if (clan != null && (clan.isOwner(player.getUniqueId()) || clan.isModerator(player.getUniqueId()))) {
                        plugin.getNexusManager().removeNexus(clan.getId());
                        player.sendMessage(plugin.getPrefix() + ChatColor.YELLOW + "Clan Nexus block broken and removed.");
                    } else {
                        event.setCancelled(true);
                        player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Only Clan Owners/Moderators can break the Clan Nexus!");
                    }
                }
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() != null) {
            Player killer = event.getEntity().getKiller();
            Clan clan = plugin.getClanManager().getClanByPlayer(killer.getUniqueId());
            if (clan != null) {
                plugin.getQuestManager().incrementQuestProgress(clan, "KILL_MOBS", 1);

                // Perk: Loot Harvester (+15% XP)
                if (plugin.getPerkManager().hasPerk(clan.getId(), "loot_harvester")) {
                    int bonusXp = (int) Math.ceil(event.getDroppedExp() * 0.15);
                    event.setDroppedExp(event.getDroppedExp() + bonusXp);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer != null && !killer.equals(victim)) {
            Clan killerClan = plugin.getClanManager().getClanByPlayer(killer.getUniqueId());
            if (killerClan != null) {
                plugin.getQuestManager().incrementQuestProgress(killerClan, "PVP_KILLS", 1);
            }

            // War kill tracker
            plugin.getWarManager().handlePvPKill(killer, victim);
        }
    }
}
