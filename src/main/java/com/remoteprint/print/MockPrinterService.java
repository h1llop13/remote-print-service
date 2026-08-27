package com.remoteprint.print;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
@Profile("!windows")
public class MockPrinterService implements PrinterService {

    private static final Logger log =
            LoggerFactory.getLogger(MockPrinterService.class);

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

        log.info(
                "Mock printing started: file={}, copies={}",
                request.getFilePath(),
                request.getCopies()
        );

        return new PrintResult(
                true,
                "Document successfully sent to mock printer"
        );
    }
}