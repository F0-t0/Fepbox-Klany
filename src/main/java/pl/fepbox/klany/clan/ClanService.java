package pl.fepbox.klany.clan;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public interface ClanService {

    Optional<Clan> getClanByTag(String tag);

    Optional<Clan> getClanByName(String name);

    Optional<Clan> getClanByPlayer(UUID uuid);

    Optional<Clan> getClanByUuid(UUID uuid);

    /**
     * Returns all clan tags (case as stored) for tab-complete/UI.
     */
    java.util.List<String> getAllTags();

    /**
     * Returns all clan display names for tab-complete/UI.
     */
    java.util.List<String> getAllNames();

    ClanRank createRank(Clan clan, String name, int weight);
    boolean deleteRank(Clan clan, String name);
    boolean togglePermission(Clan clan, String rankName, ClanPermission permission);
    boolean assignRank(Clan clan, java.util.UUID playerUuid, String rankName);
    ClanRank getRank(Clan clan, String name);

    Clan createClan(Player creator, String tag, String name, String color);

    void invitePlayer(Clan clan, Player inviter, OfflinePlayer target);

    void joinClan(Player player, String tagOrName);

    void leaveClan(Player player);

    void kickMember(Player actor, OfflinePlayer target);

    List<Clan> getTopClans(int page, int pageSize);

    double getClanAveragePoints(Clan clan);

    void dissolveClan(Clan clan);

    void setAlliance(Clan clan, Clan otherClan, boolean allied);

    boolean areAllied(Clan clan, Clan otherClan);

    void updateClanColor(Clan clan, String color);

    void removeMemberFromClan(java.util.UUID playerUuid);

    void renameClan(Clan clan, String newName);

    void retagClan(Clan clan, String newTag);

    int getMemberLimit(Clan clan);

    int getAllyLimit(Clan clan);

    boolean togglePvp(Clan clan, boolean enabled);

    boolean tryUpgrade(Clan clan, ClanUpgradeType type, Player payer);
}
