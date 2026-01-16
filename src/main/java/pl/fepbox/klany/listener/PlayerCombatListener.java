package pl.fepbox.klany.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.Plugin;
import pl.fepbox.klany.clan.ClanService;
import pl.fepbox.klany.config.PluginConfig;
import pl.fepbox.klany.config.TitlesConfig;
import pl.fepbox.klany.player.PlayerProfileService;
import pl.fepbox.klany.points.KillResult;
import pl.fepbox.klany.points.PointsService;

public class PlayerCombatListener implements Listener {

    private final Plugin plugin;
    private final PlayerProfileService profileService;
    private final PointsService pointsService;
    private final ClanService clanService;
    private final PluginConfig config;

    public PlayerCombatListener(Plugin plugin, PlayerProfileService profileService, PointsService pointsService,
                                ClanService clanService, PluginConfig config) {
        this.plugin = plugin;
        this.profileService = profileService;
        this.pointsService = pointsService;
        this.clanService = clanService;
        this.config = config;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer != null && !killer.equals(victim)) {
            handleKill(victim, killer, event);
        } else {
            handleSelfDeath(victim, event);
        }
    }

    private void handleKill(Player victim, Player killer, PlayerDeathEvent event) {
        KillResult result = pointsService.applyKill(killer.getUniqueId(), victim.getUniqueId());

        TitlesConfig titles = config.getUi().getTitles();
        TitlesConfig.KillTitleConfig killCfg = titles.getKill();
        TitlesConfig.SelfDeathTitleConfig selfCfg = titles.getSelfDeath();
        String symbol = config.getUi().getSkullSymbol();

        String killerTitle = killCfg.getTitle().replace("<symbol>", symbol);
        String victimTitle = selfCfg.getTitle().replace("<symbol>", symbol);

        String killerSub = killCfg.getSubtitleKiller()
                .replace("<delta_killer>", String.valueOf(result.getKillerDelta()))
                .replace("<points_killer_after>", String.valueOf(result.getKillerAfter()));
        String victimSub = killCfg.getSubtitleVictim()
                .replace("<delta_victim>", String.valueOf(-result.getVictimDelta()))
                .replace("<points_victim_after>", String.valueOf(result.getVictimAfter()));

        int fadeIn = titles.getTimings().getFadeIn();
        int stay = titles.getTimings().getStay();
        int fadeOut = titles.getTimings().getFadeOut();

        killer.sendTitle(killerTitle, killerSub, fadeIn, stay, fadeOut);
        victim.sendTitle(victimTitle, victimSub, fadeIn, stay, fadeOut);

        String killerMsg = plugin.getConfig().getString("ui.messages.kill.killer",
                "&aZabiłeś &f<victim_name>&a! &7(+<delta_killer> pkt, masz teraz &f<points_killer_after>&7)");
        String victimMsg = plugin.getConfig().getString("ui.messages.kill.victim",
                "&cZginąłeś z rąk &f<killer_name>&c! &7(-<delta_victim> pkt, masz teraz &f<points_victim_after>&7)");
        killerMsg = killerMsg
                .replace("<victim_name>", victim.getName())
                .replace("<delta_killer>", String.valueOf(result.getKillerDelta()))
                .replace("<points_killer_after>", String.valueOf(result.getKillerAfter()));
        victimMsg = victimMsg
                .replace("<killer_name>", killer.getName())
                .replace("<delta_victim>", String.valueOf(-result.getVictimDelta()))
                .replace("<points_victim_after>", String.valueOf(result.getVictimAfter()));

        killer.sendMessage(colorize(killerMsg));
        victim.sendMessage(colorize(victimMsg));

        String broadcast = plugin.getConfig().getString(
                "ui.messages.kill.broadcast",
                "&f<victim_name> &7(-<delta_victim>) został zabity przez &f<killer_name> &7(+<delta_killer>)"
        );
        broadcast = broadcast
                .replace("<victim_name>", victim.getName())
                .replace("<killer_name>", killer.getName())
                .replace("<delta_killer>", String.valueOf(result.getKillerDelta()))
                .replace("<delta_victim>", String.valueOf(-result.getVictimDelta()));
        event.setDeathMessage(colorize(broadcast));
    }

    private void handleSelfDeath(Player victim, PlayerDeathEvent event) {
        EntityDamageEvent lastDamage = event.getEntity().getLastDamageCause();
        EntityDamageEvent.DamageCause cause = lastDamage != null ? lastDamage.getCause() : EntityDamageEvent.DamageCause.CUSTOM;
        int loss = pointsService.applySelfDeath(victim.getUniqueId(), cause);
        int after = pointsService.getPoints(victim.getUniqueId());

        TitlesConfig titles = config.getUi().getTitles();
        TitlesConfig.SelfDeathTitleConfig selfCfg = titles.getSelfDeath();
        String symbol = config.getUi().getSkullSymbol();

        String titleText = selfCfg.getTitle().replace("<symbol>", symbol);
        String subtitle = selfCfg.getSubtitle()
                .replace("<delta_victim>", String.valueOf(loss))
                .replace("<points_victim_after>", String.valueOf(after));

        int fadeIn = titles.getTimings().getFadeIn();
        int stay = titles.getTimings().getStay();
        int fadeOut = titles.getTimings().getFadeOut();

        victim.sendTitle(titleText, subtitle, fadeIn, stay, fadeOut);

        String path = "ui.messages.selfDeath.causes." + cause.name();
        String msg = plugin.getConfig().getString(path, plugin.getConfig().getString(
                "ui.messages.selfDeath.default",
                "&cZginąłeś! &7(-<delta_victim> pkt, masz teraz &f<points_victim_after>&7)"
        ));
        msg = msg.replace("<delta_victim>", String.valueOf(loss))
                .replace("<points_victim_after>", String.valueOf(after));
        victim.sendMessage(colorize(msg));

        String broadcast = plugin.getConfig().getString(
                "ui.messages.selfDeath.broadcast." + cause.name(),
                plugin.getConfig().getString(
                        "ui.messages.selfDeath.broadcast.default",
                        "&f<victim_name> &7(-<delta_victim>) zginął."
                )
        );
        broadcast = broadcast
                .replace("<victim_name>", victim.getName())
                .replace("<delta_victim>", String.valueOf(loss));
        event.setDeathMessage(colorize(broadcast));
    }

    private String colorize(String input) {
        return input.replace("&", "§");
    }
}

