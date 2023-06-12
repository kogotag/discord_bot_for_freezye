package bot.modules;

import bot.config.DynamicConfig;
import bot.config.GuildConfig;
import bot.config.StaticConfig;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BotModule extends ListenerAdapter {
    protected JDA jda;
    protected StaticConfig staticConfig;
    protected DynamicConfig dynamicConfig;
    protected Random random;
    protected static final String defaultAcceptEmoji = "✅";
    protected static final String defaultRejectEmoji = "❎";

    public BotModule(JDA jda) {
        this.jda = jda;
        jda.addEventListener(this);
        staticConfig = StaticConfig.getBotConfig();
        dynamicConfig = DynamicConfig.getConfig();
        random = new Random();
    }

    public static boolean hasBotAdminRights(User user, Guild guild) {
        Member member = null;
        try {
            member = guild.retrieveMemberById(user.getId()).complete();
        } catch (ErrorResponseException e) {
            return false;
        }

        if (member == null) {
            return false;
        }

        if (guild.getOwnerId().equals(user.getId())) {
            return true;
        }

        GuildConfig guildConfig = DynamicConfig.getConfig().getGuildConfigById(guild.getId());

        for (Role role :
                member.getRoles()) {
            if (guildConfig.getBotAdminRoles().contains(role.getId())) {
                return true;
            }
        }

        return false;
    }

    public static boolean isPositiveLong(String str) {
        Pattern pattern = Pattern.compile("^\\d+$", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(str);
        return matcher.matches();
    }

    public static boolean isValidRole(String roleId, Guild guild) {
        if (!isPositiveLong(roleId)) {
            return false;
        }

        Role role = guild.getRoleById(roleId);

        return role != null;
    }

    public static List<String> getArgs(String cmd, String fullString) {
        String argsRaw = fullString.substring(cmd.length());
        if (argsRaw.length() <= 0) {
            return new ArrayList<String>();
        }
        argsRaw = argsRaw.trim();
        return Arrays.asList(argsRaw.split("\\s+"));
    }


}
