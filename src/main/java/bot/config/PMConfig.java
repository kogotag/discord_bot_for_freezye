package bot.config;

public class PMConfig {
    private static PMConfig pmConfig;
    private String prefix;
    private static final String defaultPrefix = "!";

    public PMConfig() {
        prefix = defaultPrefix;
    }

    public static PMConfig getPmConfig() {
        if (pmConfig == null) {
            pmConfig = new PMConfig();
        }
        return pmConfig;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
        DynamicConfig.save();
    }
}
