package bot.config;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Objects;

public class SubscriptionBuilder {
    public enum Stage {
        NAME,
        CHANNEL,
        DESCRIPTION,
        DICTIONARY_ROLE,
        DICTIONARY_EMOJI,
        DICTIONARY_QUESTION_MORE,
        TRIGGER,
        COOLDOWN_FLAG,
        COOLDOWN_TIME,
        BUILD
    }

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

    private String builderUserId;
    private String builderChannelId;
    private String builderGuildId;
    private LocalDateTime lastInteractionTime;
    private Stage currentStage;
    private String builderQuestionMessageId;
    private String tempRoleId;

    public SubscriptionBuilder(String builderUserId, String builderChannelId, String builderGuildId) {
        this.builderUserId = builderUserId;
        this.builderChannelId = builderChannelId;
        this.builderGuildId = builderGuildId;
        lastInteractionTime = LocalDateTime.now();
        currentStage = Stage.NAME;
        guildId = builderGuildId;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setGuildId(String guildId) {
        this.guildId = guildId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setEmojiRoleIdDictionary(HashMap<String, String> emojiRoleIdDictionary) {
        this.emojiRoleIdDictionary = emojiRoleIdDictionary;
    }

    public HashMap<String, String> getEmojiRoleIdDictionary() {
        return emojiRoleIdDictionary;
    }

    public void setTrigger(boolean trigger) {
        this.trigger = trigger;
    }

    public void setCooldown(boolean cooldown) {
        this.cooldown = cooldown;
    }

    public void setCooldownInSeconds(long cooldownInSeconds) {
        this.cooldownInSeconds = cooldownInSeconds;
    }

    public Subscription build() {
        return new Subscription(
                id,
                name,
                guildId,
                channelId,
                messageId,
                description,
                emojiRoleIdDictionary,
                trigger,
                cooldown,
                cooldownInSeconds);
    }

    public String getBuilderUserId() {
        return builderUserId;
    }

    public String getBuilderChannelId() {
        return builderChannelId;
    }

    public String getBuilderGuildId() {
        return builderGuildId;
    }

    public LocalDateTime getLastInteractionTime() {
        return lastInteractionTime;
    }

    public Stage getCurrentStage() {
        return currentStage;
    }

    public void setCurrentStage(Stage currentStage) {
        this.currentStage = currentStage;
    }

    public boolean isCurrentStageOnReaction() {
        if (currentStage.equals(Stage.DICTIONARY_QUESTION_MORE) ||
                currentStage.equals(Stage.TRIGGER) ||
                currentStage.equals(Stage.COOLDOWN_FLAG)) {
            return true;
        } else {
            return false;
        }
    }

    public String getBuilderQuestionMessageId() {
        return builderQuestionMessageId;
    }

    public void setBuilderQuestionMessageId(String builderQuestionMessageId) {
        this.builderQuestionMessageId = builderQuestionMessageId;
    }

    public String getTempRoleId() {
        return tempRoleId;
    }

    public void setTempRoleId(String tempRoleId) {
        this.tempRoleId = tempRoleId;
    }

    public int getId() {
        return id;
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

    public boolean isTrigger() {
        return trigger;
    }

    public boolean isCooldown() {
        return cooldown;
    }

    public long getCooldownInSeconds() {
        return cooldownInSeconds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SubscriptionBuilder that = (SubscriptionBuilder) o;
        return Objects.equals(builderUserId, that.builderUserId) && Objects.equals(builderChannelId, that.builderChannelId) && Objects.equals(builderGuildId, that.builderGuildId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(builderUserId, builderChannelId, builderGuildId);
    }
}
