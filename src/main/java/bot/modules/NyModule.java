package bot.modules;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NyModule extends BotModule {
    public NyModule(JDA jda) {
        super(jda);
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        super.onMessageReceived(event);

        if (event.getAuthor().equals(jda.getSelfUser())) {
            return;
        }

        String content = event.getMessage().getContentRaw().toLowerCase(Locale.ROOT);

        Matcher nyMatcher = Pattern.compile("ну").matcher(content);

        if (!nyMatcher.find()) {
            return;
        }

        event.getChannel().sendMessage("Баранки гну").queue();
    }
}
