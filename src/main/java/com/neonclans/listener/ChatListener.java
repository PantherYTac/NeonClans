package com.neonclans.listener;

import com.neonclans.NeonClans;
import com.neonclans.model.Clan;
import com.neonclans.model.ClanMember;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;

public class ChatListener implements Listener {
    private final NeonClans plugin;
    private final Set<UUID> clanChatToggled = new HashSet<>();
    private final Set<UUID> allyChatToggled = new HashSet<>();

    public ChatListener(NeonClans plugin) {
        this.plugin = plugin;
    }

    public boolean toggleClanChat(UUID uuid) {
        allyChatToggled.remove(uuid); // Disable ally chat if enabling clan chat
        if (clanChatToggled.contains(uuid)) {
            clanChatToggled.remove(uuid);
            return false;
        } else {
            clanChatToggled.add(uuid);
            return true;
        }
    }

    public boolean toggleAllyChat(UUID uuid) {
        clanChatToggled.remove(uuid); // Disable clan chat if enabling ally chat
        if (allyChatToggled.contains(uuid)) {
            allyChatToggled.remove(uuid);
            return false;
        } else {
            allyChatToggled.add(uuid);
            return true;
        }
    }

    public boolean isClanChat(UUID uuid) {
        return clanChatToggled.contains(uuid);
    }

    public boolean isAllyChat(UUID uuid) {
        return allyChatToggled.contains(uuid);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        Clan clan = plugin.getClanManager().getClanByPlayer(uuid);
        if (clan == null) {
            // Remove toggled chats if player leaves or is kicked from clan
            clanChatToggled.remove(uuid);
            allyChatToggled.remove(uuid);
            return;
        }

        if (clanChatToggled.contains(uuid)) {
            event.setCancelled(true);
            sendClanMessage(player, clan, event.getMessage());
        } else if (allyChatToggled.contains(uuid)) {
            event.setCancelled(true);
            sendAllyMessage(player, clan, event.getMessage());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        clanChatToggled.remove(uuid);
        allyChatToggled.remove(uuid);
    }

    public void sendClanMessage(Player player, Clan clan, String message) {
        ClanMember member = clan.getMember(player.getUniqueId());
        String roleSuffix = "";
        if (member != null) {
            switch (member.getRole()) {
                case OWNER:
                    roleSuffix = plugin.getConfig().getString("roles.owner.suffix", "**");
                    break;
                case MODERATOR:
                    roleSuffix = plugin.getConfig().getString("roles.moderator.suffix", "*");
                    break;
                case MEMBER:
                    roleSuffix = plugin.getConfig().getString("roles.member.suffix", "");
                    break;
            }
        }

        String format = plugin.getConfig().getString("chat.clan-chat-format", "&d[Clan Chat] &7{role_suffix}{player}&f: {message}");
        String formatted = format
                .replace("{role_suffix}", roleSuffix)
                .replace("{player}", player.getName())
                .replace("{message}", message);
        
        String colorized = ChatColor.translateAlternateColorCodes('&', formatted);

        for (ClanMember m : clan.getMembers().values()) {
            Player onlinePlayer = Bukkit.getPlayer(m.getUuid());
            if (onlinePlayer != null && onlinePlayer.isOnline()) {
                onlinePlayer.sendMessage(colorized);
            }
        }
        
        // Log to console
        plugin.getLogger().info("[Clan Chat: " + clan.getName() + "] " + player.getName() + ": " + message);
    }

    public void sendAllyMessage(Player player, Clan clan, String message) {
        ClanMember member = clan.getMember(player.getUniqueId());
        String roleSuffix = "";
        if (member != null) {
            switch (member.getRole()) {
                case OWNER:
                    roleSuffix = plugin.getConfig().getString("roles.owner.suffix", "**");
                    break;
                case MODERATOR:
                    roleSuffix = plugin.getConfig().getString("roles.moderator.suffix", "*");
                    break;
                case MEMBER:
                    roleSuffix = plugin.getConfig().getString("roles.member.suffix", "");
                    break;
            }
        }

        String format = plugin.getConfig().getString("chat.ally-chat-format", "&b[Ally Chat] &e{clan} &7{role_suffix}{player}&f: {message}");
        String formatted = format
                .replace("{clan}", clan.getName())
                .replace("{role_suffix}", roleSuffix)
                .replace("{player}", player.getName())
                .replace("{message}", message);
        
        String colorized = ChatColor.translateAlternateColorCodes('&', formatted);

        // Send to own clan
        for (ClanMember m : clan.getMembers().values()) {
            Player onlinePlayer = Bukkit.getPlayer(m.getUuid());
            if (onlinePlayer != null && onlinePlayer.isOnline()) {
                onlinePlayer.sendMessage(colorized);
            }
        }

        // Send to allied clans
        for (String allyId : clan.getAllies()) {
            Clan ally = plugin.getClanManager().getClan(allyId);
            if (ally != null) {
                for (ClanMember m : ally.getMembers().values()) {
                    Player onlinePlayer = Bukkit.getPlayer(m.getUuid());
                    if (onlinePlayer != null && onlinePlayer.isOnline()) {
                        onlinePlayer.sendMessage(colorized);
                    }
                }
            }
        }

        // Log to console
        plugin.getLogger().info("[Ally Chat: " + clan.getName() + "] " + player.getName() + ": " + message);
    }
}
