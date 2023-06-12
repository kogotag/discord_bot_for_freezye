package bot.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class DynamicConfig {
    private static DynamicConfig config;
    private static String configFileName = "/dynamic_config.json";
    private List<GuildConfig> guildConfigs;
    private PMConfig pmConfig;
    private int lastSubscriptionId;

    public DynamicConfig() {
        guildConfigs = new ArrayList<>();
        pmConfig = new PMConfig();
    }

    public static DynamicConfig getConfig() {
        if (config == null) {
            Gson gson = new GsonBuilder().create();
            String pathToConfig = new File(StaticConfig.class.getProtectionDomain().getCodeSource().getLocation()
                    .getPath()).getParent() + "/" + configFileName;
            File configFile = new File(pathToConfig);
            try {
                if (configFile.exists()) {
                    JsonReader reader = new JsonReader(new FileReader(configFile, StandardCharsets.UTF_8));
                    config = gson.fromJson(reader, DynamicConfig.class);
                    reader.close();
                } else {
                    config = new DynamicConfig();
                    save();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return config;
    }

    public static void save() {
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
        String pathToConfig = new File(StaticConfig.class.getProtectionDomain().getCodeSource().getLocation()
                .getPath()).getParent() + "/" + configFileName;
        File configFile = new File(pathToConfig);
        try {
            FileWriter writer = new FileWriter(configFile, StandardCharsets.UTF_8, false);
            writer.write(gson.toJson(config, DynamicConfig.class));
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public GuildConfig getGuildConfigById(String id) {
        return guildConfigs
                .stream()
                .filter(guildConfig -> guildConfig.getGuildId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public List<GuildConfig> getGuildConfigs() {
        return guildConfigs;
    }

    public PMConfig getPmConfig() {
        return pmConfig;
    }

    public void addGuildConfig(GuildConfig guildConfig) {
        guildConfigs.add(guildConfig);
        DynamicConfig.save();
    }

    public int getLastSubscriptionId() {
        return lastSubscriptionId;
    }

    public void setLastSubscriptionId(int lastSubscriptionId) {
        this.lastSubscriptionId = lastSubscriptionId;
        save();
    }

    public void incLastSubscriptionId() {
        lastSubscriptionId++;
        save();
    }
}
