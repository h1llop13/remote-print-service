package com.remoteprint.print;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MockPrinterService implements PrinterService {

    @Override
    public List<String> getAvailablePrinters() {
        return List.of("Mock Canon LBP2900");
    }

    @Override
    public boolean isPrinterAvailable() {
        return true;
    }

    @Override
    public String getDefaultPrinter() {
        return "Mock Canon LBP2900";
    }

    @Override
    public PrinterStatus getStatus() {
        return PrinterStatus.AVAILABLE;
    }
}
