package pl.fepbox.klany.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import pl.fepbox.klany.player.PlayerProfileService;

public class PlayerConnectionListener implements Listener {

    private final PlayerProfileService profileService;

    public PlayerConnectionListener(PlayerProfileService profileService) {
        this.profileService = profileService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        profileService.getOrCreateProfile(player.getUniqueId(), player.getName());
    }
}

