package pl.fepbox.klany.config;

import java.util.List;

public class FilterConfig {
    private final String allowedTagRegex;
    private final String allowedNameRegex;
    private final List<String> blockedWords;

    public FilterConfig(String allowedTagRegex, String allowedNameRegex, List<String> blockedWords) {
        this.allowedTagRegex = allowedTagRegex;
        this.allowedNameRegex = allowedNameRegex;
        this.blockedWords = blockedWords;
    }

    public String getAllowedTagRegex() {
        return allowedTagRegex;
    }

    public String getAllowedNameRegex() {
        return allowedNameRegex;
    }

    public List<String> getBlockedWords() {
        return blockedWords;
    }
}

