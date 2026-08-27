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
import com.remoteprint.dto.PrintJobResponse;
import com.remoteprint.mapper.PrintJobMapper;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

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
    private final PrintJobMapper printJobMapper;

    public PrintJobController(
            PrintJobService printJobService,
            FileStorageService fileStorageService,
            PdfService pdfService,
            PageRangeService pageRangeService,
            DocumentValidationService documentValidationService,
            PrintJobMapper printJobMapper
    ) {
        this.printJobService = printJobService;
        this.fileStorageService = fileStorageService;
        this.pdfService = pdfService;
        this.pageRangeService = pageRangeService;
        this.documentValidationService = documentValidationService;
        this.printJobMapper = printJobMapper;
    }

    @PostMapping
    public ResponseEntity<PrintJobResponse> createPrintJob(
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

        return ResponseEntity.ok(
                printJobMapper.toResponse(processedJob)
        );

    }

    @GetMapping("/{id}")
    public ResponseEntity<PrintJobResponse> getPrintJob(
            @PathVariable UUID id
    ) {

        PrintJob job = printJobService.getJob(id);

        return ResponseEntity.ok(
                printJobMapper.toResponse(job)
        );
    }

    @GetMapping
    public ResponseEntity<List<PrintJobResponse>> getPrintJobs() {

        List<PrintJobResponse> response =
                printJobService.getJobs()
                        .stream()
                        .map(printJobMapper::toResponse)
                        .toList();

        return ResponseEntity.ok(response);
    }
}