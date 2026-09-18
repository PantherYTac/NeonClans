# NeonClans

A feature-rich, modern Minecraft Paper/Spigot clan plugin compatible with versions 1.20 and 1.21+. NeonClans features GUI menus, Vault economy integration, PlaceholderAPI placeholders, and SQLite database storage.

---

## Key Features

* **Dynamic Tag Suffixes & Level Colors**: Ranks define member suffixes (`**` for Owner, `*` for Moderator). Upgrading clan level updates the tag color in chat and scoreboards. Max-level clan owners unlock custom tag layout formatting (`/clan tag`).
* **Clan Nexus & Territory Protection**: Place a Clan Nexus to grant passive buffs (Speed, Regeneration, Haste) to nearby online clan members and receive intruder warnings when non-allied players enter territory.
* **Daily Clan Quests**: Cooperative daily objectives (mine blocks, kill mobs, PvP kills) rewarding money directly into the Clan Bank upon completion.
* **Clan Wars & Rivalries**: Initiate 24-hour wars with live kill counters. The winning clan captures a percentage of the losing clan's bank balance.
* **Skill Tree & Perks**: Unlock passive perks including Loot Harvester (+15% EXP/drops), Teleport Haste (50% faster `/clan home`), EXP Sharing, and Vault Protection.
* **Upgradable Clan Vault**: Storage chest capacity expands up to 54 slots (double chest) as the clan levels up.
* **Audit Logs**: In-game transparent logs (`/clan logs`) recording bank deposits, vault edits, kicks, promotions, and war declarations.
* **Visual Effects**: Player glowing outlines (`/clan glow`) and particle synergy rings when members stand near each other.
* **Discord Webhook Alerts**: Posts webhook embeds for clan creation, level upgrades, and war outcomes.
* **Top Clans Leaderboard**: Interactive GUI (`/clan top`) ranking clans by level, bank balance, war kills, and quests completed.

---

## Commands & Permissions

| Command | Permission | Description |
| :--- | :--- | :--- |
| `/clan` | `neonclans.use` | Opens the main interactive Clan GUI. |
| `/clan create <name> <tag>` | `neonclans.use` | Creates a new clan. |
| `/clan info [clan]` | `neonclans.use` | Displays stats for a clan. |
| `/clan invite <player>` | `neonclans.use` | Invites a player to join your clan. |
| `/clan join <clan>` | `neonclans.use` | Accepts a pending invitation. |
| `/clan leave` | `neonclans.use` | Leaves your current clan. |
| `/clan kick <player>` | `neonclans.use` | Kicks a member from the clan. |
| `/clan promote <player>` | `neonclans.use` | Promotes a member to Moderator. |
| `/clan demote <player>` | `neonclans.use` | Demotes a moderator back to Member. |
| `/clan transfer <player>` | `neonclans.use` | Transfers clan ownership. |
| `/clan sethome` | `neonclans.use` | Sets the clan home coordinates. |
| `/clan home` | `neonclans.use` | Teleports to clan home. |
| `/clan bank <deposit/withdraw/balance>` | `neonclans.use` | Manages clan bank funds. |
| `/clan ally <add/remove/chat> [clan]` | `neonclans.use` | Manages alliances or chats with allies. |
| `/clan chat [message]` | `neonclans.use` | Toggles or sends messages to clan chat. |
| `/clan top` | `neonclans.use` | Opens Top Clans Leaderboard GUI. |
| `/clan nexus <place/remove/info>` | `neonclans.use` | Manages your Clan Nexus block. |
| `/clan quest` | `neonclans.use` | Views daily clan objectives and progress. |
| `/clan war <declare/accept/status/surrender>` | `neonclans.use` | Manages clan wars. |
| `/clan perks` | `neonclans.use` | Opens Clan Skill Tree GUI. |
| `/clan logs` | `neonclans.use` | Views recent clan audit logs. |
| `/clan glow` | `neonclans.use` | Toggles neon player glow aura. |
| `/clan reload` | `neonclans.admin` | Reloads configuration files and database. |

---

## PlaceholderAPI Support

| Placeholder | Output Description |
| :--- | :--- |
| `%neonclans_name%` | Name of player's clan. |
| `%neonclans_tag%` or `%neonclans_clan_tag%` | Colorized tag of player's clan with rank suffix. |
| `%neonclans_raw_tag%` | Raw unformatted clan tag. |
| `%neonclans_role%` | Player's role in the clan (Owner, Moderator, Member). |
| `%neonclans_role_suffix%` | Suffix based on role (`**`, `*`, or empty). |
| `%neonclans_level%` | Clan level. |
| `%neonclans_balance%` | Formatted clan bank balance. |
| `%neonclans_members_count%` | Current number of members. |
| `%neonclans_max_members%` | Maximum member capacity for current level. |

---

## Building from Source

Build using Maven with Java 17+:

```bash
git clone https://github.com/PantherYTac/NeonClans.git
cd NeonClans
mvn clean package
```

The compiled jar will be located under `target/NeonClans-1.0.0.jar`.

---

## License
This project is licensed under the MIT License.
