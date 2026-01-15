package pl.fepbox.klany.config;

public class StorageConfig {

    private final String type;
    private final String file;

    public StorageConfig(String type, String file) {
        this.type = type;
        this.file = file;
    }

    public String getType() {
        return type;
    }

    public String getFile() {
        return file;
    }
}

