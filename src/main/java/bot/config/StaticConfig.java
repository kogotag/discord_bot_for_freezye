package bot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;

public class StaticConfig {
    private static StaticConfig botConfig;
    private static String configFileName = "/static_config.yaml";

    private String token;

    public static StaticConfig getBotConfig() {
        if (botConfig == null) {
            String pathToConfig = new File(StaticConfig.class.getProtectionDomain().getCodeSource().getLocation()
                    .getPath()).getParent() + "/" + configFileName;
            File configFile = new File(pathToConfig);
            try {
                if (configFile.exists()) {
                    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
                    botConfig = mapper.readValue(configFile, StaticConfig.class);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return botConfig;
    }

    public String getToken() {
        return token;
    }
}
