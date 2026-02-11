package pl.fepbox.klany.player;

import java.util.UUID;

public interface PlayerProfileService {

    PlayerProfile getOrCreateProfile(UUID uuid, String name);

    /**
     * Returns profile if present in cache or database without mutating stored name.
     * If not found, returns null.
     */
    PlayerProfile getProfile(UUID uuid);

    void saveProfileAsync(PlayerProfile profile);
}
