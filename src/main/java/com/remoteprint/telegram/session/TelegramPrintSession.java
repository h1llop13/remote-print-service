package com.remoteprint.telegram.session;

import org.springframework.web.multipart.MultipartFile;

public class TelegramPrintSession {

    private final MultipartFile originalFile;
    private final MultipartFile printableSourceFile;
    private final int pageCount;
    private String pageRange;

    public TelegramPrintSession(
            MultipartFile originalFile,
            MultipartFile printableSourceFile,
            int pageCount
    ) {
        this.originalFile = originalFile;
        this.printableSourceFile = printableSourceFile;
        this.pageCount = pageCount;
    }

    public MultipartFile getOriginalFile() {
        return originalFile;
    }

    public MultipartFile getPrintableSourceFile() {
        return printableSourceFile;
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