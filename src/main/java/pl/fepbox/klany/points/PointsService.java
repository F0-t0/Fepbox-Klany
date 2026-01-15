package pl.fepbox.klany.points;

import java.util.UUID;
import org.bukkit.event.entity.EntityDamageEvent;

public interface PointsService {

    int getPoints(UUID uuid);

    void setPoints(UUID uuid, int value);

    void addPoints(UUID uuid, int delta);

    KillResult applyKill(UUID killerUuid, UUID victimUuid);

    int applySelfDeath(UUID uuid, EntityDamageEvent.DamageCause cause);
}

