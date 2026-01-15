package pl.fepbox.klany.config;

public class UIConfig {

    private final String skullSymbol;
    private final TitlesConfig titles;

    public UIConfig(String skullSymbol, TitlesConfig titles) {
        this.skullSymbol = skullSymbol;
        this.titles = titles;
    }

    public String getSkullSymbol() {
        return skullSymbol;
    }

    public TitlesConfig getTitles() {
        return titles;
    }
}

