package pl.fepbox.klany.player;

import java.util.UUID;

public class PlayerProfile {

    private final UUID uuid;
    private String name;
    private int points;

    public PlayerProfile(UUID uuid, String name, int points) {
        this.uuid = uuid;
        this.name = name;
        this.points = points;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }
}

