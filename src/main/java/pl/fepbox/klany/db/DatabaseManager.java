package pl.fepbox.klany.db;

import org.bukkit.plugin.Plugin;
import pl.fepbox.klany.config.StorageConfig;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

public class DatabaseManager {

    private final Plugin plugin;
    private final StorageConfig storageConfig;
    private Connection connection;

    public DatabaseManager(Plugin plugin, StorageConfig storageConfig) {
        this.plugin = plugin;
        this.storageConfig = storageConfig;
    }

    public synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection("jdbc:sqlite:" + storageConfig.getFile());
        }
        return connection;
    }

    public void initialize() throws SQLException {
        Logger log = plugin.getLogger();
        if (!"sqlite".equalsIgnoreCase(storageConfig.getType())) {
            throw new SQLException("Unsupported storage type: " + storageConfig.getType());
        }

        File dbFile = new File(storageConfig.getFile());
        File parent = dbFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new SQLException("Nie udalo sie utworzyc katalogu bazy danych: " + parent);
        }

        try (Connection conn = getConnection(); Statement st = conn.createStatement()) {
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS players (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        uuid TEXT UNIQUE NOT NULL,
                        name TEXT NOT NULL,
                        points INTEGER NOT NULL,
                        created_at INTEGER
                    )
                    """);
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS clans (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        uuid TEXT UNIQUE NOT NULL,
                        tag TEXT UNIQUE NOT NULL,
                        name TEXT NOT NULL,
                        color TEXT NOT NULL,
                        owner_uuid TEXT NOT NULL,
                        created_at INTEGER,
                        pvp_enabled INTEGER DEFAULT 1,
                        member_level INTEGER DEFAULT 0,
                        ally_level INTEGER DEFAULT 0
                    )
                    """);
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS clan_members (
                        clan_id INTEGER NOT NULL,
                        player_uuid TEXT NOT NULL,
                        role TEXT NOT NULL,
                        rank_name TEXT,
                        joined_at INTEGER,
                        PRIMARY KEY (clan_id, player_uuid)
                    )
                    """);
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS clan_allies (
                        clan_id INTEGER NOT NULL,
                        ally_clan_id INTEGER NOT NULL,
                        PRIMARY KEY (clan_id, ally_clan_id)
                    )
                    """);
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS clan_rank_permissions (
                        clan_id INTEGER NOT NULL,
                        rank_name TEXT NOT NULL,
                        permission TEXT NOT NULL,
                        PRIMARY KEY (clan_id, rank_name, permission)
                    )
                    """);

            ensureColumn(st, "clans", "pvp_enabled", "INTEGER DEFAULT 1");
            ensureColumn(st, "clans", "member_level", "INTEGER DEFAULT 0");
            ensureColumn(st, "clans", "ally_level", "INTEGER DEFAULT 0");
            ensureColumn(st, "clan_members", "rank_name", "TEXT");
        }
        log.info("Zainicjalizowano baze danych SQLite dla Fepbox-Klany.");
    }

    private void ensureColumn(Statement st, String table, String column, String definition) throws SQLException {
        String existsQuery = "SELECT 1 FROM pragma_table_info('" + table + "') WHERE name='" + column + "'";
        try (var rs = st.executeQuery(existsQuery)) {
            if (!rs.next()) {
                st.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
            }
        }
    }

    public void shutdown() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            try {
                connection.close();
            } finally {
                connection = null;
            }
        }
    }
}
