package pl.fepbox.klany.points;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.event.entity.EntityDamageEvent;
import pl.fepbox.klany.config.PointsConfig;
import pl.fepbox.klany.db.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class PointsServiceImpl implements PointsService {

    private final Plugin plugin;
    private final DatabaseManager databaseManager;
    private final PointsConfig config;
    private final Map<UUID, Integer> cache = new ConcurrentHashMap<>();
    private final Map<String, Long> lastKillTimestamps = new ConcurrentHashMap<>();

    public PointsServiceImpl(Plugin plugin, DatabaseManager databaseManager, PointsConfig config) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.config = config;
    }

    @Override
    public int getPoints(UUID uuid) {
        Integer cached = cache.get(uuid);
        if (cached != null) {
            return cached;
        }
        int points = config.getStartPoints();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT points FROM players WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    points = rs.getInt("points");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Nie udało się pobrać punktów dla " + uuid, e);
        }
        cache.put(uuid, points);
        return points;
    }

    @Override
    public void setPoints(UUID uuid, int value) {
        int clamped = clamp(value);
        cache.put(uuid, clamped);
        saveAsync(uuid, clamped);
    }

    @Override
    public void addPoints(UUID uuid, int delta) {
        int current = getPoints(uuid);
        setPoints(uuid, current + delta);
    }

    @Override
    public KillResult applyKill(UUID killerUuid, UUID victimUuid) {
        int killerPoints = getPoints(killerUuid);
        int victimPoints = getPoints(victimUuid);

        String key = killerUuid.toString() + ":" + victimUuid;
        long now = System.currentTimeMillis();
        long windowMillis = 30L * 60L * 1000L; // 30 minut
        Long last = lastKillTimestamps.get(key);
        if (last != null && (now - last) < windowMillis) {
            return new KillResult(0, 0, killerPoints, victimPoints);
        }

        int delta = computeKillDelta(killerPoints, victimPoints);
        int killerAfter = clamp(killerPoints + delta);
        int victimDelta = -delta;
        int victimAfter = clamp(victimPoints + victimDelta);

        cache.put(killerUuid, killerAfter);
        cache.put(victimUuid, victimAfter);

        saveAsync(killerUuid, killerAfter);
        saveAsync(victimUuid, victimAfter);

        lastKillTimestamps.put(key, now);

        return new KillResult(delta, victimDelta, killerAfter, victimAfter);
    }

    @Override
    public int applySelfDeath(UUID uuid, EntityDamageEvent.DamageCause cause) {
        int current = getPoints(uuid);
        int loss = config.getSelfDeathLoss(cause);
        int after = clamp(current - loss);
        cache.put(uuid, after);
        saveAsync(uuid, after);
        return loss;
    }

    private int computeKillDelta(int killerPoints, int victimPoints) {
        double diff = victimPoints - killerPoints;
        double value = config.getBaseReward() + config.getFactor() * diff;
        int rounded = (int) Math.round(value);
        if (rounded < config.getMinChange()) {
            rounded = config.getMinChange();
        }
        if (rounded > config.getMaxChange()) {
            rounded = config.getMaxChange();
        }
        return rounded;
    }

    private int clamp(int value) {
        if (value < config.getMinPoints()) {
            return config.getMinPoints();
        }
        if (value > config.getMaxPoints()) {
            return config.getMaxPoints();
        }
        return value;
    }

    private void saveAsync(UUID uuid, int points) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement("UPDATE players SET points = ? WHERE uuid = ?")) {
                ps.setInt(1, points);
                ps.setString(2, uuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Nie udało się zapisać punktów dla " + uuid, e);
            }
        });
    }
}
