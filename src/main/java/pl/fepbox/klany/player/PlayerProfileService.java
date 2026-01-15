package pl.fepbox.klany.player;

import java.util.UUID;

public interface PlayerProfileService {

    PlayerProfile getOrCreateProfile(UUID uuid, String name);

    void saveProfileAsync(PlayerProfile profile);
}

