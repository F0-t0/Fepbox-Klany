package pl.fepbox.klany.points;

public class KillResult {

    private final int killerDelta;
    private final int victimDelta;
    private final int killerAfter;
    private final int victimAfter;

    public KillResult(int killerDelta, int victimDelta, int killerAfter, int victimAfter) {
        this.killerDelta = killerDelta;
        this.victimDelta = victimDelta;
        this.killerAfter = killerAfter;
        this.victimAfter = victimAfter;
    }

    public int getKillerDelta() {
        return killerDelta;
    }

    public int getVictimDelta() {
        return victimDelta;
    }

    public int getKillerAfter() {
        return killerAfter;
    }

    public int getVictimAfter() {
        return victimAfter;
    }
}

