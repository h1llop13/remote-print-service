package com.remoteprint.controller;

import com.remoteprint.document.DocumentValidationService;
import com.remoteprint.document.PageRangeService;
import com.remoteprint.document.PdfService;
import com.remoteprint.job.PrintJob;
import com.remoteprint.job.PrintJobService;
import com.remoteprint.storage.FileStorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/print-jobs")
public class PrintJobController {

    private static final Logger log =
            LoggerFactory.getLogger(PrintJobController.class);

    private final PrintJobService printJobService;
    private final FileStorageService fileStorageService;
    private final PdfService pdfService;
    private final PageRangeService pageRangeService;
    private final DocumentValidationService documentValidationService;

    public PrintJobController(
            PrintJobService printJobService,
            FileStorageService fileStorageService,
            PdfService pdfService,
            PageRangeService pageRangeService,
            DocumentValidationService documentValidationService
    ) {
        this.printJobService = printJobService;
        this.fileStorageService = fileStorageService;
        this.pdfService = pdfService;
        this.pageRangeService = pageRangeService;
        this.documentValidationService = documentValidationService;
    }

    @PostMapping
    public ResponseEntity<PrintJob> createPrintJob(
            @RequestParam("file") MultipartFile file,
            @RequestParam(
                    value = "pageRange",
                    defaultValue = "ALL"
            ) String pageRange,
            @RequestParam(
                    value = "copies",
                    defaultValue = "1"
            ) int copies
    ) throws IOException {

        documentValidationService.validatePdf(file);

        PrintJob job = printJobService.createJob(
                file.getOriginalFilename(),
                file.getContentType()
        );

        String filePath = fileStorageService.saveOriginalFile(
                job.getId(),
                file
        );

        job.setOriginalFilePath(filePath);

        int pageCount = pdfService.getPageCount(filePath);

        var selectedPages = pageRangeService.parse(
                pageRange,
                pageCount
        );

        job.setPageRange(pageRange);

        if (copies < 1 || copies > 20) {
            throw new IllegalArgumentException(
                    "Copies must be between 1 and 20"
            );
        }

        job.setCopies(copies);

        String printableFilePath = pdfService.createPrintablePdf(
                filePath,
                selectedPages
        );

        job.setPrintableFilePath(printableFilePath);

        log.info(
                "Print job {} prepared: pages={}, printableFile={}",
                job.getId(),
                selectedPages,
                printableFilePath
        );

        PrintJob processedJob = printJobService.processJob(job);

        return ResponseEntity.ok(processedJob);
    }
}