package pl.fepbox.klany.player;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import pl.fepbox.klany.config.PointsConfig;
import pl.fepbox.klany.db.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class PlayerProfileServiceImpl implements PlayerProfileService {

    private final Plugin plugin;
    private final DatabaseManager databaseManager;
    private final PointsConfig pointsConfig;
    private final Map<UUID, PlayerProfile> cache = new ConcurrentHashMap<>();

    public PlayerProfileServiceImpl(Plugin plugin, DatabaseManager databaseManager, PointsConfig pointsConfig) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.pointsConfig = pointsConfig;
    }

    @Override
    public PlayerProfile getOrCreateProfile(UUID uuid, String name) {
        PlayerProfile cached = cache.get(uuid);
        if (cached != null) {
            if (!cached.getName().equals(name)) {
                cached.setName(name);
                saveProfileAsync(cached);
            }
            return cached;
        }

        PlayerProfile profile = loadProfile(uuid, name);
        cache.put(uuid, profile);
        return profile;
    }

    private PlayerProfile loadProfile(UUID uuid, String name) {
        try (Connection conn = databaseManager.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT name, points FROM players WHERE uuid = ?"
            )) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String storedName = rs.getString("name");
                        int points = rs.getInt("points");
                        if (!storedName.equals(name)) {
                            try (PreparedStatement upd = conn.prepareStatement(
                                    "UPDATE players SET name = ? WHERE uuid = ?"
                            )) {
                                upd.setString(1, name);
                                upd.setString(2, uuid.toString());
                                upd.executeUpdate();
                            }
                        }
                        return new PlayerProfile(uuid, name, points);
                    }
                }
            }

            int initialPoints = pointsConfig.getStartPoints();
            try (PreparedStatement ins = conn.prepareStatement(
                    "INSERT INTO players(uuid, name, points, created_at) VALUES(?,?,?,?)"
            )) {
                ins.setString(1, uuid.toString());
                ins.setString(2, name);
                ins.setInt(3, initialPoints);
                ins.setLong(4, Instant.now().getEpochSecond());
                ins.executeUpdate();
            }
            return new PlayerProfile(uuid, name, initialPoints);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Nie udało się załadować profilu gracza " + name, e);
            return new PlayerProfile(uuid, name, pointsConfig.getStartPoints());
        }
    }

    @Override
    public void saveProfileAsync(PlayerProfile profile) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "UPDATE players SET name = ?, points = ? WHERE uuid = ?"
                 )) {
                ps.setString(1, profile.getName());
                ps.setInt(2, profile.getPoints());
                ps.setString(3, profile.getUuid().toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Nie udało się zapisać profilu gracza " + profile.getName(), e);
            }
        });
    }
}

