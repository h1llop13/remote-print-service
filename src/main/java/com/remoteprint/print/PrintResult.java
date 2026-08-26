package com.remoteprint.print;

public class PrintResult {

    private final boolean success;
    private final String message;

    public PrintResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
