package pl.fepbox.klany.config;

public class LimitsConfig {
    private final int tagMaxLength;
    private final int nameMaxLength;

    public LimitsConfig(int tagMaxLength, int nameMaxLength) {
        this.tagMaxLength = tagMaxLength;
        this.nameMaxLength = nameMaxLength;
    }

    public int getTagMaxLength() {
        return tagMaxLength;
    }

    public int getNameMaxLength() {
        return nameMaxLength;
    }
}

