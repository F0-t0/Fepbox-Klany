package pl.fepbox.klany.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.Plugin;
import pl.fepbox.klany.clan.Clan;
import pl.fepbox.klany.clan.ClanService;
import pl.fepbox.klany.config.Messages;
import pl.fepbox.klany.config.PluginConfig;
import pl.fepbox.klany.config.TitlesConfig;
import pl.fepbox.klany.points.KillResult;
import pl.fepbox.klany.points.PointsService;
import pl.fepbox.klany.util.ColorUtil;

public class PlayerCombatListener implements Listener {

    private final Plugin plugin;
    private final PointsService pointsService;
    private final ClanService clanService;
    private final PluginConfig config;
    private final Messages messages;

    public PlayerCombatListener(Plugin plugin, PointsService pointsService,
                                ClanService clanService, PluginConfig config, Messages messages) {
        this.plugin = plugin;
        this.pointsService = pointsService;
        this.clanService = clanService;
        this.config = config;
        this.messages = messages;
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }
        var victimClan = clanService.getClanByPlayer(victim.getUniqueId());
        var attackerClan = clanService.getClanByPlayer(attacker.getUniqueId());
        if (victimClan.isPresent() && attackerClan.isPresent() && victimClan.get().getUuid().equals(attackerClan.get().getUuid())) {
            if (!victimClan.get().isPvpEnabled()) {
                attacker.sendMessage(ColorUtil.colorize(messages.get("ui.friendlyFireBlocked", "<RED>PvP w klanie jest wylaczone")));
                event.setCancelled(true);
            }
        }
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
        if (config.getRanking().isIgnoreSameIpKills() && sameIp(killer, victim)) {
            String msg = messages.get("ui.kill.sameIp", "<RED>Zabojstwo nie liczy sie (to samo IP).");
            killer.sendMessage(ColorUtil.colorize(msg));
            victim.sendMessage(ColorUtil.colorize(msg));
            event.setDeathMessage(null);
            return;
        }

        var killerClanOpt = clanService.getClanByPlayer(killer.getUniqueId());
        var victimClanOpt = clanService.getClanByPlayer(victim.getUniqueId());
        if (killerClanOpt.isPresent() && victimClanOpt.isPresent()
                && killerClanOpt.get().getUuid().equals(victimClanOpt.get().getUuid())
                && !killerClanOpt.get().isPvpEnabled()) {
            String msg = messages.get("ui.friendlyFireBlocked", "<RED>PvP w klanie jest wylaczone");
            killer.sendMessage(ColorUtil.colorize(msg));
            event.setDeathMessage(null);
            return;
        }

        KillResult result = pointsService.applyKill(killer.getUniqueId(), victim.getUniqueId());
        if (result.getKillerDelta() == 0 && result.getVictimDelta() == 0) {
            String msg = messages.get("ui.kill.cooldown", "<YELLOW>Brak punktow - ofiara ma jeszcze cooldown");
            killer.sendMessage(ColorUtil.colorize(msg.replace("<victim_name>", victim.getName())));
            victim.sendMessage(ColorUtil.colorize(msg.replace("<victim_name>", victim.getName())));
            return;
        }

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

        String killerMsg = messages.get("ui.kill.killer",
                "&aZabiles &f<victim_name>&a! &7(+<delta_killer> pkt, masz teraz &f<points_killer_after>&7)");
        String victimMsg = messages.get("ui.kill.victim",
                "&cZginales z rak &f<killer_name>&c! &7(-<delta_victim> pkt, masz teraz &f<points_victim_after>&7)");
        killerMsg = killerMsg
                .replace("<victim_name>", victim.getName())
                .replace("<delta_killer>", String.valueOf(result.getKillerDelta()))
                .replace("<points_killer_after>", String.valueOf(result.getKillerAfter()));
        victimMsg = victimMsg
                .replace("<killer_name>", killer.getName())
                .replace("<delta_victim>", String.valueOf(-result.getVictimDelta()))
                .replace("<points_victim_after>", String.valueOf(result.getVictimAfter()));

        killer.sendMessage(ColorUtil.colorize(killerMsg));
        victim.sendMessage(ColorUtil.colorize(victimMsg));

        String broadcast = messages.get(
                "ui.kill.broadcast",
                "&f<victim_name> &7(-<delta_victim>) zostal zabity przez &f<killer_name> &7(+<delta_killer>)"
        );
        broadcast = broadcast
                .replace("<victim_name>", victim.getName())
                .replace("<killer_name>", killer.getName())
                .replace("<delta_killer>", String.valueOf(result.getKillerDelta()))
                .replace("<delta_victim>", String.valueOf(-result.getVictimDelta()));
        event.setDeathMessage(ColorUtil.colorize(broadcast));
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

        String path = "ui.selfDeath.causes." + cause.name();
        String msg = messages.get(path, messages.get("ui.selfDeath.default",
                "&cZginales! &7(-<delta_victim> pkt, masz teraz &f<points_victim_after>&7)"));
        msg = msg.replace("<delta_victim>", String.valueOf(loss))
                .replace("<points_victim_after>", String.valueOf(after));
        victim.sendMessage(ColorUtil.colorize(msg));

        String broadcast = messages.get(
                "ui.selfDeath.broadcast." + cause.name(),
                messages.get("ui.selfDeath.broadcast.default",
                        "&f<victim_name> &7(-<delta_victim>) zginal."
                )
        );
        broadcast = broadcast
                .replace("<victim_name>", victim.getName())
                .replace("<delta_victim>", String.valueOf(loss));
        event.setDeathMessage(ColorUtil.colorize(broadcast));
    }

    private boolean sameIp(Player a, Player b) {
        if (a.getAddress() == null || b.getAddress() == null) {
            return false;
        }
        return a.getAddress().getAddress().getHostAddress().equalsIgnoreCase(b.getAddress().getAddress().getHostAddress());
    }
}
