package com.remoteprint.dto;

import com.remoteprint.print.PrinterStatus;

import java.util.List;

public record PrinterStatusResponse(
        String configuredPrinter,
        String defaultPrinter,
        PrinterStatus status,
        List<String> availablePrinters
) {
}
