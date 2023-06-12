package bot.modules;

import bot.config.DynamicConfig;
import bot.config.GuildConfig;
import bot.config.Subscription;
import bot.config.SubscriptionBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class SubscriptionModule extends BotModule {
    private List<SubscriptionBuilder> subscriptionBuilders;
    private List<UserSubscriptionInfo> userSubscriptionInfoList;

    public SubscriptionModule(JDA jda) {
        super(jda);
        subscriptionBuilders = new ArrayList<>();
        userSubscriptionInfoList = new ArrayList<>();
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        super.onSlashCommandInteraction(event);

        if (event.getGuild() == null) {
            return;
        }

        GuildConfig guildConfig = DynamicConfig.getConfig().getGuildConfigById(event.getGuild().getId());

        if (event.getName().equals("subscriptions_list")) {
            subscriptionsList(event, guildConfig);
        } else if (event.getName().equals("subscriptions_remove")) {
            subscriptionsRemove(event, guildConfig);
        } else if (event.getName().equals("subscriptions_create")) {
            subscriptionsCreate(event, guildConfig);
        }
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        super.onMessageReceived(event);

        if (event.getAuthor().equals(jda.getSelfUser())) {
            return;
        }

        Guild guild = event.getGuild();

        List<SubscriptionBuilder> userSubscriptionBuilders = subscriptionBuilders.stream()
                .filter(subscriptionBuilder -> subscriptionBuilder.getBuilderGuildId().equals(guild.getId()))
                .filter(subscriptionBuilder -> subscriptionBuilder.getBuilderChannelId().equals(event.getChannel().getId()))
                .filter(subscriptionBuilder -> subscriptionBuilder.getBuilderUserId().equals(event.getAuthor().getId()))
                .collect(Collectors.toList());

        if (userSubscriptionBuilders.isEmpty()) {
            return;
        }

        SubscriptionBuilder subscriptionBuilder = userSubscriptionBuilders.get(0);

        if (subscriptionBuilder.isCurrentStageOnReaction()) {
            return;
        }

        subscriptionCreateStage(subscriptionBuilder, event.getMessage().getContentRaw(), event.getMessageId(), false);
    }

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        super.onMessageReactionAdd(event);

        if (event.getUserId().equals(jda.getSelfUser().getId())) {
            return;
        }

        Guild guild = event.getGuild();
        GuildConfig guildConfig = dynamicConfig.getGuildConfigById(guild.getId());

        reactionAddSubscriptionBuilder(event, guild);
        reactionAddSubscription(event, guild, guildConfig);
    }

    @Override
    public void onMessageReactionRemove(@NotNull MessageReactionRemoveEvent event) {
        super.onMessageReactionRemove(event);

        if (event.getUserId().equals(jda.getSelfUser().getId())) {
            return;
        }

        Guild guild = event.getGuild();
        GuildConfig guildConfig = dynamicConfig.getGuildConfigById(guild.getId());

        reactionRemoveSubscription(event, guild, guildConfig);
    }

    private void reactionAddSubscriptionBuilder(MessageReactionAddEvent event, Guild guild) {
        List<SubscriptionBuilder> userSubscriptionBuilders = subscriptionBuilders.stream()
                .filter(subscriptionBuilder -> guild.getId().equals(subscriptionBuilder.getBuilderGuildId()))
                .filter(subscriptionBuilder -> event.getMessageId().equals(subscriptionBuilder.getBuilderQuestionMessageId()))
                .filter(subscriptionBuilder -> event.getChannel().getId().equals(subscriptionBuilder.getBuilderChannelId()))
                .filter(subscriptionBuilder -> event.getUserId().equals(subscriptionBuilder.getBuilderUserId()))
                .collect(Collectors.toList());

        if (userSubscriptionBuilders.isEmpty()) {
            return;
        }

        SubscriptionBuilder subscriptionBuilder = userSubscriptionBuilders.get(0);

        if (!subscriptionBuilder.isCurrentStageOnReaction()) {
            return;
        }

        boolean userYesNoAnswer = false;

        if (event.getEmoji().getFormatted().equals(defaultAcceptEmoji)) {
            userYesNoAnswer = true;
        } else if (!event.getEmoji().getFormatted().equals(defaultRejectEmoji)) {
            return;
        }

        subscriptionCreateStage(subscriptionBuilder, "", "", userYesNoAnswer);
    }

    private void reactionAddSubscription(MessageReactionAddEvent event, Guild guild, GuildConfig guildConfig) {
        Subscription subscription = guildConfig.getSubscriptions()
                .stream()
                .filter(subscription1 -> guild.getId().equals(subscription1.getGuildId()))
                .filter(subscription1 -> event.getChannel().getId().equals(subscription1.getChannelId()))
                .filter(subscription1 -> event.getMessageId().equals(subscription1.getMessageId()))
                .findFirst()
                .orElse(null);

        if (subscription == null) {
            return;
        }

        UserSubscriptionInfo userSubscriptionInfo = userSubscriptionInfoList
                .stream()
                .filter(userSubscriptionInfo1 -> userSubscriptionInfo1.getUserId().equals(event.getUserId()))
                .filter(userSubscriptionInfo1 -> userSubscriptionInfo1.getSubscriptionId() == subscription.getId())
                .findFirst()
                .orElse(null);

        User user = jda.retrieveUserById(event.getUserId()).complete();
        Member member = guild.retrieveMemberById(event.getUserId()).complete();

        if (subscription.isCooldown()
                && userSubscriptionInfo != null
                && Math.abs(Duration.between(LocalDateTime.now(), userSubscriptionInfo.getLastSubscription()).getSeconds()) < subscription.getCooldownInSeconds()) {
            user.openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage("Вы можете получить эту роль не раньше, чем через "
                    + (subscription.getCooldownInSeconds() - Math.abs(Duration.between(LocalDateTime.now(), userSubscriptionInfo.getLastSubscription()).getSeconds()))
                    + " секунд").queue());
            return;
        }

        Emoji emoji = event.getEmoji();

        if (!subscription.getEmojiRoleIdDictionary().containsKey(emoji.getFormatted())) {
            return;
        }

        if (subscription.isTrigger()) {
            List<Role> rolesToRemoveFromUser = member.getRoles()
                    .stream()
                    .filter(role -> subscription.getEmojiRoleIdDictionary().containsValue(role.getId()))
                    .collect(Collectors.toList());

            for (Role role : rolesToRemoveFromUser) {
                guild.removeRoleFromMember(member, role).queue();
            }
        }

        Role selectedRole = guild.getRoleById(subscription.getEmojiRoleIdDictionary().get(emoji.getFormatted()));

        if (selectedRole == null) {
            System.out.println("subscription " + subscription.getName()
                    + "in guild " + guild.getName()
                    + " role not found: " + subscription.getEmojiRoleIdDictionary().get(emoji.getFormatted()));
            guildConfig.removeSubscription(subscription);
            return;
        }

        guild.addRoleToMember(member, selectedRole).queue();
        if (userSubscriptionInfo == null) {
            userSubscriptionInfoList.add(new UserSubscriptionInfo(subscription.getId(), event.getUserId()));
        } else {
            userSubscriptionInfo.writeNewSubscriptionTime();
        }
    }

    private void reactionRemoveSubscription(MessageReactionRemoveEvent event, Guild guild, GuildConfig guildConfig) {
        Subscription subscription = guildConfig.getSubscriptions()
                .stream()
                .filter(subscription1 -> guild.getId().equals(subscription1.getGuildId()))
                .filter(subscription1 -> event.getChannel().getId().equals(subscription1.getChannelId()))
                .filter(subscription1 -> event.getMessageId().equals(subscription1.getMessageId()))
                .findFirst()
                .orElse(null);

        if (subscription == null) {
            return;
        }

        Emoji emoji = event.getEmoji();

        if (!subscription.getEmojiRoleIdDictionary().containsKey(emoji.getFormatted())) {
            return;
        }

        Member member = guild.retrieveMemberById(event.getUserId()).complete();

        Role selectedRole = guild.getRoleById(subscription.getEmojiRoleIdDictionary().get(emoji.getFormatted()));

        if (selectedRole == null) {
            System.out.println("subscription " + subscription.getName()
                    + "in guild " + guild.getName()
                    + " role not found: " + subscription.getEmojiRoleIdDictionary().get(emoji.getFormatted()));
            guildConfig.removeSubscription(subscription);
            return;
        }

        guild.removeRoleFromMember(member, selectedRole).queue();
    }

    private void subscriptionsList(SlashCommandInteractionEvent event, GuildConfig config) {
        if (!hasBotAdminRights(event.getUser(), Objects.requireNonNull(event.getGuild()))) {
            event.reply("У вас нет прав на использование этой команды").setEphemeral(true).queue();
            return;
        }

        Guild guild = event.getGuild();

        if (guild == null) {
            return;
        }

        List<Subscription> subscriptions = config.getSubscriptions()
                .stream()
                .filter(subscription -> subscription.getGuildId().equals(guild.getId()))
                .collect(Collectors.toList());

        StringBuilder subscriptionsListString = new StringBuilder();
        subscriptionsListString.append("Список всех подписок на вашем сервере:\n");

        for (Subscription subscription : subscriptions) {
            subscriptionsListString
                    .append("**")
                    .append(subscription.getId())
                    .append("** ")
                    .append(subscription.getName())
                    .append(" в канале <#")
                    .append(subscription.getChannelId())
                    .append(">\n");
        }

        event.reply(subscriptionsListString.toString().trim()).queue();
    }

    private void subscriptionsRemove(SlashCommandInteractionEvent event, GuildConfig config) {
        if (!hasBotAdminRights(event.getUser(), Objects.requireNonNull(event.getGuild()))) {
            event.reply("У вас нет прав на использование этой команды").setEphemeral(true).queue();
            return;
        }

        Guild guild = event.getGuild();

        if (guild == null) {
            return;
        }

        List<Subscription> subscriptions = config.getSubscriptions()
                .stream()
                .filter(subscription -> subscription.getGuildId().equals(guild.getId()))
                .collect(Collectors.toList());

        if (subscriptions.isEmpty()) {
            event.reply("На вашем сервере подписок не найдено").queue();
            return;
        }

        int subscriptionId = Objects.requireNonNull(event.getOption("id")).getAsInt();

        Subscription subscription = subscriptions.stream()
                .filter(subscription1 -> subscription1.getId() == subscriptionId)
                .findFirst()
                .orElse(null);

        if (subscription == null) {
            event.reply("Подписка с этим id не найдена").queue();
            return;
        }

        TextChannel channel = guild.getTextChannelById(subscription.getChannelId());

        if (channel != null) {
            channel.retrieveMessageById(subscription.getMessageId()).queue(message -> message.delete().queue());
        }

        config.removeSubscription(subscription);
        event.reply("Подписка успешно удалена").queue();
    }

    private void subscriptionsCreate(SlashCommandInteractionEvent event, GuildConfig config) {
        if (!hasBotAdminRights(event.getUser(), Objects.requireNonNull(event.getGuild()))) {
            event.reply("У вас нет прав на использование этой команды").setEphemeral(true).queue();
            return;
        }

        subscriptionBuilders.add(new SubscriptionBuilder(event.getUser().getId(),
                event.getChannel().getId(),
                event.getGuild().getId()));
        event.reply("Конструктор подписок создан").queue();
        event.getChannel().sendMessage("Укажите имя подписки. (Имя подписки будет выводится в списке " +
                "всех подписок)").queue();
    }

    private void subscriptionCreateStage(SubscriptionBuilder subscriptionBuilder,
                                         String userResponse,
                                         String userResponseMessageId,
                                         boolean userYesNoAnswer) {
        Guild guild = jda.getGuildById(subscriptionBuilder.getBuilderGuildId());

        if (guild == null) {
            return;
        }

        TextChannel channel = guild.getTextChannelById(subscriptionBuilder.getBuilderChannelId());

        if (channel == null) {
            return;
        }

        if (!subscriptionBuilder.isCurrentStageOnReaction() && (userResponse == null || userResponse.isEmpty())) {
            channel.sendMessage("Пожалуйста, дайте текстовый ответ").queue();
            return;
        }

        GuildConfig guildConfig = DynamicConfig.getConfig().getGuildConfigById(subscriptionBuilder.getBuilderGuildId());

        if (subscriptionBuilder.getCurrentStage().equals(SubscriptionBuilder.Stage.NAME)) {
            subscriptionStageName(subscriptionBuilder, userResponse, channel);
        } else if (subscriptionBuilder.getCurrentStage().equals(SubscriptionBuilder.Stage.CHANNEL)) {
            subscriptionStageChannel(subscriptionBuilder, userResponse, guild, channel);
        } else if (subscriptionBuilder.getCurrentStage().equals(SubscriptionBuilder.Stage.DESCRIPTION)) {
            subscriptionStageDescription(subscriptionBuilder, userResponse, channel);
        } else if (subscriptionBuilder.getCurrentStage().equals(SubscriptionBuilder.Stage.DICTIONARY_ROLE)) {
            subscriptionStageRole(subscriptionBuilder, userResponse, guild, channel);
        } else if (subscriptionBuilder.getCurrentStage().equals(SubscriptionBuilder.Stage.DICTIONARY_EMOJI)) {
            subscriptionStageEmoji(subscriptionBuilder, userResponse, userResponseMessageId, channel);
        } else if (subscriptionBuilder.getCurrentStage().equals(SubscriptionBuilder.Stage.DICTIONARY_QUESTION_MORE)) {
            subscriptionStageQuestionMore(subscriptionBuilder, userYesNoAnswer, channel);
        } else if (subscriptionBuilder.getCurrentStage().equals(SubscriptionBuilder.Stage.TRIGGER)) {
            subscriptionStageTrigger(subscriptionBuilder, userYesNoAnswer, channel);
        } else if (subscriptionBuilder.getCurrentStage().equals(SubscriptionBuilder.Stage.COOLDOWN_FLAG)) {
            subscriptionStageCooldownFlag(subscriptionBuilder, userResponse, userResponseMessageId, userYesNoAnswer, guild, guildConfig, channel);
        } else if (subscriptionBuilder.getCurrentStage().equals(SubscriptionBuilder.Stage.COOLDOWN_TIME)) {
            subscriptionStageCooldownTime(subscriptionBuilder, userResponse, userResponseMessageId, userYesNoAnswer, guild, guildConfig, channel);
        }
    }

    private void subscriptionStageName(SubscriptionBuilder subscriptionBuilder,
                                       String userResponse,
                                       TextChannel channel) {
        subscriptionBuilder.setName(userResponse);
        subscriptionBuilder.setCurrentStage(SubscriptionBuilder.Stage.CHANNEL);
        channel.sendMessage("Укажите ID канала, в котором нужно разместить подписку").queue();
    }

    private void subscriptionStageChannel(SubscriptionBuilder subscriptionBuilder,
                                          String userResponse,
                                          Guild guild,
                                          TextChannel channel) {
        if (!isPositiveLong(userResponse)) {
            channel.sendMessage("Ваш ответ должен быть числом, являющимся ID канала").queue();
            return;
        }

        TextChannel subscriptionChannel = guild.getTextChannelById(userResponse);

        if (subscriptionChannel == null) {
            channel.sendMessage("Канал с этим ID не найден").queue();
            return;
        }

        subscriptionBuilder.setChannelId(userResponse);
        subscriptionBuilder.setCurrentStage(SubscriptionBuilder.Stage.DESCRIPTION);
        channel.sendMessage("Укажите текстовое описание, которое будет в сообщении с подпиской").queue();
    }

    private void subscriptionStageDescription(SubscriptionBuilder subscriptionBuilder,
                                              String userResponse,
                                              TextChannel channel) {
        subscriptionBuilder.setDescription(userResponse);
        subscriptionBuilder.setEmojiRoleIdDictionary(new HashMap<>());
        subscriptionBuilder.setCurrentStage(SubscriptionBuilder.Stage.DICTIONARY_ROLE);
        channel.sendMessage("Теперь нужно указать роли и эмодзи, соответствующие этим ролям. " +
                "К сообщению с подпиской в реакции будут добавлены эмодзи, нажав на которые " +
                "можно будет получить соответствующую роль\nА теперь укажите ID первой роли для подписки").queue();
    }

    private void subscriptionStageRole(SubscriptionBuilder subscriptionBuilder,
                                       String userResponse,
                                       Guild guild,
                                       TextChannel channel) {
        if (!isPositiveLong(userResponse)) {
            channel.sendMessage("Ваш ответ должен быть числом, являющимся ID роли").queue();
            return;
        }

        if (subscriptionBuilder.getEmojiRoleIdDictionary().containsValue(userResponse)) {
            channel.sendMessage("Эта роль уже добавлена").queue();
            return;
        }

        Role role = guild.getRoleById(userResponse);

        if (role == null) {
            channel.sendMessage("Роль с данным ID не найдена").queue();
            return;
        }

        subscriptionBuilder.setTempRoleId(userResponse);
        subscriptionBuilder.setCurrentStage(SubscriptionBuilder.Stage.DICTIONARY_EMOJI);
        channel.sendMessage("Укажите эмодзи, соответствующий этой роли в подписке. " +
                "Бот попробует добавить этот эмодзи к вашему сообщению, чтобы проверить его валидность").queue();
    }

    private void subscriptionStageEmoji(SubscriptionBuilder subscriptionBuilder,
                                        String userResponse,
                                        String userResponseMessageId,
                                        TextChannel channel) {
        Emoji emoji = Emoji.fromFormatted(userResponse);
        try {
            Message userMessage = channel.retrieveMessageById(userResponseMessageId).complete();
            userMessage.addReaction(emoji).complete();
        } catch (ErrorResponseException e) {
            if (e.getErrorResponse().equals(ErrorResponse.UNKNOWN_EMOJI)) {
                channel.sendMessage("Неверный эмодзи").queue();
                return;
            }
        }

        if (subscriptionBuilder.getEmojiRoleIdDictionary().containsKey(userResponse)) {
            channel.sendMessage("Этот эмодзи уже использован в текущей подписке").queue();
            return;
        }

        subscriptionBuilder.getEmojiRoleIdDictionary().put(userResponse, subscriptionBuilder.getTempRoleId());
        subscriptionBuilder.setCurrentStage(SubscriptionBuilder.Stage.DICTIONARY_QUESTION_MORE);
        Message questionMessage = channel.sendMessage("Желаете ли вы добавить больше ролей в подписку?").complete();
        questionMessage.addReaction(Emoji.fromFormatted(defaultAcceptEmoji)).queue();
        questionMessage.addReaction(Emoji.fromFormatted(defaultRejectEmoji)).queue();
        subscriptionBuilder.setBuilderQuestionMessageId(questionMessage.getId());
    }

    private void subscriptionStageQuestionMore(SubscriptionBuilder subscriptionBuilder,
                                               boolean userYesNoAnswer,
                                               TextChannel channel) {
        if (userYesNoAnswer) {
            subscriptionBuilder.setCurrentStage(SubscriptionBuilder.Stage.DICTIONARY_ROLE);
            channel.sendMessage("Укажите ID роли для подписки").queue();
        } else {
            subscriptionBuilder.setCurrentStage(SubscriptionBuilder.Stage.TRIGGER);
            Message questionMessage = channel.sendMessage("Желаете ли вы сделать подписку переключаемой? " +
                    "Пользователь может выбрать только одну роль из подписки," +
                    " теряя при этом предыдущую роль").complete();
            questionMessage.addReaction(Emoji.fromFormatted(defaultAcceptEmoji)).queue();
            questionMessage.addReaction(Emoji.fromFormatted(defaultRejectEmoji)).queue();
            subscriptionBuilder.setBuilderQuestionMessageId(questionMessage.getId());
        }
    }

    private void subscriptionStageTrigger(SubscriptionBuilder subscriptionBuilder,
                                          boolean userYesNoAnswer,
                                          TextChannel channel) {
        subscriptionBuilder.setTrigger(userYesNoAnswer);
        subscriptionBuilder.setCurrentStage(SubscriptionBuilder.Stage.COOLDOWN_FLAG);
        Message questionMessage = channel.sendMessage("Хотите ли вы добавить задержку после выбора роли? " +
                "Пользователь сможет выбрать роль из подписки только 1 раз за " +
                "выбранный промежуток времени").complete();
        questionMessage.addReaction(Emoji.fromFormatted(defaultAcceptEmoji)).queue();
        questionMessage.addReaction(Emoji.fromFormatted(defaultRejectEmoji)).queue();
        subscriptionBuilder.setBuilderQuestionMessageId(questionMessage.getId());
    }

    private void subscriptionStageCooldownFlag(SubscriptionBuilder subscriptionBuilder,
                                               String userResponse,
                                               String userResponseMessageId,
                                               boolean userYesNoAnswer,
                                               Guild guild,
                                               GuildConfig guildConfig,
                                               TextChannel channel) {
        if (userYesNoAnswer) {
            subscriptionBuilder.setCooldown(true);
            subscriptionBuilder.setCurrentStage(SubscriptionBuilder.Stage.COOLDOWN_TIME);
            channel.sendMessage("Укажите время отката в секундах").queue();
        } else {
            subscriptionBuilder.setCurrentStage(SubscriptionBuilder.Stage.BUILD);
            channel.sendMessage("Настройка завершена. Бот добавит подписку в выбранный вами канал").queue();
            subscriptionStageBuild(subscriptionBuilder, guild, guildConfig);
        }
    }

    private void subscriptionStageCooldownTime(SubscriptionBuilder subscriptionBuilder,
                                               String userResponse,
                                               String userResponseMessageId,
                                               boolean userYesNoAnswer,
                                               Guild guild,
                                               GuildConfig guildConfig,
                                               TextChannel channel) {
        if (!isPositiveLong(userResponse)) {
            channel.sendMessage("Ваш ответ должен быть положительным числом").queue();
            return;
        }

        long number;
        try {
            number = Long.parseLong(userResponse);
        } catch (NumberFormatException e) {
            System.out.println("regex didn't check long how it was supposed to");
            return;
        }

        subscriptionBuilder.setCooldownInSeconds(number);
        subscriptionBuilder.setCurrentStage(SubscriptionBuilder.Stage.BUILD);
        channel.sendMessage("Настройка завершена. Бот добавит подписку в выбранный вами канал").queue();
        subscriptionStageBuild(subscriptionBuilder, guild, guildConfig);
    }

    private void subscriptionStageBuild(SubscriptionBuilder subscriptionBuilder,
                                        Guild guild,
                                        GuildConfig guildConfig) {
        TextChannel subscriptionChannel = guild.getTextChannelById(subscriptionBuilder.getChannelId());

        if (subscriptionChannel == null) {
            return;
        }

        StringBuilder description = new StringBuilder();
        description.append(subscriptionBuilder.getDescription()).append("\n\n");

        Iterator<String> subscriptionEmojiRoleIdDictionaryIterator = subscriptionBuilder.getEmojiRoleIdDictionary().keySet().iterator();
        String[] emojis = new String[subscriptionBuilder.getEmojiRoleIdDictionary().size()];

        for (int i = subscriptionBuilder.getEmojiRoleIdDictionary().size() - 1; subscriptionEmojiRoleIdDictionaryIterator.hasNext(); i--) {
            emojis[i] = subscriptionEmojiRoleIdDictionaryIterator.next();
        }

        for (int i = 0; i < emojis.length; i++) {
            Role role = guild.getRoleById(subscriptionBuilder.getEmojiRoleIdDictionary().get(emojis[i]));

            if (role == null) {
                continue;
            }

            description.append(emojis[i])
                    .append(" — ")
                    .append(role.getName())
                    .append("\n");
        }

        Message subscriptionMessage = subscriptionChannel.sendMessage(description.toString().trim()).complete();

        for (int i = 0; i < emojis.length; i++) {
            subscriptionMessage.addReaction(Emoji.fromFormatted(emojis[i])).complete();
        }

        subscriptionBuilder.setId(dynamicConfig.getLastSubscriptionId() + 1);
        subscriptionBuilder.setMessageId(subscriptionMessage.getId());
        dynamicConfig.incLastSubscriptionId();
        guildConfig.addSubscription(subscriptionBuilder.build());
        subscriptionBuilders.remove(subscriptionBuilder);
    }
}
