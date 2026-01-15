package pl.fepbox.klany.config;

public class TitlesConfig {

    private final KillTitleConfig kill;
    private final SelfDeathTitleConfig selfDeath;
    private final TitleTimings timings;

    public TitlesConfig(KillTitleConfig kill, SelfDeathTitleConfig selfDeath, TitleTimings timings) {
        this.kill = kill;
        this.selfDeath = selfDeath;
        this.timings = timings;
    }

    public KillTitleConfig getKill() {
        return kill;
    }

    public SelfDeathTitleConfig getSelfDeath() {
        return selfDeath;
    }

    public TitleTimings getTimings() {
        return timings;
    }

    public static class KillTitleConfig {
        private final String title;
        private final String subtitleKiller;
        private final String subtitleVictim;

        public KillTitleConfig(String title, String subtitleKiller, String subtitleVictim) {
            this.title = title;
            this.subtitleKiller = subtitleKiller;
            this.subtitleVictim = subtitleVictim;
        }

        public String getTitle() {
            return title;
        }

        public String getSubtitleKiller() {
            return subtitleKiller;
        }

        public String getSubtitleVictim() {
            return subtitleVictim;
        }
    }

    public static class SelfDeathTitleConfig {
        private final String title;
        private final String subtitle;

        public SelfDeathTitleConfig(String title, String subtitle) {
            this.title = title;
            this.subtitle = subtitle;
        }

        public String getTitle() {
            return title;
        }

        public String getSubtitle() {
            return subtitle;
        }
    }

    public static class TitleTimings {
        private final int fadeIn;
        private final int stay;
        private final int fadeOut;

        public TitleTimings(int fadeIn, int stay, int fadeOut) {
            this.fadeIn = fadeIn;
            this.stay = stay;
            this.fadeOut = fadeOut;
        }

        public int getFadeIn() {
            return fadeIn;
        }

        public int getStay() {
            return stay;
        }

        public int getFadeOut() {
            return fadeOut;
        }
    }
}

