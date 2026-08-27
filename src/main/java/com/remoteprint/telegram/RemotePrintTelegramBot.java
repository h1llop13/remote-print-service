package com.remoteprint.telegram;

import com.remoteprint.config.TelegramProperties;
import com.remoteprint.document.DocumentConverter;
import com.remoteprint.document.DocumentValidationService;
import com.remoteprint.document.PageRangeService;
import com.remoteprint.document.PdfService;
import com.remoteprint.job.PrintJob;
import com.remoteprint.job.PrintJobService;
import com.remoteprint.storage.FileStorageService;
import com.remoteprint.telegram.session.TelegramPrintSession;
import com.remoteprint.telegram.session.TelegramSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.springframework.web.multipart.MultipartFile;
import com.remoteprint.job.PrintQueueService;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.FileInputStream;

@Component
public class RemotePrintTelegramBot
        implements LongPollingSingleThreadUpdateConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(RemotePrintTelegramBot.class);

    private final TelegramClient telegramClient;
    private final TelegramProperties telegramProperties;
    private final DocumentValidationService documentValidationService;
    private final PrintJobService printJobService;
    private final FileStorageService fileStorageService;
    private final PdfService pdfService;
    private final PageRangeService pageRangeService;
    private final TelegramSessionService telegramSessionService;
    private final DocumentConverter documentConverter;
    private final PrintQueueService printQueueService;

    public RemotePrintTelegramBot(
            TelegramProperties telegramProperties,
            DocumentValidationService documentValidationService,
            PrintJobService printJobService,
            FileStorageService fileStorageService,
            PdfService pdfService,
            PageRangeService pageRangeService,
            TelegramSessionService telegramSessionService,
            DocumentConverter documentConverter,
            PrintQueueService printQueueService
    ) {
        this.telegramProperties = telegramProperties;
        this.documentValidationService = documentValidationService;
        this.printJobService = printJobService;
        this.fileStorageService = fileStorageService;
        this.pdfService = pdfService;
        this.pageRangeService = pageRangeService;
        this.telegramSessionService = telegramSessionService;
        this.documentConverter = documentConverter;
        this.printQueueService = printQueueService;

        this.telegramClient =
                new OkHttpTelegramClient(
                        telegramProperties.getBotToken()
                );
    }

    @Override
    public void consume(Update update) {

        if (!update.hasMessage()) {
            return;
        }

        long chatId = update.getMessage().getChatId();

        if (!telegramProperties.getAllowedChatIdSet().contains(chatId)) {
            sendMessage(chatId, "Access denied.");
            return;
        }

        if (update.getMessage().hasDocument()) {
            handleDocument(
                    chatId,
                    update.getMessage().getDocument()
            );

            return;
        }

        if (update.getMessage().hasText()) {

            String text = update.getMessage()
                    .getText()
                    .trim();

            if ("/start".equalsIgnoreCase(text)) {
                sendMessage(
                        chatId,
                        "Remote Print Service is ready.\n\n"
                                + "Send me a PDF document to print."
                );

                return;
            }

            handleTextMessage(
                    chatId,
                    text
            );
        }
    }

    private void handleDocument(
            long chatId,
            Document telegramDocument
    ) {

        Path tempDirectory = null;

        try {
            String fileName = telegramDocument.getFileName();

            if (fileName == null) {
                sendMessage(chatId, "File name is missing.");
                return;
            }

            String lowerFileName = fileName.toLowerCase();

            boolean pdf = lowerFileName.endsWith(".pdf");
            boolean word = lowerFileName.endsWith(".doc")
                    || lowerFileName.endsWith(".docx");

            if (!pdf && !word) {
                sendMessage(
                        chatId,
                        "Supported formats: PDF, DOC, DOCX."
                );
                return;
            }

            sendMessage(
                    chatId,
                    "Document received. Processing..."
            );

            GetFile getFile = GetFile.builder()
                    .fileId(telegramDocument.getFileId())
                    .build();

            org.telegram.telegrambots.meta.api.objects.File telegramFile =
                    telegramClient.execute(getFile);

            File downloadedFile =
                    telegramClient.downloadFile(
                            telegramFile.getFilePath()
                    );

            MockMultipartFile originalFile;

            try (FileInputStream inputStream =
                         new FileInputStream(downloadedFile)) {

                originalFile = new MockMultipartFile(
                        "file",
                        fileName,
                        telegramDocument.getMimeType(),
                        inputStream
                );
            }

            MultipartFile printableSourceFile;

            if (pdf) {

                documentValidationService.validatePdf(
                        originalFile
                );

                printableSourceFile = originalFile;

            } else {

                tempDirectory =
                        Files.createTempDirectory(
                                "remote-print-conversion-"
                        );

                Path sourceFile =
                        tempDirectory.resolve(fileName);

                originalFile.transferTo(sourceFile);

                String convertedPdfPath =
                        documentConverter.convertToPdf(
                                sourceFile.toString(),
                                tempDirectory.toString()
                        );

                byte[] convertedBytes =
                        Files.readAllBytes(
                                Path.of(convertedPdfPath)
                        );

                printableSourceFile =
                        new MockMultipartFile(
                                "file",
                                "converted.pdf",
                                "application/pdf",
                                convertedBytes
                        );

                documentValidationService.validatePdf(
                        printableSourceFile
                );
            }

            int pageCount =
                    pdfService.getPageCount(
                            printableSourceFile
                    );

            telegramSessionService.save(
                    chatId,
                    new TelegramPrintSession(
                            originalFile,
                            printableSourceFile,
                            pageCount
                    )
            );

            sendMessage(
                    chatId,
                    "Document ready.\n\n"
                            + "File: " + fileName + "\n"
                            + "Pages: " + pageCount + "\n\n"
                            + "Send page range:\n"
                            + "ALL\n"
                            + "1\n"
                            + "1-3\n"
                            + "1,3,5\n"
                            + "1-3,5"
            );

        } catch (Exception exception) {

            log.error(
                    "Telegram document processing failed: chatId={}",
                    chatId,
                    exception
            );

            sendMessage(
                    chatId,
                    "Failed to process the document."
            );

        } finally {

            if (tempDirectory != null) {

                try (var paths = Files.walk(tempDirectory)) {

                    paths.sorted(
                                    java.util.Comparator.reverseOrder()
                            )
                            .forEach(path -> {
                                try {
                                    Files.deleteIfExists(path);
                                } catch (Exception exception) {
                                    log.warn(
                                            "Failed to delete temporary file: {}",
                                            path
                                    );
                                }
                            });

                } catch (Exception exception) {

                    log.warn(
                            "Failed to clean conversion directory: {}",
                            tempDirectory
                    );
                }
            }
        }
    }

    private void handleTextMessage(
            long chatId,
            String text
    ) {

        var sessionOptional =
                telegramSessionService.get(chatId);

        if (sessionOptional.isEmpty()) {
            sendMessage(
                    chatId,
                    "Send me a PDF document first."
            );

            return;
        }

        TelegramPrintSession session =
                sessionOptional.get();

        if ("CANCEL".equalsIgnoreCase(text)) {

            telegramSessionService.remove(chatId);

            sendMessage(
                    chatId,
                    "Print job cancelled."
            );

            return;
        }

        if ("CONFIRM".equalsIgnoreCase(text)) {

            if (!session.hasPageRange()) {
                sendMessage(
                        chatId,
                        "Select page range first."
                );

                return;
            }

            startPrinting(
                    chatId,
                    session
            );

            return;
        }

        if (session.hasPageRange()) {
            sendMessage(
                    chatId,
                    "Print job is ready.\n\n"
                            + "Send CONFIRM to start printing\n"
                            + "or CANCEL to cancel."
            );

            return;
        }

        try {
            pageRangeService.parse(
                    text,
                    session.getPageCount()
            );

            session.setPageRange(text);

            sendMessage(
                    chatId,
                    "Ready to print.\n\n"
                            + "File: "
                            + session.getOriginalFile().getOriginalFilename()
                            + "\n"
                            + "Pages: "
                            + text
                            + "\n"
                            + "Copies: 1\n\n"
                            + "Send CONFIRM to start printing\n"
                            + "or CANCEL to cancel."
            );

        } catch (IllegalArgumentException exception) {

            sendMessage(
                    chatId,
                    "Invalid page range.\n\n"
                            + exception.getMessage()
            );
        }
    }

    private void startPrinting(
            long chatId,
            TelegramPrintSession session
    ) {

        try {
            sendMessage(
                    chatId,
                    "Processing..."
            );

            var selectedPages =
                    pageRangeService.parse(
                            session.getPageRange(),
                            session.getPageCount()
                    );

            PrintJob job =
                    printJobService.createJob(
                            session.getOriginalFile()
                                    .getOriginalFilename(),
                            session.getOriginalFile()
                                    .getContentType()
                    );

            String originalFilePath =
                    fileStorageService.saveOriginalFile(
                            job.getId(),
                            session.getOriginalFile()
                    );

            job.setOriginalFilePath(
                    originalFilePath
            );

            job.setPageRange(
                    session.getPageRange()
            );

            String sourcePdfPath =
                    fileStorageService.savePrintableSourceFile(
                            job.getId(),
                            session.getPrintableSourceFile()
                    );

            String printableFilePath =
                    pdfService.createPrintablePdf(
                            sourcePdfPath,
                            selectedPages
                    );

            job.setPrintableFilePath(
                    printableFilePath
            );

            printQueueService.enqueue(job);

            telegramSessionService.remove(chatId);

            sendMessage(
                    chatId,
                    "Print job added to queue.\n\n"
                            + "Job ID: " + job.getId()
            );

        } catch (Exception exception) {

            log.error(
                    "Telegram print failed: chatId={}",
                    chatId,
                    exception
            );

            sendMessage(
                    chatId,
                    "Failed to start printing."
            );
        }
    }

    private void sendMessage(
            long chatId,
            String text
    ) {

        SendMessage message =
                SendMessage.builder()
                        .chatId(chatId)
                        .text(text)
                        .build();

        try {
            telegramClient.execute(message);

        } catch (TelegramApiException exception) {

            log.error(
                    "Failed to send Telegram message to chatId={}",
                    chatId,
                    exception
            );
        }
    }
}