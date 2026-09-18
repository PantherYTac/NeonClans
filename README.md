# ⚡ NeonClans - Ultimate Minecraft Clan Plugin

![NeonClans Banner](https://raw.githubusercontent.com/PantherYTac/NeonClans/main/src/main/resources/banner.jpg)

**NeonClans** is a feature-rich, modern, and highly customizable Minecraft Paper/Spigot clan plugin (targeting version 1.20-1.21+). Designed with an intuitive GUI, economy integration via **Vault**, placeholder support via **PlaceholderAPI**, and persistent local **SQLite** storage.

---

## 🌟 Key Features

* 🏷️ **Dynamic Tag Suffixes & Level Colors**: Suffixes based on roles (`**` Owner, `*` Moderator). Upgrading your clan level changes the tag color. Max-level owners unlock custom tag layout formatting (`/clan tag`).
* 🏛️ **Clan Nexus & Territory Protection**: Place a Clan Nexus to grant passive buffs (Speed, Regeneration, Haste) to online clanmates and trigger intruder alerts when enemies enter your territory.
* 📜 **Daily Clan Quests**: Collective daily objectives (`MINE_BLOCKS`, `KILL_MOBS`, `PVP_KILLS`) that reward money directly to the Clan Bank upon completion.
* ⚔️ **Clan Wars & Rivalries**: Initiate 24-hour wars with live kill counters. The winning clan loots 15% of the losing clan's bank balance!
* 🌳 **Skill Tree & Perks**: Unlock passive perks:
  * *Loot Harvester*: +15% EXP & mob loot.
  * *Teleport Haste*: 50% faster `/clan home` teleport.
  * *EXP Sharing*: Share 10% EXP with nearby online members.
  * *Vault Protection*: Restricts Clan Vault access to staff.
* 📦 **Upgradable Clan Vault**: Cloud chest storage that grows up to 54 slots (double chest) as your clan levels up.
* 📊 **Audit Logs**: In-game transparent logs (`/clan logs`) tracking bank deposits, vault edits, kicks, promotions, and war declarations.
* ✨ **Neon Visual Effects**: Glowing entity outlines (`/clan glow`) and End Rod particle synergy rings when members stand together.
* 📢 **Discord Webhook Alerts**: Asynchronously notifies your Discord server when clans are created, war is declared/won, or max levels are reached.
* 🏆 **Top Clans Leaderboard**: Interactive GUI (`/clan top`) ranking clans by Level, Balance, War Kills, and Completed Quests.

---

## 📜 Commands & Permissions

| Command | Permission | Description |
| :--- | :--- | :--- |
| `/clan` | `neonclans.use` | Opens the main interactive Clan GUI. |
| `/clan create <name> <tag>` | `neonclans.use` | Form a new clan (costs $100 default). |
| `/clan info [clan]` | `neonclans.use` | View detailed stats of a clan. |
| `/clan invite <player>` | `neonclans.use` | Invite a player to join your clan. |
| `/clan join <clan>` | `neonclans.use` | Accept a pending clan invitation. |
| `/clan leave` | `neonclans.use` | Resign from your current clan. |
| `/clan kick <player>` | `neonclans.use` | Evict a member from the clan. |
| `/clan promote <player>` | `neonclans.use` | Promote a member to Moderator (`*` suffix). |
| `/clan demote <player>` | `neonclans.use` | Demote a moderator back to Member. |
| `/clan transfer <player>` | `neonclans.use` | Appoint a new Clan Owner. |
| `/clan sethome` | `neonclans.use` | Set the clan home coordinates. |
| `/clan home` | `neonclans.use` | Teleport to clan home (with warmup & cooldown). |
| `/clan bank <deposit/withdraw/balance>` | `neonclans.use` | Manage shared clan bank funds. |
| `/clan ally <add/remove/chat> [clan]` | `neonclans.use` | Manage alliances or chat with allies. |
| `/clan chat [message]` | `neonclans.use` | Toggle or send messages to clan chat. |
| `/clan top` | `neonclans.use` | Open Top Clans Leaderboard GUI. |
| `/clan nexus <place/remove/info>` | `neonclans.use` | Manage your Clan Nexus block. |
| `/clan quest` | `neonclans.use` | View daily clan objectives and progress. |
| `/clan war <declare/accept/status/surrender>` | `neonclans.use` | Declare or manage clan wars. |
| `/clan perks` | `neonclans.use` | Open Clan Skill Tree GUI. |
| `/clan logs` | `neonclans.use` | View recent clan audit logs. |
| `/clan glow` | `neonclans.use` | Toggle neon player glow aura. |
| `/clan reload` | `neonclans.admin` | Reload configuration files and database. |

---

## 🧩 PlaceholderAPI Support

| Placeholder | Output Description |
| :--- | :--- |
| `%neonclans_name%` | Name of player's clan. |
| `%neonclans_tag%` or `%neonclans_clan_tag%` | Colorized tag of player's clan with rank suffix. |
| `%neonclans_raw_tag%` | Raw unformatted clan tag. |
| `%neonclans_role%` | Player's role in the clan (Owner, Moderator, Member). |
| `%neonclans_role_suffix%` | Suffix based on role (`**`, `*`, or empty). |
| `%neonclans_level%` | Clan level. |
| `%neonclans_balance%` | Clan bank balance formatted. |
| `%neonclans_members_count%` | Current number of members. |
| `%neonclans_max_members%` | Maximum member capacity for current level. |

---

## 🛠️ Building from Source

Clone the repository and build using **Maven** with Java 17+:

```bash
git clone https://github.com/PantherYTac/NeonClans.git
cd NeonClans
mvn clean package
```

The compiled jar will be created under `target/NeonClans-1.0.0.jar`.

---

## 📄 License
This project is licensed under the MIT License.
