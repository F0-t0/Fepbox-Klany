package pl.fepbox.klany.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.entity.EntityDamageEvent;
import pl.fepbox.klany.FepboxKlanyPlugin;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

public class PluginConfigLoader {

    private final FepboxKlanyPlugin plugin;

    public PluginConfigLoader(FepboxKlanyPlugin plugin) {
        this.plugin = plugin;
    }

    public PluginConfig load() {
        FileConfiguration cfg = plugin.getConfig();
        Logger log = plugin.getLogger();

        int tagMaxLen = Math.max(1, cfg.getInt("limits.tagMaxLength", 4));
        int nameMaxLen = Math.max(1, cfg.getInt("limits.nameMaxLength", 16));
        LimitsConfig limits = new LimitsConfig(tagMaxLen, nameMaxLen);

        String allowedTagRegex = cfg.getString("filter.allowedTagRegex", "^[A-Za-z0-9]{1,4}$");
        String allowedNameRegex = cfg.getString("filter.allowedNameRegex", "^[A-Za-z0-9 _]{1,16}$");
        List<String> blocked = new ArrayList<>();
        for (String word : cfg.getStringList("filter.blockedWords")) {
            if (word != null && !word.isBlank()) {
                blocked.add(word.toLowerCase(Locale.ROOT));
            }
        }
        FilterConfig filter = new FilterConfig(allowedTagRegex, allowedNameRegex, blocked);

        int startPoints = cfg.getInt("points.startPoints", 1000);
        int minPoints = cfg.getInt("points.minPoints", 0);
        int maxPoints = cfg.getInt("points.maxPoints", 100000);
        int effectiveMax = Math.max(minPoints, maxPoints);
        double base = cfg.getDouble("points.killScaling.baseReward", 20.0);
        double factor = cfg.getDouble("points.killScaling.factor", 0.02);
        int minChange = cfg.getInt("points.killScaling.minChange", 5);
        int maxChange = cfg.getInt("points.killScaling.maxChange", 50);
        if (minChange < 0) minChange = 0;
        if (maxChange < minChange) maxChange = minChange;
        int defaultLoss = cfg.getInt("points.selfDeathLoss.defaultLoss", 10);
        Map<EntityDamageEvent.DamageCause, Integer> causeMap = new EnumMap<>(EntityDamageEvent.DamageCause.class);
        ConfigurationSection causesSec = cfg.getConfigurationSection("points.selfDeathLoss.causes");
        if (causesSec != null) {
            for (String key : causesSec.getKeys(false)) {
                try {
                    EntityDamageEvent.DamageCause cause = EntityDamageEvent.DamageCause.valueOf(key.toUpperCase(Locale.ROOT));
                    causeMap.put(cause, causesSec.getInt(key));
                } catch (IllegalArgumentException ex) {
                    log.warning("Nieznana przyczyna śmierci w configu: " + key);
                }
            }
        }
        PointsConfig points = new PointsConfig(
                startPoints,
                minPoints,
                effectiveMax,
                base,
                factor,
                minChange,
                maxChange,
                defaultLoss,
                causeMap
        );

        String skullSymbol = cfg.getString("ui.skullSymbol", "💀");

        String killTitle = cfg.getString("ui.titles.kill.title", "<symbol> ZABÓJSTWO <symbol>");
        String killerSub = cfg.getString("ui.titles.kill.subtitleKiller", "+<delta_killer> | <points_killer_after> pkt");
        String victimSub = cfg.getString("ui.titles.kill.subtitleVictim", "-<delta_victim> | <points_victim_after> pkt");
        TitlesConfig.KillTitleConfig killConfig = new TitlesConfig.KillTitleConfig(killTitle, killerSub, victimSub);

        String selfTitle = cfg.getString("ui.titles.selfDeath.title", "<symbol> ŚMIERĆ <symbol>");
        String selfSub = cfg.getString("ui.titles.selfDeath.subtitle", "-<delta_victim> | <points_victim_after> pkt");
        TitlesConfig.SelfDeathTitleConfig selfConfig = new TitlesConfig.SelfDeathTitleConfig(selfTitle, selfSub);

        int fadeIn = cfg.getInt("ui.titles.timings.fadeIn", 10);
        int stay = cfg.getInt("ui.titles.timings.stay", 40);
        int fadeOut = cfg.getInt("ui.titles.timings.fadeOut", 10);
        TitlesConfig.TitleTimings timings = new TitlesConfig.TitleTimings(fadeIn, stay, fadeOut);

        TitlesConfig titles = new TitlesConfig(killConfig, selfConfig, timings);
        UIConfig ui = new UIConfig(skullSymbol, titles);

        int pageSize = Math.max(1, cfg.getInt("ranking.pageSize", 10));
        String playerFormat = cfg.getString("ranking.playerFormat", "&e#<position> &f<player_name> &7- &b<points> pkt");
        String playerFormatSelf = cfg.getString("ranking.playerFormatSelf", "&6#<position> &f<player_name> &7- &a<points> pkt");
        String clanFormat = cfg.getString("ranking.clanFormat", "&e#<position> &f<clan_display> &7- &b<points> pkt");
        String clanFormatSelf = cfg.getString("ranking.clanFormatSelf", "&6#<position> &f<clan_display> &7- &a<points> pkt");
        RankingConfig ranking = new RankingConfig(pageSize, playerFormat, playerFormatSelf, clanFormat, clanFormatSelf);

        String noClanText = cfg.getString("placeholders.noClanText", "-");
        PlaceholderConfig placeholders = new PlaceholderConfig(noClanText);

        String storageType = cfg.getString("storage.type", "sqlite");
        String storageFile = cfg.getString("storage.file", "plugins/Fepbox-Klany/data.db");
        StorageConfig storage = new StorageConfig(storageType, storageFile);

        return new PluginConfig(limits, filter, points, ui, ranking, placeholders, storage);
    }
}

