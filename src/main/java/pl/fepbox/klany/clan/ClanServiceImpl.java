package pl.fepbox.klany.clan;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import pl.fepbox.klany.config.PluginConfig;
import pl.fepbox.klany.db.DatabaseManager;
import pl.fepbox.klany.player.PlayerProfileService;
import pl.fepbox.klany.points.PointsService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class ClanServiceImpl implements ClanService {

    private final Plugin plugin;
    private final DatabaseManager databaseManager;
    private final PlayerProfileService profileService;
    private final PointsService pointsService;
    private final PluginConfig config;

    private final Map<UUID, Clan> clansByUuid = new ConcurrentHashMap<>();
    private final Map<String, Clan> clansByTag = new ConcurrentHashMap<>();
    private final Map<UUID, Clan> clansByMember = new ConcurrentHashMap<>();

    public ClanServiceImpl(Plugin plugin,
                           DatabaseManager databaseManager,
                           PlayerProfileService profileService,
                           PointsService pointsService,
                           PluginConfig config) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.profileService = profileService;
        this.pointsService = pointsService;
        this.config = config;
        loadAllClans();
    }

    private void loadAllClans() {
        clansByUuid.clear();
        clansByTag.clear();
        clansByMember.clear();

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id, uuid, tag, name, color, owner_uuid, created_at FROM clans");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                String tag = rs.getString("tag");
                String name = rs.getString("name");
                String color = rs.getString("color");
                UUID ownerUuid = UUID.fromString(rs.getString("owner_uuid"));
                long createdAtSeconds = rs.getLong("created_at");
                Instant createdAt = Instant.ofEpochSecond(createdAtSeconds);

                Clan clan = new Clan(uuid, tag, name, color, ownerUuid, createdAt);
                clansByUuid.put(uuid, clan);
                clansByTag.put(tag.toLowerCase(Locale.ROOT), clan);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Nie udało się załadować klanów z bazy.", e);
        }

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT clan_id, player_uuid, role FROM clan_members");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int clanId = rs.getInt("clan_id");
                String playerUuidStr = rs.getString("player_uuid");
                String roleStr = rs.getString("role");

                UUID playerUuid = UUID.fromString(playerUuidStr);
                ClanRole role = ClanRole.valueOf(roleStr);

                UUID clanUuid = getClanUuidById(conn, clanId);
                if (clanUuid == null) {
                    continue;
                }
                Clan clan = clansByUuid.get(clanUuid);
                if (clan == null) {
                    continue;
                }
                clan.setMemberRole(playerUuid, role);
                clansByMember.put(playerUuid, clan);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Nie udało się załadować członków klanów.", e);
        }

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT clan_id, ally_clan_id FROM clan_allies");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int clanId = rs.getInt("clan_id");
                int allyId = rs.getInt("ally_clan_id");

                UUID clanUuid = getClanUuidById(conn, clanId);
                UUID allyUuid = getClanUuidById(conn, allyId);
                if (clanUuid == null || allyUuid == null) {
                    continue;
                }
                Clan clan = clansByUuid.get(clanUuid);
                Clan ally = clansByUuid.get(allyUuid);
                if (clan == null || ally == null) {
                    continue;
                }
                clan.addAlly(allyUuid);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Nie udało się załadować sojuszy klanów.", e);
        }
    }

    private UUID getClanUuidById(Connection conn, int clanId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT uuid FROM clans WHERE id = ?")) {
            ps.setInt(1, clanId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return UUID.fromString(rs.getString("uuid"));
                }
            }
        }
        return null;
    }

    private Integer getClanIdByUuid(Connection conn, UUID clanUuid) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM clans WHERE uuid = ?")) {
            ps.setString(1, clanUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        return null;
    }

    @Override
    public Optional<Clan> getClanByTag(String tag) {
        if (tag == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(clansByTag.get(tag.toLowerCase(Locale.ROOT)));
    }

    @Override
    public Optional<Clan> getClanByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return clansByUuid.values().stream()
                .filter(c -> c.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    @Override
    public Optional<Clan> getClanByPlayer(UUID uuid) {
        return Optional.ofNullable(clansByMember.get(uuid));
    }

    @Override
    public Clan createClan(Player creator, String tag, String name, String color) {
        UUID clanUuid = UUID.randomUUID();
        Instant now = Instant.now();
        Clan clan = new Clan(clanUuid, tag, name, color, creator.getUniqueId(), now);
        clan.setMemberRole(creator.getUniqueId(), ClanRole.LEADER);

        clansByUuid.put(clanUuid, clan);
        clansByTag.put(tag.toLowerCase(Locale.ROOT), clan);
        clansByMember.put(creator.getUniqueId(), clan);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = databaseManager.getConnection()) {
                Integer clanId;
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO clans(uuid, tag, name, color, owner_uuid, created_at) VALUES(?,?,?,?,?,?)"
                )) {
                    ps.setString(1, clanUuid.toString());
                    ps.setString(2, tag);
                    ps.setString(3, name);
                    ps.setString(4, color);
                    ps.setString(5, creator.getUniqueId().toString());
                    ps.setLong(6, now.getEpochSecond());
                    ps.executeUpdate();
                }
                clanId = getClanIdByUuid(conn, clanUuid);
                if (clanId == null) {
                    return;
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO clan_members(clan_id, player_uuid, role, joined_at) VALUES(?,?,?,?)"
                )) {
                    ps.setInt(1, clanId);
                    ps.setString(2, creator.getUniqueId().toString());
                    ps.setString(3, ClanRole.LEADER.name());
                    ps.setLong(4, now.getEpochSecond());
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Nie udało się zapisać nowego klanu.", e);
            }
        });

        return clan;
    }

    @Override
    public void invitePlayer(Clan clan, Player inviter, OfflinePlayer target) {
        if (target.isOnline()) {
            Player online = target.getPlayer();
            if (online != null) {
                online.sendMessage("Zostałeś zaproszony do klanu " + clan.getTag() + " przez " + inviter.getName() + ".");
            }
        }
        joinClanDirect(clan, target.getUniqueId());
    }

    @Override
    public void joinClan(Player player, String tagOrName) {
        Optional<Clan> clanOpt = getClanByTag(tagOrName);
        if (clanOpt.isEmpty()) {
            clanOpt = getClanByName(tagOrName);
        }
        clanOpt.ifPresent(clan -> joinClanDirect(clan, player.getUniqueId()));
    }

    private void joinClanDirect(Clan clan, UUID playerUuid) {
        clan.setMemberRole(playerUuid, ClanRole.MEMBER);
        clansByMember.put(playerUuid, clan);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = databaseManager.getConnection()) {
                Integer clanId = getClanIdByUuid(conn, clan.getUuid());
                if (clanId == null) {
                    return;
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT OR REPLACE INTO clan_members(clan_id, player_uuid, role, joined_at) VALUES(?,?,?,?)"
                )) {
                    ps.setInt(1, clanId);
                    ps.setString(2, playerUuid.toString());
                    ps.setString(3, ClanRole.MEMBER.name());
                    ps.setLong(4, Instant.now().getEpochSecond());
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Nie udało się dołączyć gracza do klanu.", e);
            }
        });
    }

    @Override
    public void leaveClan(Player player) {
        Clan clan = clansByMember.remove(player.getUniqueId());
        if (clan == null) {
            return;
        }
        clan.removeMember(player.getUniqueId());

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "DELETE FROM clan_members WHERE player_uuid = ?"
                 )) {
                ps.setString(1, player.getUniqueId().toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Nie udało się usunąć gracza z klanu.", e);
            }
        });
    }

    @Override
    public void kickMember(Player actor, OfflinePlayer target) {
        Clan clan = clansByMember.get(actor.getUniqueId());
        if (clan == null) {
            return;
        }

        clansByMember.remove(target.getUniqueId());
        clan.removeMember(target.getUniqueId());

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "DELETE FROM clan_members WHERE player_uuid = ?"
                 )) {
                ps.setString(1, target.getUniqueId().toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Nie udało się wyrzucić gracza z klanu.", e);
            }
        });
    }

    @Override
    public void dissolveClan(Clan clan) {
        UUID clanUuid = clan.getUuid();
        String tagKey = clan.getTag().toLowerCase(Locale.ROOT);

        for (UUID memberUuid : new ArrayList<>(clan.getMembers().keySet())) {
            clansByMember.remove(memberUuid);
        }

        for (Clan other : clansByUuid.values()) {
            if (!other.equals(clan)) {
                other.removeAlly(clanUuid);
            }
        }

        clansByUuid.remove(clanUuid);
        clansByTag.remove(tagKey);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = databaseManager.getConnection()) {
                Integer clanId = getClanIdByUuid(conn, clanUuid);
                if (clanId == null) {
                    return;
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM clan_members WHERE clan_id = ?"
                )) {
                    ps.setInt(1, clanId);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM clan_allies WHERE clan_id = ? OR ally_clan_id = ?"
                )) {
                    ps.setInt(1, clanId);
                    ps.setInt(2, clanId);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM clans WHERE id = ?"
                )) {
                    ps.setInt(1, clanId);
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Nie udało się rozwiązać klanu w bazie danych.", e);
            }
        });
    }

    @Override
    public void setAlliance(Clan clan, Clan otherClan, boolean allied) {
        if (clan.getUuid().equals(otherClan.getUuid())) {
            return;
        }

        UUID clanUuid = clan.getUuid();
        UUID otherUuid = otherClan.getUuid();

        if (allied) {
            clan.addAlly(otherUuid);
            otherClan.addAlly(clanUuid);
        } else {
            clan.removeAlly(otherUuid);
            otherClan.removeAlly(clanUuid);
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = databaseManager.getConnection()) {
                Integer clanId = getClanIdByUuid(conn, clanUuid);
                Integer otherId = getClanIdByUuid(conn, otherUuid);
                if (clanId == null || otherId == null) {
                    return;
                }
                if (allied) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT OR IGNORE INTO clan_allies(clan_id, ally_clan_id) VALUES(?,?)"
                    )) {
                        ps.setInt(1, clanId);
                        ps.setInt(2, otherId);
                        ps.executeUpdate();
                    }
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT OR IGNORE INTO clan_allies(clan_id, ally_clan_id) VALUES(?,?)"
                    )) {
                        ps.setInt(1, otherId);
                        ps.setInt(2, clanId);
                        ps.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "DELETE FROM clan_allies WHERE (clan_id = ? AND ally_clan_id = ?) OR (clan_id = ? AND ally_clan_id = ?)"
                    )) {
                        ps.setInt(1, clanId);
                        ps.setInt(2, otherId);
                        ps.setInt(3, otherId);
                        ps.setInt(4, clanId);
                        ps.executeUpdate();
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Nie udało się zaktualizować sojuszu klanów.", e);
            }
        });
    }

    @Override
    public boolean areAllied(Clan clan, Clan otherClan) {
        return clan.getAllies().contains(otherClan.getUuid());
    }

    @Override
    public void updateClanColor(Clan clan, String color) {
        clan.setColor(color);
        UUID clanUuid = clan.getUuid();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "UPDATE clans SET color = ? WHERE uuid = ?"
                 )) {
                ps.setString(1, color);
                ps.setString(2, clanUuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Nie udało się zaktualizować koloru klanu w bazie danych.", e);
            }
        });
    }

    @Override
    public List<Clan> getTopClans(int page, int pageSize) {
        List<Clan> all = new ArrayList<>(clansByUuid.values());
        all.sort(Comparator.comparingDouble(this::getClanAveragePoints).reversed());
        int from = Math.max(0, (page - 1) * pageSize);
        int to = Math.min(all.size(), from + pageSize);
        if (from >= to) {
            return Collections.emptyList();
        }
        return all.subList(from, to);
    }

    @Override
    public double getClanAveragePoints(Clan clan) {
        if (clan.getMembers().isEmpty()) {
            return 0.0;
        }
        int sum = 0;
        for (UUID memberUuid : clan.getMembers().keySet()) {
            sum += pointsService.getPoints(memberUuid);
        }
        return sum / (double) clan.getMembers().size();
    }

    @Override
    public void removeMemberFromClan(UUID playerUuid) {
        Clan clan = clansByMember.remove(playerUuid);
        if (clan == null) {
            return;
        }
        clan.removeMember(playerUuid);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "DELETE FROM clan_members WHERE player_uuid = ?"
                 )) {
                ps.setString(1, playerUuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Nie udało się usunąć gracza z klanu (admin).", e);
            }
        });
    }

    @Override
    public void renameClan(Clan clan, String newName) {
        clan.setName(newName);
        UUID clanUuid = clan.getUuid();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "UPDATE clans SET name = ? WHERE uuid = ?"
                 )) {
                ps.setString(1, newName);
                ps.setString(2, clanUuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Nie udało się zmienić nazwy klanu w bazie danych.", e);
            }
        });
    }

    @Override
    public void retagClan(Clan clan, String newTag) {
        String oldKey = clan.getTag().toLowerCase(Locale.ROOT);
        String newKey = newTag.toLowerCase(Locale.ROOT);

        clansByTag.remove(oldKey);
        clan.setTag(newTag);
        clansByTag.put(newKey, clan);

        UUID clanUuid = clan.getUuid();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "UPDATE clans SET tag = ? WHERE uuid = ?"
                 )) {
                ps.setString(1, newTag);
                ps.setString(2, clanUuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Nie udało się zmienić tagu klanu w bazie danych.", e);
            }
        });
    }
}

