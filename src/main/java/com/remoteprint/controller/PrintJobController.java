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

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/print-jobs")
public class PrintJobController {

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
            ) String pageRange
    ) throws IOException {

        documentValidationService.validatePdf(file);

        PrintJob job = printJobService.createJob(
                file.getOriginalFilename(),
                file.getContentType(),
                null
        );

        String filePath = fileStorageService.saveOriginalFile(
                job.getId(),
                file
        );

        job.setFilePath(filePath);

        int pageCount = pdfService.getPageCount(filePath);

        var selectedPages = pageRangeService.parse(
                pageRange,
                pageCount
        );

        job.setPageRange(pageRange);

        String printableFilePath = pdfService.createPrintablePdf(
                filePath,
                selectedPages
        );

        System.out.println("PDF page count: " + pageCount);
        System.out.println("Selected pages: " + selectedPages);
        System.out.println("Printable PDF: " + printableFilePath);

        PrintJob processedJob = printJobService.processJob(job);

        return ResponseEntity.ok(processedJob);
    }
}