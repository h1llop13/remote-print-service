package com.remoteprint.print;

import java.util.List;

public interface PrinterService {

    List<String> getAvailablePrinters();

    boolean isPrinterAvailable();

    String getDefaultPrinter();

    PrinterStatus getStatus();
}
