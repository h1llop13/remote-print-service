package com.remoteprint.controller;

import com.remoteprint.config.PrinterProperties;
import com.remoteprint.dto.PrinterStatusResponse;
import com.remoteprint.print.PrinterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/printers")
public class PrinterController {

    private final PrinterService printerService;
    private final PrinterProperties printerProperties;

    public PrinterController(
            PrinterService printerService,
            PrinterProperties printerProperties
    ) {
        this.printerService = printerService;
        this.printerProperties = printerProperties;
    }

    @GetMapping("/status")
    public ResponseEntity<PrinterStatusResponse> getPrinterStatus() {

        PrinterStatusResponse response = new PrinterStatusResponse(
                printerProperties.getName(),
                printerService.getDefaultPrinter(),
                printerService.getStatus(),
                printerService.getAvailablePrinters()
        );

        return ResponseEntity.ok(response);
    }
}
