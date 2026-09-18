package com.neonclans.database;

import com.neonclans.NeonClans;
import com.neonclans.model.Clan;
import com.neonclans.model.ClanMember;
import com.neonclans.model.ClanRole;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;

public class DatabaseManager {
    private final NeonClans plugin;
    private Connection connection;
    private final File dbFile;

    public DatabaseManager(NeonClans plugin) {
        this.plugin = plugin;
        this.dbFile = new File(plugin.getDataFolder(), "clans.db");
    }

    public synchronized void initialize() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        try {
            if (connection != null && !connection.isClosed()) {
                return;
            }
            
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            
            createTables();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize SQLite database", e);
        }
    }

    public synchronized void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to close SQLite connection", e);
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Clans Table
            stmt.execute("CREATE TABLE IF NOT EXISTS clans (" +
                    "id TEXT PRIMARY KEY," +
                    "name TEXT UNIQUE NOT NULL," +
                    "tag TEXT NOT NULL," +
                    "owner_uuid TEXT NOT NULL," +
                    "level INTEGER DEFAULT 1," +
                    "balance DOUBLE DEFAULT 0.0," +
                    "home_world TEXT," +
                    "home_x DOUBLE," +
                    "home_y DOUBLE," +
                    "home_z DOUBLE," +
                    "home_yaw REAL," +
                    "home_pitch REAL," +
                    "friendly_fire INTEGER DEFAULT 0," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ");");

            // Clan Members Table
            stmt.execute("CREATE TABLE IF NOT EXISTS clan_members (" +
                    "uuid TEXT PRIMARY KEY," +
                    "clan_id TEXT NOT NULL," +
                    "role TEXT NOT NULL," +
                    "joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (clan_id) REFERENCES clans(id) ON DELETE CASCADE" +
                    ");");

            // Alliances Table
            stmt.execute("CREATE TABLE IF NOT EXISTS clan_alliances (" +
                    "clan_id1 TEXT NOT NULL," +
                    "clan_id2 TEXT NOT NULL," +
                    "PRIMARY KEY (clan_id1, clan_id2)," +
                    "FOREIGN KEY (clan_id1) REFERENCES clans(id) ON DELETE CASCADE," +
                    "FOREIGN KEY (clan_id2) REFERENCES clans(id) ON DELETE CASCADE" +
                    ");");

            // Clan Vaults Table
            stmt.execute("CREATE TABLE IF NOT EXISTS clan_vaults (" +
                    "clan_id TEXT PRIMARY KEY," +
                    "vault_data TEXT NOT NULL," +
                    "FOREIGN KEY (clan_id) REFERENCES clans(id) ON DELETE CASCADE" +
                    ");");

            // Clan Nexus Table
            stmt.execute("CREATE TABLE IF NOT EXISTS clan_nexus (" +
                    "clan_id TEXT PRIMARY KEY," +
                    "world TEXT NOT NULL," +
                    "x DOUBLE NOT NULL," +
                    "y DOUBLE NOT NULL," +
                    "z DOUBLE NOT NULL," +
                    "FOREIGN KEY (clan_id) REFERENCES clans(id) ON DELETE CASCADE" +
                    ");");

            // Clan Quests Table
            stmt.execute("CREATE TABLE IF NOT EXISTS clan_quests (" +
                    "clan_id TEXT PRIMARY KEY," +
                    "quest_type TEXT NOT NULL," +
                    "target_amount INTEGER NOT NULL," +
                    "current_progress INTEGER DEFAULT 0," +
                    "completed INTEGER DEFAULT 0," +
                    "FOREIGN KEY (clan_id) REFERENCES clans(id) ON DELETE CASCADE" +
                    ");");

            // Clan Wars Table
            stmt.execute("CREATE TABLE IF NOT EXISTS clan_wars (" +
                    "war_id TEXT PRIMARY KEY," +
                    "clan1_id TEXT NOT NULL," +
                    "clan2_id TEXT NOT NULL," +
                    "clan1_kills INTEGER DEFAULT 0," +
                    "clan2_kills INTEGER DEFAULT 0," +
                    "start_time INTEGER NOT NULL," +
                    "end_time INTEGER NOT NULL," +
                    "status TEXT NOT NULL," +
                    "FOREIGN KEY (clan1_id) REFERENCES clans(id) ON DELETE CASCADE," +
                    "FOREIGN KEY (clan2_id) REFERENCES clans(id) ON DELETE CASCADE" +
                    ");");

            // Clan Perks Table
            stmt.execute("CREATE TABLE IF NOT EXISTS clan_perks (" +
                    "clan_id TEXT NOT NULL," +
                    "perk_id TEXT NOT NULL," +
                    "level INTEGER DEFAULT 1," +
                    "PRIMARY KEY (clan_id, perk_id)," +
                    "FOREIGN KEY (clan_id) REFERENCES clans(id) ON DELETE CASCADE" +
                    ");");

            // Clan Audit Logs Table
            stmt.execute("CREATE TABLE IF NOT EXISTS clan_logs (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "clan_id TEXT NOT NULL," +
                    "player_name TEXT NOT NULL," +
                    "action_type TEXT NOT NULL," +
                    "details TEXT NOT NULL," +
                    "timestamp INTEGER NOT NULL," +
                    "FOREIGN KEY (clan_id) REFERENCES clans(id) ON DELETE CASCADE" +
                    ");");
        }
    }

    public synchronized Map<String, Clan> loadClans() {
        Map<String, Clan> clans = new HashMap<>();
        
        String selectClans = "SELECT * FROM clans;";
        String selectMembers = "SELECT * FROM clan_members;";
        String selectAlliances = "SELECT * FROM clan_alliances;";
        String selectVaults = "SELECT * FROM clan_vaults;";

        try (Statement stmt = connection.createStatement()) {
            // 1. Load Clans
            try (ResultSet rs = stmt.executeQuery(selectClans)) {
                while (rs.next()) {
                    String id = rs.getString("id");
                    String name = rs.getString("name");
                    String tag = rs.getString("tag");
                    UUID ownerUuid = UUID.fromString(rs.getString("owner_uuid"));
                    int level = rs.getInt("level");
                    double balance = rs.getDouble("balance");
                    
                    Location home = null;
                    String worldName = rs.getString("home_world");
                    if (worldName != null) {
                        World world = Bukkit.getWorld(worldName);
                        if (world != null) {
                            double x = rs.getDouble("home_x");
                            double y = rs.getDouble("home_y");
                            double z = rs.getDouble("home_z");
                            float yaw = rs.getFloat("home_yaw");
                            float pitch = rs.getFloat("home_pitch");
                            home = new Location(world, x, y, z, yaw, pitch);
                        }
                    }
                    
                    boolean friendlyFire = rs.getInt("friendly_fire") == 1;
                    
                    // Parse created_at timestamp
                    Timestamp ts = rs.getTimestamp("created_at");
                    long createdAt = ts != null ? ts.getTime() : System.currentTimeMillis();

                    Clan clan = new Clan(id, name, tag, ownerUuid, level, balance, home, friendlyFire, createdAt);
                    clans.put(id, clan);
                }
            }

            // 2. Load Members
            try (ResultSet rs = stmt.executeQuery(selectMembers)) {
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    String clanId = rs.getString("clan_id");
                    ClanRole role = ClanRole.fromString(rs.getString("role"));
                    Timestamp ts = rs.getTimestamp("joined_at");
                    long joinedAt = ts != null ? ts.getTime() : System.currentTimeMillis();

                    Clan clan = clans.get(clanId);
                    if (clan != null) {
                        ClanMember member = new ClanMember(uuid, clanId, role, joinedAt);
                        clan.addMember(member);
                    }
                }
            }

            // 3. Load Alliances
            try (ResultSet rs = stmt.executeQuery(selectAlliances)) {
                while (rs.next()) {
                    String clanId1 = rs.getString("clan_id1");
                    String clanId2 = rs.getString("clan_id2");

                    Clan clan1 = clans.get(clanId1);
                    Clan clan2 = clans.get(clanId2);
                    
                    if (clan1 != null) {
                        clan1.addAlly(clanId2);
                    }
                    if (clan2 != null) {
                        clan2.addAlly(clanId1);
                    }
                }
            }

            // 4. Load Vaults
            try (ResultSet rs = stmt.executeQuery(selectVaults)) {
                while (rs.next()) {
                    String clanId = rs.getString("clan_id");
                    String vaultData = rs.getString("vault_data");

                    Clan clan = clans.get(clanId);
                    if (clan != null) {
                        clan.setVaultData(vaultData);
                    }
                }
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load clans from database", e);
        }

        return clans;
    }

    public synchronized void saveClan(Clan clan) {
        String query = "INSERT OR REPLACE INTO clans (id, name, tag, owner_uuid, level, balance, home_world, home_x, home_y, home_z, home_yaw, home_pitch, friendly_fire) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
        
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, clan.getId());
            pstmt.setString(2, clan.getName());
            pstmt.setString(3, clan.getTag());
            pstmt.setString(4, clan.getOwnerUuid().toString());
            pstmt.setInt(5, clan.getLevel());
            pstmt.setDouble(6, clan.getBalance());
            
            Location home = clan.getHome();
            if (home != null && home.getWorld() != null) {
                pstmt.setString(7, home.getWorld().getName());
                pstmt.setDouble(8, home.getX());
                pstmt.setDouble(9, home.getY());
                pstmt.setDouble(10, home.getZ());
                pstmt.setFloat(11, home.getYaw());
                pstmt.setFloat(12, home.getPitch());
            } else {
                pstmt.setNull(7, Types.VARCHAR);
                pstmt.setNull(8, Types.DOUBLE);
                pstmt.setNull(9, Types.DOUBLE);
                pstmt.setNull(10, Types.DOUBLE);
                pstmt.setNull(11, Types.REAL);
                pstmt.setNull(12, Types.REAL);
            }
            
            pstmt.setInt(13, clan.isFriendlyFireEnabled() ? 1 : 0);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save clan " + clan.getName() + " to database", e);
        }
    }

    public synchronized void deleteClan(String clanId) {
        String deleteClanQuery = "DELETE FROM clans WHERE id = ?;";
        try (PreparedStatement pstmt = connection.prepareStatement(deleteClanQuery)) {
            pstmt.setString(1, clanId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete clan " + clanId + " from database", e);
        }
    }

    public synchronized void saveMember(ClanMember member) {
        String query = "INSERT OR REPLACE INTO clan_members (uuid, clan_id, role) VALUES (?, ?, ?);";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, member.getUuid().toString());
            pstmt.setString(2, member.getClanId());
            pstmt.setString(3, member.getRole().name());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save member " + member.getUuid() + " to database", e);
        }
    }

    public synchronized void removeMember(UUID uuid) {
        String query = "DELETE FROM clan_members WHERE uuid = ?;";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, uuid.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to remove member " + uuid + " from database", e);
        }
    }

    public synchronized void addAlliance(String clanId1, String clanId2) {
        String query = "INSERT OR REPLACE INTO clan_alliances (clan_id1, clan_id2) VALUES (?, ?);";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            // Standardize order to prevent duplicate rows (clan1_clan2 vs clan2_clan1)
            if (clanId1.compareTo(clanId2) < 0) {
                pstmt.setString(1, clanId1);
                pstmt.setString(2, clanId2);
            } else {
                pstmt.setString(1, clanId2);
                pstmt.setString(2, clanId1);
            }
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to add alliance between " + clanId1 + " and " + clanId2, e);
        }
    }

    public synchronized void removeAlliance(String clanId1, String clanId2) {
        String query = "DELETE FROM clan_alliances WHERE (clan_id1 = ? AND clan_id2 = ?) OR (clan_id1 = ? AND clan_id2 = ?);";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, clanId1);
            pstmt.setString(2, clanId2);
            pstmt.setString(3, clanId2);
            pstmt.setString(4, clanId1);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to remove alliance between " + clanId1 + " and " + clanId2, e);
        }
    }

    public synchronized void saveVault(String clanId, String vaultData) {
        String query = "INSERT OR REPLACE INTO clan_vaults (clan_id, vault_data) VALUES (?, ?);";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, clanId);
            pstmt.setString(2, vaultData);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save vault data for clan " + clanId, e);
        }
    }

    // Nexus Methods
    public synchronized Map<String, com.neonclans.model.ClanNexus> loadNexusMap() {
        Map<String, com.neonclans.model.ClanNexus> map = new HashMap<>();
        String query = "SELECT * FROM clan_nexus;";
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                String clanId = rs.getString("clan_id");
                String worldName = rs.getString("world");
                double x = rs.getDouble("x");
                double y = rs.getDouble("y");
                double z = rs.getDouble("z");
                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    Location loc = new Location(world, x, y, z);
                    map.put(clanId, new com.neonclans.model.ClanNexus(clanId, loc));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load clan nexus data", e);
        }
        return map;
    }

    public synchronized void saveNexus(com.neonclans.model.ClanNexus nexus) {
        String query = "INSERT OR REPLACE INTO clan_nexus (clan_id, world, x, y, z) VALUES (?, ?, ?, ?, ?);";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, nexus.getClanId());
            pstmt.setString(2, nexus.getLocation().getWorld().getName());
            pstmt.setDouble(3, nexus.getLocation().getX());
            pstmt.setDouble(4, nexus.getLocation().getY());
            pstmt.setDouble(5, nexus.getLocation().getZ());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save nexus for clan " + nexus.getClanId(), e);
        }
    }

    public synchronized void deleteNexus(String clanId) {
        String query = "DELETE FROM clan_nexus WHERE clan_id = ?;";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, clanId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete nexus for clan " + clanId, e);
        }
    }

    // Quests Methods
    public synchronized Map<String, com.neonclans.model.ClanQuest> loadQuests() {
        Map<String, com.neonclans.model.ClanQuest> map = new HashMap<>();
        String query = "SELECT * FROM clan_quests;";
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                String clanId = rs.getString("clan_id");
                String type = rs.getString("quest_type");
                int target = rs.getInt("target_amount");
                int progress = rs.getInt("current_progress");
                boolean completed = rs.getInt("completed") == 1;
                map.put(clanId, new com.neonclans.model.ClanQuest(clanId, type, target, progress, completed));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load clan quests", e);
        }
        return map;
    }

    public synchronized void saveQuest(com.neonclans.model.ClanQuest quest) {
        String query = "INSERT OR REPLACE INTO clan_quests (clan_id, quest_type, target_amount, current_progress, completed) VALUES (?, ?, ?, ?, ?);";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, quest.getClanId());
            pstmt.setString(2, quest.getQuestType());
            pstmt.setInt(3, quest.getTargetAmount());
            pstmt.setInt(4, quest.getCurrentProgress());
            pstmt.setInt(5, quest.isCompleted() ? 1 : 0);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save quest for clan " + quest.getClanId(), e);
        }
    }

    // Wars Methods
    public synchronized List<com.neonclans.model.ClanWar> loadWars() {
        List<com.neonclans.model.ClanWar> list = new ArrayList<>();
        String query = "SELECT * FROM clan_wars;";
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                String warId = rs.getString("war_id");
                String c1 = rs.getString("clan1_id");
                String c2 = rs.getString("clan2_id");
                int k1 = rs.getInt("clan1_kills");
                int k2 = rs.getInt("clan2_kills");
                long start = rs.getLong("start_time");
                long end = rs.getLong("end_time");
                String status = rs.getString("status");
                list.add(new com.neonclans.model.ClanWar(warId, c1, c2, k1, k2, start, end, status));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load clan wars", e);
        }
        return list;
    }

    public synchronized void saveWar(com.neonclans.model.ClanWar war) {
        String query = "INSERT OR REPLACE INTO clan_wars (war_id, clan1_id, clan2_id, clan1_kills, clan2_kills, start_time, end_time, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, war.getWarId());
            pstmt.setString(2, war.getClan1Id());
            pstmt.setString(3, war.getClan2Id());
            pstmt.setInt(4, war.getClan1Kills());
            pstmt.setInt(5, war.getClan2Kills());
            pstmt.setLong(6, war.getStartTime());
            pstmt.setLong(7, war.getEndTime());
            pstmt.setString(8, war.getStatus());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save war " + war.getWarId(), e);
        }
    }

    // Perks Methods
    public synchronized Map<String, Map<String, com.neonclans.model.ClanPerk>> loadPerks() {
        Map<String, Map<String, com.neonclans.model.ClanPerk>> map = new HashMap<>();
        String query = "SELECT * FROM clan_perks;";
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                String clanId = rs.getString("clan_id");
                String perkId = rs.getString("perk_id");
                int level = rs.getInt("level");
                map.computeIfAbsent(clanId, k -> new HashMap<>()).put(perkId, new com.neonclans.model.ClanPerk(clanId, perkId, level));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load clan perks", e);
        }
        return map;
    }

    public synchronized void savePerk(com.neonclans.model.ClanPerk perk) {
        String query = "INSERT OR REPLACE INTO clan_perks (clan_id, perk_id, level) VALUES (?, ?, ?);";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, perk.getClanId());
            pstmt.setString(2, perk.getPerkId());
            pstmt.setInt(3, perk.getLevel());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save perk " + perk.getPerkId() + " for clan " + perk.getClanId(), e);
        }
    }

    // Audit Logs Methods
    public synchronized void addLog(String clanId, String playerName, String actionType, String details) {
        String query = "INSERT INTO clan_logs (clan_id, player_name, action_type, details, timestamp) VALUES (?, ?, ?, ?, ?);";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, clanId);
            pstmt.setString(2, playerName);
            pstmt.setString(3, actionType);
            pstmt.setString(4, details);
            pstmt.setLong(5, System.currentTimeMillis());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to add audit log for clan " + clanId, e);
        }
    }

    public synchronized List<com.neonclans.model.ClanLog> getLogs(String clanId, int limit) {
        List<com.neonclans.model.ClanLog> list = new ArrayList<>();
        String query = "SELECT * FROM clan_logs WHERE clan_id = ? ORDER BY id DESC LIMIT ?;";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, clanId);
            pstmt.setInt(2, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String pName = rs.getString("player_name");
                    String action = rs.getString("action_type");
                    String details = rs.getString("details");
                    long ts = rs.getLong("timestamp");
                    list.add(new com.neonclans.model.ClanLog(id, clanId, pName, action, details, ts));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to fetch audit logs for clan " + clanId, e);
        }
        return list;
    }
}
