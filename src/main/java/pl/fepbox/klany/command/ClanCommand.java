package pl.fepbox.klany.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import pl.fepbox.klany.clan.Clan;
import pl.fepbox.klany.clan.ClanService;
import pl.fepbox.klany.config.PluginConfig;
import pl.fepbox.klany.player.PlayerProfileService;
import pl.fepbox.klany.points.PointsService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ClanCommand implements CommandExecutor, TabCompleter {

    private final org.bukkit.plugin.Plugin plugin;
    private final ClanService clanService;
    private final PointsService pointsService;
    private final PlayerProfileService profileService;
    private final PluginConfig config;

    public ClanCommand(org.bukkit.plugin.Plugin plugin, ClanService clanService,
                       PointsService pointsService,
                       PlayerProfileService profileService,
                       PluginConfig config) {
        this.plugin = plugin;
        this.clanService = clanService;
        this.pointsService = pointsService;
        this.profileService = profileService;
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Ta komenda jest tylko dla graczy.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "zaloz" -> handleCreate(player, args);
            case "info" -> handleInfo(player, args);
            case "punkty" -> handlePoints(player, args);
            default -> sendHelp(player);
        }
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6/klan zaloz <TAG> <NAZWA> §7- tworzy klan");
        player.sendMessage("§6/klan info [klan|tag] §7- informacje o klanie");
        player.sendMessage("§6/klan punkty [gracz] §7- punkty PvP");
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§cUżycie: /klan zaloz <TAG> <NAZWA>");
            return;
        }
        String tag = args[1];
        String name = args[2];

        if (tag.length() > config.getLimits().getTagMaxLength()) {
            player.sendMessage("§cTag jest za długi. Maksymalna długość: " + config.getLimits().getTagMaxLength());
            return;
        }
        if (name.length() > config.getLimits().getNameMaxLength()) {
            player.sendMessage("§cNazwa jest za długa. Maksymalna długość: " + config.getLimits().getNameMaxLength());
            return;
        }
        if (clanService.getClanByTag(tag).isPresent()) {
            player.sendMessage("§cKlan o takim tagu już istnieje.");
            return;
        }

        Clan clan = clanService.createClan(player, tag, name, "§f");
        player.sendMessage("§aStworzono klan §f[" + clan.getTag() + "] " + clan.getName());
    }

    private void handleInfo(Player player, String[] args) {
        Optional<Clan> clanOpt;
        if (args.length >= 2) {
            clanOpt = clanService.getClanByTag(args[1]);
            if (clanOpt.isEmpty()) {
                clanOpt = clanService.getClanByName(args[1]);
            }
        } else {
            clanOpt = clanService.getClanByPlayer(player.getUniqueId());
        }
        if (clanOpt.isEmpty()) {
            player.sendMessage("§cNie znaleziono klanu.");
            return;
        }
        Clan clan = clanOpt.get();
        player.sendMessage("§6Klan: §f[" + clan.getTag() + "] " + clan.getName());
        player.sendMessage("§7Lider: §f" + clan.getOwnerUuid());
        player.sendMessage("§7Średnia punktów: §b" + Math.round(clanService.getClanAveragePoints(clan)));
    }

    private void handlePoints(Player player, String[] args) {
        Player target = player;
        if (args.length >= 2) {
            Player found = plugin.getServer().getPlayer(args[1]);
            if (found != null) {
                target = found;
            }
        }
        int points = pointsService.getPoints(target.getUniqueId());
        player.sendMessage("§7Punkty PvP gracza §f" + target.getName() + "§7: §b" + points);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> base = List.of("zaloz", "info", "punkty");
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> out = new ArrayList<>();
            for (String s : base) {
                if (s.startsWith(prefix)) {
                    out.add(s);
                }
            }
            return out;
        }
        return Collections.emptyList();
    }
}

