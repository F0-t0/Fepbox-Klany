package pl.fepbox.klany;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import pl.fepbox.klany.clan.ClanService;
import pl.fepbox.klany.clan.ClanServiceImpl;
import pl.fepbox.klany.command.ClanCommand;
import pl.fepbox.klany.command.FepboxKlanyAdminCommand;
import pl.fepbox.klany.config.PluginConfig;
import pl.fepbox.klany.config.PluginConfigLoader;
import pl.fepbox.klany.config.Messages;
import pl.fepbox.klany.db.DatabaseManager;
import pl.fepbox.klany.listener.PlayerCombatListener;
import pl.fepbox.klany.listener.PlayerConnectionListener;
import pl.fepbox.klany.placeholder.FepboxKlanyPlaceholderExpansion;
import pl.fepbox.klany.points.PointsService;
import pl.fepbox.klany.points.PointsServiceImpl;
import pl.fepbox.klany.player.PlayerProfileService;
import pl.fepbox.klany.player.PlayerProfileServiceImpl;
import pl.fepbox.klany.gui.ClanUpgradeMenu;
import pl.fepbox.klany.gui.RankPermissionMenu;

import java.util.logging.Level;
import java.util.logging.Logger;

public class FepboxKlanyPlugin extends JavaPlugin {

    private PluginConfig configModel;
    private DatabaseManager databaseManager;
    private PlayerProfileService profileService;
    private PointsService pointsService;
    private ClanService clanService;
    private ClanUpgradeMenu upgradeMenu;
    private RankPermissionMenu rankPermissionMenu;
    private Messages messages;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);

        Logger logger = getLogger();
        logger.info("Ładowanie konfiguracji Fepbox-Klany...");
        PluginConfigLoader configLoader = new PluginConfigLoader(this);
        this.configModel = configLoader.load();
        this.messages = new Messages(this);

        try {
            this.databaseManager = new DatabaseManager(this, configModel.getStorage());
            this.databaseManager.initialize();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Nie udało się zainicjalizować bazy danych, wyłączam plugin.", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.profileService = new PlayerProfileServiceImpl(this, databaseManager, configModel.getPoints());
        this.pointsService = new PointsServiceImpl(this, databaseManager, configModel.getPoints(), configModel.getRanking());
        this.clanService = new ClanServiceImpl(this, databaseManager, profileService, pointsService, configModel);
        this.upgradeMenu = new ClanUpgradeMenu(configModel, clanService);
        this.rankPermissionMenu = new RankPermissionMenu(clanService);

        getServer().getPluginManager().registerEvents(
                new PlayerConnectionListener(profileService),
                this
        );
        getServer().getPluginManager().registerEvents(
                new PlayerCombatListener(this, pointsService, clanService, configModel, messages),
                this
        );
        getServer().getPluginManager().registerEvents(upgradeMenu, this);
        getServer().getPluginManager().registerEvents(rankPermissionMenu, this);

        if (getCommand("klan") != null) {
            ClanCommand clanCommand = new ClanCommand(this, clanService, pointsService, profileService, configModel, upgradeMenu, rankPermissionMenu);
            getCommand("klan").setExecutor(clanCommand);
            getCommand("klan").setTabCompleter(clanCommand);
        }
        if (getCommand("fepboxklany") != null) {
            FepboxKlanyAdminCommand adminCommand = new FepboxKlanyAdminCommand(this, clanService, pointsService, profileService, configModel);
            getCommand("fepboxklany").setExecutor(adminCommand);
            getCommand("fepboxklany").setTabCompleter(adminCommand);
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new FepboxKlanyPlaceholderExpansion(this, profileService, clanService, pointsService, configModel).register();
            logger.info("Zarejestrowano PlaceholderAPI expansion dla Fepbox-Klany.");
        } else {
            logger.info("PlaceholderAPI nie znaleziono, placeholdery Fepbox-Klany nie będą dostępne.");
        }

        logger.info("Plugin Fepbox-Klany został pomyślnie włączony.");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            try {
                databaseManager.shutdown();
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Błąd podczas zamykania bazy danych.", e);
            }
        }
    }

    public PluginConfig getConfigModel() {
        return configModel;
    }

    public PlayerProfileService getProfileService() {
        return profileService;
    }

    public PointsService getPointsService() {
        return pointsService;
    }

    public ClanService getClanService() {
        return clanService;
    }

    public Messages getMessages() { return messages; }
}


