package com.remoteprint.controller;

import com.remoteprint.job.PrintJob;
import com.remoteprint.job.PrintJobService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/print-jobs")
public class PrintJobController {

    private final PrintJobService printJobService;

    public PrintJobController(PrintJobService printJobService) {
        this.printJobService = printJobService;
    }

    @PostMapping
    public ResponseEntity<PrintJob> createPrintJob(
            @RequestParam("file") MultipartFile file
    ) {

        PrintJob job = printJobService.createJob(
                file.getOriginalFilename(),
                file.getContentType(),
                "not-saved-yet"
        );

        PrintJob processedJob = printJobService.processJob(job);

        return ResponseEntity.ok(processedJob);
    }
}