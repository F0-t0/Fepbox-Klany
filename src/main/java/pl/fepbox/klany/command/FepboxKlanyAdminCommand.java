package pl.fepbox.klany.command;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.inventory.ItemStack;
import pl.fepbox.klany.clan.Clan;
import pl.fepbox.klany.clan.ClanService;
import pl.fepbox.klany.config.PluginConfig;
import pl.fepbox.klany.player.PlayerProfileService;
import pl.fepbox.klany.points.PointsService;
import pl.fepbox.klany.util.ColorUtil;

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
            sender.sendMessage(ColorUtil.colorize("<RED>Brak uprawnien."));
            return true;
        }

        if (args.length == 0 || !args[0].equalsIgnoreCase("admin")) {
            sendAdminHelp(sender);
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ColorUtil.colorize("<RED>Za malo argumentow."));
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
            case "setcoin" -> handleSetCoin(sender);
            case "reload" -> handleReload(sender);
            default -> sender.sendMessage(ColorUtil.colorize("<RED>Nieznana podkomenda admin."));
        }
        return true;
    }

    private void sendAdminHelp(CommandSender sender) {
        sender.sendMessage(ColorUtil.colorize("<GOLD>/fepboxklany admin setpoints <gracz> <wartosc>"));
        sender.sendMessage(ColorUtil.colorize("<GOLD>/fepboxklany admin addpoints <gracz> <wartosc>"));
        sender.sendMessage(ColorUtil.colorize("<GOLD>/fepboxklany admin forcekick <gracz>"));
        sender.sendMessage(ColorUtil.colorize("<GOLD>/fepboxklany admin dissolve <tag|nazwa>"));
        sender.sendMessage(ColorUtil.colorize("<GOLD>/fepboxklany admin rename <tag|nazwa> <nowa_nazwa>"));
        sender.sendMessage(ColorUtil.colorize("<GOLD>/fepboxklany admin retag <tag|nazwa> <nowy_tag>"));
        sender.sendMessage(ColorUtil.colorize("<GOLD>/fepboxklany admin setcoin <GRAY>- ustawia walute klanowa"));
        sender.sendMessage(ColorUtil.colorize("<GOLD>/fepboxklany admin reload"));
    }

    private void handleSetPoints(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ColorUtil.colorize("<RED>Uzycie: /fepboxklany admin setpoints <gracz> <wartosc>"));
            return;
        }
        Player target = plugin.getServer().getPlayer(args[2]);
        if (target == null) {
            sender.sendMessage(ColorUtil.colorize("<RED>Gracz nie jest online."));
            return;
        }
        try {
            int val = Integer.parseInt(args[3]);
            pointsService.setPoints(target.getUniqueId(), val);
            sender.sendMessage(ColorUtil.colorize("<GREEN>Ustawiono " + val + " punktow dla " + target.getName()));
        } catch (NumberFormatException ex) {
            sender.sendMessage(ColorUtil.colorize("<RED>Nieprawidlowa wartosc."));
        }
    }

    private void handleAddPoints(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ColorUtil.colorize("<RED>Uzycie: /fepboxklany admin addpoints <gracz> <wartosc>"));
            return;
        }
        Player target = plugin.getServer().getPlayer(args[2]);
        if (target == null) {
            sender.sendMessage(ColorUtil.colorize("<RED>Gracz nie jest online."));
            return;
        }
        try {
            int val = Integer.parseInt(args[3]);
            pointsService.addPoints(target.getUniqueId(), val);
            sender.sendMessage(ColorUtil.colorize("<GREEN>Dodano " + val + " punktow dla " + target.getName()));
        } catch (NumberFormatException ex) {
            sender.sendMessage(ColorUtil.colorize("<RED>Nieprawidlowa wartosc."));
        }
    }

    private void handleForceKick(CommandSender sender, String[] args) {
        if (!sender.hasPermission("fepboxklany.admin.forcekick")) {
            sender.sendMessage(ColorUtil.colorize("<RED>Brak uprawnien (fepboxklany.admin.forcekick)."));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(ColorUtil.colorize("<RED>Uzycie: /fepboxklany admin forcekick <gracz>"));
            return;
        }
        OfflinePlayer target = plugin.getServer().getOfflinePlayer(args[2]);
        if (target == null || target.getUniqueId() == null) {
            sender.sendMessage(ColorUtil.colorize("<RED>Nie znaleziono gracza."));
            return;
        }
        clanService.removeMemberFromClan(target.getUniqueId());
        sender.sendMessage(ColorUtil.colorize("<GREEN>Usunieto gracza <WHITE>" + target.getName() + " <GREEN>z jego klanu (jesli byl w klanie)."));
    }

    private void handleDissolve(CommandSender sender, String[] args) {
        if (!sender.hasPermission("fepboxklany.admin.recalc")) {
            // uzywamy istniejacego uprawnienia admin.*; mozna tez dodac osobne jesli chcesz
        }
        if (args.length < 3) {
            sender.sendMessage(ColorUtil.colorize("<RED>Uzycie: /fepboxklany admin dissolve <tag|nazwa>"));
            return;
        }
        String id = args[2];
        Optional<Clan> clanOpt = clanService.getClanByTag(id);
        if (clanOpt.isEmpty()) {
            clanOpt = clanService.getClanByName(id);
        }
        if (clanOpt.isEmpty()) {
            sender.sendMessage(ColorUtil.colorize("<RED>Nie znaleziono klanu o podanym tagu/nazwie."));
            return;
        }
        Clan clan = clanOpt.get();
        clanService.dissolveClan(clan);
        sender.sendMessage(ColorUtil.colorize("<RED>Rozwiazano klan <WHITE>[" + clan.getTag() + "] " + clan.getName() + "<RED>."));
    }

    private void handleRename(CommandSender sender, String[] args) {
        if (!sender.hasPermission("fepboxklany.admin.rename")) {
            sender.sendMessage(ColorUtil.colorize("<RED>Brak uprawnien (fepboxklany.admin.rename)."));
            return;
        }
        if (args.length < 4) {
            sender.sendMessage(ColorUtil.colorize("<RED>Uzycie: /fepboxklany admin rename <tag|nazwa> <nowa_nazwa>"));
            return;
        }
        String id = args[2];
        String newName = args[3];

        if (newName.length() > config.getLimits().getNameMaxLength()) {
            sender.sendMessage(ColorUtil.colorize("<RED>Nazwa jest za dluga. Maksymalna dlugosc: " + config.getLimits().getNameMaxLength()));
            return;
        }

        Optional<Clan> clanOpt = clanService.getClanByTag(id);
        if (clanOpt.isEmpty()) {
            clanOpt = clanService.getClanByName(id);
        }
        if (clanOpt.isEmpty()) {
            sender.sendMessage(ColorUtil.colorize("<RED>Nie znaleziono klanu o podanym tagu/nazwie."));
            return;
        }
        Clan clan = clanOpt.get();
        clanService.renameClan(clan, newName);
        sender.sendMessage(ColorUtil.colorize("<GREEN>Zmieniono nazwe klanu na <WHITE>" + newName + "<GREEN>."));
    }

    private void handleRetag(CommandSender sender, String[] args) {
        if (!sender.hasPermission("fepboxklany.admin.retag")) {
            sender.sendMessage(ColorUtil.colorize("<RED>Brak uprawnien (fepboxklany.admin.retag)."));
            return;
        }
        if (args.length < 4) {
            sender.sendMessage(ColorUtil.colorize("<RED>Uzycie: /fepboxklany admin retag <tag|nazwa> <nowy_tag>"));
            return;
        }
        String id = args[2];
        String newTag = args[3];

        if (newTag.length() > config.getLimits().getTagMaxLength()) {
            sender.sendMessage(ColorUtil.colorize("<RED>Tag jest za dlugi. Maksymalna dlugosc: " + config.getLimits().getTagMaxLength()));
            return;
        }

        Optional<Clan> clanOpt = clanService.getClanByTag(id);
        if (clanOpt.isEmpty()) {
            clanOpt = clanService.getClanByName(id);
        }
        if (clanOpt.isEmpty()) {
            sender.sendMessage(ColorUtil.colorize("<RED>Nie znaleziono klanu o podanym tagu/nazwie."));
            return;
        }
        Clan clan = clanOpt.get();

        Optional<Clan> existingWithTag = clanService.getClanByTag(newTag);
        if (existingWithTag.isPresent() && !existingWithTag.get().getUuid().equals(clan.getUuid())) {
            sender.sendMessage(ColorUtil.colorize("<RED>Inny klan ma juz ten tag."));
            return;
        }

        clanService.retagClan(clan, newTag);
        sender.sendMessage(ColorUtil.colorize("<GREEN>Zmieniono tag klanu na <WHITE>" + newTag + "<GREEN>."));
    }

    private void handleSetCoin(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtil.colorize("<RED>Tylko gracz moze uzyc tej komendy."));
            return;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            sender.sendMessage(ColorUtil.colorize("<RED>Wez do reki przedmiot, ktory ma byc waluta klanowa."));
            return;
        }
        plugin.getConfig().set("clan.currency", item);
        plugin.saveConfig();
        sender.sendMessage(ColorUtil.colorize("<GREEN>Ustawiono walute klanowa. Zrestartuj / przeladuj plugin aby zastosowac."));
    }

    private void handleReload(CommandSender sender) {
        plugin.reloadConfig();
        sender.sendMessage(ColorUtil.colorize("<GREEN>Przeladowano plik config.yml (niektore wartosci moga wymagac restartu pluginu)."));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Collections.singletonList("admin");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
            List<String> sub = List.of("setpoints", "addpoints", "forcekick", "dissolve", "rename", "retag", "setcoin", "reload");
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
            if (sub.equals("dissolve") || sub.equals("rename") || sub.equals("retag")) {
                List<String> out = new ArrayList<>();
                out.addAll(clanService.getAllTags());
                out.addAll(clanService.getAllNames());
                return out;
            }
        }
        return Collections.emptyList();
    }
}
