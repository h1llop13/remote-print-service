package com.remoteprint.telegram;

import com.remoteprint.config.TelegramProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;

@Component
public class TelegramBotRunner {

    private static final Logger log =
            LoggerFactory.getLogger(TelegramBotRunner.class);

    private final TelegramProperties telegramProperties;
    private final RemotePrintTelegramBot telegramBot;

    private TelegramBotsLongPollingApplication botsApplication;

    public TelegramBotRunner(
            TelegramProperties telegramProperties,
            RemotePrintTelegramBot telegramBot
    ) {
        this.telegramProperties = telegramProperties;
        this.telegramBot = telegramBot;
    }

    @PostConstruct
    public void start() throws Exception {

        String token = telegramProperties.getBotToken();

        if (token == null || token.isBlank()) {
            log.warn(
                    "Telegram bot token is not configured. Telegram bot will not start."
            );
            return;
        }

        botsApplication =
                new TelegramBotsLongPollingApplication();

        botsApplication.registerBot(
                token,
                telegramBot
        );

        log.info("Telegram bot started");
    }

    @PreDestroy
    public void stop() throws Exception {

        if (botsApplication != null) {
            botsApplication.close();
        }
    }
}