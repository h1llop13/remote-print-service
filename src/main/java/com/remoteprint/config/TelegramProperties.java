package com.remoteprint.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@ConfigurationProperties(prefix = "telegram")
public class TelegramProperties {

    private String botToken;
    private String allowedChatIds;

    public String getBotToken() {
        return botToken;
    }

    public void setBotToken(String botToken) {
        this.botToken = botToken;
    }

    public String getAllowedChatIds() {
        return allowedChatIds;
    }

    public void setAllowedChatIds(String allowedChatIds) {
        this.allowedChatIds = allowedChatIds;
    }

    public Set<Long> getAllowedChatIdSet() {

        if (allowedChatIds == null || allowedChatIds.isBlank()) {
            return Set.of();
        }

        return Arrays.stream(allowedChatIds.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(Long::parseLong)
                .collect(Collectors.toSet());
    }
}