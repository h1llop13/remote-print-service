package com.remoteprint.telegram.session;

import org.springframework.web.multipart.MultipartFile;

public class TelegramPrintSession {

    private final MultipartFile file;
    private final int pageCount;

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
}
