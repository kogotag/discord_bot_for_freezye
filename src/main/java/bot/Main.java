package bot;

import bot.config.StaticConfig;
import bot.modules.BotModule;
import bot.modules.MainModule;
import bot.modules.RulesModule;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class Main {
    public static void main(String[] args) {
        JDA jda = JDABuilder.createDefault(StaticConfig.getBotConfig().getToken(),
                        GatewayIntent.MESSAGE_CONTENT,
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.GUILD_MEMBERS,
                        GatewayIntent.GUILD_MESSAGE_REACTIONS)
                .build();

        BotModule mainModule = new MainModule(jda);
        BotModule rulesModule = new RulesModule(jda);
    }
}
