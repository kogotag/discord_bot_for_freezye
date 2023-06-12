package bot.config;

import java.util.ArrayList;
import java.util.List;

public class GuildConfig {
    private String prefix;
    private static final String defaultPrefix = "!";
    private static final String defaultRulesEmoji = "✅";
    private final String guildId;
    private List<String> botAdminRoles;
    private boolean rulesEnabled;
    private String rulesChannelId;
    private String rulesText;
    private String rulesAcceptedRoleId;
    private String rulesMessageId;
    private List<Subscription> subscriptions;

    public GuildConfig(String guildId) {
        this.guildId = guildId;
        this.prefix = GuildConfig.defaultPrefix;
        botAdminRoles = new ArrayList<>();
        subscriptions = new ArrayList<>();
        rulesText = "";
    }

    public String getPrefix() {
        if (prefix == null || prefix.isEmpty()) {
            prefix = defaultPrefix;
        }

        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
        DynamicConfig.save();
    }

    public String getGuildId() {
        return guildId;
    }

    public List<String> getBotAdminRoles() {
        return botAdminRoles;
    }

    public void setBotAdminRoles(List<String> botAdminRoles) {
        this.botAdminRoles = botAdminRoles;
    }

    public void addBotAdminRole(String roleId) {
        botAdminRoles.add(roleId);
        DynamicConfig.save();
    }

    public void removeBotAdminRole(String roleId) {
        botAdminRoles.remove(roleId);
        DynamicConfig.save();
    }

    public boolean areRulesEnabled() {
        return rulesEnabled;
    }

    public void setRulesEnabled(boolean rulesEnabled) {
        this.rulesEnabled = rulesEnabled;
        DynamicConfig.save();
    }

    public String getRulesChannelId() {
        return rulesChannelId;
    }

    public void setRulesChannelId(String rulesChannelId) {
        this.rulesChannelId = rulesChannelId;
        DynamicConfig.save();
    }

    public String getRulesText() {
        return rulesText;
    }

    public void setRulesText(String rulesText) {
        this.rulesText = rulesText;
        DynamicConfig.save();
    }

    public String getRulesAcceptedRoleId() {
        return rulesAcceptedRoleId;
    }

    public void setRulesAcceptedRoleId(String rulesAcceptedRoleId) {
        this.rulesAcceptedRoleId = rulesAcceptedRoleId;
        DynamicConfig.save();
    }

    public String getRulesMessageId() {
        return rulesMessageId;
    }

    public void setRulesMessageId(String rulesMessageId) {
        this.rulesMessageId = rulesMessageId;
        DynamicConfig.save();
    }

    public static String getDefaultPrefix() {
        return defaultPrefix;
    }

    public static String getDefaultRulesEmoji() {
        return defaultRulesEmoji;
    }

    public List<Subscription> getSubscriptions() {
        if (subscriptions == null) {
            subscriptions = new ArrayList<>();
        }
        return subscriptions;
    }

    public void setSubscriptions(List<Subscription> subscriptions) {
        this.subscriptions = subscriptions;
        DynamicConfig.save();
    }

    public void addSubscription(Subscription subscription) {
        subscriptions.add(subscription);
        DynamicConfig.save();
    }

    public void removeSubscription(Subscription subscription) {
        subscriptions.remove(subscription);
        DynamicConfig.save();
    }
}
