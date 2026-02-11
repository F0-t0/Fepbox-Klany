package pl.fepbox.klany.config;

public class RankingConfig {
    private final int pageSize;
    private final String playerFormat;
    private final String playerFormatSelf;
    private final String clanFormat;
    private final String clanFormatSelf;
    private final long killCooldownSeconds;
    private final boolean ignoreSameIpKills;

    public RankingConfig(int pageSize,
                         String playerFormat,
                         String playerFormatSelf,
                         String clanFormat,
                         String clanFormatSelf,
                         long killCooldownSeconds,
                         boolean ignoreSameIpKills) {
        this.pageSize = pageSize;
        this.playerFormat = playerFormat;
        this.playerFormatSelf = playerFormatSelf;
        this.clanFormat = clanFormat;
        this.clanFormatSelf = clanFormatSelf;
        this.killCooldownSeconds = killCooldownSeconds;
        this.ignoreSameIpKills = ignoreSameIpKills;
    }

    public int getPageSize() {
        return pageSize;
    }

    public String getPlayerFormat() {
        return playerFormat;
    }

    public String getPlayerFormatSelf() {
        return playerFormatSelf;
    }

    public String getClanFormat() {
        return clanFormat;
    }

    public String getClanFormatSelf() {
        return clanFormatSelf;
    }

    public long getKillCooldownSeconds() {
        return killCooldownSeconds;
    }

    public boolean isIgnoreSameIpKills() {
        return ignoreSameIpKills;
    }
}
