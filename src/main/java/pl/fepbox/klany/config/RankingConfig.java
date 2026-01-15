package pl.fepbox.klany.config;

public class RankingConfig {
    private final int pageSize;
    private final String playerFormat;
    private final String playerFormatSelf;
    private final String clanFormat;
    private final String clanFormatSelf;

    public RankingConfig(int pageSize, String playerFormat, String playerFormatSelf, String clanFormat, String clanFormatSelf) {
        this.pageSize = pageSize;
        this.playerFormat = playerFormat;
        this.playerFormatSelf = playerFormatSelf;
        this.clanFormat = clanFormat;
        this.clanFormatSelf = clanFormatSelf;
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
}

