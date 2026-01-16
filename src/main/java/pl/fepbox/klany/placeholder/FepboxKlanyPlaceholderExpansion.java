package pl.fepbox.klany.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import pl.fepbox.klany.FepboxKlanyPlugin;
import pl.fepbox.klany.clan.Clan;
import pl.fepbox.klany.clan.ClanService;
import pl.fepbox.klany.config.PluginConfig;
import pl.fepbox.klany.player.PlayerProfileService;
import pl.fepbox.klany.points.PointsService;

import java.util.Optional;

public class FepboxKlanyPlaceholderExpansion extends PlaceholderExpansion {

    private final FepboxKlanyPlugin plugin;
    private final PlayerProfileService profileService;
    private final ClanService clanService;
    private final PointsService pointsService;
    private final PluginConfig config;

    public FepboxKlanyPlaceholderExpansion(FepboxKlanyPlugin plugin,
                                           PlayerProfileService profileService,
                                           ClanService clanService,
                                           PointsService pointsService,
                                           PluginConfig config) {
        this.plugin = plugin;
        this.profileService = profileService;
        this.clanService = clanService;
        this.pointsService = pointsService;
        this.config = config;
    }

    @Override
    public String getIdentifier() {
        return "fepbox";
    }

    @Override
    public String getAuthor() {
        return "Fepbox";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null || player.getUniqueId() == null) {
            return "";
        }
        String noClan = config.getPlaceholders().getNoClanText();

        if (params.equalsIgnoreCase("klan_tag")) {
            Optional<Clan> clan = clanService.getClanByPlayer(player.getUniqueId());
            return clan.map(Clan::getTag).orElse(noClan);
        }
        if (params.equalsIgnoreCase("klan_name")) {
            Optional<Clan> clan = clanService.getClanByPlayer(player.getUniqueId());
            return clan.map(Clan::getName).orElse(noClan);
        }
        if (params.equalsIgnoreCase("klan_color")) {
            Optional<Clan> clan = clanService.getClanByPlayer(player.getUniqueId());
            return clan.map(Clan::getColor).orElse(noClan);
        }
        if (params.equalsIgnoreCase("klan_display")) {
            Optional<Clan> clan = clanService.getClanByPlayer(player.getUniqueId());
            return clan.map(c -> c.getColor() + "[" + c.getTag() + "]").orElse(noClan);
        }
        if (params.equalsIgnoreCase("points")) {
            return String.valueOf(pointsService.getPoints(player.getUniqueId()));
        }
        if (params.equalsIgnoreCase("points_formatted")) {
            return String.format("%,d", pointsService.getPoints(player.getUniqueId()));
        }
        if (params.equalsIgnoreCase("clan_points")) {
            Optional<Clan> clan = clanService.getClanByPlayer(player.getUniqueId());
            return clan.map(c -> String.valueOf(Math.round(clanService.getClanAveragePoints(c)))).orElse(noClan);
        }

        return null;
    }
}
