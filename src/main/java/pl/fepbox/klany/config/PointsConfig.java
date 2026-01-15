package pl.fepbox.klany.config;

import java.util.EnumMap;
import java.util.Map;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class PointsConfig {

    private final int startPoints;
    private final int minPoints;
    private final int maxPoints;
    private final double baseReward;
    private final double factor;
    private final int minChange;
    private final int maxChange;
    private final int defaultSelfDeathLoss;
    private final Map<DamageCause, Integer> selfDeathLossByCause;

    public PointsConfig(
            int startPoints,
            int minPoints,
            int maxPoints,
            double baseReward,
            double factor,
            int minChange,
            int maxChange,
            int defaultSelfDeathLoss,
            Map<DamageCause, Integer> selfDeathLossByCause
    ) {
        this.startPoints = startPoints;
        this.minPoints = minPoints;
        this.maxPoints = maxPoints;
        this.baseReward = baseReward;
        this.factor = factor;
        this.minChange = minChange;
        this.maxChange = maxChange;
        this.defaultSelfDeathLoss = defaultSelfDeathLoss;
        this.selfDeathLossByCause = new EnumMap<>(selfDeathLossByCause);
    }

    public int getStartPoints() {
        return startPoints;
    }

    public int getMinPoints() {
        return minPoints;
    }

    public int getMaxPoints() {
        return maxPoints;
    }

    public double getBaseReward() {
        return baseReward;
    }

    public double getFactor() {
        return factor;
    }

    public int getMinChange() {
        return minChange;
    }

    public int getMaxChange() {
        return maxChange;
    }

    public int getDefaultSelfDeathLoss() {
        return defaultSelfDeathLoss;
    }

    public int getSelfDeathLoss(DamageCause cause) {
        return selfDeathLossByCause.getOrDefault(cause, defaultSelfDeathLoss);
    }
}

