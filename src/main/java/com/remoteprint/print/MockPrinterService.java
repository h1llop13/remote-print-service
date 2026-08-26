package com.remoteprint.print;

import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
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

    @Override
    public PrintResult print(PrintRequest request) {

        Path filePath = Path.of(request.getFilePath());

        if (!Files.exists(filePath)) {
            return new PrintResult(
                    false,
                    "Printable file does not exist"
            );
        }

        System.out.println(
                "Mock printing file: " + request.getFilePath()
        );

        System.out.println(
                "Copies: " + request.getCopies()
        );

        return new PrintResult(
                true,
                "Document successfully sent to mock printer"
        );
    }
}