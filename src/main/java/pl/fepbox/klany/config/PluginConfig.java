package pl.fepbox.klany.config;

public class PluginConfig {

    private final LimitsConfig limits;
    private final FilterConfig filter;
    private final PointsConfig points;
    private final UIConfig ui;
    private final RankingConfig ranking;
    private final PlaceholderConfig placeholders;
    private final StorageConfig storage;

    public PluginConfig(
            LimitsConfig limits,
            FilterConfig filter,
            PointsConfig points,
            UIConfig ui,
            RankingConfig ranking,
            PlaceholderConfig placeholders,
            StorageConfig storage
    ) {
        this.limits = limits;
        this.filter = filter;
        this.points = points;
        this.ui = ui;
        this.ranking = ranking;
        this.placeholders = placeholders;
        this.storage = storage;
    }

    public LimitsConfig getLimits() {
        return limits;
    }

    public FilterConfig getFilter() {
        return filter;
    }

    public PointsConfig getPoints() {
        return points;
    }

    public UIConfig getUi() {
        return ui;
    }

    public RankingConfig getRanking() {
        return ranking;
    }

    public PlaceholderConfig getPlaceholders() {
        return placeholders;
    }

    public StorageConfig getStorage() {
        return storage;
    }
}

