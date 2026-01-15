package pl.fepbox.klany.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import pl.fepbox.klany.clan.ClanService;
import pl.fepbox.klany.config.PluginConfig;
import pl.fepbox.klany.player.PlayerProfileService;
import pl.fepbox.klany.points.PointsService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

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
            sender.sendMessage("§6/fepboxklany admin setpoints <gracz> <wartosc>");
            sender.sendMessage("§6/fepboxklany admin addpoints <gracz> <wartosc>");
            sender.sendMessage("§6/fepboxklany reload");
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
            default -> sender.sendMessage("§cNieznana podkomenda admin.");
        }
        return true;
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Collections.singletonList("admin");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
            List<String> sub = List.of("setpoints", "addpoints");
            String prefix = args[1].toLowerCase(Locale.ROOT);
            List<String> out = new ArrayList<>();
            for (String s : sub) {
                if (s.startsWith(prefix)) out.add(s);
            }
            return out;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("admin")) {
            List<String> names = new ArrayList<>();
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                names.add(p.getName());
            }
            return names;
        }
        return Collections.emptyList();
    }
}

