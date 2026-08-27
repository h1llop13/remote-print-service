package com.remoteprint.telegram.session;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TelegramSessionService {

    private final Map<Long, TelegramPrintSession> sessions =
            new ConcurrentHashMap<>();

    public void save(
            long chatId,
            TelegramPrintSession session
    ) {
        sessions.put(chatId, session);
    }

    public Optional<TelegramPrintSession> get(long chatId) {
        return Optional.ofNullable(
                sessions.get(chatId)
        );
    }

    public void remove(long chatId) {
        sessions.remove(chatId);
    }
}