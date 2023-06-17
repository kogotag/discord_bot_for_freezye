package bot.modules.rules;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.time.LocalDateTime;

public class RulesSetTextInfo {
    private String userId;
    private String guildId;
    private LocalDateTime localDateTime;
    private String channelId;

    public RulesSetTextInfo(String userId, String guildId, LocalDateTime localDateTime, String channelId) {
        this.userId = userId;
        this.guildId = guildId;
        this.localDateTime = localDateTime;
        this.channelId = channelId;
    }

    public String getUserId() {
        return userId;
    }

    public String getGuildId() {
        return guildId;
    }

    public LocalDateTime getLocalDateTime() {
        return localDateTime;
    }

    public String getChannelId() {
        return channelId;
    }
}
