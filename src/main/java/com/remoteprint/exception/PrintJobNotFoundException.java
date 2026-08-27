package com.remoteprint.exception;

import java.util.UUID;

public class PrintJobNotFoundException extends RuntimeException {

    public PrintJobNotFoundException(UUID id) {
        super("Print job not found: " + id);
    }
}
