package bot.modules.ny;

import bot.modules.BotModule;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Timer;
import java.util.TimerTask;

public class NyModule extends BotModule {
    private NyProcessor nyProcessor;

    public NyModule(JDA jda) {
        super(jda);
        nyProcessor = new NyProcessor();
    }

    @Override
    public void onReady(ReadyEvent event) {
        super.onReady(event);
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            private LocalDateTime lastTime = LocalDateTime.now();
            @Override
            public void run() {
                long deltaTime = Math.abs(Duration.between(LocalDateTime.now(), lastTime).getSeconds());
                lastTime = LocalDateTime.now();
                nyProcessor.increaseResponseProbabilityByTime(deltaTime);
            }
        }, 0, 10000);
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        super.onMessageReceived(event);

        if (event.getAuthor().equals(jda.getSelfUser())) {
            return;
        }

        String nyResponse = nyProcessor.onEvent(event.getMessage().getContentRaw());

        if (nyResponse == null) {
            return;
        }

        event.getChannel().sendMessage(nyResponse).queue();
    }
}
