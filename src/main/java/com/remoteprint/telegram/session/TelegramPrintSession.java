package com.remoteprint.telegram.session;

import org.springframework.web.multipart.MultipartFile;

public class TelegramPrintSession {

    private final MultipartFile file;
    private final int pageCount;
    private String pageRange;

    public TelegramPrintSession(
            MultipartFile file,
            int pageCount
    ) {
        this.file = file;
        this.pageCount = pageCount;
    }

    public MultipartFile getFile() {
        return file;
    }

    public int getPageCount() {
        return pageCount;
    }

    public String getPageRange() {
        return pageRange;
    }

    public void setPageRange(String pageRange) {
        this.pageRange = pageRange;
    }

    public boolean hasPageRange() {
        return pageRange != null
                && !pageRange.isBlank();
    }
}