package com.remoteprint.telegram;

import com.remoteprint.config.TelegramProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Component
public class RemotePrintTelegramBot
        implements LongPollingSingleThreadUpdateConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(RemotePrintTelegramBot.class);

    private final TelegramClient telegramClient;
    private final TelegramProperties telegramProperties;

    public RemotePrintTelegramBot(
            TelegramProperties telegramProperties
    ) {
        this.telegramProperties = telegramProperties;

        this.telegramClient =
                new OkHttpTelegramClient(
                        telegramProperties.getBotToken()
                );
    }

    @Override
    public void consume(Update update) {

        if (!update.hasMessage()) {
            return;
        }

        long chatId = update.getMessage().getChatId();

        log.info(
                "Telegram message received: chatId={}",
                chatId
        );

        if (!telegramProperties.getAllowedChatIdSet().contains(chatId)) {

            log.warn(
                    "Unauthorized Telegram access attempt: chatId={}",
                    chatId
            );

            sendMessage(
                    chatId,
                    "Access denied."
            );

            return;
        }

        if (update.getMessage().hasText()) {

            String text = update.getMessage().getText();

            if ("/start".equals(text)) {
                sendMessage(
                        chatId,
                        "Remote Print Service is ready.\n\n"
                                + "Send me a PDF document to print."
                );
            }
        }
    }

    private void sendMessage(
            long chatId,
            String text
    ) {

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .build();

        try {
            telegramClient.execute(message);

        } catch (TelegramApiException exception) {

            log.error(
                    "Failed to send Telegram message to chatId={}",
                    chatId,
                    exception
            );
        }
    }
}