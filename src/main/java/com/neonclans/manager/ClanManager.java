package com.neonclans.manager;

import com.neonclans.NeonClans;
import com.neonclans.model.Clan;
import com.neonclans.model.ClanMember;
import com.neonclans.model.ClanRole;
import org.bukkit.Location;

import java.util.*;

public class ClanManager {
    private final NeonClans plugin;
    private final Map<String, Clan> clans = new HashMap<>(); // ID -> Clan
    private final Map<UUID, String> playerClanMap = new HashMap<>(); // Player UUID -> Clan ID
    
    // Invites cache: Invitee UUID -> Set of Clan IDs
    private final Map<UUID, Set<String>> pendingInvites = new HashMap<>();

    public ClanManager(NeonClans plugin) {
        this.plugin = plugin;
    }

    public void loadClans() {
        clans.clear();
        playerClanMap.clear();
        
        Map<String, Clan> loaded = plugin.getDatabaseManager().loadClans();
        clans.putAll(loaded);
        
        for (Clan clan : clans.values()) {
            for (UUID memberUuid : clan.getMembers().keySet()) {
                playerClanMap.put(memberUuid, clan.getId());
            }
        }
        plugin.getLogger().info("Loaded " + clans.size() + " clans from database.");
    }

    public void saveAll() {
        for (Clan clan : clans.values()) {
            plugin.getDatabaseManager().saveClan(clan);
            for (ClanMember member : clan.getMembers().values()) {
                plugin.getDatabaseManager().saveMember(member);
            }
        }
    }

    public Map<String, Clan> getClans() {
        return clans;
    }

    public Clan getClan(String id) {
        return clans.get(id);
    }

    public Clan getClanByName(String name) {
        for (Clan clan : clans.values()) {
            if (clan.getName().equalsIgnoreCase(name)) {
                return clan;
            }
        }
        return null;
    }

    public Clan getClanByTag(String tag) {
        for (Clan clan : clans.values()) {
            if (clan.getTag().equalsIgnoreCase(tag)) {
                return clan;
            }
        }
        return null;
    }

    public Clan getClanByPlayer(UUID uuid) {
        String clanId = playerClanMap.get(uuid);
        if (clanId != null) {
            return clans.get(clanId);
        }
        return null;
    }

    public Clan createClan(String name, String tag, UUID ownerUuid) {
        String id = UUID.randomUUID().toString();
        Clan clan = new Clan(id, name, tag, ownerUuid, 1, 0.0, null, false, System.currentTimeMillis());
        
        ClanMember owner = new ClanMember(ownerUuid, id, ClanRole.OWNER, System.currentTimeMillis());
        clan.addMember(owner);
        
        clans.put(id, clan);
        playerClanMap.put(ownerUuid, id);
        
        // Save async
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getDatabaseManager().saveClan(clan);
            plugin.getDatabaseManager().saveMember(owner);
        });
        
        return clan;
    }

    public void disbandClan(String clanId) {
        Clan clan = clans.remove(clanId);
        if (clan == null) return;

        for (UUID memberUuid : clan.getMembers().keySet()) {
            playerClanMap.remove(memberUuid);
        }

        // Dissolve alliances with other clans
        for (String allyId : new ArrayList<>(clan.getAllies())) {
            Clan ally = clans.get(allyId);
            if (ally != null) {
                ally.removeAlly(clanId);
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> 
                    plugin.getDatabaseManager().removeAlliance(clanId, allyId)
                );
            }
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> 
            plugin.getDatabaseManager().deleteClan(clanId)
        );
    }

    public boolean addMemberToClan(Clan clan, UUID uuid) {
        if (playerClanMap.containsKey(uuid)) return false;
        
        ClanMember member = new ClanMember(uuid, clan.getId(), ClanRole.MEMBER, System.currentTimeMillis());
        clan.addMember(member);
        playerClanMap.put(uuid, clan.getId());
        
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> 
            plugin.getDatabaseManager().saveMember(member)
        );
        return true;
    }

    public boolean removeMemberFromClan(Clan clan, UUID uuid) {
        if (!clan.isMember(uuid)) return false;
        
        clan.removeMember(uuid);
        playerClanMap.remove(uuid);
        
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> 
            plugin.getDatabaseManager().removeMember(uuid)
        );
        return true;
    }

    public boolean promoteMember(Clan clan, UUID uuid) {
        ClanMember member = clan.getMember(uuid);
        if (member == null || member.getRole() != ClanRole.MEMBER) return false;
        
        member.setRole(ClanRole.MODERATOR);
        
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> 
            plugin.getDatabaseManager().saveMember(member)
        );
        return true;
    }

    public boolean demoteMember(Clan clan, UUID uuid) {
        ClanMember member = clan.getMember(uuid);
        if (member == null || member.getRole() != ClanRole.MODERATOR) return false;
        
        member.setRole(ClanRole.MEMBER);
        
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> 
            plugin.getDatabaseManager().saveMember(member)
        );
        return true;
    }

    public boolean transferOwnership(Clan clan, UUID targetUuid) {
        ClanMember target = clan.getMember(targetUuid);
        if (target == null) return false;
        
        UUID oldOwnerUuid = clan.getOwnerUuid();
        ClanMember oldOwner = clan.getMember(oldOwnerUuid);
        
        clan.setOwnerUuid(targetUuid);
        target.setRole(ClanRole.OWNER);
        
        if (oldOwner != null) {
            oldOwner.setRole(ClanRole.MODERATOR); // Demote old owner to moderator by default
        }
        
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getDatabaseManager().saveClan(clan);
            plugin.getDatabaseManager().saveMember(target);
            if (oldOwner != null) {
                plugin.getDatabaseManager().saveMember(oldOwner);
            }
        });
        return true;
    }

    public void setClanHome(Clan clan, Location home) {
        clan.setHome(home);
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> 
            plugin.getDatabaseManager().saveClan(clan)
        );
    }

    public void setFriendlyFire(Clan clan, boolean enabled) {
        clan.setFriendlyFire(enabled);
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> 
            plugin.getDatabaseManager().saveClan(clan)
        );
    }

    public boolean addAlliance(Clan clan1, Clan clan2) {
        if (clan1.isAlly(clan2.getId())) return false;
        
        clan1.addAlly(clan2.getId());
        clan2.addAlly(clan1.getId());
        
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> 
            plugin.getDatabaseManager().addAlliance(clan1.getId(), clan2.getId())
        );
        return true;
    }

    public boolean removeAlliance(Clan clan1, Clan clan2) {
        if (!clan1.isAlly(clan2.getId())) return false;
        
        clan1.removeAlly(clan2.getId());
        clan2.removeAlly(clan1.getId());
        
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> 
            plugin.getDatabaseManager().removeAlliance(clan1.getId(), clan2.getId())
        );
        return true;
    }

    // Invites system
    public void invitePlayer(UUID inviteeUuid, String clanId) {
        pendingInvites.computeIfAbsent(inviteeUuid, k -> new HashSet<>()).add(clanId);
    }

    public boolean hasInvite(UUID inviteeUuid, String clanId) {
        Set<String> invites = pendingInvites.get(inviteeUuid);
        return invites != null && invites.contains(clanId);
    }

    public void removeInvite(UUID inviteeUuid, String clanId) {
        Set<String> invites = pendingInvites.get(inviteeUuid);
        if (invites != null) {
            invites.remove(clanId);
            if (invites.isEmpty()) {
                pendingInvites.remove(inviteeUuid);
            }
        }
    }

    // Config Lookups
    public int getMaxMembers(int level) {
        return plugin.getConfig().getInt("upgrades." + level + ".max-members", 5);
    }

    public int getMaxAllies(int level) {
        return plugin.getConfig().getInt("upgrades." + level + ".max-allies", 1);
    }

    public double getUpgradeCost(int nextLevel) {
        return plugin.getConfig().getDouble("upgrades." + nextLevel + ".cost", 0.0);
    }

    public int getVaultRows(int level) {
        return plugin.getConfig().getInt("upgrades." + level + ".vault-rows", 1);
    }

    public String getClanTagColor(Clan clan) {
        // If maxed and custom colors are used, it will be stored in the tag itself (which can be customized)
        // Otherwise, read config level tag color
        return plugin.getConfig().getString("upgrades." + clan.getLevel() + ".tag-color", "&7");
    }

    public boolean isMaxLevel(Clan clan) {
        int nextLevel = clan.getLevel() + 1;
        return !plugin.getConfig().contains("upgrades." + nextLevel);
    }

    public int getMaxLevel() {
        int lvl = 1;
        while (plugin.getConfig().contains("upgrades." + (lvl + 1))) {
            lvl++;
        }
        return lvl;
    }

    public boolean canCustomiseTag(Clan clan) {
        return isMaxLevel(clan) && plugin.getConfig().getBoolean("max-level-custom-tag.enabled", true);
    }
}
