package bot.modules;

import bot.config.DynamicConfig;
import bot.config.GuildConfig;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.apache.commons.collections4.KeyValue;
import org.apache.commons.text.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class RulesModule extends BotModule {
    private Message rulesMessage;
    private List<RulesSetTextInfo> rulesSetTextInfoList;

    public RulesModule(JDA jda) {
        super(jda);
        rulesSetTextInfoList = new ArrayList<>();
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        super.onReady(event);
        List<Guild> guilds = jda.getSelfUser().getMutualGuilds();

        for (Guild guild :
                guilds) {
            GuildConfig guildConfig = DynamicConfig.getConfig().getGuildConfigById(guild.getId());

            if (guildConfig == null) {
                System.out.println("GuildConfig not found on guild: " + guild.getName());
                continue;
            }

            if (!guildConfig.areRulesEnabled()) {
                continue;
            }

            TextChannel channel = guild.getTextChannelById(guildConfig.getRulesChannelId());
            if (channel == null) {
                System.out.println("rules channel not found on guild: " + guild.getName());
                guildConfig.setRulesEnabled(false);
                continue;
            }

            String messageId = guildConfig.getRulesMessageId();

            if (messageId == null) {
                System.out.println("message id is null on guild: " + guild.getName());
                guildConfig.setRulesEnabled(false);
                continue;
            }

            try {
                rulesMessage = channel.retrieveMessageById(guildConfig.getRulesMessageId()).complete();
            } catch (ErrorResponseException e) {
                if (e.getErrorResponse().equals(ErrorResponse.UNKNOWN_MESSAGE)) {
                    System.out.println("rules message not found on guild: " + guild.getName());
                    guildConfig.setRulesEnabled(false);
                }
            }
        }

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                List<RulesSetTextInfo> toRemove = new ArrayList<>();
                LocalDateTime now = LocalDateTime.now();
                for (RulesSetTextInfo info : rulesSetTextInfoList) {
                    if (Math.abs(Duration.between(now, info.getLocalDateTime()).toMinutes()) >= 5) {
                        toRemove.add(info);
                    }
                }

                for (RulesSetTextInfo info : toRemove) {
                    rulesSetTextInfoList.remove(info);
                }
            }
        }, 0, 120000);
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        super.onSlashCommandInteraction(event);

        Guild guild = event.getGuild();

        if (guild == null) {
            return;
        }

        GuildConfig guildConfig = DynamicConfig.getConfig().getGuildConfigById(guild.getId());

        if (guildConfig == null) {
            return;
        }

        if (event.getName().equals("rules_set_channel")) {
            rulesSetChannel(event, guildConfig);
        } else if (event.getName().equals("rules_set_text")) {
            rulesSetText(event, guildConfig);
        } else if (event.getName().equals("rules_set_role")) {
            rulesSetRole(event, guildConfig);
        } else if (event.getName().equals("rules_enable")) {
            rulesEnable(event, guildConfig);
        } else if (event.getName().equals("rules_disable")) {
            rulesDisable(event, guildConfig);
        } else if (event.getName().equals("rules_info")) {
            rulesInfo(event);
        } else if (event.getName().equals("rules_send_new_message")) {
            rulesSendNewMessage(event, guildConfig);
        } else if (event.getName().equals("rules_update_message")) {
            rulesUpdateMessage(event, guildConfig);
        }
    }

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        super.onMessageReactionAdd(event);

        if (Objects.equals(event.getUser(), jda.getSelfUser())) {
            return;
        }

        Guild guild = event.getGuild();
        GuildConfig guildConfig = DynamicConfig.getConfig().getGuildConfigById(guild.getId());

        if (rulesMessage == null) {
            return;
        }

        if (!event.getMessageId().equals(rulesMessage.getId())) {
            return;
        }

        if (!event.getEmoji().getFormatted().equals(GuildConfig.getDefaultRulesEmoji())) {
            return;
        }

        Role role = guild.getRoleById(guildConfig.getRulesAcceptedRoleId());

        if (role == null) {
            System.out.println("rules role not found on guild: " + guild.getName());
            guildConfig.setRulesEnabled(false);
            return;
        }

        String userId = event.getUserId();
        Member member = null;
        try {
            member = guild.retrieveMemberById(userId).complete();
        } catch (ErrorResponseException ignored) {
        }

        if (member == null) {
            event.getChannel().sendMessage("Простите, я опять мразь, и не вижу юзера\n" +
                    "вот userId из эвента: " + event.getUserId()
                    + "\nА вот я пытаюсь сделать ретрив мембера: "
                    + guild.retrieveMemberById(event.getUserId()).complete()).queue();
            return;
        }

        guild.addRoleToMember(member, role).queue(null, new ErrorHandler()
                .handle(ErrorResponse.MISSING_PERMISSIONS, e -> {
                    System.out.println("missing permissions to add roles to users on guild: " + guild.getName());
                    guildConfig.setRulesEnabled(false);
                }));
    }

    @Override
    public void onMessageReactionRemove(@NotNull MessageReactionRemoveEvent event) {
        super.onMessageReactionRemove(event);

        if (Objects.equals(event.getUser(), jda.getSelfUser())) {
            return;
        }

        Guild guild = event.getGuild();
        GuildConfig guildConfig = DynamicConfig.getConfig().getGuildConfigById(guild.getId());

        if (rulesMessage == null) {
            return;
        }

        if (!event.getMessageId().equals(rulesMessage.getId())) {
            return;
        }

        if (!event.getEmoji().getFormatted().equals(GuildConfig.getDefaultRulesEmoji())) {
            return;
        }

        Role role = guild.getRoleById(guildConfig.getRulesAcceptedRoleId());

        if (role == null) {
            System.out.println("rules role not found on guild: " + guild.getName());
            guildConfig.setRulesEnabled(false);
            return;
        }

        String userId = event.getUserId();
        Member member = null;
        try {
            member = guild.retrieveMemberById(userId).complete();
        } catch (ErrorResponseException ignored) {
        }

        if (member == null) {
            return;
        }

        guild.removeRoleFromMember(member, role).queue(null, new ErrorHandler()
                .handle(ErrorResponse.MISSING_PERMISSIONS, e -> {
                    System.out.println("missing permissions to add roles to users on guild: " + guild.getName());
                    guildConfig.setRulesEnabled(false);
                }));
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        super.onMessageReceived(event);

        if (jda.getSelfUser().equals(event.getAuthor())) {
            return;
        }

        if (!event.getChannel().getType().equals(ChannelType.TEXT)) {
            return;
        }

        if (event.getMessage().getContentRaw().isEmpty()) {
            return;
        }

        RulesSetTextInfo rulesSetTextInfo = rulesSetTextInfoList
                .stream()
                .filter(info -> info.getUserId().equals(event.getAuthor().getId()) &&
                        info.getGuildId().equals(event.getGuild().getId()) &&
                        info.getChannelId().equals(event.getChannel().asTextChannel().getId()))
                .findFirst()
                .orElse(null);

        if (rulesSetTextInfo == null) {
            return;
        }

        GuildConfig guildConfig = DynamicConfig.getConfig().getGuildConfigById(event.getGuild().getId());
        guildConfig.setRulesText(event.getMessage().getContentRaw());
        rulesSetTextInfoList.remove(rulesSetTextInfo);
        event.getChannel().sendMessage("Текст правил успешно обновлён").queue();
    }

    private void rulesSetChannel(SlashCommandInteractionEvent event, GuildConfig guildConfig) {
        if (!hasBotAdminRights(event.getUser(), Objects.requireNonNull(event.getGuild()))) {
            event.reply("У вас нет прав на использование этой команды").setEphemeral(true).queue();
            return;
        }

        GuildChannelUnion channelUnion = Objects.requireNonNull(event.getOption("channel")).getAsChannel();
        if (!channelUnion.getType().equals(ChannelType.TEXT)) {
            event.reply("Канал должен быть текстовым").setEphemeral(true).queue();
            return;
        }

        if (!channelUnion.getGuild().getId().equals(event.getGuild().getId())) {
            event.reply("Канал должен быть из этой гильдии").setEphemeral(true).queue();
            return;
        }

        guildConfig.setRulesChannelId(channelUnion.getId());
        event.reply("Канал для правил назначен").setEphemeral(true).queue();
    }

    private void rulesSetText(SlashCommandInteractionEvent event, GuildConfig guildConfig) {
        if (!hasBotAdminRights(event.getUser(), Objects.requireNonNull(event.getGuild()))) {
            event.reply("У вас нет прав на использование этой команды").setEphemeral(true).queue();
            return;
        }

        if (!event.getChannel().getType().equals(ChannelType.TEXT)) {
            event.reply("Команда доступна только из текстовых каналов").setEphemeral(true).queue();
            return;
        }

        TextChannel channel = event.getChannel().asTextChannel();

        rulesSetTextInfoList.add(new RulesSetTextInfo(
                event.getUser().getId(),
                event.getGuild().getId(),
                LocalDateTime.now(),
                channel.getId()));

        event.reply("Отправьте текст вашим следующим сообщением в этом канале").queue();
    }

    private void rulesSetRole(SlashCommandInteractionEvent event, GuildConfig guildConfig) {
        if (!hasBotAdminRights(event.getUser(), Objects.requireNonNull(event.getGuild()))) {
            event.reply("У вас нет прав на использование этой команды").setEphemeral(true).queue();
            return;
        }

        Role role = Objects.requireNonNull(event.getOption("role")).getAsRole();

        guildConfig.setRulesAcceptedRoleId(role.getId());
        event.reply("Роль установлена").setEphemeral(true).queue();
    }

    private void rulesEnable(SlashCommandInteractionEvent event, GuildConfig guildConfig) {
        if (!hasBotAdminRights(event.getUser(), Objects.requireNonNull(event.getGuild()))) {
            event.reply("У вас нет прав на использование этой команды").setEphemeral(true).queue();
            return;
        }

        if (guildConfig.areRulesEnabled()) {
            event.reply("Модуль правил уже запущен").setEphemeral(true).queue();
            return;
        }

        String channelId = guildConfig.getRulesChannelId();

        if (channelId == null) {
            event.reply("Канал для правил не задан").setEphemeral(true).queue();
            return;
        }

        TextChannel channel = event.getGuild().getTextChannelById(channelId);

        if (channel == null) {
            event.reply("В этой гильдии такой канал не найден").setEphemeral(true).queue();
            return;
        }

        String text = guildConfig.getRulesText();

        if (text == null || text.isEmpty()) {
            event.reply("Текст правил не задан").setEphemeral(true).queue();
            return;
        }

        if (text.length() > 2000) {
            event.reply("Текст правил слишком длинный. На настоящий момент длинные сообщения для правил не поддерживаются").setEphemeral(true).queue();
            return;
        }

        if (guildConfig.getRulesAcceptedRoleId() == null || guildConfig.getRulesAcceptedRoleId().isEmpty()) {
            event.reply("Роль для согласившихся с правилами не определена").setEphemeral(true).queue();
            return;
        }

        Role role = event.getGuild().getRoleById(guildConfig.getRulesAcceptedRoleId());

        if (role == null) {
            event.reply("Такая роль не найдена").setEphemeral(true).queue();
            return;
        }

        Emoji emoji = Emoji.fromFormatted(GuildConfig.getDefaultRulesEmoji());

        Message message = channel.sendMessage(text).complete();

        try {
            message.addReaction(emoji).complete();
        } catch (ErrorResponseException e) {
            if (e.getErrorResponse().equals(ErrorResponse.UNKNOWN_EMOJI)) {
                event.reply("Ошибка: emoji не найден").queue();
                message.delete().queue();
                return;
            }
        }

        guildConfig.setRulesMessageId(message.getId());
        rulesMessage = message;
        guildConfig.setRulesEnabled(true);
        event.reply("Модуль правил успешно запущен").setEphemeral(true).queue();
    }

    private void rulesDisable(SlashCommandInteractionEvent event, GuildConfig guildConfig) {
        if (!hasBotAdminRights(event.getUser(), Objects.requireNonNull(event.getGuild()))) {
            event.reply("У вас нет прав на использование этой команды").setEphemeral(true).queue();
            return;
        }

        if (!guildConfig.areRulesEnabled()) {
            event.reply("Модуль правил уже отключён").setEphemeral(true).queue();
            return;
        }

        guildConfig.setRulesEnabled(false);
        event.reply("Модуль правил отключён").setEphemeral(true).queue();
    }

    private void rulesInfo(SlashCommandInteractionEvent event) {
        event.reply("Для настройки модуля правил на вашем сервере Discord следуйте инструкции:\n\n" +
                        "**1.** Задайте текст правил с помощью команды /rules_set_text\n" +
                        "**2.** Задайте роль, которую следует выдавать после согласия с правилами с помощью команды /rules_set_role\n" +
                        "**3.** Выберите канал, в котором бот разместит правила с помощью команды /rules_set_channel\n" +
                        "**4.** Включите модуль правил с помощью команды /rules_enable\n\n" +
                        "Модуль правил может быть отключен с помощью команды /rules_disable.\n" +
                        "Отправить новое сообщение в канале для правил можно с помощью команды /rules_send_new_message." +
                        "При этом бот попытается удалить предыдущее сообщение, если оно находится в выбранном канале.\n" +
                        "Отредактировать сообщение с правилами можно с помощью команды /rules_update_message." +
                        " При этом сам текст можно изменить с помощью команды /rules_set_text")
                .queue();
    }

    private void rulesSendNewMessage(SlashCommandInteractionEvent event, GuildConfig guildConfig) {
        if (!hasBotAdminRights(event.getUser(), Objects.requireNonNull(event.getGuild()))) {
            event.reply("У вас нет прав на использование этой команды").setEphemeral(true).queue();
            return;
        }

        String channelId = guildConfig.getRulesChannelId();

        if (channelId == null) {
            event.reply("Канал для правил не задан").setEphemeral(true).queue();
            return;
        }

        TextChannel channel = event.getGuild().getTextChannelById(channelId);

        if (channel == null) {
            event.reply("В этой гильдии такой канал не найден").setEphemeral(true).queue();
            return;
        }

        String text = guildConfig.getRulesText();

        if (text == null || text.isEmpty()) {
            event.reply("Текст правил не задан").setEphemeral(true).queue();
            return;
        }

        if (text.length() > 2000) {
            event.reply("Текст правил слишком длинный. На настоящий момент длинные сообщения для правил не поддерживаются").setEphemeral(true).queue();
            return;
        }

        String oldMessageId = guildConfig.getRulesMessageId();
        if (oldMessageId != null) {
            channel.retrieveMessageById(oldMessageId).queue(oldMessage -> oldMessage.delete().queue(), new ErrorHandler()
                    .ignore(ErrorResponse.UNKNOWN_MESSAGE));
        }

        Message message = channel.sendMessage(text).complete();

        if (message == null) {
            event.reply("Ошибка: сообщение не найдено").setEphemeral(true).queue();
            return;
        }

        Emoji emoji = Emoji.fromFormatted(GuildConfig.getDefaultRulesEmoji());

        message.addReaction(emoji).queue();

        guildConfig.setRulesMessageId(message.getId());
        rulesMessage = message;
        event.reply("Новое сообщение отправлено").setEphemeral(true).queue();
    }

    private void rulesUpdateMessage(SlashCommandInteractionEvent event, GuildConfig guildConfig) {
        if (!hasBotAdminRights(event.getUser(), Objects.requireNonNull(event.getGuild()))) {
            event.reply("У вас нет прав на использование этой команды").setEphemeral(true).queue();
            return;
        }

        String channelId = guildConfig.getRulesChannelId();

        if (channelId == null) {
            event.reply("Канал для правил не задан").setEphemeral(true).queue();
            return;
        }

        TextChannel channel = event.getGuild().getTextChannelById(channelId);

        if (channel == null) {
            event.reply("В этой гильдии такой канал не найден").setEphemeral(true).queue();
            return;
        }

        String text = guildConfig.getRulesText();

        if (text == null || text.isEmpty()) {
            event.reply("Текст правил не задан").setEphemeral(true).queue();
            return;
        }

        if (text.length() > 2000) {
            event.reply("Текст правил слишком длинный. На настоящий момент длинные сообщения для правил не поддерживаются").setEphemeral(true).queue();
            return;
        }

        channel.retrieveMessageById(guildConfig.getRulesMessageId()).queue(message -> {
                    message.editMessage(text).queue();
                    event.reply("Сообщение отредактировано").setEphemeral(true).queue();
                },
                new ErrorHandler().handle(ErrorResponse.UNKNOWN_MESSAGE, error -> {
                    event.reply("Сообщение для редактирования не найдено. Модуль правил отключен").setEphemeral(true).queue();
                    guildConfig.setRulesEnabled(false);
                }));
    }
}
