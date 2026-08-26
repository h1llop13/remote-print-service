package com.remoteprint.print;

public class PrintRequest {

    private final String filePath;
    private final int copies;

    public PrintRequest(String filePath, int copies) {
        this.filePath = filePath;
        this.copies = copies;
    }

    public int getCopies() {
        return copies;
    }

    public String getFilePath() {
        return filePath;
    }
}
