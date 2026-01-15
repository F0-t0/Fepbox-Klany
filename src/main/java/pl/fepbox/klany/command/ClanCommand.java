package pl.fepbox.klany.command;

import org.bukkit.OfflinePlayer;
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

    public ClanCommand(org.bukkit.plugin.Plugin plugin,
                       ClanService clanService,
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
            case "zapros" -> handleInvite(player, args);
            case "opusc" -> handleLeave(player);
            case "wyrzuc" -> handleKick(player, args);
            case "rozwiaz" -> handleDissolve(player);
            case "sojusz" -> handleAlly(player, args);
            default -> sendHelp(player);
        }
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6/klan zaloz <TAG> <NAZWA> §7- tworzy klan");
        player.sendMessage("§6/klan info [klan|tag] §7- informacje o klanie");
        player.sendMessage("§6/klan punkty [gracz] §7- punkty PvP");
        player.sendMessage("§6/klan zapros <gracz> §7- zaprasza gracza do klanu");
        player.sendMessage("§6/klan opusc §7- opuszcza obecny klan");
        player.sendMessage("§6/klan wyrzuc <gracz> §7- wyrzuca gracza z klanu (leader)");
        player.sendMessage("§6/klan rozwiaz §7- rozwiązuje klan (leader)");
        player.sendMessage("§6/klan sojusz <tag|nazwa> §7- przełącza sojusz z innym klanem (leader)");
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

    private void handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUżycie: /klan zapros <gracz>");
            return;
        }
        Optional<Clan> clanOpt = clanService.getClanByPlayer(player.getUniqueId());
        if (clanOpt.isEmpty()) {
            player.sendMessage("§cNie masz klanu.");
            return;
        }
        Clan clan = clanOpt.get();
        if (!clan.getOwnerUuid().equals(player.getUniqueId())) {
            player.sendMessage("§cTylko lider klanu może zapraszać graczy.");
            return;
        }
        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            player.sendMessage("§cGracz musi być online.");
            return;
        }
        clanService.invitePlayer(clan, player, target);
        player.sendMessage("§aZaproszono gracza §f" + target.getName() + " §ado klanu.");
    }

    private void handleLeave(Player player) {
        Optional<Clan> clanOpt = clanService.getClanByPlayer(player.getUniqueId());
        if (clanOpt.isEmpty()) {
            player.sendMessage("§cNie jesteś w klanie.");
            return;
        }
        Clan clan = clanOpt.get();
        if (clan.getOwnerUuid().equals(player.getUniqueId()) && clan.getMembers().size() > 1) {
            player.sendMessage("§cJesteś liderem. Najpierw przekaż lidera lub użyj /klan rozwiaz.");
            return;
        }
        clanService.leaveClan(player);
        player.sendMessage("§aOpuściłeś klan §f[" + clan.getTag() + "] " + clan.getName() + "§a.");
    }

    private void handleKick(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUżycie: /klan wyrzuc <gracz>");
            return;
        }
        Optional<Clan> clanOpt = clanService.getClanByPlayer(player.getUniqueId());
        if (clanOpt.isEmpty()) {
            player.sendMessage("§cNie masz klanu.");
            return;
        }
        Clan clan = clanOpt.get();
        if (!clan.getOwnerUuid().equals(player.getUniqueId())) {
            player.sendMessage("§cTylko lider klanu może wyrzucać graczy.");
            return;
        }
        OfflinePlayer target = plugin.getServer().getOfflinePlayer(args[1]);
        if (target == null || target.getUniqueId() == null) {
            player.sendMessage("§cNie znaleziono gracza.");
            return;
        }
        if (!clan.getMembers().containsKey(target.getUniqueId())) {
            player.sendMessage("§cTen gracz nie jest w twoim klanie.");
            return;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage("§cNie możesz wyrzucić samego siebie.");
            return;
        }
        clanService.kickMember(player, target);
        player.sendMessage("§aWyrzucono gracza §f" + target.getName() + " §az klanu.");
    }

    private void handleDissolve(Player player) {
        Optional<Clan> clanOpt = clanService.getClanByPlayer(player.getUniqueId());
        if (clanOpt.isEmpty()) {
            player.sendMessage("§cNie masz klanu.");
            return;
        }
        Clan clan = clanOpt.get();
        if (!clan.getOwnerUuid().equals(player.getUniqueId())) {
            player.sendMessage("§cTylko lider klanu może rozwiązać klan.");
            return;
        }
        clanService.dissolveClan(clan);
        player.sendMessage("§cRozwiązałeś klan §f[" + clan.getTag() + "] " + clan.getName() + "§c.");
    }

    private void handleAlly(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUżycie: /klan sojusz <tag|nazwa>");
            return;
        }
        Optional<Clan> ownOpt = clanService.getClanByPlayer(player.getUniqueId());
        if (ownOpt.isEmpty()) {
            player.sendMessage("§cNie masz klanu.");
            return;
        }
        Clan own = ownOpt.get();
        if (!own.getOwnerUuid().equals(player.getUniqueId())) {
            player.sendMessage("§cTylko lider klanu może zmieniać sojusze.");
            return;
        }
        String targetName = args[1];
        Optional<Clan> targetOpt = clanService.getClanByTag(targetName);
        if (targetOpt.isEmpty()) {
            targetOpt = clanService.getClanByName(targetName);
        }
        if (targetOpt.isEmpty()) {
            player.sendMessage("§cNie znaleziono klanu o podanym tagu/nazwie.");
            return;
        }
        Clan target = targetOpt.get();
        if (target.getUuid().equals(own.getUuid())) {
            player.sendMessage("§cNie możesz zawrzeć sojuszu z własnym klanem.");
            return;
        }
        boolean currentlyAllied = clanService.areAllied(own, target);
        clanService.setAlliance(own, target, !currentlyAllied);
        if (currentlyAllied) {
            player.sendMessage("§cSojusz z klanem §f[" + target.getTag() + "] " + target.getName() + " §czostał zerwany.");
        } else {
            player.sendMessage("§aZawarto sojusz z klanem §f[" + target.getTag() + "] " + target.getName() + "§a.");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> base = List.of("zaloz", "info", "punkty", "zapros", "opusc", "wyrzuc", "rozwiaz", "sojusz");
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> out = new ArrayList<>();
            for (String s : base) {
                if (s.startsWith(prefix)) {
                    out.add(s);
                }
            }
            return out;
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("zapros") || args[0].equalsIgnoreCase("wyrzuc"))) {
            List<String> names = new ArrayList<>();
            if (sender instanceof Player player) {
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    if (!p.equals(player)) {
                        names.add(p.getName());
                    }
                }
            }
            return names;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("sojusz")) {
            List<String> tags = new ArrayList<>();
            // Brak bezpośredniego dostępu do listy klanów, więc pozostawiamy pustą listę / manualne wpisanie.
            return tags;
        }
        return Collections.emptyList();
    }
}

