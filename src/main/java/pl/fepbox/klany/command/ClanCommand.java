package pl.fepbox.klany.command;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import pl.fepbox.klany.clan.Clan;
import pl.fepbox.klany.clan.ClanRole;
import pl.fepbox.klany.clan.ClanService;
import pl.fepbox.klany.clan.ClanUpgradeType;
import pl.fepbox.klany.clan.ClanRank;
import pl.fepbox.klany.clan.ClanPermission;
import pl.fepbox.klany.config.PluginConfig;
import pl.fepbox.klany.gui.ClanUpgradeMenu;
import pl.fepbox.klany.gui.RankPermissionMenu;
import pl.fepbox.klany.points.PointsService;
import pl.fepbox.klany.player.PlayerProfileService;
import pl.fepbox.klany.util.ColorUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class ClanCommand implements CommandExecutor, TabCompleter {

    private final org.bukkit.plugin.Plugin plugin;
    private final ClanService clanService;
    private final PointsService pointsService;
    private final PlayerProfileService profileService;
    private final PluginConfig config;
    private final ClanUpgradeMenu upgradeMenu;
    private final RankPermissionMenu rankMenu;

    public ClanCommand(org.bukkit.plugin.Plugin plugin,
                       ClanService clanService,
                       PointsService pointsService,
                       PlayerProfileService profileService,
                       PluginConfig config,
                       ClanUpgradeMenu upgradeMenu,
                       RankPermissionMenu rankMenu) {
        this.plugin = plugin;
        this.clanService = clanService;
        this.pointsService = pointsService;
        this.profileService = profileService;
        this.config = config;
        this.upgradeMenu = upgradeMenu;
        this.rankMenu = rankMenu;
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
            case "kolor" -> handleColor(player, args);
            case "pvp" -> handlePvp(player, args);
            case "ulepszenia" -> handleUpgrades(player);
            case "rangi" -> handleRanks(player, args);
            default -> sendHelp(player);
        }
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(ColorUtil.colorize("<GOLD>/klan zaloz <TAG> <NAZWA> <GRAY>- tworzy klan"));
        player.sendMessage(ColorUtil.colorize("<GOLD>/klan info [klan|tag] <GRAY>- informacje o klanie"));
        player.sendMessage(ColorUtil.colorize("<GOLD>/klan zapros <gracz> <GRAY>- zaprasza gracza do klanu"));
        player.sendMessage(ColorUtil.colorize("<GOLD>/klan opusc <GRAY>- opuszcza klan"));
        player.sendMessage(ColorUtil.colorize("<GOLD>/klan wyrzuc <gracz> <GRAY>- wyrzuca gracza (leader)"));
        player.sendMessage(ColorUtil.colorize("<GOLD>/klan sojusz <tag|nazwa> <GRAY>- przelacza sojusz (leader)"));
        player.sendMessage(ColorUtil.colorize("<GOLD>/klan kolor <nazwa koloru> <GRAY>- ustawia kolor klanu (leader)"));
        player.sendMessage(ColorUtil.colorize("<GOLD>/klan pvp <on|off|toggle> <GRAY>- PVP w klanie (leader)"));
        player.sendMessage(ColorUtil.colorize("<GOLD>/klan ulepszenia <GRAY>- GUI ulepszen klanu"));
        player.sendMessage(ColorUtil.colorize("<GOLD>/klan rangi <dodaj/usun/permisje/gracz> <GRAY>- zarzadzanie rangami (w przygotowaniu)"));
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ColorUtil.colorize("<RED>Uzycie: /klan zaloz <TAG> <NAZWA>"));
            return;
        }
        String tag = args[1];
        String name = args[2];

        if (tag.length() > config.getLimits().getTagMaxLength()) {
            player.sendMessage(ColorUtil.colorize("<RED>Tag jest za dlugi. Maks: " + config.getLimits().getTagMaxLength()));
            return;
        }
        if (name.length() > config.getLimits().getNameMaxLength()) {
            player.sendMessage(ColorUtil.colorize("<RED>Nazwa jest za dluga. Maks: " + config.getLimits().getNameMaxLength()));
            return;
        }
        if (clanService.getClanByTag(tag).isPresent()) {
            player.sendMessage(ColorUtil.colorize("<RED>Klan o takim tagu juz istnieje."));
            return;
        }

        Clan clan = clanService.createClan(player, tag, name, net.md_5.bungee.api.ChatColor.WHITE.toString());
        player.sendMessage(ColorUtil.colorize("<GREEN>Stworzono klan <WHITE>[" + clan.getTag() + "] " + clan.getName()));
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
            player.sendMessage(ColorUtil.colorize("<RED>Nie znaleziono klanu."));
            return;
        }
        Clan clan = clanOpt.get();
        var infoCfg = config.getClanSettings().getInfo();
        player.sendMessage(ColorUtil.colorize(infoCfg.getTitle()));
        for (String line : infoCfg.getLines()) {
            player.sendMessage(ColorUtil.colorize(applyClanPlaceholders(line, clan)));
        }
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
        player.sendMessage(ColorUtil.colorize("<GRAY>Punkty PvP gracza <WHITE>" + target.getName() + "<GRAY>: <AQUA>" + points));
    }

    private void handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ColorUtil.colorize("<RED>Uzycie: /klan zapros <gracz>"));
            return;
        }
        Optional<Clan> clanOpt = clanService.getClanByPlayer(player.getUniqueId());
        if (clanOpt.isEmpty()) {
            player.sendMessage(ColorUtil.colorize("<RED>Nie masz klanu."));
            return;
        }
        Clan clan = clanOpt.get();
        if (!clan.getOwnerUuid().equals(player.getUniqueId())) {
            player.sendMessage(ColorUtil.colorize("<RED>Tylko lider klanu moze zapraszac graczy."));
            return;
        }
        if (clan.getMembers().size() >= clanService.getMemberLimit(clan)) {
            player.sendMessage(ColorUtil.colorize("<RED>Klan osiagnal limit czlonkow. Ulepsz limit w /klan ulepszenia."));
            return;
        }
        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(ColorUtil.colorize("<RED>Gracz musi byc online."));
            return;
        }
        clanService.invitePlayer(clan, player, target);
        player.sendMessage(ColorUtil.colorize("<GREEN>Zaproszono gracza <WHITE>" + target.getName() + " <GREEN>do klanu."));
    }

    private void handleLeave(Player player) {
        Optional<Clan> clanOpt = clanService.getClanByPlayer(player.getUniqueId());
        if (clanOpt.isEmpty()) {
            player.sendMessage(ColorUtil.colorize("<RED>Nie jestes w klanie."));
            return;
        }
        Clan clan = clanOpt.get();
        if (clan.getOwnerUuid().equals(player.getUniqueId()) && clan.getMembers().size() > 1) {
            player.sendMessage(ColorUtil.colorize("<RED>Jestes liderem. Przekaz lidera lub uzyj /klan rozwiaz."));
            return;
        }
        clanService.leaveClan(player);
        player.sendMessage(ColorUtil.colorize("<GREEN>Opusciles klan <WHITE>[" + clan.getTag() + "] " + clan.getName()));
    }

    private void handleKick(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ColorUtil.colorize("<RED>Uzycie: /klan wyrzuc <gracz>"));
            return;
        }
        Optional<Clan> clanOpt = clanService.getClanByPlayer(player.getUniqueId());
        if (clanOpt.isEmpty()) {
            player.sendMessage(ColorUtil.colorize("<RED>Nie masz klanu."));
            return;
        }
        Clan clan = clanOpt.get();
        if (!clan.getOwnerUuid().equals(player.getUniqueId())) {
            player.sendMessage(ColorUtil.colorize("<RED>Tylko lider klanu moze wyrzucac graczy."));
            return;
        }
        OfflinePlayer target = plugin.getServer().getOfflinePlayer(args[1]);
        if (target == null || target.getUniqueId() == null) {
            player.sendMessage(ColorUtil.colorize("<RED>Nie znaleziono gracza."));
            return;
        }
        if (!clan.getMembers().containsKey(target.getUniqueId())) {
            player.sendMessage(ColorUtil.colorize("<RED>Ten gracz nie jest w twoim klanie."));
            return;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(ColorUtil.colorize("<RED>Nie mozesz wyrzucic samego siebie."));
            return;
        }
        clanService.kickMember(player, target);
        player.sendMessage(ColorUtil.colorize("<GREEN>Wyrzucono gracza <WHITE>" + target.getName() + " <GREEN>z klanu."));
    }

    private void handleDissolve(Player player) {
        Optional<Clan> clanOpt = clanService.getClanByPlayer(player.getUniqueId());
        if (clanOpt.isEmpty()) {
            player.sendMessage(ColorUtil.colorize("<RED>Nie masz klanu."));
            return;
        }
        Clan clan = clanOpt.get();
        if (!clan.getOwnerUuid().equals(player.getUniqueId())) {
            player.sendMessage(ColorUtil.colorize("<RED>Tylko lider klanu moze rozwiazac klan."));
            return;
        }
        clanService.dissolveClan(clan);
        player.sendMessage(ColorUtil.colorize("<RED>Rozwiazales klan <WHITE>[" + clan.getTag() + "] " + clan.getName()));
    }

    private void handleAlly(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ColorUtil.colorize("<RED>Uzycie: /klan sojusz <tag|nazwa>"));
            return;
        }
        Optional<Clan> ownOpt = clanService.getClanByPlayer(player.getUniqueId());
        if (ownOpt.isEmpty()) {
            player.sendMessage(ColorUtil.colorize("<RED>Nie masz klanu."));
            return;
        }
        Clan own = ownOpt.get();
        if (!own.getOwnerUuid().equals(player.getUniqueId())) {
            player.sendMessage(ColorUtil.colorize("<RED>Tylko lider klanu moze zmieniac sojusze."));
            return;
        }
        String targetName = args[1];
        Optional<Clan> targetOpt = clanService.getClanByTag(targetName);
        if (targetOpt.isEmpty()) {
            targetOpt = clanService.getClanByName(targetName);
        }
        if (targetOpt.isEmpty()) {
            player.sendMessage(ColorUtil.colorize("<RED>Nie znaleziono klanu o podanym tagu/nazwie."));
            return;
        }
        Clan target = targetOpt.get();
        if (target.getUuid().equals(own.getUuid())) {
            player.sendMessage(ColorUtil.colorize("<RED>Nie mozesz zawrzec sojuszu z wlasnym klanem."));
            return;
        }
        boolean currentlyAllied = clanService.areAllied(own, target);
        if (!currentlyAllied && own.getAllies().size() >= clanService.getAllyLimit(own)) {
            player.sendMessage(ColorUtil.colorize("<RED>Osiagnieto limit sojuszow. Ulepsz w /klan ulepszenia."));
            return;
        }
        clanService.setAlliance(own, target, !currentlyAllied);
        if (currentlyAllied) {
            player.sendMessage(ColorUtil.colorize("<RED>Sojusz z klanem <WHITE>[" + target.getTag() + "] " + target.getName() + " <RED>zostal zerwany."));
        } else {
            player.sendMessage(ColorUtil.colorize("<GREEN>Zawarto sojusz z klanem <WHITE>[" + target.getTag() + "] " + target.getName()));
        }
    }

    private void handleColor(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ColorUtil.colorize("<RED>Uzycie: /klan kolor <nazwa_koloru>"));
            player.sendMessage(ColorUtil.colorize("<GRAY>Przyklad: <WHITE>/klan kolor YELLOW"));
            return;
        }
        Optional<Clan> clanOpt = clanService.getClanByPlayer(player.getUniqueId());
        if (clanOpt.isEmpty()) {
            player.sendMessage(ColorUtil.colorize("<RED>Nie masz klanu."));
            return;
        }
        Clan clan = clanOpt.get();
        if (!clan.getOwnerUuid().equals(player.getUniqueId())) {
            player.sendMessage(ColorUtil.colorize("<RED>Tylko lider klanu moze zmieniac kolor klanu."));
            return;
        }
        String code = args[1].toUpperCase(Locale.ROOT);
        String color;
        try {
            color = net.md_5.bungee.api.ChatColor.valueOf(code).toString();
        } catch (IllegalArgumentException ex) {
            player.sendMessage(ColorUtil.colorize("<RED>Nieprawidlowy kolor. Uzyj nazw np. RED, GREEN, YELLOW."));
            return;
        }
        clanService.updateClanColor(clan, color);
        player.sendMessage(ColorUtil.colorize("<GREEN>Ustawiono kolor klanu na " + color + "ten kolor"));
    }

    private void handlePvp(Player player, String[] args) {
        Optional<Clan> clanOpt = clanService.getClanByPlayer(player.getUniqueId());
        if (clanOpt.isEmpty()) {
            player.sendMessage(ColorUtil.colorize("<RED>Nie masz klanu."));
            return;
        }
        Clan clan = clanOpt.get();
        if (!clan.getOwnerUuid().equals(player.getUniqueId())) {
            player.sendMessage(ColorUtil.colorize("<RED>Tylko lider klanu moze zmienic PVP."));
            return;
        }
        boolean newState = !clan.isPvpEnabled();
        if (args.length >= 2) {
            String val = args[1].toLowerCase(Locale.ROOT);
            if (val.equals("on") || val.equals("true")) newState = true;
            else if (val.equals("off") || val.equals("false")) newState = false;
        }
        clanService.togglePvp(clan, newState);
        player.sendMessage(ColorUtil.colorize(newState ? "<GREEN>PvP w klanie wlaczone." : "<RED>PvP w klanie wylaczone."));
    }

    private void handleUpgrades(Player player) {
        Optional<Clan> clanOpt = clanService.getClanByPlayer(player.getUniqueId());
        if (clanOpt.isEmpty()) {
            player.sendMessage(ColorUtil.colorize("<RED>Nie masz klanu."));
            return;
        }
        Clan clan = clanOpt.get();
        if (!clan.getOwnerUuid().equals(player.getUniqueId())) {
            player.sendMessage(ColorUtil.colorize("<RED>Tylko lider klanu moze zarzadzac ulepszeniami."));
            return;
        }
        upgradeMenu.open(player, clan);
    }

    private void handleRanks(Player player, String[] args) {
        Optional<Clan> clanOpt = clanService.getClanByPlayer(player.getUniqueId());
        if (clanOpt.isEmpty()) {
            player.sendMessage(ColorUtil.colorize("<RED>Nie masz klanu."));
            return;
        }
        Clan clan = clanOpt.get();
        if (!canManageRanks(player, clan)) {
            player.sendMessage(ColorUtil.colorize("<RED>Brak uprawnien do zarzadzania rangami."));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(ColorUtil.colorize("<YELLOW>Uzycie: /klan rangi <dodaj/usun/permisje/gracz> ..."));
            return;
        }
        String sub = args[1].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "dodaj" -> {
                if (args.length < 4) {
                    player.sendMessage(ColorUtil.colorize("<YELLOW>/klan rangi dodaj <nazwa> <waga>"));
                    return;
                }
                String name = args[2];
                int weight;
                try {
                    weight = Integer.parseInt(args[3]);
                } catch (NumberFormatException ex) {
                    player.sendMessage(ColorUtil.colorize("<RED>Waga musi byc liczba."));
                    return;
                }
                if (clanService.getRank(clan, name) != null) {
                    player.sendMessage(ColorUtil.colorize("<RED>Taka ranga juz istnieje."));
                    return;
                }
                clanService.createRank(clan, name, weight);
                player.sendMessage(ColorUtil.colorize("<GREEN>Dodano range <WHITE>" + name + " <GRAY>(waga " + weight + ")"));
            }
            case "usun" -> {
                if (args.length < 3) {
                    player.sendMessage(ColorUtil.colorize("<YELLOW>/klan rangi usun <nazwa>"));
                    return;
                }
                String name = args[2];
                if (clanService.deleteRank(clan, name)) {
                    player.sendMessage(ColorUtil.colorize("<GREEN>Usunieto range <WHITE>" + name));
                } else {
                    player.sendMessage(ColorUtil.colorize("<RED>Nie mozna usunac tej rangi."));
                }
            }
            case "permisje" -> {
                if (args.length < 3) {
                    player.sendMessage(ColorUtil.colorize("<YELLOW>/klan rangi permisje <nazwa>"));
                    return;
                }
                String name = args[2];
                ClanRank rank = clanService.getRank(clan, name);
                if (rank == null) {
                    player.sendMessage(ColorUtil.colorize("<RED>Nie ma takiej rangi."));
                    return;
                }
                rankMenu.open(player, clan, rank);
            }
            case "gracz" -> {
                if (args.length < 4) {
                    player.sendMessage(ColorUtil.colorize("<YELLOW>/klan rangi gracz <gracz> <nazwa>"));
                    return;
                }
                Player target = plugin.getServer().getPlayer(args[2]);
                if (target == null) {
                    player.sendMessage(ColorUtil.colorize("<RED>Gracz musi byc online."));
                    return;
                }
                ClanRank rank = clanService.getRank(clan, args[3]);
                if (rank == null) {
                    player.sendMessage(ColorUtil.colorize("<RED>Nie ma takiej rangi."));
                    return;
                }
                if (clanService.assignRank(clan, target.getUniqueId(), rank.getName())) {
                    player.sendMessage(ColorUtil.colorize("<GREEN>Nadano range <WHITE>" + rank.getName() + "<GREEN> graczowi <WHITE>" + target.getName()));
                } else {
                    player.sendMessage(ColorUtil.colorize("<RED>Nie udalo sie nadac rangi."));
                }
            }
            default -> player.sendMessage(ColorUtil.colorize("<YELLOW>Uzycie: /klan rangi <dodaj/usun/permisje/gracz> ..."));
        }
    }

    private boolean canManageRanks(Player player, Clan clan) {
        if (clan.getOwnerUuid().equals(player.getUniqueId())) return true;
        String rankName = clan.getMemberRank(player.getUniqueId());
        if (rankName == null) return false;
        ClanRank rank = clanService.getRank(clan, rankName);
        return rank != null && rank.has(ClanPermission.MANAGE_RANKS);
    }

    private String applyClanPlaceholders(String line, Clan clan) {
        String ownerName = resolveName(clan.getOwnerUuid());
        List<String> officers = clan.getMembers().entrySet().stream()
                .filter(e -> e.getValue() == ClanRole.OFFICER)
                .map(e -> resolveName(e.getKey()))
                .filter(n -> n != null && !n.isBlank())
                .toList();
        List<String> members = clan.getMembers().entrySet().stream()
                .map(e -> resolveName(e.getKey()))
                .filter(n -> n != null && !n.isBlank())
                .toList();
        List<String> allies = clan.getAllies().stream()
                .map(uuid -> clanService.getClanByUuid(uuid).orElse(null))
                .filter(c -> c != null)
                .map(c -> c.getTag())
                .toList();

        String officersStr = officers.isEmpty() ? "<DARK_GRAY>Brak" : "<GOLD>" + String.join("<GRAY>, <GOLD>", officers);
        String membersStr = members.isEmpty() ? "<DARK_GRAY>Brak" : "<YELLOW>" + String.join("<GRAY>, <YELLOW>", members);
        String alliesStr = allies.isEmpty() ? "<DARK_GRAY>Brak" : "<GREEN>" + String.join("<GRAY>, <GREEN>", allies);
        String pvpStr = clan.isPvpEnabled() ? "<GREEN>Wlaczone" : "<RED>Wylaczone";

        return line
                .replace("<clan_name>", clan.getName())
                .replace("<clan_tag>", clan.getTag())
                .replace("<clan_leader>", ownerName == null ? "-" : "<GOLD>" + ownerName)
                .replace("<clan_officers>", officersStr)
                .replace("<clan_members>", membersStr)
                .replace("<members_used>", String.valueOf(clan.getMembers().size()))
                .replace("<members_limit>", String.valueOf(clanService.getMemberLimit(clan)))
                .replace("<ally_used>", String.valueOf(clan.getAllies().size()))
                .replace("<ally_limit>", String.valueOf(clanService.getAllyLimit(clan)))
                .replace("<clan_allies>", alliesStr)
                .replace("<clan_points>", String.valueOf(Math.round(clanService.getClanAveragePoints(clan))))
                .replace("<clan_pvp>", pvpStr);
    }

    private String resolveName(UUID uuid) {
        if (uuid == null) return "";
        var profile = profileService.getProfile(uuid);
        if (profile != null && profile.getName() != null) {
            return profile.getName();
        }
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        if (offlinePlayer != null && offlinePlayer.getName() != null) {
            return offlinePlayer.getName();
        }
        return uuid.toString().substring(0, 8);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> base = List.of("zaloz", "info", "punkty", "zapros", "opusc", "wyrzuc", "rozwiaz", "sojusz", "kolor", "pvp", "ulepszenia", "rangi");
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
        if (args.length == 2 && args[0].equalsIgnoreCase("kolor")) {
            return List.of("RED", "BLUE", "GREEN", "YELLOW", "GOLD", "WHITE", "BLACK", "DARK_RED", "DARK_PURPLE");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("pvp")) {
            return List.of("on", "off", "toggle");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("rangi")) {
            return List.of("dodaj", "usun", "permisje", "gracz");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("rangi")) {
            String sub = args[1].toLowerCase(Locale.ROOT);
            if (sub.equals("gracz")) {
                List<String> names = new ArrayList<>();
                for (Player p : plugin.getServer().getOnlinePlayers()) names.add(p.getName());
                return names;
            }
            if (sub.equals("usun") || sub.equals("permisje")) {
                Optional<Clan> clanOpt = (sender instanceof Player pl) ? clanService.getClanByPlayer(pl.getUniqueId()) : Optional.empty();
                if (clanOpt.isPresent()) {
                    return new ArrayList<>(clanOpt.get().getRanks().keySet());
                }
            }
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("rangi") && args[1].equalsIgnoreCase("gracz")) {
            Optional<Clan> clanOpt = (sender instanceof Player pl) ? clanService.getClanByPlayer(pl.getUniqueId()) : Optional.empty();
            if (clanOpt.isPresent()) {
                return new ArrayList<>(clanOpt.get().getRanks().keySet());
            }
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("sojusz"))) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            List<String> out = new ArrayList<>();
            List<String> candidates = new ArrayList<>();
            candidates.addAll(clanService.getAllTags());
            candidates.addAll(clanService.getAllNames());
            for (String c : candidates) {
                if (c != null && c.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    out.add(c);
                }
            }
            return out;
        }
        return Collections.emptyList();
    }
}



















