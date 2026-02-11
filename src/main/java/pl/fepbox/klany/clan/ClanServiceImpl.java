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
import java.util.*;
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
    // Caches used for tab-complete so we can drop stale entries after rename/retag.
    private final Set<String> cachedTags = ConcurrentHashMap.newKeySet();
    private final Set<String> cachedNames = ConcurrentHashMap.newKeySet();

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
        cachedTags.clear();
        cachedNames.clear();

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id, uuid, tag, name, color, owner_uuid, created_at FROM clans");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                String tag = rs.getString("tag");
                String name = rs.getString("name");
                String color = rs.getString("color");
                UUID ownerUuid = UUID.fromString(rs.getString("owner_uuid"));
                Instant createdAt = Instant.ofEpochSecond(rs.getLong("created_at"));
                Clan clan = new Clan(uuid, tag, name, color, ownerUuid, createdAt);
                clansByUuid.put(uuid, clan);
                clansByTag.put(tag.toLowerCase(Locale.ROOT), clan);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Nie udalo sie zaladowac klanw z bazy.", e);
        }

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT clan_id, player_uuid, role, rank_name FROM clan_members");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int clanId = rs.getInt("clan_id");
                UUID playerUuid = UUID.fromString(rs.getString("player_uuid"));
                ClanRole role = ClanRole.valueOf(rs.getString("role"));
                String rankName = rs.getString("rank_name");

                UUID clanUuid = getClanUuidById(conn, clanId);
                if (clanUuid == null) continue;
                Clan clan = clansByUuid.get(clanUuid);
                if (clan == null) continue;

                clan.setMemberRole(playerUuid, role);
                clansByMember.put(playerUuid, clan);
                if (rankName != null) clan.setMemberRank(playerUuid, rankName);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Nie udalo sie zaladowac czlonkw klanw.", e);
        }

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT clan_id, ally_clan_id FROM clan_allies");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int clanId = rs.getInt("clan_id");
                int allyId = rs.getInt("ally_clan_id");
                UUID clanUuid = getClanUuidById(conn, clanId);
                UUID allyUuid = getClanUuidById(conn, allyId);
                if (clanUuid == null || allyUuid == null) continue;
                Clan clan = clansByUuid.get(clanUuid);
                Clan ally = clansByUuid.get(allyUuid);
                if (clan == null || ally == null) continue;
                clan.addAlly(allyUuid);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Nie udalo sie zaladowac sojuszy klanw.", e);
        }

        // load ranks permissions
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT clan_id, rank_name, permission FROM clan_rank_permissions");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int clanId = rs.getInt("clan_id");
                String rankName = rs.getString("rank_name");
                String perm = rs.getString("permission");
                UUID clanUuid = getClanUuidById(conn, clanId);
                if (clanUuid == null) continue;
                Clan clan = clansByUuid.get(clanUuid);
                if (clan == null) continue;
                ClanRank rank = clan.getRank(rankName);
                if (rank == null) {
                    rank = new ClanRank(rankName, 0, EnumSet.noneOf(ClanPermission.class));
                    clan.setRank(rank);
                }
                try {
                    rank.getPermissions().add(ClanPermission.valueOf(perm));
                } catch (IllegalArgumentException ignored) {}
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Nie udalo sie zaladowac rang klanw.", e);
        }

        // defaults
        for (Clan clan : clansByUuid.values()) {
            ensureDefaultRanks(clan);
            if (clan.getMemberRank(clan.getOwnerUuid()) == null) {
                clan.setMemberRank(clan.getOwnerUuid(), "Zalozyciel");
            }
            for (UUID uid : clan.getMembers().keySet()) {
                if (clan.getMemberRank(uid) == null) clan.setMemberRank(uid, "Czlonek");
            }
        }
        rebuildCaches();
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
        if (tag == null) return Optional.empty();
        return Optional.ofNullable(clansByTag.get(tag.toLowerCase(Locale.ROOT)));
    }

    @Override
    public Optional<Clan> getClanByName(String name) {
        if (name == null) return Optional.empty();
        return clansByUuid.values().stream().filter(c -> c.getName().equalsIgnoreCase(name)).findFirst();
    }

    @Override
    public Optional<Clan> getClanByPlayer(UUID uuid) {
        return Optional.ofNullable(clansByMember.get(uuid));
    }

    @Override
    public Optional<Clan> getClanByUuid(UUID uuid) {
        return Optional.ofNullable(clansByUuid.get(uuid));
    }

    @Override
    public List<String> getAllTags() {
        return new ArrayList<>(cachedTags);
    }

    @Override
    public List<String> getAllNames() {
        return new ArrayList<>(cachedNames);
    }

    @Override
    public Clan createClan(Player creator, String tag, String name, String color) {
        UUID clanUuid = UUID.randomUUID();
        Instant now = Instant.now();
        Clan clan = new Clan(clanUuid, tag, name, color, creator.getUniqueId(), now);
        clan.setMemberRole(creator.getUniqueId(), ClanRole.LEADER);
        ensureDefaultRanks(clan);
        clan.setMemberRank(creator.getUniqueId(), "Zalozyciel");

        clansByUuid.put(clanUuid, clan);
        clansByTag.put(tag.toLowerCase(Locale.ROOT), clan);
        clansByMember.put(creator.getUniqueId(), clan);
        rebuildCaches();

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
                if (clanId == null) return;
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO clan_members(clan_id, player_uuid, role, rank_name, joined_at) VALUES(?,?,?,?,?)"
                )) {
                    ps.setInt(1, clanId);
                    ps.setString(2, creator.getUniqueId().toString());
                    ps.setString(3, ClanRole.LEADER.name());
                    ps.setString(4, "Zalozyciel");
                    ps.setLong(5, now.getEpochSecond());
                    ps.executeUpdate();
                }
                saveRanksAsync(clan);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Nie udalo sie zapisac nowego klanu.", e);
            }
        });
        return clan;
    }

    @Override
    public void invitePlayer(Clan clan, Player inviter, OfflinePlayer target) {
        if (target.isOnline()) {
            Player online = target.getPlayer();
            if (online != null) {
                online.sendMessage("Zostales zaproszony do klanu " + clan.getTag() + " przez " + inviter.getName() + ".");
            }
        }
        joinClanDirect(clan, target.getUniqueId());
    }

    @Override
    public void joinClan(Player player, String tagOrName) {
        Optional<Clan> clanOpt = getClanByTag(tagOrName);
        if (clanOpt.isEmpty()) clanOpt = getClanByName(tagOrName);
        clanOpt.ifPresent(clan -> joinClanDirect(clan, player.getUniqueId()));
    }

    private void joinClanDirect(Clan clan, UUID playerUuid) {
        clan.setMemberRole(playerUuid, ClanRole.MEMBER);
        clan.setMemberRank(playerUuid, "Czlonek");
        clansByMember.put(playerUuid, clan);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = databaseManager.getConnection()) {
                Integer clanId = getClanIdByUuid(conn, clan.getUuid());
                if (clanId == null) return;
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT OR REPLACE INTO clan_members(clan_id, player_uuid, role, rank_name, joined_at) VALUES(?,?,?,?,?)"
                )) {
                    ps.setInt(1, clanId);
                    ps.setString(2, playerUuid.toString());
                    ps.setString(3, ClanRole.MEMBER.name());
                    ps.setString(4, "Czlonek");
                    ps.setLong(5, Instant.now().getEpochSecond());
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Nie udalo sie dolaczyc gracza do klanu.", e);
            }
        });
    }

    @Override
    public void leaveClan(Player player) {
        Clan clan = clansByMember.remove(player.getUniqueId());
        if (clan == null) return;
        clan.removeMember(player.getUniqueId());

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM clan_members WHERE player_uuid = ?")) {
                ps.setString(1, player.getUniqueId().toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Nie udalo sie usunac gracza z klanu.", e);
            }
        });
    }

    @Override
    public void kickMember(Player actor, OfflinePlayer target) {
        Clan clan = clansByMember.get(actor.getUniqueId());
        if (clan == null) return;

        clansByMember.remove(target.getUniqueId());
        clan.removeMember(target.getUniqueId());

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM clan_members WHERE player_uuid = ?")) {
                ps.setString(1, target.getUniqueId().toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Nie udalo sie wyrzucic gracza z klanu.", e);
            }
        });
    }

    @Override
    public void dissolveClan(Clan clan) {
        UUID clanUuid = clan.getUuid();
        String tagKey = clan.getTag().toLowerCase(Locale.ROOT);

        for (UUID memberUuid : new ArrayList<>(clan.getMembers().keySet())) clansByMember.remove(memberUuid);
        for (Clan other : clansByUuid.values()) if (!other.equals(clan)) other.removeAlly(clanUuid);

        clansByUuid.remove(clanUuid);
        clansByTag.remove(tagKey);
        rebuildCaches();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = databaseManager.getConnection()) {
                Integer clanId = getClanIdByUuid(conn, clanUuid);
                if (clanId == null) return;
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM clan_members WHERE clan_id = ?")) {
                    ps.setInt(1, clanId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM clan_allies WHERE clan_id = ? OR ally_clan_id = ?")) {
                    ps.setInt(1, clanId);
                    ps.setInt(2, clanId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM clan_rank_permissions WHERE clan_id = ?")) {
                    ps.setInt(1, clanId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM clans WHERE id = ?")) {
                    ps.setInt(1, clanId);
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Nie udalo sie rozwiazac klanu w bazie danych.", e);
            }
        });
    }

    @Override
    public void setAlliance(Clan clan, Clan otherClan, boolean allied) {
        if (clan.getUuid().equals(otherClan.getUuid())) return;
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
                if (clanId == null || otherId == null) return;
                if (allied) {
                    try (PreparedStatement ps = conn.prepareStatement("INSERT OR IGNORE INTO clan_allies(clan_id, ally_clan_id) VALUES(?,?)")) {
                        ps.setInt(1, clanId);
                        ps.setInt(2, otherId);
                        ps.executeUpdate();
                    }
                    try (PreparedStatement ps = conn.prepareStatement("INSERT OR IGNORE INTO clan_allies(clan_id, ally_clan_id) VALUES(?,?)")) {
                        ps.setInt(1, otherId);
                        ps.setInt(2, clanId);
                        ps.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = conn.prepareStatement("DELETE FROM clan_allies WHERE (clan_id = ? AND ally_clan_id = ?) OR (clan_id = ? AND ally_clan_id = ?)")) {
                        ps.setInt(1, clanId);
                        ps.setInt(2, otherId);
                        ps.setInt(3, otherId);
                        ps.setInt(4, clanId);
                        ps.executeUpdate();
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Nie udalo sie zaktualizowac sojuszu klanw.", e);
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
                 PreparedStatement ps = conn.prepareStatement("UPDATE clans SET color = ? WHERE uuid = ?")) {
                ps.setString(1, color);
                ps.setString(2, clanUuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Nie udalo sie zaktualizowac koloru klanu w bazie danych.", e);
            }
        });
    }

    @Override
    public List<Clan> getTopClans(int page, int pageSize) {
        List<Clan> all = new ArrayList<>(clansByUuid.values());
        all.sort(Comparator.comparingDouble(this::getClanAveragePoints).reversed());
        int from = Math.max(0, (page - 1) * pageSize);
        int to = Math.min(all.size(), from + pageSize);
        if (from >= to) return Collections.emptyList();
        return all.subList(from, to);
    }

    @Override
    public double getClanAveragePoints(Clan clan) {
        if (clan.getMembers().isEmpty()) return 0.0;
        int sum = 0;
        for (UUID memberUuid : clan.getMembers().keySet()) sum += pointsService.getPoints(memberUuid);
        return sum / (double) clan.getMembers().size();
    }

    @Override
    public void removeMemberFromClan(UUID playerUuid) {
        Clan clan = clansByMember.remove(playerUuid);
        if (clan == null) return;
        clan.removeMember(playerUuid);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM clan_members WHERE player_uuid = ?")) {
                ps.setString(1, playerUuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Nie udalo sie usunac gracza z klanu (admin).", e);
            }
        });
    }

    @Override
    public void renameClan(Clan clan, String newName) {
        clan.setName(newName);
        UUID clanUuid = clan.getUuid();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement("UPDATE clans SET name = ? WHERE uuid = ?")) {
                ps.setString(1, newName);
                ps.setString(2, clanUuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Nie udalo sie zmienic nazwy klanu w bazie danych.", e);
            }
        });
        rebuildCaches();
    }

    @Override
    public void retagClan(Clan clan, String newTag) {
        String oldKey = clan.getTag().toLowerCase(Locale.ROOT);
        String newKey = newTag.toLowerCase(Locale.ROOT);
        clansByTag.remove(oldKey);
        clan.setTag(newTag);
        clansByTag.put(newKey, clan);
        rebuildCaches();

        UUID clanUuid = clan.getUuid();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement("UPDATE clans SET tag = ? WHERE uuid = ?")) {
                ps.setString(1, newTag);
                ps.setString(2, clanUuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Nie udalo sie zmienic tagu klanu w bazie danych.", e);
            }
        });
    }

    // ------- RANKS --------
    private void ensureDefaultRanks(Clan clan) {
        if (!clan.getRanks().isEmpty()) return;
        EnumSet<ClanPermission> all = EnumSet.allOf(ClanPermission.class);
        ClanRank founder = new ClanRank("Zalozyciel", 100, all);
        EnumSet<ClanPermission> dep = EnumSet.allOf(ClanPermission.class);
        dep.remove(ClanPermission.MANAGE_RANKS);
        ClanRank deputy = new ClanRank("Zastepca", 80, dep);
        ClanRank member = new ClanRank("Czlonek", 10, EnumSet.of(ClanPermission.INVITE));
        clan.setRank(founder);
        clan.setRank(deputy);
        clan.setRank(member);
    }

    private void saveRanksAsync(Clan clan) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = databaseManager.getConnection()) {
                Integer clanId = getClanIdByUuid(conn, clan.getUuid());
                if (clanId == null) return;
                try (PreparedStatement del = conn.prepareStatement("DELETE FROM clan_rank_permissions WHERE clan_id = ?")) {
                    del.setInt(1, clanId);
                    del.executeUpdate();
                }
                try (PreparedStatement ins = conn.prepareStatement("INSERT INTO clan_rank_permissions(clan_id, rank_name, permission) VALUES(?,?,?)")) {
                    for (ClanRank rank : clan.getRanks().values()) {
                        for (ClanPermission perm : rank.getPermissions()) {
                            ins.setInt(1, clanId);
                            ins.setString(2, rank.getName());
                            ins.setString(3, perm.name());
                            ins.addBatch();
                        }
                    }
                    ins.executeBatch();
                }
                try (PreparedStatement upd = conn.prepareStatement("UPDATE clan_members SET rank_name = ? WHERE clan_id = ? AND player_uuid = ?")) {
                    for (Map.Entry<UUID, String> e : clan.getMemberRanks().entrySet()) {
                        upd.setString(1, e.getValue());
                        upd.setInt(2, clanId);
                        upd.setString(3, e.getKey().toString());
                        upd.addBatch();
                    }
                    upd.executeBatch();
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Nie udalo sie zapisac rang klanu.", e);
            }
        });
    }

    @Override
    public ClanRank createRank(Clan clan, String name, int weight) {
        ClanRank rank = new ClanRank(name, weight, EnumSet.noneOf(ClanPermission.class));
        clan.setRank(rank);
        saveRanksAsync(clan);
        return rank;
    }

    @Override
    public boolean deleteRank(Clan clan, String name) {
        if (name.equalsIgnoreCase("Zalozyciel") || name.equalsIgnoreCase("Zastepca") || name.equalsIgnoreCase("Czlonek")) return false;
        clan.removeRank(name);
        clan.getMemberRanks().forEach((uuid, r) -> {
            if (r != null && r.equalsIgnoreCase(name)) clan.setMemberRank(uuid, "Czlonek");
        });
        saveRanksAsync(clan);
        return true;
    }

    @Override
    public boolean togglePermission(Clan clan, String rankName, ClanPermission permission) {
        ClanRank rank = clan.getRank(rankName);
        if (rank == null) return false;
        rank.toggle(permission);
        saveRanksAsync(clan);
        return true;
    }

    @Override
    public boolean assignRank(Clan clan, UUID playerUuid, String rankName) {
        ClanRank rank = clan.getRank(rankName);
        if (rank == null) return false;
        clan.setMemberRank(playerUuid, rank.getName());
        saveRanksAsync(clan);
        return true;
    }

    @Override
    public ClanRank getRank(Clan clan, String name) {
        return clan.getRank(name);
    }

    // limits & pvp placeholders (not implemented in this branch)
    @Override
    public int getMemberLimit(Clan clan) { return Integer.MAX_VALUE; }

    @Override
    public int getAllyLimit(Clan clan) { return Integer.MAX_VALUE; }

    @Override
    public boolean togglePvp(Clan clan, boolean enabled) { clan.setPvpEnabled(enabled); return true; }

    @Override
    public boolean tryUpgrade(Clan clan, ClanUpgradeType type, Player payer) { return false; }

    /**
     * Rebuilds cached tag/name lists used for tab-complete so that stale
     * identifiers disappear immediately after rename/retag/dissolve/create.
     */
    private void rebuildCaches() {
        cachedTags.clear();
        cachedNames.clear();
        for (Clan c : clansByTag.values()) {
            if (c.getTag() != null) cachedTags.add(c.getTag());
        }
        for (Clan c : clansByUuid.values()) {
            if (c.getName() != null) cachedNames.add(c.getName());
        }
    }
}

