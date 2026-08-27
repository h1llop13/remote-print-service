package com.remoteprint.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "document")
public class DocumentProperties {

    private long maxSizeBytes;
    private String libreofficePath;

    public long getMaxSizeBytes() {
        return maxSizeBytes;
    }

    public void setMaxSizeBytes(long maxSizeBytes) {
        this.maxSizeBytes = maxSizeBytes;
    }

    public String getLibreofficePath() {
        return libreofficePath;
    }

    public void setLibreofficePath(String libreofficePath) {
        this.libreofficePath = libreofficePath;
    }
}
