package com.remoteprint.controller;

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

    public PrintJobController(
            PrintJobService printJobService,
            FileStorageService fileStorageService
    ) {
        this.printJobService = printJobService;
        this.fileStorageService = fileStorageService;
    }

    @PostMapping
    public ResponseEntity<PrintJob> createPrintJob(
            @RequestParam("file") MultipartFile file
    ) throws IOException {

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

        PrintJob processedJob = printJobService.processJob(job);

        return ResponseEntity.ok(processedJob);
    }
}