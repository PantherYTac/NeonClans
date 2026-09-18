package com.neonclans.hook;

import com.neonclans.NeonClans;
import com.neonclans.model.Clan;
import com.neonclans.model.ClanMember;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class PlaceholderAPIHook extends PlaceholderExpansion {
    private final NeonClans plugin;

    public PlaceholderAPIHook(NeonClans plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "neonclans";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Antigravity";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            if (params.equalsIgnoreCase("name") || params.equalsIgnoreCase("tag") || params.equalsIgnoreCase("clan_tag") || params.equalsIgnoreCase("raw_tag")) {
                return "";
            }
            if (params.equalsIgnoreCase("role") || params.equalsIgnoreCase("role_suffix")) {
                return "";
            }
            if (params.equalsIgnoreCase("level")) {
                return "0";
            }
            if (params.equalsIgnoreCase("balance")) {
                return "0.0";
            }
            if (params.equalsIgnoreCase("members_count")) {
                return "0";
            }
            return "";
        }

        ClanMember member = clan.getMember(player.getUniqueId());
        String roleSuffix = "";
        String roleName = "";
        if (member != null) {
            switch (member.getRole()) {
                case OWNER:
                    roleSuffix = plugin.getConfig().getString("roles.owner.suffix", "**");
                    roleName = plugin.getConfig().getString("roles.owner.display-name", "Owner");
                    break;
                case MODERATOR:
                    roleSuffix = plugin.getConfig().getString("roles.moderator.suffix", "*");
                    roleName = plugin.getConfig().getString("roles.moderator.display-name", "Moderator");
                    break;
                case MEMBER:
                    roleSuffix = plugin.getConfig().getString("roles.member.suffix", "");
                    roleName = plugin.getConfig().getString("roles.member.display-name", "Member");
                    break;
            }
        }

        switch (params.toLowerCase()) {
            case "name":
                return clan.getName();
            case "tag":
            case "clan_tag":
                // Return colorized tag + role suffix
                String tagColor = plugin.getClanManager().getClanTagColor(clan);
                return ChatColor.translateAlternateColorCodes('&', tagColor + clan.getTag() + roleSuffix);
            case "raw_tag":
                return clan.getTag();
            case "role":
                return roleName;
            case "role_suffix":
                return ChatColor.translateAlternateColorCodes('&', roleSuffix);
            case "level":
                return String.valueOf(clan.getLevel());
            case "balance":
                return String.format("%.2f", clan.getBalance());
            case "members_count":
                return String.valueOf(clan.getMembers().size());
            case "max_members":
                return String.valueOf(plugin.getClanManager().getMaxMembers(clan.getLevel()));
            default:
                return "";
        }
    }
}
