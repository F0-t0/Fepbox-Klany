package pl.fepbox.klany.command;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
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

public class FepboxKlanyAdminCommand implements CommandExecutor, TabCompleter {

    private final Plugin plugin;
    private final ClanService clanService;
    private final PointsService pointsService;
    private final PlayerProfileService profileService;
    private final PluginConfig config;

    public FepboxKlanyAdminCommand(Plugin plugin, ClanService clanService,
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
        if (!sender.hasPermission("fepboxklany.admin.base")) {
            sender.sendMessage("§cBrak uprawnień.");
            return true;
        }

        if (args.length == 0 || !args[0].equalsIgnoreCase("admin")) {
            sendAdminHelp(sender);
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cZa mało argumentów.");
            return true;
        }

        String sub = args[1].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "setpoints" -> handleSetPoints(sender, args);
            case "addpoints" -> handleAddPoints(sender, args);
            case "forcekick" -> handleForceKick(sender, args);
            case "dissolve" -> handleDissolve(sender, args);
            case "rename" -> handleRename(sender, args);
            case "retag" -> handleRetag(sender, args);
            default -> sender.sendMessage("§cNieznana podkomenda admin.");
        }
        return true;
    }

    private void sendAdminHelp(CommandSender sender) {
        sender.sendMessage("§6/fepboxklany admin setpoints <gracz> <wartosc>");
        sender.sendMessage("§6/fepboxklany admin addpoints <gracz> <wartosc>");
        sender.sendMessage("§6/fepboxklany admin forcekick <gracz>");
        sender.sendMessage("§6/fepboxklany admin dissolve <tag|nazwa>");
        sender.sendMessage("§6/fepboxklany admin rename <tag|nazwa> <nowa_nazwa>");
        sender.sendMessage("§6/fepboxklany admin retag <tag|nazwa> <nowy_tag>");
        sender.sendMessage("§6/fepboxklany reload");
    }

    private void handleSetPoints(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUżycie: /fepboxklany admin setpoints <gracz> <wartosc>");
            return;
        }
        Player target = plugin.getServer().getPlayer(args[2]);
        if (target == null) {
            sender.sendMessage("§cGracz nie jest online.");
            return;
        }
        try {
            int val = Integer.parseInt(args[3]);
            pointsService.setPoints(target.getUniqueId(), val);
            sender.sendMessage("§aUstawiono " + val + " punktów dla " + target.getName());
        } catch (NumberFormatException ex) {
            sender.sendMessage("§cNieprawidłowa wartość.");
        }
    }

    private void handleAddPoints(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUżycie: /fepboxklany admin addpoints <gracz> <wartosc>");
            return;
        }
        Player target = plugin.getServer().getPlayer(args[2]);
        if (target == null) {
            sender.sendMessage("§cGracz nie jest online.");
            return;
        }
        try {
            int val = Integer.parseInt(args[3]);
            pointsService.addPoints(target.getUniqueId(), val);
            sender.sendMessage("§aDodano " + val + " punktów dla " + target.getName());
        } catch (NumberFormatException ex) {
            sender.sendMessage("§cNieprawidłowa wartość.");
        }
    }

    private void handleForceKick(CommandSender sender, String[] args) {
        if (!sender.hasPermission("fepboxklany.admin.forcekick")) {
            sender.sendMessage("§cBrak uprawnień (fepboxklany.admin.forcekick).");
            return;
        }
        if (args.length < 3) {
            sender.sendMessage("§cUżycie: /fepboxklany admin forcekick <gracz>");
            return;
        }
        OfflinePlayer target = plugin.getServer().getOfflinePlayer(args[2]);
        if (target == null || target.getUniqueId() == null) {
            sender.sendMessage("§cNie znaleziono gracza.");
            return;
        }
        clanService.removeMemberFromClan(target.getUniqueId());
        sender.sendMessage("§aUsunięto gracza §f" + target.getName() + " §az jego klanu (jeśli był w klanie).");
    }

    private void handleDissolve(CommandSender sender, String[] args) {
        if (!sender.hasPermission("fepboxklany.admin.recalc")) {
            // używamy istniejącego uprawnienia admin.*; można też dodać osobne jeśli chcesz
        }
        if (args.length < 3) {
            sender.sendMessage("§cUżycie: /fepboxklany admin dissolve <tag|nazwa>");
            return;
        }
        String id = args[2];
        Optional<Clan> clanOpt = clanService.getClanByTag(id);
        if (clanOpt.isEmpty()) {
            clanOpt = clanService.getClanByName(id);
        }
        if (clanOpt.isEmpty()) {
            sender.sendMessage("§cNie znaleziono klanu o podanym tagu/nazwie.");
            return;
        }
        Clan clan = clanOpt.get();
        clanService.dissolveClan(clan);
        sender.sendMessage("§cRozwiązano klan §f[" + clan.getTag() + "] " + clan.getName() + "§c.");
    }

    private void handleRename(CommandSender sender, String[] args) {
        if (!sender.hasPermission("fepboxklany.admin.rename")) {
            sender.sendMessage("§cBrak uprawnień (fepboxklany.admin.rename).");
            return;
        }
        if (args.length < 4) {
            sender.sendMessage("§cUżycie: /fepboxklany admin rename <tag|nazwa> <nowa_nazwa>");
            return;
        }
        String id = args[2];
        String newName = args[3];

        if (newName.length() > config.getLimits().getNameMaxLength()) {
            sender.sendMessage("§cNazwa jest za długa. Maksymalna długość: " + config.getLimits().getNameMaxLength());
            return;
        }

        Optional<Clan> clanOpt = clanService.getClanByTag(id);
        if (clanOpt.isEmpty()) {
            clanOpt = clanService.getClanByName(id);
        }
        if (clanOpt.isEmpty()) {
            sender.sendMessage("§cNie znaleziono klanu o podanym tagu/nazwie.");
            return;
        }
        Clan clan = clanOpt.get();
        clanService.renameClan(clan, newName);
        sender.sendMessage("§aZmieniono nazwę klanu na §f" + newName + "§a.");
    }

    private void handleRetag(CommandSender sender, String[] args) {
        if (!sender.hasPermission("fepboxklany.admin.retag")) {
            sender.sendMessage("§cBrak uprawnień (fepboxklany.admin.retag).");
            return;
        }
        if (args.length < 4) {
            sender.sendMessage("§cUżycie: /fepboxklany admin retag <tag|nazwa> <nowy_tag>");
            return;
        }
        String id = args[2];
        String newTag = args[3];

        if (newTag.length() > config.getLimits().getTagMaxLength()) {
            sender.sendMessage("§cTag jest za długi. Maksymalna długość: " + config.getLimits().getTagMaxLength());
            return;
        }

        Optional<Clan> clanOpt = clanService.getClanByTag(id);
        if (clanOpt.isEmpty()) {
            clanOpt = clanService.getClanByName(id);
        }
        if (clanOpt.isEmpty()) {
            sender.sendMessage("§cNie znaleziono klanu o podanym tagu/nazwie.");
            return;
        }
        Clan clan = clanOpt.get();

        Optional<Clan> existingWithTag = clanService.getClanByTag(newTag);
        if (existingWithTag.isPresent() && !existingWithTag.get().getUuid().equals(clan.getUuid())) {
            sender.sendMessage("§cInny klan ma już ten tag.");
            return;
        }

        clanService.retagClan(clan, newTag);
        sender.sendMessage("§aZmieniono tag klanu na §f" + newTag + "§a.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Collections.singletonList("admin");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
            List<String> sub = List.of("setpoints", "addpoints", "forcekick", "dissolve", "rename", "retag");
            String prefix = args[1].toLowerCase(Locale.ROOT);
            List<String> out = new ArrayList<>();
            for (String s : sub) {
                if (s.startsWith(prefix)) {
                    out.add(s);
                }
            }
            return out;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("admin")) {
            String sub = args[1].toLowerCase(Locale.ROOT);
            if (sub.equals("setpoints") || sub.equals("addpoints") || sub.equals("forcekick")) {
                List<String> names = new ArrayList<>();
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    names.add(p.getName());
                }
                return names;
            }
        }
        return Collections.emptyList();
    }
}

