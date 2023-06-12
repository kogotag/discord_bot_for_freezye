package bot.modules;

import bot.config.DynamicConfig;
import bot.config.GuildConfig;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.NotNull;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainModule extends BotModule {
    private enum Cmd {
        HELP,
        CHANGE_PREFIX,
        ADD_BOT_ADMIN_ROLE,
        REMOVE_BOT_ADMIN_ROLE,
        SHOW_BOT_ADMIN_ROLES,
        REFRESH_SLASH_COMMANDS
    }

    public MainModule(JDA jda) {
        super(jda);
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        super.onMessageReceived(event);

        if (event.getAuthor().equals(jda.getSelfUser())) {
            return;
        }

        String guildId = event.getGuild().getId();
        GuildConfig guildConfig = DynamicConfig.getConfig().getGuildConfigById(guildId);

        if (guildConfig == null) {
            return;
        }

        String content = event.getMessage().getContentRaw();

        Dictionary<Cmd, String> cmdDictionary = new Hashtable<>();
        cmdDictionary.put(Cmd.HELP, guildConfig.getPrefix() + "help");
        cmdDictionary.put(Cmd.CHANGE_PREFIX, guildConfig.getPrefix() + "change_prefix");
        cmdDictionary.put(Cmd.ADD_BOT_ADMIN_ROLE, guildConfig.getPrefix() + "add_bot_admin_role");
        cmdDictionary.put(Cmd.REMOVE_BOT_ADMIN_ROLE, guildConfig.getPrefix() + "remove_bot_admin_role");
        cmdDictionary.put(Cmd.SHOW_BOT_ADMIN_ROLES, guildConfig.getPrefix() + "show_bot_admin_roles");
        cmdDictionary.put(Cmd.REFRESH_SLASH_COMMANDS, guildConfig.getPrefix() + "refresh_slash_commands");

        if (content.toLowerCase().startsWith(cmdDictionary.get(Cmd.HELP))) {
            messageHelp(event, cmdDictionary);
        } else if (content.toLowerCase().startsWith(cmdDictionary.get(Cmd.CHANGE_PREFIX))) {
            messageChangePrefix(event, guildConfig, cmdDictionary.get(Cmd.CHANGE_PREFIX), content);
        } else if (content.toLowerCase().startsWith(cmdDictionary.get(Cmd.ADD_BOT_ADMIN_ROLE))) {
            messageAddBotAdminRole(event, guildConfig, cmdDictionary.get(Cmd.ADD_BOT_ADMIN_ROLE), content);
        } else if (content.toLowerCase().startsWith(cmdDictionary.get(Cmd.REMOVE_BOT_ADMIN_ROLE))) {
            messageRemoveBotAdminRole(event, guildConfig, cmdDictionary.get(Cmd.REMOVE_BOT_ADMIN_ROLE), content);
        } else if (content.toLowerCase().startsWith(cmdDictionary.get(Cmd.SHOW_BOT_ADMIN_ROLES))) {
            messageShowBotAdminRoles(event, guildConfig);
        } else if (content.toLowerCase().startsWith(cmdDictionary.get(Cmd.REFRESH_SLASH_COMMANDS))) {
            messageRefreshSlashCommands(event);
        }
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

        if (event.getName().equals("help")) {
            slashHelp(event);
        } else if (event.getName().equals("change_prefix")) {
            slashChangePrefix(event, guildConfig);
        } else if (event.getName().equals("add_bot_admin_role")) {
            slashAddBotAdminRole(event, guildConfig);
        } else if (event.getName().equals("remove_bot_admin_role")) {
            slashRemoveBotAdminRole(event, guildConfig);
        } else if (event.getName().equals("show_bot_admin_roles")) {
            slashShowBotAdminRoles(event, guildConfig);
        } else if (event.getName().equals("refresh_slash_commands")) {
            slashRefreshSlashCommands(event);
        }
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        super.onReady(event);
        List<Guild> guilds = jda.getSelfUser().getMutualGuilds();

        for (Guild guild :
                guilds) {
            if (DynamicConfig.getConfig().getGuildConfigs().stream()
                    .map(GuildConfig::getGuildId)
                    .anyMatch(guildId -> guildId.equals(guild.getId()))) {
                continue;
            }

            GuildConfig guildConfig = new GuildConfig(guild.getId());
            DynamicConfig.getConfig().addGuildConfig(guildConfig);
        }
    }

    private void refreshSlashCommands(Guild guild) {
        guild
                .updateCommands()
                .addCommands(Commands.slash("help", "Выводит справочную информацию"))
                .addCommands(Commands.slash("change_prefix", "Изменяет префикс для текущей гильдии")
                        .addOption(OptionType.STRING, "new_prefix", "Новый префикс", true))
                .addCommands(Commands.slash("add_bot_admin_role", "Добавляет роль в список ролей администраторов бота")
                        .addOption(OptionType.ROLE, "role", "роль", true))
                .addCommands(Commands.slash("remove_bot_admin_role", "Убирает роль из списка ролей администраторов бота")
                        .addOption(OptionType.ROLE, "role", "роль", true))
                .addCommands(Commands.slash("show_bot_admin_roles", "Выводит список ролей из списка ролей администраторов бота"))
                .addCommands(Commands.slash("refresh_slash_commands", "Обновляет слэш-комманды"))
                .addCommands(Commands.slash("rules_set_channel", "Устанавливает текстовый канал для правил")
                        .addOption(OptionType.CHANNEL, "channel", "выбирайте только текстовый канал", true))
                .addCommands(Commands.slash("rules_set_text", "Устанавливает текст правил. Не более 2000 символов"))
                .addCommands(Commands.slash("rules_set_role", "Выберите, какую роль выдавать при принятии правил")
                        .addOption(OptionType.ROLE, "role", "роль", true))
                .addCommands(Commands.slash("rules_enable", "Проверяет настройки правил на текущем сервере и запускает этот функционал"))
                .addCommands(Commands.slash("rules_disable", "Выключает систему правил на этом сервере"))
                .addCommands(Commands.slash("rules_info", "Выводит инструкцию по установке модуля правил на ваш сервер"))
                .addCommands(Commands.slash("rules_send_new_message", "Отправляет новое сообщение в канал правил и предпринимает попытку удалить предыдущее"))
                .addCommands(Commands.slash("rules_update_message", "Редактирует сообщение с правилами"))
                .addCommands(Commands.slash("subscriptions_list", "Выводит список текущих подписок"))
                .addCommands(Commands.slash("subscriptions_remove", "Удаляет подписку по id")
                        .addOption(OptionType.INTEGER, "id", "id подписки", true))
                .addCommands(Commands.slash("subscriptions_create", "Запускает конструктор подписок"))
                .queue();
    }

    private void messageHelp(MessageReceivedEvent event, Dictionary<Cmd, String> cmdDictionary) {
        event.getChannel().sendMessage(cmdDictionary.get(Cmd.CHANGE_PREFIX) + " — позволяет изменить префикс на вашем сервере\n"
                + cmdDictionary.get(Cmd.ADD_BOT_ADMIN_ROLE) + " — добавляет роль в список администраторов бота\n"
                + cmdDictionary.get(Cmd.REMOVE_BOT_ADMIN_ROLE) + " — убирает роль из списка администраторов бота\n"
                + cmdDictionary.get(Cmd.SHOW_BOT_ADMIN_ROLES) + " — показывает список администраторов бота\n"
                + cmdDictionary.get(Cmd.REFRESH_SLASH_COMMANDS) + " — обновляет слэш-комманды").queue();
    }

    private void messageChangePrefix(MessageReceivedEvent event, GuildConfig guildConfig, String cmd, String content) {
        if (!hasBotAdminRights(event.getAuthor(), event.getGuild())) {
            event.getChannel().sendMessage("У вас нет прав на использование этой команды").queue();
            return;
        }

        List<String> args = getArgs(cmd, content);
        if (args.size() < 1) {
            event.getChannel().sendMessage("У этой команды должен быть аргумент: новый префикс").queue();
            return;
        }

        String newPrefix = args.get(0);
        Pattern badPrefixPattern = Pattern.compile("^\\w.*?$", Pattern.DOTALL);
        Matcher badPrefixMatcher = badPrefixPattern.matcher(newPrefix);

        if (badPrefixMatcher.find()) {
            event.getChannel().sendMessage("Плохой префикс. Префикс должен начинаться со специального символа").queue();
            return;
        }

        Pattern newLinePattern = Pattern.compile("\\n", Pattern.DOTALL);
        Matcher newLineMatcher = newLinePattern.matcher(newPrefix);

        if (newLineMatcher.find()) {
            event.getChannel().sendMessage("Символ переноса новой строки не может быть использован в префиксе").queue();
            return;
        }

        guildConfig.setPrefix(newPrefix);

        event.getChannel().sendMessage("Префикс изменён успешно!\nНовый префикс: `" + guildConfig.getPrefix() + "`").queue();
    }

    private void messageAddBotAdminRole(MessageReceivedEvent event, GuildConfig guildConfig, String cmd, String content) {
        if (!hasBotAdminRights(event.getAuthor(), event.getGuild())) {
            event.getChannel().sendMessage("У вас нет прав на использование этой команды").queue();
            return;
        }

        List<String> args = getArgs(cmd, content);
        if (args.size() < 1) {
            event.getChannel().sendMessage("У этой команды должен быть аргумент: Id роли").queue();
            return;
        }

        String roleId = args.get(0);
        if (!isValidRole(roleId, event.getGuild())) {
            event.getChannel().sendMessage("Роль с этим Id не найдена").queue();
            return;
        }

        guildConfig.addBotAdminRole(roleId);
        event.getChannel().sendMessage("Роль успешно добавлена в список ролей администраторов бота").queue();
    }

    private void messageRemoveBotAdminRole(MessageReceivedEvent event, GuildConfig guildConfig, String cmd, String content) {
        if (!hasBotAdminRights(event.getAuthor(), event.getGuild())) {
            event.getChannel().sendMessage("У вас нет прав на использование этой команды").queue();
            return;
        }

        List<String> args = getArgs(cmd, content);
        if (args.size() < 1) {
            event.getChannel().sendMessage("У этой команды должен быть аргумент: Id роли").queue();
            return;
        }

        String roleId = args.get(0);
        if (!isValidRole(roleId, event.getGuild())) {
            event.getChannel().sendMessage("Роль с этим Id не найдена").queue();
            return;
        }

        guildConfig.removeBotAdminRole(roleId);
        event.getChannel().sendMessage("Роль успешно удалена из списка ролей администраторов бота").queue();
    }

    private void messageShowBotAdminRoles(MessageReceivedEvent event, GuildConfig guildConfig) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String roleId :
                guildConfig.getBotAdminRoles()) {
            Role role = event.getGuild().getRoleById(roleId);
            if (role == null) {
                stringBuilder.append("null");
            } else {
                stringBuilder.append(role.getName());
            }
            stringBuilder.append("\n");
        }
        event.getChannel().sendMessage("Список ролей администраторов бота:\n" + stringBuilder).queue();
    }

    private void messageRefreshSlashCommands(MessageReceivedEvent event) {
        if (!hasBotAdminRights(event.getAuthor(), event.getGuild())) {
            event.getChannel().sendMessage("У вас нет прав на использование этой команды").queue();
            return;
        }

        refreshSlashCommands(event.getGuild());
    }

    private void slashHelp(SlashCommandInteractionEvent event) {
        event.reply("/change_prefix — позволяет изменить префикс на вашем сервере\n"
                        + "/add_bot_admin_role — добавляет роль в список администраторов бота\n"
                        + "/remove_bot_admin_role — убирает роль из списка администраторов бота\n"
                        + "/show_bot_admin_roles — показывает список администраторов бота\n"
                        + "/refresh_slash_commands — обновляет слэш-комманды")
                .setEphemeral(true).queue();
    }

    private void slashChangePrefix(SlashCommandInteractionEvent event, GuildConfig guildConfig) {
        if (!hasBotAdminRights(event.getUser(), Objects.requireNonNull(event.getGuild()))) {
            event.reply("У вас нет прав на использование этой команды").setEphemeral(true).queue();
            return;
        }

        String newPrefix = Objects.requireNonNull(event.getOption("new_prefix")).getAsString();
        Pattern badPrefixPattern = Pattern.compile("^\\w.*?$", Pattern.DOTALL);
        Matcher badPrefixMatcher = badPrefixPattern.matcher(newPrefix);

        if (badPrefixMatcher.find()) {
            event.reply("Плохой префикс. Префикс должен начинаться со специального символа").setEphemeral(true).queue();
            return;
        }

        Pattern newLinePattern = Pattern.compile("\\n", Pattern.DOTALL);
        Matcher newLineMatcher = newLinePattern.matcher(newPrefix);

        if (newLineMatcher.find()) {
            event.reply("Символ переноса новой строки не может быть использован в префиксе").setEphemeral(true).queue();
            return;
        }

        guildConfig.setPrefix(newPrefix);

        event.reply("Префикс изменён успешно!\nНовый префикс: `" + guildConfig.getPrefix() + "`").queue();
    }

    private void slashAddBotAdminRole(SlashCommandInteractionEvent event, GuildConfig guildConfig) {
        if (!hasBotAdminRights(event.getUser(), Objects.requireNonNull(event.getGuild()))) {
            event.reply("У вас нет прав на использование этой команды").setEphemeral(true).queue();
            return;
        }

        Role role = Objects.requireNonNull(event.getOption("role")).getAsRole();
        guildConfig.addBotAdminRole(role.getId());
        event.reply("Роль \"" + role.getName() + "\" успешно добавлена в список ролей администраторов бота").queue();
    }

    private void slashRemoveBotAdminRole(SlashCommandInteractionEvent event, GuildConfig guildConfig) {
        if (!hasBotAdminRights(event.getUser(), Objects.requireNonNull(event.getGuild()))) {
            event.reply("У вас нет прав на использование этой команды").setEphemeral(true).queue();
            return;
        }

        Role role = Objects.requireNonNull(event.getOption("role")).getAsRole();
        guildConfig.removeBotAdminRole(role.getId());
        event.reply("Роль \"" + role.getName() + "\" успешно удалена из списка ролей администраторов бота").queue();
    }

    private void slashShowBotAdminRoles(SlashCommandInteractionEvent event, GuildConfig guildConfig) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String roleId :
                guildConfig.getBotAdminRoles()) {
            Role role = Objects.requireNonNull(event.getGuild()).getRoleById(roleId);
            if (role == null) {
                stringBuilder.append("null");
            } else {
                stringBuilder.append(role.getName());
            }
            stringBuilder.append("\n");
        }
        event.reply("Список ролей администраторов бота:\n" + stringBuilder).setEphemeral(true).queue();
    }

    private void slashRefreshSlashCommands(SlashCommandInteractionEvent event) {
        if (!hasBotAdminRights(event.getUser(), Objects.requireNonNull(event.getGuild()))) {
            event.reply("У вас нет прав на использование этой команды").setEphemeral(true).queue();
            return;
        }

        refreshSlashCommands(event.getGuild());
        event.reply("Слэш-комманды обновлены").setEphemeral(true).queue();
    }
}
