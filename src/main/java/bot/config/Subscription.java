package bot.config;

import java.util.HashMap;
import java.util.Objects;

public class Subscription {
    private int id;
    private String name;
    private String guildId;
    private String channelId;
    private String messageId;
    private String description;
    private HashMap<String, String> emojiRoleIdDictionary;
    private boolean trigger;
    private boolean cooldown;
    private long cooldownInSeconds;

    public Subscription(int id, String name, String guildId, String channelId, String messageId, String description, HashMap<String, String> emojiRoleIdDictionary, boolean trigger, boolean cooldown, long cooldownInSeconds) {
        this.id = id;
        this.name = name;
        this.guildId = guildId;
        this.channelId = channelId;
        this.messageId = messageId;
        this.description = description;
        this.emojiRoleIdDictionary = emojiRoleIdDictionary;
        this.trigger = trigger;
        this.cooldown = cooldown;
        this.cooldownInSeconds = cooldownInSeconds;
    }

    public String getName() {
        return name;
    }

    public String getGuildId() {
        return guildId;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getDescription() {
        return description;
    }

    public HashMap<String, String> getEmojiRoleIdDictionary() {
        return emojiRoleIdDictionary;
    }

    public boolean isTrigger() {
        return trigger;
    }

    public boolean isCooldown() {
        return cooldown;
    }

    public long getCooldownInSeconds() {
        return cooldownInSeconds;
    }

    public int getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Subscription that = (Subscription) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
