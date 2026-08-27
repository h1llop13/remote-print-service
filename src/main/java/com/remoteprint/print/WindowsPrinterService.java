package com.remoteprint.print;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPageable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.awt.print.PrinterJob;
import java.io.File;
import java.util.Arrays;
import java.util.List;

@Service
@Profile("windows")
public class WindowsPrinterService implements PrinterService {

    private static final Logger log =
            LoggerFactory.getLogger(WindowsPrinterService.class);

    private static final String PRINTER_NAME = "Canon LBP2900";

    @Override
    public List<String> getAvailablePrinters() {
        return Arrays.stream(
                        PrintServiceLookup.lookupPrintServices(null, null)
                )
                .map(PrintService::getName)
                .toList();
    }

    @Override
    public boolean isPrinterAvailable() {
        return findConfiguredPrinter() != null;
    }

    @Override
    public String getDefaultPrinter() {

        PrintService defaultPrinter =
                PrintServiceLookup.lookupDefaultPrintService();

        if (defaultPrinter == null) {
            return null;
        }

        return defaultPrinter.getName();
    }

    @Override
    public PrinterStatus getStatus() {

        if (isPrinterAvailable()) {
            return PrinterStatus.AVAILABLE;
        }

        return PrinterStatus.UNAVAILABLE;
    }

    @Override
    public PrintResult print(PrintRequest request) {

        PrintService printer = findConfiguredPrinter();

        if (printer == null) {
            return new PrintResult(
                    false,
                    "Canon LBP2900 is unavailable"
            );
        }

        File pdfFile = new File(request.getFilePath());

        if (!pdfFile.exists()) {
            return new PrintResult(
                    false,
                    "Printable file does not exist"
            );
        }

        try (PDDocument document = Loader.loadPDF(pdfFile)) {

            PrinterJob printerJob = PrinterJob.getPrinterJob();

            printerJob.setPrintService(printer);
            printerJob.setJobName("Remote Print Service");

            printerJob.setPageable(
                    new PDFPageable(document)
            );

            printerJob.setCopies(
                    request.getCopies()
            );

            log.info(
                    "Sending PDF to Windows printer: printer={}, file={}, copies={}",
                    printer.getName(),
                    request.getFilePath(),
                    request.getCopies()
            );

            printerJob.print();

            log.info(
                    "Print job submitted to Windows printer: {}",
                    printer.getName()
            );

            return new PrintResult(
                    true,
                    "Document submitted to Windows printer"
            );

        } catch (Exception exception) {

            log.error(
                    "Windows printing failed: file={}",
                    request.getFilePath(),
                    exception
            );

            return new PrintResult(
                    false,
                    "Windows printing failed: " + exception.getMessage()
            );
        }
    }

    private PrintService findConfiguredPrinter() {

        PrintService[] printers =
                PrintServiceLookup.lookupPrintServices(null, null);

        for (PrintService printer : printers) {

            if (printer.getName().equalsIgnoreCase(PRINTER_NAME)) {
                return printer;
            }
        }

        return null;
    }
}